#!/bin/bash
# Restore de backup MySQL
# Uso: ./scripts/restore.sh backups/backup_20260421_020000.sql.gz
#
# ATENCIÓN: sobreescribe la base de datos completa.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/../.env.prod"
if [[ -z "${DB_PASSWORD:-}" && -f "${ENV_FILE}" ]]; then
  DB_PASSWORD=$(grep '^DB_PASSWORD=' "${ENV_FILE}" | cut -d'=' -f2-)
fi

if [[ -z "${1:-}" ]]; then
  echo "Uso: $0 <archivo.sql.gz>"
  exit 1
fi

BACKUP_FILE="$1"
if [[ ! -f "${BACKUP_FILE}" ]]; then
  echo "Error: archivo no encontrado: ${BACKUP_FILE}"
  exit 1
fi

CONTAINER="consultorio-odontologico-db-1"
DB_NAME="consultorio_db"

echo "[restore] Restaurando desde ${BACKUP_FILE}..."
gunzip -c "${BACKUP_FILE}" | docker exec -i "${CONTAINER}" \
  mysql -u root -p"${DB_PASSWORD}" "${DB_NAME}"

echo "[restore] Completado."
