# DEPLOY — Runbook de producción

## Infraestructura

| Ítem | Valor |
|------|-------|
| Proveedor | Hetzner Cloud |
| Plan | CX-23 (2 vCPU, 4GB RAM, 40GB SSD) |
| OS | Ubuntu 22.04 LTS |
| IP pública | 167.235.134.150 |
| Dominio | turnouy.com |
| DNS | Cloudflare (A record @ → 167.235.134.150, DNS only) |

## Acceso SSH

```bash
ssh sebastian@167.235.134.150
```

- Usuario no-root con sudo y grupo docker
- Login root por SSH deshabilitado (`PermitRootLogin no`)
- Autenticación por SSH key únicamente

## Firewall (Hetzner Cloud)

| Dirección | Protocolo | Puerto | Fuente |
|-----------|-----------|--------|--------|
| Entrada | TCP | 22 | IP del admin (solo) |
| Entrada | TCP | 80 | Any |
| Entrada | TCP | 443 | Any |
| Salida | TCP | Any | Any |
| Salida | UDP | Any | Any |

## Stack en producción

- **Docker** 29.1.3 + **docker-compose-plugin** v5.1.3
- **Contenedores:** db (MySQL 8.0), backend (Spring Boot), frontend (Nginx)
- **Reverse proxy HTTPS:** Caddy (pendiente)

## Repositorio

```bash
# Ubicación en el server
/home/sebastian/consultorio-odontologico

# Clonar (si hay que reinstalar)
git clone https://github.com/sebitas71133/consultorio-odontologico.git
```

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

## Backup de base de datos

Script: `scripts/backup.sh`  
Requiere `DB_PASSWORD` en el entorno.

```bash
# Configurar cronjob (pendiente)
crontab -e
# Agregar: 0 2 * * * DB_PASSWORD=xxx /home/sebastian/consultorio-odontologico/scripts/backup.sh
```

Los backups se guardan en `/backups/` con retención de 7 días.

## Reprovisionar el VPS desde cero (disaster recovery)

Usar si el VPS se perdió. Dado que `scripts/backup.sh` guarda los dumps en `backups/` **del mismo servidor**, un VPS perdido puede llevarse los backups con él: antes de empezar, confirmar dónde hay una copia (descarga manual, snapshot de Hetzner, etc.). Backup off-site automático = P3-10 del roadmap.

1. **Crear el VPS** (Hetzner, Ubuntu 22.04, ver plan arriba). Usuario no-root con sudo y grupo docker, login root SSH deshabilitado, solo SSH key.
2. **Firewall:** aplicar la tabla de arriba (22 solo desde la IP del admin, 80, 443). Opcional pero recomendado: `ufw` + `fail2ban` en el host.
3. **Instalar Docker** + docker-compose-plugin y clonar el repo en `/home/sebastian/consultorio-odontologico`.
4. **Crear `.env.prod`** a partir de `.env.example`. Variables que consume `docker-compose.prod.yml`:
   `DB_PASSWORD`, `JWT_SECRET` (generar uno **nuevo** con `openssl rand -hex 32`; esto desloguea sesiones viejas), `CORS_ALLOWED_ORIGINS`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `DOMAIN`, `GHCR_IMAGE_PREFIX`, `SENTRY_DSN`, `GOOGLE_PLACES_API_KEY`, `GOOGLE_PLACE_ID`, más las de branding (`CLINIC_*`, `BRAND_*`, `FEATURE_*`). Credenciales en Bitwarden.
5. **Levantar solo la DB y restaurar el backup:**
   ```bash
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d db
   ./scripts/restore.sh backups/backup_YYYYMMDD_HHMMSS.sql.gz
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
10. **Archivos subidos (`uploads_data`):** las fotos de branding y futuros archivos clínicos viven en el volumen del backend, **no** en el dump SQL. Si no hay copia, hay que volver a subirlas (ver "Cambiar una foto").

## Pendientes antes de ir a producción

- [x] Caddy como reverse proxy con TLS automático
- [x] CI/CD — push imágenes a GHCR + workflow de deploy SSH
- [x] Cron de backup configurado en el server
- [x] Validación fail-fast de variables de entorno en el backend
- [] Smoke test completo antes de apuntar DNS definitivo
