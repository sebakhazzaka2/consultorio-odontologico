#!/bin/bash
# Instala el cronjob de backup en el servidor Hetzner.
# Ejecutar una sola vez después del deploy inicial.
# Requiere: docker, acceso al .env.prod en el directorio raíz del proyecto.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_SCRIPT="${SCRIPT_DIR}/backup.sh"
LOG_FILE="/var/log/consultorio-backup.log"
CRON_ENTRY="0 2 * * * ${BACKUP_SCRIPT} >> ${LOG_FILE} 2>&1"

chmod +x "${BACKUP_SCRIPT}"

if crontab -l 2>/dev/null | grep -qF "${BACKUP_SCRIPT}"; then
  echo "[cron] El cronjob ya está instalado."
  exit 0
fi

(crontab -l 2>/dev/null; echo "${CRON_ENTRY}") | crontab -
echo "[cron] Cronjob instalado: ${CRON_ENTRY}"
echo "[cron] Logs en: ${LOG_FILE}"
