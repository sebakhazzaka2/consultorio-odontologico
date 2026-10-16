# DEPLOY — Runbook de producción

## Instancias

Una instancia = un cliente = un droplet con su propio Docker Compose, su `.env.prod`, sus secretos y su bucket de backup. Nada se comparte entre instancias salvo las imágenes de GHCR.

| Instancia (GitHub Environment) | Dominio | Rubro | Estado |
|---|---|---|---|
| `neodental` | `neodentalmaster.turnosuy.com` | Odontología (Montevideo) | Live — droplet de abajo |
| `guille` | `guilleblanchetbeauty.turnosuy.com` | Belleza (Uruguay) | Pendiente de alta — droplet aún no creado (ver "Alta de una instancia nueva") |

> El nombre de la columna 1 es el del GitHub Environment y el valor que va en `DEPLOY_INSTANCES`. `guille` es una sugerencia: se puede elegir otro al darla de alta, mientras sea el mismo en los dos lados.

## Infraestructura (instancia `neodental`)

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

`docker-compose.prod.yml` fija `name: consultorio-odontologico`, así que containers (`consultorio-odontologico-db-1`, …) y volúmenes (`consultorio-odontologico_mysql_data`, …) se llaman igual en todas las instancias, sin importar la carpeta del clone. Es el mismo nombre que Compose derivaba de la carpeta en `neodental`: los volúmenes existentes se reusan, no hay migración.

## Deploy automático (CI)

Cada merge a `main` corre `.github/workflows/ci.yml`: tests → imágenes a GHCR (`:latest` y `:<sha>`) → job `deploy`, **una vez por instancia** (matrix). Cada corrida usa el GitHub Environment de esa instancia:

| Dónde | Nombre | Qué es |
|---|---|---|
| Secret del Environment | `DEPLOY_HOST` | IP del droplet |
| Secret del Environment | `DEPLOY_USER` | Usuario SSH (`sebastian`) |
| Secret del Environment | `DEPLOY_SSH_KEY` | Key privada SSH con acceso a ese droplet |
| Variable del Environment (opcional) | `DEPLOY_PATH` | Ruta del clone; vacío = `/home/sebastian/consultorio-odontologico` |
| Variable del repo | `DEPLOY_INSTANCES` | JSON con los Environments a desplegar, ej. `["neodental","guille"]`. Sin definir = `["neodental"]` |
| Secret del repo | `GHCR_TOKEN` | PAT con `read:packages` (compartido, solo lectura de imágenes) |

En el server el job hace lo de siempre (`docker login`, `git fetch` + `git reset --hard origin/main`, `pull`, `up -d --no-build`, limpieza de imágenes), pero con `IMAGE_TAG=<sha del commit>`: corre la imagen exacta de ese commit, no "la última". Después retaggea esa imagen como `:latest` local, para que un `docker compose up -d` manual sin `IMAGE_TAG` no vuelva a una versión vieja.

- `fail-fast: false`: si un host falla, los demás se despliegan igual. El job rojo es solo el de esa instancia.
- Un deploy a la vez por instancia (`concurrency`): dos merges seguidos no se pisan.
- Si un deploy falla, la instancia **sigue corriendo la versión anterior** (el `up -d` es lo último que cambia algo).

### Rollback por SHA

Las imágenes de cada commit quedan en GHCR con su SHA. Para volver a una versión anterior en una instancia:

```bash
cd /home/sebastian/consultorio-odontologico
IMAGE_TAG=<sha-completo-del-commit-bueno> docker compose --env-file .env.prod -f docker-compose.prod.yml pull backend frontend
IMAGE_TAG=<sha-completo-del-commit-bueno> docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-build
```

- El SHA sale de `git log` o de la pestaña Actions (tiene que ser un commit de `main` que haya pasado el CI; los de PR no se pushean a GHCR).
- Solo cambia las imágenes. El `docker-compose.prod.yml` y los scripts siguen siendo los de `main`. Si el commit malo trajo una migración de Flyway, volver la imagen atrás **no** deshace la migración: evaluar antes (puede requerir restore).
- El próximo merge a `main` vuelve a desplegar lo último. Para que el rollback "dure", revertir el commit en `main` (`git revert` + PR) en vez de dejarlo solo en el server.
- Alternativa desde GitHub: re-correr el job `deploy` de una corrida vieja (Actions → corrida → *Re-run jobs*) usa el SHA de esa corrida, pero ojo: el `git reset --hard origin/main` trae igual el compose/scripts actuales.

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
Lee de `.env.prod` (salvo que ya estén en el entorno): `DB_PASSWORD` y `BACKUP_B2_REMOTE` (bucket off-site de esta instancia). Encuentra el container de la DB con `docker compose -f docker-compose.prod.yml ps -q db`; si no está corriendo, falla con un mensaje claro.

Cada corrida genera tres archivos en `backups/`:
- `backup_<timestamp>.sql.gz` — dump de `consultorio_db`.
- `uploads_<timestamp>.tar.gz` — contenido del volumen `uploads_data` (fotos de branding, archivos clínicos). **No** está en el dump SQL — antes de P3-10 no se backupeaba en absoluto.
- `kuma_<timestamp>.tar.gz` — contenido del volumen `kuma_data` (monitores y notificaciones de Uptime Kuma). Copia en caliente, con Kuma corriendo: alcanza para no reconfigurar todo a mano. Si el volumen no existe, se saltea con un warning.

Retención local: 7 días (`find ... -mtime +7 -delete`).

### Backup off-site (Backblaze B2) — P3-10

Motivo (ver `tasks/lessons.md` § 2026-10-15, incidente Hetzner): un backup que vive en el mismo proveedor que el server se pierde junto con el server. B2 es un proveedor **distinto** a DigitalOcean, con su propia cuenta/billing.

**Setup (una vez por instancia):**
1. Cuenta en Backblaze B2 (email/tarjeta distintos a los de DigitalOcean). Una sola cuenta sirve para todas las instancias.
2. Bucket privado **por instancia**: `consultorio-backups-<cliente>` (el de `neodental` es `consultorio-backups-neodental`).
3. Application Key scopeada solo a ese bucket (read+write) — no la master key, y no reusar la key de otra instancia. Así un server comprometido no puede leer ni borrar los backups de otro cliente.
4. En el droplet:
   ```bash
   curl -fsSL https://rclone.org/install.sh | sudo -E bash
   rclone config
   # n) New remote → name: b2 → type: b2 → account: <keyID> → key: <applicationKey>
   ```
   El `keyID`/`applicationKey` se pegan directo en el prompt de `rclone config` — nunca se guardan en el repo ni se comparten fuera del servidor.
5. En `.env.prod`: `BACKUP_B2_REMOTE=b2:consultorio-backups-<cliente>`.
6. Verificar: `rclone ls b2:consultorio-backups-<cliente>` (vacío la primera vez, sin error) y correr `./scripts/backup.sh` a mano una vez.

Con el remote `b2` configurado, `backup.sh` sube los tres archivos a `BACKUP_B2_REMOTE` después del backup local. Si `rclone` no está instalado o el remote no existe, el script sigue haciendo el backup local pero imprime un warning y **no** sube nada — no falla el cron por esto. Si el remote `b2` existe pero `BACKUP_B2_REMOTE` está vacío (o no empieza con `b2:`), el script **falla** (exit 1) después de dejar el backup local: nunca adivina el bucket, para no subir el backup de un cliente al bucket de otro.

**Retención off-site:** por ahora sin lifecycle rule automática en B2 — se sube todo, sin borrar. Revisar manualmente el tamaño del bucket cada tanto o configurar una lifecycle rule desde la consola de B2 si crece demasiado (pendiente, no bloqueante).

**Restore desde B2** (servidor nuevo o backups locales perdidos). `<remote>` = el `BACKUP_B2_REMOTE` de esa instancia (ej. `b2:consultorio-backups-neodental`):
```bash
rclone copy <remote>/backup_YYYYMMDD_HHMMSS.sql.gz backups/
rclone copy <remote>/uploads_YYYYMMDD_HHMMSS.tar.gz backups/
rclone copy <remote>/kuma_YYYYMMDD_HHMMSS.tar.gz backups/   # solo backups desde 2026-10-16
./scripts/restore.sh --kuma backups/kuma_YYYYMMDD_HHMMSS.tar.gz \
  backups/backup_YYYYMMDD_HHMMSS.sql.gz backups/uploads_YYYYMMDD_HHMMSS.tar.gz
```
`--kuma` es opcional. Con `--kuma`, el script para `uptime-kuma`, pisa el volumen `kuma_data` y lo vuelve a levantar si estaba corriendo (en un server nuevo lo deja creado y lo arranca el `up -d` siguiente). El dump de la DB es obligatorio; uploads y Kuma no.

**Verificado (2026-10-15):** ciclo completo probado contra un contenedor MySQL y volúmenes descartables (nunca contra prod) — backup real subido a B2, descargado de vuelta y restaurado con los mismos comandos que usan `backup.sh`/`restore.sh`, DB y uploads íntegros. Detalle en `tasks/lessons.md`, entrada "VPS de Hetzner dado de baja..." § Verificación end-to-end.

## Reprovisionar el VPS desde cero (disaster recovery)

Usar si el VPS se perdió. `scripts/backup.sh` sube copia off-site a Backblaze B2 (ver sección anterior) — si el VPS se perdió, los backups están en el bucket B2 de esa instancia (`BACKUP_B2_REMOTE`, ej. `b2:consultorio-backups-neodental`), no en el server. Primero descargarlos de ahí (`rclone copy <remote>/... backups/`), nunca asumir que están en el disco local del VPS nuevo.

1. **Crear el VPS** (DigitalOcean, Ubuntu 24.04, ver plan arriba). Usuario no-root con sudo y grupo docker, login root SSH deshabilitado, solo SSH key.
2. **Firewall:** aplicar la tabla de arriba (22 solo desde la IP del admin, 80, 443). Opcional pero recomendado: `ufw` + `fail2ban` en el host.
3. **Instalar Docker** + docker-compose-plugin y clonar el repo en `/home/sebastian/consultorio-odontologico`.
4. **Crear `.env.prod`** a partir de `.env.example`. Variables que consume `docker-compose.prod.yml`:
   `DB_PASSWORD`, `JWT_SECRET` (generar uno **nuevo** con `openssl rand -hex 32`; esto desloguea sesiones viejas), `CORS_ALLOWED_ORIGINS`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `DOMAIN`, `GHCR_IMAGE_PREFIX`, `SENTRY_DSN`, `GOOGLE_PLACES_API_KEY`, `GOOGLE_PLACE_ID`, `BACKUP_B2_REMOTE`, más los datos de la clínica (`CLINIC_*`). Los flags `FEATURE_*` y el theming todavía no existen en el código: llegan en F1 (colores/texto del rubro) y F3 (`FEATURE_*`). Credenciales en Bitwarden.
5. **Descargar el último backup de B2 y restaurar (DB + uploads + Kuma):**
   ```bash
   curl -fsSL https://rclone.org/install.sh | sudo -E bash
   rclone config  # remote "b2", con la key de ESTA instancia (scopeada a su bucket)
   R=b2:consultorio-backups-<cliente>   # = BACKUP_B2_REMOTE
   rclone copy "$R/backup_YYYYMMDD_HHMMSS.sql.gz" backups/
   rclone copy "$R/uploads_YYYYMMDD_HHMMSS.tar.gz" backups/
   rclone copy "$R/kuma_YYYYMMDD_HHMMSS.tar.gz" backups/
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d db
   ./scripts/restore.sh --kuma backups/kuma_YYYYMMDD_HHMMSS.tar.gz \
     backups/backup_YYYYMMDD_HHMMSS.sql.gz backups/uploads_YYYYMMDD_HHMMSS.tar.gz
   ```
6. **Auditar usuarios restaurados** (a raíz del fix de `/auth/register`, PR #26): borrar cualquier cuenta que no se reconozca.
   ```bash
   docker exec -it consultorio-odontologico-db-1 mysql -uroot -p consultorio_db \
     -e "SELECT id, email, role, created_at FROM users;"
   ```
7. **Levantar el resto** (`up -d --build`, o `pull` de las imágenes GHCR si se usa CI/CD) y **apuntar el DNS** (Cloudflare, registro A `@` y `status`) a la IP nueva. Actualizar la IP en esta página y los secrets del Environment de esa instancia (`DEPLOY_HOST`, y `DEPLOY_SSH_KEY` si cambió la key; ver "Deploy automático (CI)").
8. **Verificar:**
   - `curl -I https://<dominio>` muestra `Strict-Transport-Security`, `X-Frame-Options`, `X-Content-Type-Options`.
   - Una reserva de prueba funciona; repetir >10 veces seguidas devuelve `429`.
   - Login admin OK, agenda con datos restaurados.
9. **Restaurar operación:** `scripts/install-cron.sh` (backup diario), verificar monitores y **alertas** en Uptime Kuma (`status.<dominio>`; si se restauró con `--kuma` ya están, si no hay que reconfigurarlos — la notificación es un webhook de Discord, ver `docs/RUNBOOK_S3.md` § 2), confirmar que Sentry recibe eventos (`SENTRY_DSN`).
10. **Archivos subidos (`uploads_data`):** si se restauró con el segundo argumento de `restore.sh` en el paso 5, ya están. Si por algún motivo no hay backup de uploads disponible, hay que volver a subirlas manualmente (ver "Cambiar una foto").

## Pasar de `DO_*` a Environments (una sola vez)

Hasta el PR de deploy multi-host, el job usaba los secrets de repo `DO_HOST`, `DO_USER`, `DO_SSH_KEY`. Ahora lee `DEPLOY_*` del Environment de cada instancia. **Hacerlo antes de mergear ese PR**: si no, el primer deploy después del merge falla por falta de secrets (prod no se cae — sigue corriendo la versión anterior —, pero no se actualiza).

1. GitHub → repo → *Settings → Environments → New environment* → `neodental`.
2. En ese Environment, *Add environment secret* ×3, con los **mismos valores** que los `DO_*` actuales (GitHub no deja leer secrets: copiarlos del gestor de contraseñas / de la key local):
   - `DEPLOY_HOST` = valor de `DO_HOST` (hoy `142.93.198.84`)
   - `DEPLOY_USER` = valor de `DO_USER` (`sebastian`)
   - `DEPLOY_SSH_KEY` = valor de `DO_SSH_KEY` (key privada completa, con las líneas `BEGIN`/`END`)
3. Opcional: variable de Environment `DEPLOY_PATH` = `/home/sebastian/consultorio-odontologico` (es el default; no hace falta).
4. **No** crear `DEPLOY_INSTANCES` todavía (sin definir = `["neodental"]`).
5. En `/home/sebastian/consultorio-odontologico/.env.prod` del droplet, agregar `BACKUP_B2_REMOTE=b2:consultorio-backups-neodental` (si no, el backup de las 02:00 queda solo local y sale con error — ver "Backup off-site").
6. Mergear. Confirmar en Actions que el job `Deploy — neodental` queda verde y que el sitio responde. En el server: `docker ps --format '{{.Image}}'` muestra las imágenes con el SHA del merge.
7. Recién ahí borrar los secrets de repo `DO_HOST`, `DO_USER`, `DO_SSH_KEY`.

Con `gh` CLI (desde una máquina con el repo, logueado como admin):

```bash
gh api -X PUT repos/{owner}/{repo}/environments/neodental   # crea el Environment
gh secret set DEPLOY_HOST --env neodental                  # pide el valor por stdin
gh secret set DEPLOY_USER --env neodental
gh secret set DEPLOY_SSH_KEY --env neodental < ~/.ssh/<key-de-deploy>
gh variable set DEPLOY_PATH --env neodental --body /home/sebastian/consultorio-odontologico   # opcional
# ... mergear y verificar el deploy verde ...
gh secret delete DO_HOST && gh secret delete DO_USER && gh secret delete DO_SSH_KEY
```

## Alta de una instancia nueva

Ejemplo: Guille Blanchet Beauty → Environment `guille`, dominio `guilleblanchetbeauty.turnosuy.com`. Regla: **todo secreto es nuevo y propio de la instancia**; nada se copia de otro `.env.prod`.

1. **Droplet:** DigitalOcean, Basic 2 GB, Ubuntu 24.04. Usuario `sebastian` no-root con sudo y grupo docker, login root deshabilitado, solo SSH key. Cloud Firewall con la tabla de "Firewall" (22 solo desde la IP del admin, 80, 443). Docker + docker-compose-plugin.
2. **DNS** (Cloudflare, DNS only): registros A `guilleblanchetbeauty` y `status.guilleblanchetbeauty` → IP del droplet nuevo.
3. **Deploy key:** en el droplet `ssh-keygen -t ed25519 -f ~/.ssh/id_ed25519_github`, agregar la pública como Deploy key (solo lectura) del repo y `~/.ssh/config` con `Host github.com` → esa key (ver "Repositorio").
4. **Clone:** `git clone git@github.com:<owner>/consultorio-odontologico.git /home/sebastian/consultorio-odontologico`. Otra ruta funciona, pero entonces hay que setear `DEPLOY_PATH` en el Environment.
5. **`.env.prod`** desde `.env.example`, con valores nuevos:
   - `JWT_SECRET=$(openssl rand -hex 32)`, `DB_PASSWORD=$(openssl rand -hex 24)`, `ADMIN_EMAIL` / `ADMIN_PASSWORD` propios (password larga, generada).
   - `DOMAIN=guilleblanchetbeauty.turnosuy.com`, `CORS_ALLOWED_ORIGINS=https://guilleblanchetbeauty.turnosuy.com`, `GHCR_IMAGE_PREFIX`, `CLINIC_*` de la clienta, `SENTRY_DSN` (puede ser el mismo proyecto de Sentry con otro `environment`, o vacío).
   - Guardar todo en Bitwarden, en una entrada por instancia.
6. **Backup:** bucket B2 propio `consultorio-backups-guille` + Application Key limitada a ese bucket, `rclone config` (remote `b2`) en el droplet y `BACKUP_B2_REMOTE=b2:consultorio-backups-guille` en `.env.prod` (ver "Backup off-site"). Después `./scripts/install-cron.sh`, correr `./scripts/backup.sh` a mano una vez y confirmar los tres archivos con `rclone ls`.
7. **Primer arranque** (a mano, antes de sumarla al CI): `echo <PAT> | docker login ghcr.io -u <usuario> --password-stdin`, después `docker compose --env-file .env.prod -f docker-compose.prod.yml pull backend frontend` y `up -d --no-build`.
8. **GitHub Environment** `guille` con `DEPLOY_HOST` (IP nueva), `DEPLOY_USER` (`sebastian`), `DEPLOY_SSH_KEY` (una key de CI **distinta** de la de neodental, con la pública en `~/.ssh/authorized_keys` del droplet nuevo) y, si la ruta no es la default, `DEPLOY_PATH`. Después la variable de repo:
   ```bash
   gh api -X PUT repos/{owner}/{repo}/environments/guille
   gh secret set DEPLOY_HOST --env guille
   gh secret set DEPLOY_USER --env guille
   gh secret set DEPLOY_SSH_KEY --env guille < ~/.ssh/<key-ci-guille>
   gh variable set DEPLOY_INSTANCES --body '["neodental","guille"]'
   ```
   Desde el próximo merge a `main`, el deploy llega a las dos. Ojo con el firewall: la tabla de arriba limita el 22 a la IP del admin, pero los runners de GitHub Actions también entran por SSH. Antes del primer deploy por CI, copiar en el droplet nuevo exactamente la regla de SSH que hoy deja entrar al CI en `neodental` (DigitalOcean → Networking → Firewalls); si no, el job de esa instancia falla por timeout de SSH.
9. **Uptime Kuma** en `https://status.guilleblanchetbeauty.turnosuy.com`: crear el usuario admin (password propia), monitores del sitio y de `/api/public/config`, y notificación a un webhook de Discord (canal propio o el mismo con nombre de instancia). La URL del webhook es un secreto: gestor de contraseñas, nunca el repo. Ver `docs/RUNBOOK_S3.md` § 2.
10. **Smoke test:** `curl -I https://guilleblanchetbeauty.turnosuy.com` con headers de seguridad, login admin, alta de un servicio y horario, una reserva de prueba desde la página pública.
11. **Checklist de aislamiento** (no pasa nada de un cliente a otro):
    - [ ] `curl -s https://guilleblanchetbeauty.turnosuy.com/api/public/config` no tiene ningún dato de neodental (nombre, teléfono, WhatsApp, dirección, email, imágenes).
    - [ ] `<title>` y `og:*` del HTML (`curl -s https://guilleblanchetbeauty.turnosuy.com | grep -E '<title>|og:'`) son los de la clienta.
    - [ ] La vista previa del link en WhatsApp muestra nombre e imagen de la clienta, no de neodental.
    - [ ] Las credenciales de admin de neodental **fallan** en la instancia nueva, y al revés.
    - [ ] Un JWT de una instancia no sirve en la otra (sale solo si los `JWT_SECRET` son distintos).
    - [ ] `rclone ls` en cada droplet solo ve su propio bucket (la key de uno no lista el bucket del otro).

## Pendientes antes de ir a producción

- [x] Caddy como reverse proxy con TLS automático
- [x] CI/CD — push imágenes a GHCR + workflow de deploy SSH
- [x] Cron de backup configurado en el server
- [x] Validación fail-fast de variables de entorno en el backend
- [] Smoke test completo antes de apuntar DNS definitivo
