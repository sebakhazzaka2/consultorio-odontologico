# Consultorio Odontológico

## Qué es este producto
Sistema de gestión de turnos para profesionales independientes de la salud y la belleza (hoy odontología; próximo cliente: estética). Incluye agenda admin, reserva pública sin login, pacientes/clientes, catálogo de servicios, historial de procedimientos con precio y pagos con saldo calculado.
Se vende como SaaS B2B en tres paquetes (Local / Web / Web + WhatsApp) con **una instancia Docker por cliente** (sin multitenancy todavía). Marca: TurnosUy (`turnosuy.com`). Un cliente live (`neodentalmaster.turnosuy.com`) más una demo en Render.
Documentación fina del estado real y de los riesgos: `docs/ESTADO.md`.

## Stack y cómo correrlo
- **Backend:** Spring Boot 3.2.5, Java 17 — ver `backend/CLAUDE.md`
- **Frontend:** Angular 19 + Angular Material — ver `frontend/CLAUDE.md`
- **Base de datos:** MySQL (`consultorio_db`, `localhost:3306`)
- **Backend:** puerto 8080, context path `/api`

Comandos (verificados contra `README.md`, `ci.yml`, `package.json` y `scripts/`):

| Qué | Comando |
|-----|---------|
| Crear DB local | `CREATE DATABASE IF NOT EXISTS consultorio_db;` (Flyway migra al arrancar) |
| Backend (dev) | `cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local` — requiere `application-local.properties` (gitignored) con `spring.datasource.password` y `jwt.secret` |
| Tests backend | `cd backend && mvn verify -B` (lo que corre CI; H2 en memoria, no necesita MySQL) |
| Maven con JDK 17 en Windows | `.\scripts\mvn17.ps1 test` (fija `JAVA_HOME` solo para ese proceso) |
| Frontend (dev) | `cd frontend && npm install && npm start` → `http://localhost:4200` |
| Tests frontend | `cd frontend && npm test` (Karma + ChromeHeadless; **CI no lo corre**) |
| Stack prod local | `docker compose --env-file .env.prod -f docker-compose.prod.yml up -d` (variables en `.env.example`) |
| Backup / restore | `scripts/backup.sh`, `scripts/restore.sh` (ver `docs/DEPLOY.md`, `docs/RUNBOOK_S3.md`) |

CI/CD (`.github/workflows/ci.yml`): `mvn verify` → build/push de imágenes a GHCR (tag = SHA del commit) → deploy por SSH a cada instancia (GitHub Environments, matriz `instance`) en cada push a `main`; ver `docs/DEPLOY.md`. Un merge a `main` **despliega a producción**.

## Arquitectura
- **Monolito modular + SPA.** Backend en capas `controller → service → repository → model`, DTOs separados de entidades, mapping manual en el service (`com.consultorio.*`). Frontend Angular standalone: `core/` (auth, config, interceptors), `features/{admin,public,auth}`, `shared/`.
- **Flujo:** navegador → Caddy (TLS, headers) → nginx (SPA, rate limit, proxy `/api`) → Spring Boot (:8080; Actuator :8081 interno) → MySQL (sin puertos publicados). Uploads en volumen Docker `uploads_data`, servidos en `/api/uploads/**`.
- **Auth:** JWT stateless (BCrypt), un solo rol `ADMIN` sin autorización por rol. Público: `POST /auth/login`, `GET /public/**`, `POST /public/reservas`, `GET /uploads/**`; todo lo demás exige JWT. Token en `localStorage` del frontend.
- **Dominio:** `Paciente`, `Cita` (PENDIENTE/CONFIRMADA/CANCELADA/COMPLETADA), `Servicio`, `HistorialProcedimientos` (snapshot de `precio_aplicado`), `Pago` (saldo = Σ historial − Σ pagos, calculado), `DisponibilidadSemanal` (UNIQUE por día → una agenda por instancia), `FechaBloqueada`, `User`. Migraciones Flyway `V1`–`V9`.
- **Integraciones:** Google Places (reseñas públicas, clave solo en backend), Sentry (backend + frontend), Uptime Kuma, Backblaze B2 (backups vía rclone). No hay email, WhatsApp ni pasarela de pago.
- **Multi-tenant: no existe.** Ninguna tabla tiene `tenant_id`; el aislamiento es una base y un contenedor por cliente. La marca vive en env vars (`ClinicProperties`, `GET /api/public/config`). Detalle y brecha en `docs/ESTADO.md`.

## Estructura del repositorio
```
consultorio-odontologico/
├── backend/          # Spring Boot — convenciones en backend/CLAUDE.md
├── frontend/         # Angular 19 — convenciones en frontend/CLAUDE.md
├── scripts/          # backup.sh, utilidades de deploy
├── CLAUDE.md         # este archivo — overview del proyecto
├── ROADMAP.md        # fases, estado y secuencia
└── WORKFLOWS.md      # orquestación, task management, principios
```

## Navegación rápida
| Qué necesitás | Dónde ir |
|--------------|----------|
| Convenciones Spring Boot, DB, diseño | `backend/CLAUDE.md` |
| Convenciones Angular, componentes, services | `frontend/CLAUDE.md` |
| Roadmap, fases, próximos pasos | `ROADMAP.md` |
| Cómo trabajar, subagents, task management | `WORKFLOWS.md` |
| Lecciones aprendidas, errores comunes | `tasks/lessons.md` |

---

## Convenciones
Resumen; las reglas completas y obligatorias están en `backend/CLAUDE.md` y `frontend/CLAUDE.md`.
- **Backend:** constructor injection (nunca `@Autowired` en campo); DTOs con `@JsonNaming(SnakeCaseStrategy)`; entidades sin lógica; `@Valid` en controllers; `Instant` para auditoría y `LocalDateTime` para fechas de negocio; SQL en snake_case; cambios de esquema solo con migración Flyway nueva.
- **Frontend:** standalone components, constructor injection (no `inject()`), tipos explícitos (sin `any`), solo tokens del design system (`var(--color-primary)`, nunca colores fijos), fechas `dd/MM/yyyy`.
- **Errores:** `GlobalExceptionHandler` mapea excepciones a 400/401/404/409/422/500 con `ErrorResponse`; mensajes de usuario en español. El frontend centraliza errores HTTP en `http-error.interceptor.ts`.
- **Idioma:** documentación y UI en español; commits y PRs en inglés (conventional commits, sin atribución a Claude), squash and merge.
- **Config:** nada específico de un cliente en el código; todo por env vars. Cada feature nueva trae su flag en `ClinicProperties` desde el día uno (regla de la sección Feature flags).
- **Secretos:** nunca en el repo; `.env.prod` vive solo en el servidor.

<!-- BEGIN COMMON -->
## Contrato común del ecosistema (v1 · 2026-10-16)

Fuente de verdad: proyecto "AI SaaS Ecosystem" (claude.ai), documento CONTRATO-COMUN.md. No editar este bloque dentro de un repo: se cambia en la fuente y se copia idéntico a TurnosUy y FrontPet.

### 1. Contexto
- Dos productos SaaS comercialmente independientes que comparten criterios y, cuando convenga, componentes:
  - TurnosUy: repo `consultorio-odontologico`, neodentalmaster.turnosuy.com, clínicas odontológicas (Uruguay).
  - FrontPet: repo `frontpet`, frontpet.com.br, pet shops (Brasil).
- Objetivo: ingresos recurrentes con agentes comerciales de IA (consultas, disponibilidad real, turnos o pedidos, derivación a una persona), sin implementaciones manuales por cliente.
- Prioridad: validar con clientes reales antes de construir plataforma. No construir portal central, suscripciones ni multitenancy completa sin demanda validada.

### 2. Convención de evidencia
- HECHO: verificado en el código (citar archivo). HIPÓTESIS: inferido o no verificable desde el repo. RECOMENDACIÓN: propuesta sin decidir. DECIDIDO / PENDIENTE: estado de una decisión.
- Nunca presentar una hipótesis como hecho. La documentación puede ir por delante del código: comprobar antes de afirmar.

### 3. Reglas de trabajo
- Cambios pequeños, con pruebas automatizadas y rollback documentado.
- No ejecutar comandos destructivos, migraciones ni despliegues sin aprobación explícita.
- No modificar producción ni acceder a sistemas ajenos sin autorización.
- No commitear secretos, tokens ni datos de clientes. No leer `.env` reales.
- Revisar licencia y seguridad de cualquier dependencia o código externo antes de incorporarlo.
- No introducir varias herramientas nuevas a la vez; cada una debe justificar un caso de uso concreto.

### 4. Seguridad
- Un agente nunca tiene acceso directo a la base de datos: solo a herramientas acotadas que llaman a APIs autorizadas.
- Credenciales, datos y conversaciones separados por cliente.
- Operaciones sensibles requieren confirmación humana.
- Verificar el resultado de cada operación comercial antes de comunicarlo al usuario final.

### 5. Decisiones tomadas (DECIDIDO)
- Los productos siguen siendo independientes comercialmente.
- No se decide de antemano microservicios ni base compartida: se compara con evidencia de los repos.
- El agente se valida primero en un producto y después se reutiliza en el otro.
- Fase actual: auditoría y diseño; no se implementan funcionalidades del agente todavía.

### 6. Decisiones abiertas (PENDIENTE: no asumir)
- Multitenancy en una instancia vs. una instancia por cliente.
- Núcleo de agente común (librería o servicio) vs. implementación por producto.
- Producto en el que se valida primero el agente.
- Canal WhatsApp (API oficial o proveedor), proveedor de LLM y política de datos personales hacia el LLM (LGPD en Brasil, Ley 18.331 en Uruguay).
- Vocabulario de dominio común (Negocio, Cliente, Recurso/Profesional, Servicio, Turno y sus estados).

### 7. Requisitos candidatos para el agente (RECOMENDACIÓN, validar con clientes)
- Credencial de máquina por negocio con scopes mínimos (leer servicios, leer disponibilidad, crear turno); el negocio se resuelve desde la credencial, no desde configuración global.
- Los turnos que crea el agente nacen en estado pendiente y los confirma una persona.
- Cancelar o reprogramar solo con identidad verificada del cliente (código o enlace por turno), nunca con credencial de admin.
- Idempotencia en reservas, límites por credencial, por teléfono y por negocio, y validación de fechas pasadas y horizonte máximo.
- Registro de auditoría con origen de la acción (humano, web, agente).
- Contrato OpenAPI y errores semánticos para que el agente interprete resultados.
- Aviso al negocio cuando llega un turno pendiente.

### 8. Brechas comunes verificadas (auditoría 2026-10-15; revisar tras cada hito)
- Ambos son single-tenant en ejecución (TurnosUy sin `tenant_id`; FrontPet con `tenant_id` en el esquema pero tenant fijo por configuración).
- Ninguno tiene autorización por rol, autenticación de máquina, cancelación segura para el cliente final, ni contrato OpenAPI.
- Ninguno envía recordatorios ni mensajes de WhatsApp desde el backend (solo enlace `wa.me`), y no hay jobs programados de notificación.
- Marca, textos, moneda y rubro están hardcodeados en frontend y modelo; sin flags de módulos por negocio.
- Backups: restauración verificada en TurnosUy (restore desde B2 probado el 2026-10-15); no verificada en FrontPet.

### 9. Diferencias a respetar (no unificar a ciegas)
- Stacks distintos: TurnosUy = Java 17, Spring Boot 3.2, Angular 19, MySQL; FrontPet = Java 21, Spring Boot 3.5, Next 16, PostgreSQL. No se asume stack común.
- Despliegue: TurnosUy = una instancia Docker Compose por cliente en DigitalOcean; FrontPet = destino Hetzner + Coolify.
- Sesión: TurnosUy = JWT en localStorage; FrontPet = JWT en cookie HttpOnly.
- Modelo: TurnosUy tiene Paciente, historial y pagos; FrontPet no tiene entidad cliente y agrega catálogo y pedidos.
- Estados de turno: TurnosUy PENDIENTE/CONFIRMADA/CANCELADA/COMPLETADA; FrontPet PENDING/CONFIRMED/CANCELLED.
<!-- END COMMON -->

---

## Git

### Convenciones
- Conventional commits: `feat:`, `fix:`, `refactor:`, `chore:`
- Un commit por módulo funcional completo y verificado
- PRs siempre con **Squash and Merge**

### Ramas activas
- `main` — rama estable

### Ramas mergeadas (referencia)
- `v2-angular-frontend`, `p1-hardening-backend`, `feat/agenda-calendar`, `feat/ux-essentials`, `feat/production-deploy`
- `feat/multi-rubro` (PR #21, 2026-05-11), `fix/safari-white-screen` (2026-05-11), `fix/admin-branding` (2026-05-11)
- `feat/s3-hardening-urgente` (PR #26), fixes de reserva concurrente y PII (PR #32–#35, 2026-10-15)

## Modelo de negocio

| Paquete | Qué incluye | Cobro |
|---------|-------------|-------|
| **Local** | Sistema local, sin página web, usuario único | Pago único |
| **Web** | Sistema + página pública + login paciente | Instalación + cuota mensual |
| **Web + WhatsApp** | Todo lo anterior + recordatorios automáticos WhatsApp | Instalación + cuota mensual mayor |

**Arquitectura de deploy:** una instancia Docker por cliente (no multitenancy todavía).
Trigger para migrar a multitenancy real: ~5 clientes activos pagando.

**Onboarding por rubro/cliente:** checklist único en `docs/ONBOARDING_CLIENTE.md` (entregable de F3 junto a los presets por rubro). Consolida datos de clínica, colores y logo, feature flags (`FEATURE_*`), servicios seed, DNS, backup y smoke test. Hasta que exista, la info vive fragmentada entre este archivo, `ClinicProperties` y `docs/DEPLOY.md`.

### Feature flags por paquete

> **Estado (2026-10-15): los `FEATURE_*` todavía no existen en el código.** `ClinicProperties.Feature` son tarjetas de marketing de la página pública, no flags. Se implementan en F3 (ver `ROADMAP.md`). Lo de abajo es el diseño objetivo.

Un solo codebase, una sola imagen Docker. Cada paquete es una combinación de env vars. El backend expondrá los flags en `GET /api/public/config` vía `ClinicProperties`; el frontend los consumirá al arrancar y ocultará rutas/botones según corresponda.

```
# Paquete Local
FEATURE_PUBLIC_PAGE=false
FEATURE_PATIENT_PORTAL=false
FEATURE_WHATSAPP=false

# Paquete Web
FEATURE_PUBLIC_PAGE=true
FEATURE_PATIENT_PORTAL=true
FEATURE_WHATSAPP=false

# Paquete Web + WhatsApp
FEATURE_PUBLIC_PAGE=true
FEATURE_PATIENT_PORTAL=true
FEATURE_WHATSAPP=true
```

**Regla:** al implementar cualquier feature nueva, agregar su flag en `ClinicProperties` desde el día uno. No hay ramas ni repos separados por paquete.

---

## Estado actual (2026-10-15)

Completado: MVP1, P1 hardening, P2 infra, P3 Fase A/B, página pública, admin polish, deploy real (live 2026-04-21), post-deploy hardening (PR #19), public page polish (PR #20), Sprint 1 multi-rubro (PR #21), Sprint 2 Disponibilidad + Reserva sin login (2026-05-20), fix uploads caching (PR #25), S3 hardening (PR #26: register cerrado, headers Caddy, `X-Real-IP` no falsificable, rate limit nginx, honeypot), migración Hetzner → DigitalOcean (PR #28–#29), demo en Render (2026-08-19), reserva concurrente con `FOR UPDATE` como primera sentencia + PII fuera de los logs de reserva (PR #32–#35).

**Instancias en producción:**
- `neodentalmaster.turnosuy.com` — única instancia live (odontología, Montevideo), en DigitalOcean (verificado 2026-10-15: sitio responde, Uptime Kuma activo). Es el mismo cliente que antes figuraba como Dental Montecaseros (cambió el nombre, paga).
- Demo pública en Render (datos fake).
- **Próxima:** Guille Blanchet Beauty (belleza, Uruguay) en un VPS nuevo con subdominio de turnosuy.com. Se levanta en F1 pero ella sigue en Ágora hasta tener recordatorios por email (F2a).

**F0 cerrada (2026-10-15):** restore y backup B2 (#37–#38), login 401 (#40), sin defaults de cliente + validación de `CLINIC_*` (#41), `show-sql` apagado y alertas Kuma→Discord (#42), `<title>`/`og:*` por instancia (#43). Prod verificado: corre el último `main`. Pendientes menores en `ROADMAP.md` § F0.

Ver la secuencia completa, los hechos verificados en el código y las preguntas abiertas en `ROADMAP.md`.

**Resumen para el proyecto de planificación (2026-10-16):** producto funcional en producción con agenda, reserva pública, pacientes, servicios, pagos y dashboard; 33 `@Test` en backend (10 clases, incluidos tests contra MySQL real con Testcontainers) y 26 `it` de frontend en 6 specs. Sin recordatorios, sin flags `FEATURE_*`, sin roles ni multitenancy. Próximo paso: F1 (instancia de Guille + importador CSV + `V10`). Evidencia, dudas y deuda priorizada en `docs/ESTADO.md`.
**Doc sync 2026-10-16:** se agregó `docs/ESTADO.md`, las secciones de producto/arquitectura/convenciones y el bloque COMMON v1. Tras sincronizar con `main` (PR #47–#49: texto del rubro por configuración, test de reserva concurrente contra MySQL real, deploy multi-host con imagen por SHA y backups por instancia) se actualizaron CI y conteo de tests. **Desvío deliberado del COMMON:** la línea de backups en el § 8 se editó para TurnosUy (restore B2 verificado el 2026-10-15, ver `tasks/lessons.md`); replicar el cambio en la fuente CONTRATO-COMUN.md para que vuelvan a ser idénticos.

## Operaciones mínimas

Checklist operacional antes de seguir agregando features sobre instancias live:

- ✅ Restore de backup probado en VM limpia (P0-1) — contenedor descartable, 2026-10-15
- ✅ Uptime monitor con alertas (P0-2) — Uptime Kuma en `status.<dominio>`, avisos a un canal de Discord (confirmado 2026-10-15)
- ✅ Sentry capturando errores backend + frontend (P0-3)
- ✅ Headers de seguridad HTTPS en Caddy (P0-4, PR #26). CSP pendiente (S9)
- ⬜ Audit log de acciones admin (P1-6, S9)
- ⬜ Logs JSON estructurados + correlationId (P1-8, S9)
- ✅ Backup off-site a Backblaze B2 (P3-10) — mergeado y verificado end-to-end (PR #37, 2026-10-15): backup real subido a B2 y restore probado en contenedor descartable (DB + uploads)

## Prioridades actuales (2026-10-15)

Secuencia por fases (detalle, horas y criterios de salida en `ROADMAP.md`). Capacidad: 15 a 20 h por semana.

✅ **F0 — Higiene** cerrada (2026-10-15).
🔴 **F1 — Instancia de Guille en paralelo + importación CSV** (42–63 h). **← PRÓXIMO** VPS nuevo, deploy multi-host, texto del rubro y colores por configuración, `V10` (email nullable + único), importador con simulación. Guille sigue en Ágora.
🔴 **F2a — Recordatorios por email** (20–28 h). `NotificationService` por canal, Brevo + Thymeleaf, cron idempotente con zona horaria por instancia, SPF/DKIM/DMARC.
🔴 **Corte de Guille** (3–5 h). Importar sus clientas y salir en vivo.
🟡 **F2b — WhatsApp** (23–33 h). Twilio, plantillas Meta, opt-in, E.164. Antes: P3-4 costo por mensaje.
🟡 **F3 — Flags y presets** (20–30 h). `FEATURE_*` reales, moneda y locale por configuración, presets por rubro, `ONBOARDING_CLIENTE.md`.
⏸️ **F3b — pt-BR** (30–45 h) — solo si firma un cliente de Brasil.
⏸️ **F4 — Fisioterapia como complemento de Vedius** — condicionada a la reunión. El timer PWA (12–16 h) puede adelantarse con un piloto pago. Profesional no se construye mientras Vedius tenga la agenda.
🟡 **F5 — Agente** (95–135 h) tras F2.
🟡 **F6 — Adquisición y embudo en turnosuy.com** (35–55 h, 6a–6d). Sitio de ventas mínimo con eventos del embudo, puntos de contacto (WhatsApp, email de bienvenida, chat solo en el sitio de ventas), onboarding asistido, cobro. Tras el corte de Guille. Anuncios pagos solo con el gate 6f cumplido. Analytics de terceros **solo** en `turnosuy.com`, nunca en el admin ni en la reserva pública.
🟢 Backlog posterior: agenda polish, theming completo, S5, S7, S8, S9, S11–S14.

**Postergados explícitamente:**
- **Fase Premium UX** (diseñador externo + delight: command palette, shared transitions, ilustraciones propias, etc.) — post 1+ cliente WhatsApp pagando 2+ meses sin churn. Antes de invertir en landing SaaS porque la landing tiene que mostrar este nivel. Detalle en `ROADMAP.md` § Fase Premium UX.
- Landing comercial **premium** `consultorio-landing` — post Fase Premium UX, con caso de éxito real. La versión **mínima** con medición entra antes, en F6a.
- Multi-tenancy real — post 5 clientes activos.

**Aprendizaje devops (proyecto + paralelo):**
- En este proyecto: Prometheus/Grafana (S13), Terraform (cliente #2+).
- Paralelo: Kubernetes (k3s + helm + CKA) — no aplicar a este proyecto.

Todo el resto (prosa de snapshot, rutas de archivos clave, preguntas abiertas, tabla 4 futuro, secuencia narrativa) no se copia a CLAUDE.md. Vive o en el commit history o se regenera si hace falta.