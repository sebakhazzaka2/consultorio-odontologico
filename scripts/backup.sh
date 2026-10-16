#!/bin/bash
# Backup automático de MySQL + uploads + Uptime Kuma — ejecutar como cronjob en el host.
# Cron: 0 2 * * * /path/to/scripts/backup.sh
#
# Lee de ../.env.prod (o del entorno, si ya están seteadas):
#   DB_PASSWORD       — password de root MySQL
#   BACKUP_B2_REMOTE  — destino off-site de ESTA instancia, ej. b2:consultorio-backups-neodental
#
# Requiere rclone configurado con un remote "b2" con una key limitada al
# bucket de esta instancia (ver docs/DEPLOY.md § Backup off-site). Si rclone
# no está instalado o el remote "b2" no existe, el backup local sigue
# funcionando igual — solo se salta la subida off-site (con warning).
# Si el remote "b2" existe pero BACKUP_B2_REMOTE está vacío, el script falla
# (exit 1) después del backup local: nunca adivina el bucket, para no subir
# el backup de un cliente al bucket de otro.
#
# Volumen del host montado en el container db como /backups
# (configurado en docker-compose.prod.yml). uploads_data y kuma_data son
# volúmenes Docker nombrados (no bind mount), así que se empaquetan desde
# un container temporal.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${REPO_DIR}/.env.prod"
if [[ -z "${DB_PASSWORD:-}" && -f "${ENV_FILE}" ]]; then
  DB_PASSWORD=$(grep '^DB_PASSWORD=' "${ENV_FILE}" | cut -d'=' -f2-)
fi
if [[ -z "${BACKUP_B2_REMOTE:-}" && -f "${ENV_FILE}" ]]; then
  BACKUP_B2_REMOTE=$(grep '^BACKUP_B2_REMOTE=' "${ENV_FILE}" | cut -d'=' -f2- || true)
fi
BACKUP_B2_REMOTE="${BACKUP_B2_REMOTE:-}"

COMPOSE=(docker compose -f "${REPO_DIR}/docker-compose.prod.yml")
if [[ -f "${ENV_FILE}" ]]; then
  COMPOSE+=(--env-file "${ENV_FILE}")
fi

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${REPO_DIR}/backups"
mkdir -p "${BACKUP_DIR}"
# Mismo valor que `name:` en docker-compose.prod.yml.
PROJECT_NAME="consultorio-odontologico"
DB_NAME="consultorio_db"
UPLOADS_VOLUME="${PROJECT_NAME}_uploads_data"
KUMA_VOLUME="${PROJECT_NAME}_kuma_data"
RETENTION_DAYS=7

CONTAINER=$("${COMPOSE[@]}" ps -q db)
if [[ -z "${CONTAINER}" ]]; then
  echo "[backup] ERROR: el container de la DB no está corriendo (docker compose -f docker-compose.prod.yml ps db)." >&2
  exit 1
fi

echo "[backup] Iniciando backup ${TIMESTAMP}..."

# --- Dump de la base de datos ---
DB_FILE="${BACKUP_DIR}/backup_${TIMESTAMP}.sql.gz"
docker exec "${CONTAINER}" \
  mysqldump -u root -p"${DB_PASSWORD}" "${DB_NAME}" \
  | gzip > "${DB_FILE}"
echo "[backup] DB: ${DB_FILE}"

# --- Archivo del volumen uploads_data (fotos, archivos clínicos) ---
UPLOADS_FILE="${BACKUP_DIR}/uploads_${TIMESTAMP}.tar.gz"
docker run --rm \
  -v "${UPLOADS_VOLUME}:/uploads:ro" \
  -v "${BACKUP_DIR}:/backups" \
  alpine:3.19 \
  tar czf "/backups/uploads_${TIMESTAMP}.tar.gz" -C /uploads .
echo "[backup] Uploads: ${UPLOADS_FILE}"

# --- Archivo del volumen kuma_data (monitores y notificaciones de Uptime Kuma) ---
# Copia en caliente (Kuma sigue corriendo): alcanza para no reconfigurar todo a
# mano. Si el volumen no existe se saltea — docker run lo crearía vacío.
KUMA_FILE=""
if docker volume inspect "${KUMA_VOLUME}" >/dev/null 2>&1; then
  KUMA_FILE="${BACKUP_DIR}/kuma_${TIMESTAMP}.tar.gz"
  docker run --rm \
    -v "${KUMA_VOLUME}:/kuma:ro" \
    -v "${BACKUP_DIR}:/backups" \
    alpine:3.19 \
    tar czf "/backups/kuma_${TIMESTAMP}.tar.gz" -C /kuma .
  echo "[backup] Kuma: ${KUMA_FILE}"
else
  echo "[backup] ADVERTENCIA: no existe el volumen ${KUMA_VOLUME} — se saltea el backup de Kuma." >&2
fi

# --- Retención local ---
find "${BACKUP_DIR}" -name "backup_*.sql.gz" -mtime "+${RETENTION_DAYS}" -delete
find "${BACKUP_DIR}" -name "uploads_*.tar.gz" -mtime "+${RETENTION_DAYS}" -delete
find "${BACKUP_DIR}" -name "kuma_*.tar.gz" -mtime "+${RETENTION_DAYS}" -delete
echo "[backup] Backups locales anteriores a ${RETENTION_DAYS} días eliminados."

# --- Subida off-site a Backblaze B2 ---
if command -v rclone >/dev/null 2>&1 && rclone listremotes | grep -qF "b2:"; then
  if [[ -z "${BACKUP_B2_REMOTE}" ]]; then
    echo "[backup] ERROR: rclone tiene un remote 'b2' pero BACKUP_B2_REMOTE está vacío en ${ENV_FILE}." >&2
    echo "[backup] Agregá BACKUP_B2_REMOTE=b2:<bucket-de-esta-instancia> — no se sube nada para no mezclar clientes." >&2
    echo "[backup] El backup local quedó en ${BACKUP_DIR}." >&2
    exit 1
  fi
  if [[ "${BACKUP_B2_REMOTE}" != b2:?* ]]; then
    echo "[backup] ERROR: BACKUP_B2_REMOTE debe tener la forma b2:<bucket> (vale '${BACKUP_B2_REMOTE}')." >&2
    exit 1
  fi
  rclone copy "${DB_FILE}" "${BACKUP_B2_REMOTE}/" --quiet
  rclone copy "${UPLOADS_FILE}" "${BACKUP_B2_REMOTE}/" --quiet
  if [[ -n "${KUMA_FILE}" ]]; then
    rclone copy "${KUMA_FILE}" "${BACKUP_B2_REMOTE}/" --quiet
  fi
  echo "[backup] Subido a ${BACKUP_B2_REMOTE}/"
else
  echo "[backup] ADVERTENCIA: rclone sin configurar (remote 'b2' no encontrado) — backup solo local, sin copia off-site." >&2
fi

echo "[backup] Completado."
