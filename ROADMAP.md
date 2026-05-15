# Roadmap — Consultorio Odontológico

> **Última actualización:** 2026-05-15 — Repriorización post Sprint 1. Reserva sin login + WhatsApp suben; MVP2 portal baja a Sprint 5.

## Secuencia recomendada
```
Multi-rubro ✅ → Deuda técnica + Disponibilidad admin + Reserva sin login (S2)
→ Diagnósticos + Presupuestos (S3) → Mails transaccionales (S4) → WhatsApp automático (S5)
→ Portal paciente opcional (S6) → Balance + Gastos (S7)
→ Demo instance + README pro (S8) → SEO (S9) → Observability (S10) → primer cliente pagando
```

## Estado por fase

| Fase | Estado |
|------|--------|
| MVP1 — Backend | ✅ Completo |
| MVP1 — Frontend | ✅ Completo |
| P1 — Hardening pre-producción | ✅ Completo |
| P2 — Infraestructura profesional | ✅ Completo |
| P3 Fase A — Producto usable | ✅ Completo |
| P3 Fase B — código deploy | ✅ Completo |
| Página pública clínica | ✅ Completo |
| Admin polish | ✅ Completo |
| Deploy real | ✅ Live en dentalmontecaseros.turnosuy.com (2026-04-21) |
| Post-deploy hardening | ✅ Completo (PR #19, 2026-04-22) |
| Public page polish | ✅ Completo (PR #20, 2026-04-28) |
| fix/safari-white-screen | ✅ Completo (2026-05-11) |
| Sprint 1 — Multi-rubro genérico | ✅ Completo (2026-05-11) — live en neodentalmaster.turnosuy.com |
| fix/admin-branding | ✅ Completo (2026-05-11) |
| **Sprint 2 — Disponibilidad + Reserva sin login** | ⏳ Próximo (~2 semanas) |
| **Sprint 3 — Diagnósticos + Presupuestos** | ⏳ Tras S2 (3-5 días) |
| **Sprint 4 — Mails transaccionales** | ⏳ Tras S3 (5-8 días) |
| **Sprint 5 — WhatsApp automático** | ⏳ Tras S4 (1-2 semanas) |
| **Sprint 6 — Portal paciente opcional** | ⏳ Tras S5 (2-3 semanas) |
| **Sprint 7 — Balance + Gastos** | ⏳ Tras S6 (1-2 semanas) |
| **Sprint 8 — Demo instance + README pro** | ⏳ Tras S7 (2-4 días) |
| **Sprint 9 — SEO multi-subdominio** | ⏳ Tras S8 (3-5 días) |
| **Sprint 10 — Observability + rollback** | ⏳ Tras S9 (1 semana) |
| 🎯 Primer cliente Web | ⏳ Tras S2 + S4 (paquete básico vendible) |
| 🎯 Primer cliente Web + WhatsApp | ⏳ Tras S5 (paquete premium vendible) |
| 🎯 Cliente #2 | ⏳ Trigger para script provisioning + Terraform |
| Landing SaaS proveedor | ⏳ Post primer cliente pagando con caso de éxito |
| V5 — Multi-tenant SaaS | ⏳ Post ~5 clientes activos |

---

## Pre-ventas — checklist mínimo antes de vender

**Paquete Local / Web básico (vendible tras S2 + S4):**
- ⬜ Reserva como invitado desde página pública (sin login requerido)
- ⬜ Admin gestiona disponibilidad de horarios
- ⬜ Admin confirma / cancela / reagenda desde dashboard
- ⬜ Email confirmación de turno al cliente
- ⬜ Script de provisioning: dado dominio + datos de clínica, levanta instancia en < 15 min
- ⬜ Backup automático verificado (cron + test de restore)
- ✅ HTTPS automático por subdominio (Caddy)

**Paquete Web + WhatsApp (vendible tras S4):**
- ⬜ Recordatorio automático 24h antes por WhatsApp
- ⬜ Notificación al cliente cuando admin confirma / cancela

**Portal paciente (valor agregado, tras S5):**
- ⬜ Registro/login opcional para ver historial, cancelar (72h), reagendar

---

## Detalle por sprint

### Sprint 2 — Disponibilidad admin + Reserva sin login (~2 semanas)

**Motivación:** cambio de enfoque — el admin siempre confirma, por lo tanto el cliente no necesita login para reservar. Esto simplifica todo el funnel de venta y hace el producto vendible antes.

**Deuda técnica resuelta (2026-05-15):** ✅ `clinic.json` / `docker-entrypoint.sh` / vars `CLINIC_*` eliminados. El core `ClinicConfigService` ahora llama a `/api/public/config` igual que la página pública. Una sola fuente de verdad en Spring Boot.

**Decisiones de diseño:**
- La duración de cada cita la define el **servicio**, no el paciente. Cada `Servicio` tiene un campo `duracionMinutos` que el admin configura al crear/editar el servicio.
- Cuando el paciente elige un servicio, el backend usa esa duración para calcular los slots disponibles. El paciente nunca ve ni elige la duración.
- El **email es obligatorio** en la reserva pública — necesario para enviar confirmación (S3) y recordatorio 24h antes (S3).
- Al confirmar una cita `PENDIENTE`, el admin ve la duración pre-cargada desde el servicio pero **puede editarla** antes de confirmar (flexibilidad clínica). La agenda bloquea la duración final confirmada.

**Paso 1 — Backend: duración en Servicio + modelo de disponibilidad persistente**
- Agregar `duracionMinutos` a la entidad `Servicio` (si no existe) + migración
- Nueva entidad `DisponibilidadSemanal` (día de semana → hora apertura/cierre + pausa opcional)
- Nueva entidad `FechaBloqueada` (fecha específica bloqueada por el admin)
- Repository + DTO + Service + Controller (`/api/admin/disponibilidad`, `/api/admin/fechas-bloqueadas`)
- Refactor `CitaService.getDisponibilidad()` para usar estas entidades en vez de valores hardcodeados

**Paso 2 — Backend: endpoints públicos de slots y reserva**
- `GET /api/public/slots?fecha=...&servicioId=...` — slots disponibles para un servicio en una fecha (sin auth). La duración se deriva del servicio.
- `POST /api/public/reservas` — crea `Cita` en estado `PENDIENTE` con: nombre, teléfono, email (obligatorio), servicioId, fechaHoraInicio. La duración se toma del servicio.

**Paso 3 — Frontend admin: configuración de horarios**
- Nueva ruta `/admin/disponibilidad`
- Tabla por día de semana: toggle activo/inactivo + hora apertura + hora cierre + pausa (opcional)
- Sección de bloqueo de fechas específicas (feriados, vacaciones) con calendario

**Paso 4 — Frontend admin: gestión de pendientes**
- Badge en sidebar con count de citas `PENDIENTE`
- Sección en Dashboard o Agenda para confirmar/rechazar reservas pendientes
- Modal de confirmación: muestra datos del paciente + servicio + duración pre-cargada (editable) + hora solicitada

**Paso 5 — Frontend público: formulario de reserva**
- Nueva ruta `/reservar` en la página pública
- Flujo: (1) elige servicio → (2) elige fecha → (3) elige slot (backend calcula con duración del servicio) → (4) completa nombre + teléfono + email → (5) envía
- Confirmación visual: "Tu solicitud fue enviada, te contactaremos para confirmar"
- Email obligatorio, validado en frontend y backend

### Sprint 3 — Diagnósticos + Presupuestos (3-5 días)

**Motivación:** el admin necesita poder armar presupuestos antes de iniciar tratamientos y enviarlos al paciente por mail. El envío por mail depende de la infraestructura de S4, pero la entidad y la UI se construyen aquí.

- Entidad `Presupuesto`: paciente, fecha, descripción libre (texto), monto total, estado (`BORRADOR` / `ENVIADO`)
- CRUD de presupuestos desde la ficha del paciente en el admin
- Sin vinculación obligatoria a tratamientos ni aranceles del sistema — descripción libre
- Vista de presupuestos por paciente en el dashboard admin
- El envío por mail se implementa en Sprint 4

### Sprint 4 — Mails transaccionales (5-8 días)

**Scope:**
- `spring-boot-starter-mail` + Brevo SMTP (free tier: 300 mails/día) en `application-prod.properties`
- Templates Thymeleaf base con header/footer compartido (nombre + logo de clínica desde `ClinicProperties`)
- Mail confirmación de turno al cliente (cuando admin confirma)
- Mail cancelación / reagendamiento al cliente
- Mail recordatorio 24h antes del turno (cron job)
- Mail notificación de pago: cuando el admin registra un pago, se envía mail al paciente con monto abonado y saldo pendiente
- Mail envío de presupuesto: admin selecciona un presupuesto (S3) y lo envía por mail al paciente con descripción y total
- Mail password reset (token + expiración + UI) — necesario para el portal de S6
- Mail alerta de fallo de backup al admin (cron existente)

### Sprint 5 — WhatsApp automático (1-2 semanas)

**Motivación:** diferencial clave del paquete premium. Con la infraestructura de mails en S4, WhatsApp es el mismo trigger en otro canal.

- Twilio Business API o UltraMsg (evaluar costo por mensaje para trasladar al precio del paquete)
- Recordatorio 24h antes del turno
- Notificación cuando admin confirma la reserva
- Notificación de cancelación / cambio de horario
- Costo por mensaje: documentar y reflejar en precio del paquete Web + WhatsApp

### Sprint 6 — Portal paciente opcional (2-3 semanas)

**Cambio respecto al plan anterior:** ya no es bloqueante de ventas. El cliente puede reservar sin login. El portal es valor agregado para retención y experiencia.

- Registro/login opcional — el cliente puede "reclamar" su historial de reservas
- Ver citas agendadas (próximas + historial)
- Cancelar cita (con restricción: solo hasta 72h antes)
- Reagendar cita
- Ver procedimientos realizados (historial clínico resumido)
- JWT con rol PACIENTE + RBAC (`@PreAuthorize` + route guard)
- Refresh tokens con rotación
- JWT en cookie HttpOnly + SameSite=Strict (mover desde localStorage)
- Consentimiento explícito de datos (Ley 18.331)
- Política de privacidad + términos de uso

### Sprint 7 — Balance + Gastos (1-2 semanas)

- CRUD de gastos con categorías
- Balance mensual: Σ cobros − Σ gastos del período
- Vista resumen en dashboard admin
- Export básico (CSV)

### Sprint 8 — Demo instance + README pro + Theming por cliente (2-5 días)

**Por qué después de Balance+Gastos:** el producto está completo — tiene todo lo que mostrar. Hacerlo antes implica actualizar los datos demo con cada sprint.

- `docker-compose.demo.yml` con datos fake realistas
- Subdominio `demo.turnosuy.com` con usuario/contraseña públicos en README
- README pro: badges, screenshot/GIF del admin, arquitectura de deploy, decisiones de diseño, link live + demo
- Sirve doble: portfolio personal + demo comercial para clientes potenciales

**Theming por cliente via env vars (~3-4 h):**
- El backend expone los colores de branding en `GET /api/public/config` junto con los datos de clínica (ya usa `ClinicProperties`)
- Agregar a `ClinicProperties`: `brandPrimary`, `brandPrimaryHover`, `brandAccent`, etc. (los 8 tokens navy del design system)
- Al arrancar, el frontend inyecta esos valores como CSS custom properties en el `<html>` root sobreescribiendo los defaults de `_design-system.scss`
- Angular Material: migrar de `indigo-pink.css` prebuilt a un tema generado con `@use '@angular/material' as mat` para que los componentes Material (date picker, botones mat) también respeten el color del cliente
- Resultado: un mismo build Docker, colores distintos por instancia via env vars `BRAND_PRIMARY=#...` etc. Sin recompilar.
- La demo en `demo.turnosuy.com` puede usar colores neutrales y servir como preview del sistema antes de que el cliente elija su paleta

### Sprint 9 — SEO multi-subdominio (3-5 días)

Con 2+ subdominios reales (dental + estética) el ROI es inmediato; el código se hace una vez y aplica a todos los clientes futuros.

- Prerender estático con `@angular/ssr` (build-time)
- Meta tags dinámicos desde `ClinicConfig` usando `Title` + `Meta` services
- JSON-LD genérico (`LocalBusiness` + tipo específico por instancia vía `ClinicProperties`)
- Endpoint backend `/sitemap.xml` por subdominio
- `robots.txt` estático con `Sitemap:`
- Headers HTTP de seguridad en `Caddyfile` (CSP, HSTS, X-Frame-Options)

### Sprint 10 — Observability + rollback (1 semana)

- Logs JSON + correlationId/MDC
- Micrometer + `actuator/prometheus` expuesto
- Smoke test post-deploy: `curl /api/actuator/health` con retry
- `scripts/rollback.sh` que vuelve al SHA anterior
- Tag de imagen por SHA en `docker-compose.prod.yml` (no `:latest`)
- Backup automático pre-deploy en CI/CD
- Notificación de deploy a Telegram/Discord
- Reglas Sentry + alertas Uptime Kuma a canal real

### Sprint 10+ — Pre-cliente #2

- Script de provisioning (< 15 min para nueva clínica)
- Terraform módulo Hetzner Cloud + Cloudflare
- Prometheus + Grafana (self-hosted)

---

## Backlog medio plazo

- Soft delete + auditoría acceso historial clínico (Ley 18.331)
- Paginación con `Pageable` en endpoints de listado
- Export PDF historial clínico
- Dark mode toggle
- Cobertura tests backend ~50% / frontend ~40%
- `FileStorageService` → `BackblazeStorageServiceImpl` cuando uploads escalen

## Backlog largo plazo (post 5 clientes activos)

- Multi-tenancy real (schema MySQL por cliente)
- i18n (es/pt/en)
- App móvil PWA
- Búsqueda global cross-módulo

## Landing SaaS proveedor ⏳ (repo `consultorio-landing`)

Post primer cliente con caso de éxito. Stack: Astro + Tailwind v4, deploy Cloudflare Pages.
