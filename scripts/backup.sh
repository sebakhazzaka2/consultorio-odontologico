#!/bin/bash
# Backup automático de MySQL — ejecutar como cronjob en el host Hetzner
# Cron: 0 2 * * * /path/to/scripts/backup.sh
#
# Requiere en el entorno:
#   DB_PASSWORD  — password de root MySQL (misma que en .env.prod)
#
# Volumen del host montado en el container db como /backups
# (configurado en docker-compose.prod.yml)

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
RETENTION_DAYS=7

echo "[backup] Iniciando backup ${TIMESTAMP}..."

# Ejecutar mysqldump dentro del container db y comprimir con gzip
docker exec "${CONTAINER}" \
  mysqldump -u root -p"${DB_PASSWORD}" "${DB_NAME}" \
  | gzip > "${BACKUP_DIR}/backup_${TIMESTAMP}.sql.gz"

echo "[backup] Archivo: ${BACKUP_DIR}/backup_${TIMESTAMP}.sql.gz"

# Eliminar backups con más de RETENTION_DAYS días
find "${BACKUP_DIR}" -name "*.sql.gz" -mtime "+${RETENTION_DAYS}" -delete

echo "[backup] Backups anteriores a ${RETENTION_DAYS} días eliminados."
echo "[backup] Completado."
