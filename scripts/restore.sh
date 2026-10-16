#!/bin/bash
# Restore de backup MySQL + uploads (+ Uptime Kuma, opcional)
# Uso: ./scripts/restore.sh [--kuma backups/kuma_20260421_020000.tar.gz] \
#        backups/backup_20260421_020000.sql.gz [backups/uploads_20260421_020000.tar.gz]
#
# Si los backups no están en el servidor (ej. reprovisioning desde cero tras
# perder el VPS), primero descargarlos del bucket B2 de ESTA instancia
# (BACKUP_B2_REMOTE en .env.prod, ej. b2:consultorio-backups-neodental):
#   rclone copy "$BACKUP_B2_REMOTE/backup_YYYYMMDD_HHMMSS.sql.gz" backups/
#   rclone copy "$BACKUP_B2_REMOTE/uploads_YYYYMMDD_HHMMSS.tar.gz" backups/
#   rclone copy "$BACKUP_B2_REMOTE/kuma_YYYYMMDD_HHMMSS.tar.gz" backups/
#
# ATENCIÓN: sobreescribe la base de datos completa y, si se pasan, también el
# contenido de los volúmenes uploads_data y kuma_data. Para Kuma el script
# para el container uptime-kuma antes de restaurar y lo vuelve a levantar si
# estaba corriendo.

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

usage() {
  echo "Uso: $0 [--kuma kuma.tar.gz] <backup.sql.gz> [uploads.tar.gz]"
  if [[ -n "${BACKUP_B2_REMOTE}" ]]; then
    echo "Backups off-site de esta instancia: rclone ls ${BACKUP_B2_REMOTE}"
  fi
}

KUMA_FILE=""
if [[ "${1:-}" == "--kuma" ]]; then
  if [[ -z "${2:-}" ]]; then
    usage
    exit 1
  fi
  KUMA_FILE="$2"
  shift 2
fi

if [[ -z "${1:-}" ]]; then
  usage
  exit 1
fi

BACKUP_FILE="$1"
UPLOADS_FILE="${2:-}"
for f in "${BACKUP_FILE}" "${UPLOADS_FILE}" "${KUMA_FILE}"; do
  if [[ -n "${f}" && ! -f "${f}" ]]; then
    echo "Error: archivo no encontrado: ${f}"
    exit 1
  fi
done

# Mismo valor que `name:` en docker-compose.prod.yml.
PROJECT_NAME="consultorio-odontologico"
DB_NAME="consultorio_db"
UPLOADS_VOLUME="${PROJECT_NAME}_uploads_data"
KUMA_VOLUME="${PROJECT_NAME}_kuma_data"

CONTAINER=$("${COMPOSE[@]}" ps -q db)
if [[ -z "${CONTAINER}" ]]; then
  echo "[restore] ERROR: el container de la DB no está corriendo. Levantalo primero:" >&2
  echo "[restore]   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d db" >&2
  exit 1
fi

echo "[restore] Restaurando DB desde ${BACKUP_FILE}..."
gunzip -c "${BACKUP_FILE}" | docker exec -i "${CONTAINER}" \
  mysql -u root -p"${DB_PASSWORD}" "${DB_NAME}"
echo "[restore] DB restaurada."

if [[ -n "${UPLOADS_FILE}" ]]; then
  echo "[restore] Restaurando uploads desde ${UPLOADS_FILE}..."
  docker run --rm \
    -v "${UPLOADS_VOLUME}:/uploads" \
    -v "$(cd "$(dirname "${UPLOADS_FILE}")" && pwd):/backups" \
    alpine:3.19 \
    sh -c "rm -rf /uploads/* && tar xzf /backups/$(basename "${UPLOADS_FILE}") -C /uploads"
  echo "[restore] Uploads restaurados."
fi

if [[ -n "${KUMA_FILE}" ]]; then
  echo "[restore] Restaurando Uptime Kuma desde ${KUMA_FILE}..."
  KUMA_WAS_RUNNING=$("${COMPOSE[@]}" ps -q uptime-kuma)
  # Kuma tiene su SQLite abierta: hay que pararlo antes de pisar el volumen.
  "${COMPOSE[@]}" stop uptime-kuma
  # Crea container y volumen (con las labels de Compose) si todavía no existen,
  # sin arrancarlo — ej. en un servidor nuevo donde solo se levantó la DB.
  "${COMPOSE[@]}" up --no-start uptime-kuma
  docker run --rm \
    -v "${KUMA_VOLUME}:/kuma" \
    -v "$(cd "$(dirname "${KUMA_FILE}")" && pwd):/backups" \
    alpine:3.19 \
    sh -c "rm -rf /kuma/* && tar xzf /backups/$(basename "${KUMA_FILE}") -C /kuma"
  if [[ -n "${KUMA_WAS_RUNNING}" ]]; then
    "${COMPOSE[@]}" start uptime-kuma
  fi
  echo "[restore] Kuma restaurado."
fi

echo "[restore] Completado."
