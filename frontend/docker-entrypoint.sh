#!/bin/sh
set -e

cat > /usr/share/nginx/html/assets/config/clinic.json << EOF
{
  "name": "${CLINIC_NAME:-Consultorio}",
  "tagline": "${CLINIC_TAGLINE:-}",
  "address": "${CLINIC_ADDRESS:-}",
  "phone": "${CLINIC_PHONE:-}",
  "whatsapp": "${CLINIC_WHATSAPP:-}",
  "email": "${CLINIC_EMAIL:-}",
  "hours": "${CLINIC_HOURS:-}"
}
EOF

exec nginx -g "daemon off;"
