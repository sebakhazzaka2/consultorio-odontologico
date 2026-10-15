# DEPLOY — Runbook de producción

## Infraestructura

| Ítem | Valor |
|------|-------|
| Proveedor | DigitalOcean (migrado 2026-10-15, Hetzner anterior dado de baja por impago — ver `tasks/lessons.md`) |
| Plan | Basic Droplet (1 vCPU, 2GB RAM, 50GB SSD, $12/mes) |
| OS | Ubuntu 24.04 LTS |
| IP pública | 142.93.198.84 |
| Dominio | turnosuy.com |
| DNS | Cloudflare (A record @ → 142.93.198.84, DNS only) — **pendiente apuntar, ver checklist de migración** |

## Acceso SSH

```bash
ssh -i ~/.ssh/id_ed25519_consultorio sebastian@142.93.198.84
```

- Usuario no-root con sudo (passwordless, solo accesible ya autenticado por key) y grupo docker
- Login root por SSH deshabilitado (`PermitRootLogin no`)
- Autenticación por SSH key únicamente (`PasswordAuthentication no`)

## Firewall (DigitalOcean Cloud Firewall + UFW en el host)

| Dirección | Protocolo | Puerto | Fuente |
|-----------|-----------|--------|--------|
| Entrada | TCP | 22 | IP del admin (solo) |
| Entrada | TCP | 80 | Any |
| Entrada | TCP | 443 | Any |
| Salida | TCP/UDP | Any | Any |

> ⚠️ **La regla de SSH (puerto 22) está atada a una IP fija**, no "cualquiera". Si te conectás desde otra red (otra PC, 4G, otro wifi) vas a quedar bloqueado hasta actualizar la IP de origen en **DigitalOcean → Networking → Firewalls → (el firewall del droplet) → regla SSH**. Hacelo ANTES de cambiar de red si podés anticiparlo (ej. antes de ir a otra ubicación), agregando también la IP de la otra PC/red como fuente adicional.

## Stack en producción

- **Docker** 29.1.3 + **docker-compose-plugin** v5.1.3
- **Contenedores:** db (MySQL 8.0), backend (Spring Boot), frontend (Nginx)
- **Reverse proxy HTTPS:** Caddy (pendiente)

## Repositorio

```bash
# Ubicación en el server
/home/sebastian/consultorio-odontologico

# Clonar (si hay que reinstalar) — vía deploy key SSH, repo es privado
# El owner real en GitHub es sebakhazzaka2 (no sebitas71133, renombrado en algún momento)
git clone git@github.com:sebakhazzaka2/consultorio-odontologico.git
```

**Deploy key:** cada server nuevo necesita su propia SSH key agregada como "Deploy key" (solo lectura) en
`https://github.com/sebakhazzaka2/consultorio-odontologico/settings/keys`, y un `~/.ssh/config` en el server
apuntando `Host github.com` a esa key (ver clave del droplet actual en el propio server, `~/.ssh/id_ed25519_github`).

## Variables de entorno

Archivo: `/home/sebastian/consultorio-odontologico/.env.prod`  
**NO commitear este archivo.** Guardá las credenciales en Bitwarden.

```env
DB_PASSWORD=<password segura>
JWT_SECRET=<string random 64 chars>
CORS_ALLOWED_ORIGINS=https://turnosuy.com
```

Para generar valores seguros:
```bash
openssl rand -hex 32
```

## Comandos de operación

```bash
cd /home/sebastian/consultorio-odontologico

# Levantar (primera vez o tras cambios)
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build

# Ver estado
docker ps

# Ver logs de un servicio
docker logs consultorio-odontologico-backend-1 --tail 50

# Bajar
docker compose -f docker-compose.prod.yml down

# Bajar y borrar volúmenes (DESTRUCTIVO — borra la DB)
docker compose -f docker-compose.prod.yml down -v
```

## Branding / fotos de la clínica

Las imágenes de la página pública (carrusel del hero y foto de ubicación) **no son assets del frontend**: son URLs que el backend lee de `.env.prod` y expone en `GET /api/public/config`.

| Qué | Variable en `.env.prod` |
|-----|--------------------------|
| Carrusel del hero | `CLINIC_HERO_IMAGENES[0]`, `[1]`, … (cualquier cantidad) |
| Foto de ubicación | `CLINIC_FOTO_UBICACION_URL` |

Las imágenes están **auto-hosteadas** en el volumen `uploads_data` del backend, bajo `/app/uploads/branding/`, y se sirven públicas en `https://<dominio>/api/uploads/branding/<archivo>` (gracias a `GET /uploads/**` permitAll en `SecurityConfig`).

### Cambiar una foto

1. Optimizá la imagen antes de subir (≤ ~1280px, JPEG q82, ~150 KB). El backend **no** la optimiza al subir.
2. Copiala al volumen del backend:
   ```bash
   scp foto.jpg sebastian@<host>:/tmp/
   docker cp /tmp/foto.jpg consultorio-odontologico-backend-1:/app/uploads/branding/
   ```
3. Si **cambiás el nombre del archivo** (recomendado por la caché): actualizá la URL en `.env.prod` y recreá el backend:
   ```bash
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --force-recreate backend
   ```
   Si reusás el mismo nombre, no hace falta tocar `.env.prod` ni recrear — pero ver caché abajo.

> ⚠️ Los archivos clínicos de pacientes van por **otro** storage, nunca en `branding/`.

## Caché de imágenes y assets

Caddy cachea en el navegador (ver `Caddyfile`), porque Spring Security sirve todo con `no-store`:
- `/api/uploads/*` → `Cache-Control: public, max-age=2592000` (30 días).
- Assets del frontend con hash (`*.js`, `*.css`, fuentes, imágenes) → `max-age=1 año, immutable`.
- El JSON de la API (`/api/public/config`, etc.) **NO** se cachea (sigue `no-store`).

**Cache-busting = cambiar el nombre del archivo.** Si reemplazás una imagen con el mismo nombre, los navegadores que ya la cachearon verán la vieja hasta 30 días. Para forzar el cambio, usá un nombre nuevo (ej. `hero1-v2.jpg`) y actualizá la URL en `.env.prod`.

## Backup de base de datos y uploads

Script: `scripts/backup.sh` (instalado como cron diario vía `scripts/install-cron.sh`).  
Requiere `DB_PASSWORD` en el entorno (se lee de `.env.prod` si no está seteada).

Cada corrida genera dos archivos en `backups/`:
- `backup_<timestamp>.sql.gz` — dump de `consultorio_db`.
- `uploads_<timestamp>.tar.gz` — contenido del volumen `uploads_data` (fotos de branding, archivos clínicos). **No** está en el dump SQL — antes de P3-10 no se backupeaba en absoluto.

Retención local: 7 días (`find ... -mtime +7 -delete`).

### Backup off-site (Backblaze B2) — P3-10

Motivo (ver `tasks/lessons.md` § 2026-10-15, incidente Hetzner): un backup que vive en el mismo proveedor que el server se pierde junto con el server. B2 es un proveedor **distinto** a DigitalOcean, con su propia cuenta/billing.

**Setup (una sola vez):**
1. Cuenta en Backblaze B2 (email/tarjeta distintos a los de DigitalOcean).
2. Bucket privado, ej. `consultorio-backups-neodental`.
3. Application Key scopeada solo a ese bucket (read+write) — no la master key.
4. En el droplet:
   ```bash
   curl -fsSL https://rclone.org/install.sh | sudo -E bash
   rclone config
   # n) New remote → name: b2 → type: b2 → account: <keyID> → key: <applicationKey>
   ```
   El `keyID`/`applicationKey` se pegan directo en el prompt de `rclone config` — nunca se guardan en el repo ni se comparten fuera del servidor.
5. Verificar: `rclone ls b2:consultorio-backups-neodental` (vacío la primera vez, sin error).

Con el remote `b2` configurado, `backup.sh` sube automáticamente ambos archivos después del backup local. Si `rclone` no está instalado o el remote no existe, el script sigue haciendo el backup local pero imprime un warning y **no** sube nada — no falla el cron por esto.

**Retención off-site:** por ahora sin lifecycle rule automática en B2 — se sube todo, sin borrar. Revisar manualmente el tamaño del bucket cada tanto o configurar una lifecycle rule desde la consola de B2 si crece demasiado (pendiente, no bloqueante).

**Restore desde B2** (servidor nuevo o backups locales perdidos):
```bash
rclone copy b2:consultorio-backups-neodental/backup_YYYYMMDD_HHMMSS.sql.gz backups/
rclone copy b2:consultorio-backups-neodental/uploads_YYYYMMDD_HHMMSS.tar.gz backups/
./scripts/restore.sh backups/backup_YYYYMMDD_HHMMSS.sql.gz backups/uploads_YYYYMMDD_HHMMSS.tar.gz
```

**Verificado (2026-10-15):** ciclo completo probado contra un contenedor MySQL y volúmenes descartables (nunca contra prod) — backup real subido a B2, descargado de vuelta y restaurado con los mismos comandos que usan `backup.sh`/`restore.sh`, DB y uploads íntegros. Detalle en `tasks/lessons.md`, entrada "VPS de Hetzner dado de baja..." § Verificación end-to-end.

## Reprovisionar el VPS desde cero (disaster recovery)

Usar si el VPS se perdió. `scripts/backup.sh` sube copia off-site a Backblaze B2 (ver sección anterior) — si el VPS se perdió, los backups están en el bucket B2, no en el server. Primero descargarlos de ahí (`rclone copy b2:consultorio-backups-neodental/... backups/`), nunca asumir que están en el disco local del VPS nuevo.

1. **Crear el VPS** (Hetzner, Ubuntu 22.04, ver plan arriba). Usuario no-root con sudo y grupo docker, login root SSH deshabilitado, solo SSH key.
2. **Firewall:** aplicar la tabla de arriba (22 solo desde la IP del admin, 80, 443). Opcional pero recomendado: `ufw` + `fail2ban` en el host.
3. **Instalar Docker** + docker-compose-plugin y clonar el repo en `/home/sebastian/consultorio-odontologico`.
4. **Crear `.env.prod`** a partir de `.env.example`. Variables que consume `docker-compose.prod.yml`:
   `DB_PASSWORD`, `JWT_SECRET` (generar uno **nuevo** con `openssl rand -hex 32`; esto desloguea sesiones viejas), `CORS_ALLOWED_ORIGINS`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `DOMAIN`, `GHCR_IMAGE_PREFIX`, `SENTRY_DSN`, `GOOGLE_PLACES_API_KEY`, `GOOGLE_PLACE_ID`, más las de branding (`CLINIC_*`, `BRAND_*`, `FEATURE_*`). Credenciales en Bitwarden.
5. **Descargar el último backup de B2 y restaurar (DB + uploads):**
   ```bash
   curl -fsSL https://rclone.org/install.sh | sudo -E bash
   rclone config  # remote "b2", mismo keyID/applicationKey de siempre
   rclone copy b2:consultorio-backups-neodental/backup_YYYYMMDD_HHMMSS.sql.gz backups/
   rclone copy b2:consultorio-backups-neodental/uploads_YYYYMMDD_HHMMSS.tar.gz backups/
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d db
   ./scripts/restore.sh backups/backup_YYYYMMDD_HHMMSS.sql.gz backups/uploads_YYYYMMDD_HHMMSS.tar.gz
   ```
6. **Auditar usuarios restaurados** (a raíz del fix de `/auth/register`, PR #26): borrar cualquier cuenta que no se reconozca.
   ```bash
   docker exec -it consultorio-odontologico-db-1 mysql -uroot -p consultorio_db \
     -e "SELECT id, email, role, created_at FROM users;"
   ```
7. **Levantar el resto** (`up -d --build`, o `pull` de las imágenes GHCR si se usa CI/CD) y **apuntar el DNS** (Cloudflare, registro A `@` y `status`) a la IP nueva. Actualizar la IP en esta página y los secrets del workflow de deploy (host/SSH key).
8. **Verificar:**
   - `curl -I https://<dominio>` muestra `Strict-Transport-Security`, `X-Frame-Options`, `X-Content-Type-Options`.
   - Una reserva de prueba funciona; repetir >10 veces seguidas devuelve `429`.
   - Login admin OK, agenda con datos restaurados.
9. **Restaurar operación:** `scripts/install-cron.sh` (backup diario), reconfigurar monitores y **alertas** en Uptime Kuma (`status.<dominio>`), confirmar que Sentry recibe eventos (`SENTRY_DSN`).
10. **Archivos subidos (`uploads_data`):** si se restauró con el segundo argumento de `restore.sh` en el paso 5, ya están. Si por algún motivo no hay backup de uploads disponible, hay que volver a subirlas manualmente (ver "Cambiar una foto").

## Pendientes antes de ir a producción

- [x] Caddy como reverse proxy con TLS automático
- [x] CI/CD — push imágenes a GHCR + workflow de deploy SSH
- [x] Cron de backup configurado en el server
- [x] Validación fail-fast de variables de entorno en el backend
- [] Smoke test completo antes de apuntar DNS definitivo
