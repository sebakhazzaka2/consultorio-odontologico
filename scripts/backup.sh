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

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups"
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
