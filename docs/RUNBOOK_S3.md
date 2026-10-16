# Runbook S3 — Cierre de hardening

**Estado: ✅ COMPLETADO (2026-10-15)** — la VPS está en pie en DigitalOcean, el restore se verificó y las alertas de Kuma llegan a Discord.

| Item | Estado |
|------|--------|
| P0-1 Restore probado fuera de producción | ✅ Contenedor descartable, DB + uploads (PR #37–#38, 2026-10-15). Cronometrarlo en VM limpia es opcional |
| P0-2 Alertas de Uptime Kuma confirmadas | ✅ Llegan avisos a un canal de Discord (confirmado por el dueño, 2026-10-15) |

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

**Estado actual (2026-10-15):** la instancia `neodentalmaster` ya tiene la notificación configurada y los avisos llegan a un **canal de Discord**. Los pasos de abajo sirven para replicarlo en una instancia nueva (p. ej. la de Guille en F1). Kuma permite elegir *Discord* como tipo de notificación (basta la URL del webhook del canal) y es la opción más simple de las tres. La URL del webhook es un secreto: guardarla en el gestor de contraseñas, no en el repo. Los datos de Kuma viven en `kuma_data`, que `backup.sh` empaqueta como `kuma_<timestamp>.tar.gz` desde 2026-10-16 (se restaura con `restore.sh --kuma`, ver `docs/DEPLOY.md` § Backup off-site). Sin ese archivo hay que reconfigurar monitores y notificación a mano (paso 9 del disaster recovery).

1. **Entrar a `https://status.<dominio>`.** Si es la primera vez, crear el usuario admin.
2. **Configurar canal de notificación** en *Settings → Notifications → Setup Notification*:
   - **Discord (el que usa `neodentalmaster`):** en el canal, *Editar canal → Integraciones → Webhooks → Nuevo webhook*; copiar la URL a Kuma.
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
