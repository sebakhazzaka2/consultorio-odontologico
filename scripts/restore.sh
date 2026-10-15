#!/bin/bash
# Restore de backup MySQL + uploads
# Uso: ./scripts/restore.sh backups/backup_20260421_020000.sql.gz [backups/uploads_20260421_020000.tar.gz]
#
# Si los backups no están en el servidor (ej. reprovisioning desde cero tras
# perder el VPS), primero descargarlos desde Backblaze B2:
#   rclone copy b2:consultorio-backups-neodental/backup_YYYYMMDD_HHMMSS.sql.gz backups/
#   rclone copy b2:consultorio-backups-neodental/uploads_YYYYMMDD_HHMMSS.tar.gz backups/
#
# ATENCIÓN: sobreescribe la base de datos completa y, si se pasa el segundo
# argumento, también el contenido del volumen uploads_data.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/../.env.prod"
if [[ -z "${DB_PASSWORD:-}" && -f "${ENV_FILE}" ]]; then
  DB_PASSWORD=$(grep '^DB_PASSWORD=' "${ENV_FILE}" | cut -d'=' -f2-)
fi

if [[ -z "${1:-}" ]]; then
  echo "Uso: $0 <backup.sql.gz> [uploads.tar.gz]"
  exit 1
fi

BACKUP_FILE="$1"
UPLOADS_FILE="${2:-}"
if [[ ! -f "${BACKUP_FILE}" ]]; then
  echo "Error: archivo no encontrado: ${BACKUP_FILE}"
  exit 1
fi

CONTAINER="consultorio-odontologico-db-1"
DB_NAME="consultorio_db"
UPLOADS_VOLUME="consultorio-odontologico_uploads_data"

echo "[restore] Restaurando DB desde ${BACKUP_FILE}..."
gunzip -c "${BACKUP_FILE}" | docker exec -i "${CONTAINER}" \
  mysql -u root -p"${DB_PASSWORD}" "${DB_NAME}"
echo "[restore] DB restaurada."

if [[ -n "${UPLOADS_FILE}" ]]; then
  if [[ ! -f "${UPLOADS_FILE}" ]]; then
    echo "Error: archivo no encontrado: ${UPLOADS_FILE}"
    exit 1
  fi
  echo "[restore] Restaurando uploads desde ${UPLOADS_FILE}..."
  docker run --rm \
    -v "${UPLOADS_VOLUME}:/uploads" \
    -v "$(cd "$(dirname "${UPLOADS_FILE}")" && pwd):/backups" \
    alpine:3.19 \
    sh -c "rm -rf /uploads/* && tar xzf /backups/$(basename "${UPLOADS_FILE}") -C /uploads"
  echo "[restore] Uploads restaurados."
fi

echo "[restore] Completado."
