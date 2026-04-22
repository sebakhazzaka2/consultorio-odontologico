# DEPLOY.MD — Runbook de producción

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

## Backup de base de datos

Script: `scripts/backup.sh`  
Requiere `DB_PASSWORD` en el entorno.

```bash
# Configurar cronjob (pendiente)
crontab -e
# Agregar: 0 2 * * * DB_PASSWORD=xxx /home/sebastian/consultorio-odontologico/scripts/backup.sh
```

Los backups se guardan en `/backups/` con retención de 7 días.

## Pendientes antes de ir a producción

- [x] Caddy como reverse proxy con TLS automático
- [x] CI/CD — push imágenes a GHCR + workflow de deploy SSH
- [x] Cron de backup configurado en el server
- [x] Validación fail-fast de variables de entorno en el backend
- [] Smoke test completo antes de apuntar DNS definitivo
