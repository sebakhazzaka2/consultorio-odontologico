# Runbook S3 — Cierre de hardening

**Estado: ⏳ PENDIENTE** — bloqueado porque la VPS está caída (2026-09-24). Ejecutar cuando vuelva.

| Item | Estado |
|------|--------|
| P0-1 Restore probado en VM limpia | ⬜ Pendiente |
| P0-2 Alertas de Uptime Kuma confirmadas | ⬜ Pendiente |

---

## 1. Restore en VM limpia (P0-1)

`restore.sh` solo restaura sobre un container `consultorio-odontologico-db-1` ya corriendo, y la base `consultorio_db` tiene que existir. En una VM limpia, levantar el stack primero.

1. **Traer un backup real de producción.** En el host de producción queda en `backups/backup_*.sql.gz`.
   ```bash
   scp usuario@host-prod:/ruta/consultorio-odontologico/backups/backup_YYYYMMDD_020000.sql.gz .
   ```
2. **Crear la VM limpia.** Multipass/VirtualBox con Ubuntu, o un Hetzner chico descartable. Instalar Docker y Compose:
   ```bash
   curl -fsSL https://get.docker.com | sh
   ```
3. **Clonar el repo y armar `.env.prod`.** Copiar `.env.example` y completar `DB_PASSWORD`, `DOMAIN`, etc. El `DOMAIN` puede ser ficticio: se prueba solo la base.
4. **Levantar solo la base.**
   ```bash
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d db
   docker ps   # el nombre debe ser consultorio-odontologico-db-1
   ```
   Si el nombre difiere (depende de la carpeta), ajustar `CONTAINER` en `restore.sh` o clonar en una carpeta llamada `consultorio-odontologico`.
5. **Crear la base si no existe** (el dump no usa `--databases`, no incluye `CREATE DATABASE`):
   ```bash
   docker exec -it consultorio-odontologico-db-1 mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS consultorio_db"
   ```
6. **Restaurar.**
   ```bash
   ./scripts/restore.sh backup_YYYYMMDD_020000.sql.gz
   ```
7. **Verificar datos.**
   ```bash
   docker exec -it consultorio-odontologico-db-1 mysql -uroot -p consultorio_db \
     -e "SHOW TABLES; SELECT COUNT(*) FROM paciente; SELECT COUNT(*) FROM cita;"
   ```
   Comparar los conteos con producción.
8. **Levantar el stack completo y smoke test.**
   ```bash
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
   ```
   Entrar al login y revisar que aparezcan pacientes y citas.
9. **Anotar tiempo total y problemas encontrados** en `docs/DEPLOY.md`. Destruir la VM.

## 2. Alertas de Uptime Kuma (P0-2)

Kuma corre en `status.{DOMAIN}`, con datos persistidos en el volumen `kuma_data`.

1. **Entrar a `https://status.<dominio>`.** Si es la primera vez, crear el usuario admin.
2. **Configurar canal de notificación** en *Settings → Notifications → Setup Notification*:
   - **Telegram:** bot con @BotFather, copiar token y chat ID.
   - **Email (SMTP):** sirve Brevo, que se usará igual en S6.
   - Marcar **"Default enabled"** para que aplique a todos los monitores.
   - Apretar **Test** y confirmar que llega el mensaje.
3. **Revisar monitores existentes**, al menos:
   - `https://neodentalmaster.turnosuy.com` (HTTP, web).
   - `https://neodentalmaster.turnosuy.com/api/public/config` (HTTP, backend).
   - Intervalo 60s, *Retries* 2–3 para evitar falsos positivos.
4. **Asignar la notificación a cada monitor** (editar → *Notifications*).
5. **Probar que alerta de verdad.** En el host de producción:
   ```bash
   docker compose -f docker-compose.prod.yml stop backend
   ```
   Esperar 2–3 minutos hasta recibir el mensaje DOWN. Luego `start backend` y confirmar el mensaje UP.
6. **Opcional:** monitor de expiración de certificado TLS y un monitor *Push* que `backup.sh` pinguee al terminar.

## Al terminar

- Marcar P0-1 y P0-2 como ✅ en `CLAUDE.md` (sección *Operaciones mínimas*) y en `ROADMAP.md`.
- Actualizar el estado de este runbook a ✅ COMPLETADO con la fecha.
