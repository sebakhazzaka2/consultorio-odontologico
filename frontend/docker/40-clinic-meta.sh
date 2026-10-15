#!/bin/sh
# Sustituye <title>, description y og:* del index.html con CLINIC_NOMBRE / CLINIC_TAGLINE
# al arrancar el contenedor. Los scrapers (WhatsApp, etc.) no ejecutan JS, así que el
# setTitle de app.component.ts no les llega: el HTML servido tiene que traer los datos.
# Siempre parte del template original, así que reiniciar el contenedor es seguro.
set -eu

TEMPLATE=/usr/share/nginx/index.html.template
TARGET=/usr/share/nginx/html/index.html

html_escape() {
  printf '%s' "$1" | tr '\r\n' '  ' | sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' \
    -e 's/>/\&gt;/g' -e 's/"/\&quot;/g' -e "s/'/\&#39;/g"
}

nombre="${CLINIC_NOMBRE:-}"
tagline="${CLINIC_TAGLINE:-}"

# Nunca caer en datos de otro cliente: sin nombre, texto neutro
if [ -z "$nombre" ]; then
  echo "$0: CLINIC_NOMBRE vacío, uso texto neutro en index.html" >&2
  nombre="Turnos online"
fi
if [ -n "$tagline" ]; then
  title="$nombre — $tagline"
  description="$tagline"
else
  title="$nombre"
  description="Reservá tu turno online."
fi

APP_TITLE="$(html_escape "$title")"
APP_OG_TITLE="$(html_escape "$nombre")"
APP_DESCRIPTION="$(html_escape "$description")"
export APP_TITLE APP_OG_TITLE APP_DESCRIPTION

# Lista explícita: cualquier otro "$" del HTML (CSS inlineado, etc.) queda intacto
envsubst '${APP_TITLE} ${APP_OG_TITLE} ${APP_DESCRIPTION}' < "$TEMPLATE" > "$TARGET.tmp"
mv "$TARGET.tmp" "$TARGET"
