#!/bin/bash
# Backup automático de MySQL + uploads — ejecutar como cronjob en el host.
# Cron: 0 2 * * * /path/to/scripts/backup.sh
#
# Requiere en el entorno:
#   DB_PASSWORD  — password de root MySQL (misma que en .env.prod)
#
# Requiere rclone configurado con un remote "b2" apuntando al bucket de
# Backblaze B2 (ver docs/DEPLOY.md § Backup off-site). Si rclone no está
# instalado o el remote "b2" no existe, el backup local sigue funcionando
# igual — solo se salta la subida off-site (con warning).
#
# Volumen del host montado en el container db como /backups
# (configurado en docker-compose.prod.yml). uploads_data es un volumen
# Docker nombrado (no bind mount), así que se empaqueta desde un
# container temporal.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/../.env.prod"
if [[ -z "${DB_PASSWORD:-}" && -f "${ENV_FILE}" ]]; then
  DB_PASSWORD=$(grep '^DB_PASSWORD=' "${ENV_FILE}" | cut -d'=' -f2-)
fi

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${SCRIPT_DIR}/../backups"
mkdir -p "${BACKUP_DIR}"
CONTAINER="consultorio-odontologico-db-1"
DB_NAME="consultorio_db"
UPLOADS_VOLUME="consultorio-odontologico_uploads_data"
RETENTION_DAYS=7
B2_REMOTE="b2:consultorio-backups-neodental"

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

# --- Subida off-site a Backblaze B2 ---
if command -v rclone >/dev/null 2>&1 && rclone listremotes | grep -qF "b2:"; then
  rclone copy "${DB_FILE}" "${B2_REMOTE}/" --quiet
  rclone copy "${UPLOADS_FILE}" "${B2_REMOTE}/" --quiet
  echo "[backup] Subido a ${B2_REMOTE}/"
else
  echo "[backup] ADVERTENCIA: rclone sin configurar (remote 'b2' no encontrado) — backup solo local, sin copia off-site." >&2
fi

# --- Retención local ---
find "${BACKUP_DIR}" -name "backup_*.sql.gz" -mtime "+${RETENTION_DAYS}" -delete
find "${BACKUP_DIR}" -name "uploads_*.tar.gz" -mtime "+${RETENTION_DAYS}" -delete
echo "[backup] Backups locales anteriores a ${RETENTION_DAYS} días eliminados."

echo "[backup] Completado."
