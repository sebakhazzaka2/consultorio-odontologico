# Roadmap — Consultorio Odontológico

> **Última actualización:** 2026-05-19 — Integración de auditoría técnica. Se inserta Sprint 2.5 (hardening urgente) y Sprint 3.5 (pre-cliente #3). Sprint 3 incorpora hardening de seguridad. Items P3 estratégicos en sección dedicada al final.

## Secuencia recomendada
```
Multi-rubro ✅ → Sprint 2.0 Theming foundations (~2 días, antes de S2)
→ Disponibilidad + Reserva sin login (S2)
→ Sprint 2.5 Hardening urgente (~1.5 días, P0 auditoría)
→ Diagnósticos + Presupuestos + Archivos + Hardening seguridad (S3)
→ Sprint 3.5 Pre-cliente #3 (~2 semanas, P2 auditoría)
→ Mails transaccionales (S4) → WhatsApp automático (S5)
→ Portal paciente opcional (S6) → Balance + Gastos (S7)
→ Demo instance (S8) → SEO (S9) → Observability (S10) → cliente #3+
```

> **Polish visual incremental:** los quick wins de UI (skeletons, density, hero pública, agenda, etc. — ver `frontend/CLAUDE.md` § Theming/Polish) NO van en sprint dedicado. Se aplican dentro del PR de cada sprint que tocá la zona correspondiente. Foundations (theming dinámico) sí van en Sprint 2.0 porque desbloquean vender "mismo sistema, tu color" y evitan deuda lineal en cada componente nuevo.

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
| **Sprint 2.0 — Theming foundations (Material dinámico + brand tokens)** | ⏳ Próximo (~2 días, antes de S2) |
| **Sprint 2 — Disponibilidad + Reserva sin login** | ⏳ Tras S2.0 (~2 semanas) |
| **Sprint 2.5 — Hardening urgente (P0 auditoría)** | ⏳ Tras S2 (~1.5 días) |
| **Sprint 3 — Diagnósticos + Presupuestos + Archivos + Hardening seguridad** | ⏳ Tras S2.5 (~2 semanas, incluye P1 auditoría) |
| **Sprint 3.5 — Pre-cliente #3 (P2 auditoría)** | ⏳ Tras S3 (~2 semanas) |
| **Sprint 4 — Mails transaccionales** | ⏳ Tras S3.5 (5-8 días) |
| **Sprint 5 — WhatsApp automático** | ⏳ Tras S4 (1-2 semanas) |
| **Sprint 6 — Portal paciente opcional** | ⏳ Tras S5 (2-3 semanas) |
| **Sprint 7 — Balance + Gastos** | ⏳ Tras S6 (1-2 semanas) |
| **Sprint 8 — Demo instance + README pro** | ⏳ Tras S7 (2-4 días) |
| **Sprint 9 — SEO multi-subdominio** | ⏳ Tras S8 (3-5 días) |
| **Sprint 10 — Observability + rollback** | ⏳ Tras S9 (1 semana) |
| **Sprint 11 — Google Calendar sync** | ⏳ Diferido — infraestructura lista (V9) |
| 🎯 Primer cliente Web | ⏳ Tras S2.0 + S2 + S2.5 + hardening P1 de S3 + S4 (paquete básico vendible, ~6-7 sem calendario) |
| 🎯 Primer cliente Web + WhatsApp | ⏳ Tras S5 (paquete premium vendible, ~10-11 sem calendario) |
| 🎯 Cliente #2 | ⏳ Trigger para script provisioning + Terraform |
| **Fase Premium UX (diseñador + delight)** | ⏳ Post 1+ cliente WhatsApp pagando 2+ meses, antes de landing SaaS |
| Landing SaaS proveedor | ⏳ Post Premium UX (la landing debe mostrar el nivel premium, no el actual) |
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

### Sprint 2.0 — Theming foundations (~2 días, antes de S2)

> **Motivación:** hacer las bases técnicas del theming por cliente *antes* de S2 evita que cada componente nuevo (wizard de reserva, configuración de disponibilidad) nazca con `--color-primary` apuntando hard al navy. Es la única parte del "SaaS Polish" que pagaría intereses si se difiere. El polish puramente visual (skeletons, density, agenda colors, hero pública) se aplica de forma incremental dentro del PR del sprint que toca cada zona.

**Lo que entra en S2.0:**

- **[F-1] Migración Angular Material prebuilt → `@use mat`** (4-5 h). Quitar `@import 'indigo-pink.css'`, configurar `mat.theme()` con typography Inter + density 0, y override de tokens MDC (`--mdc-filled-button-container-color`, `--mat-toolbar-container-background-color`, datepicker, checkbox, switch, tabs, etc.) apuntando a `--color-primary`. Riesgo medio: verificar todos los componentes Material en uso.
- **[F-2] Capa brand tokens en `_design-system.scss`** (1 h). Introducir `--brand-primary`, `--brand-primary-hover`, `--brand-primary-contrast`, `--brand-accent`, `--brand-surface-tint`, `--brand-focus-ring` como capa intermedia. Los roles semánticos (`--color-primary` etc.) pasan a apuntar a `--brand-*`. Defaults = navy actual → cero regresiones si no hay branding.
- **[F-3] `applyBranding` en `ClinicConfigService`** (3-4 h). Extender `ClinicConfig` con bloque opcional `branding: { primary?, primaryHover?, accent?, surfaceTint?, logoUrl? }`. En `load()`, tras setear el signal, aplicar a `document.documentElement.style.setProperty(...)`. Helpers: `shade()` (oscurecer 12% si falta `primaryHover`), `alpha()` (derivar `surfaceTint` al 6%), `bestContrast()` (WCAG → blanco o ink-900 para `primaryContrast`).
- **[F-4] Backend `ClinicProperties.branding`** (2 h). Leer `BRAND_PRIMARY`, `BRAND_PRIMARY_HOVER`, `BRAND_ACCENT` del env. Incluirlos en `GET /api/public/config`. Si no están seteados, no enviar el bloque → frontend cae a defaults.
- **[F-5] Nuevos design tokens base** (2 h). Agregar a `_design-system.scss`: gradientes (`--gradient-brand`), shadows brand-aware (`--shadow-brand`, `--shadow-focus`), motion (`--ease-out`, `--ease-in-out`, `--duration-*`), z-index scale, letter-spacing, `--surface-overlay`. Sin estos, los próximos componentes los hardcodearían.
- **[F-6] QA visual en 2 brand configs** (3-4 h). Levantar la app con `BRAND_PRIMARY=#0B1A3A` (navy actual) y con `BRAND_PRIMARY=#A6428A` (magenta hipotético). Verificar admin completo + página pública. Detectar overrides Material que se escaparon.

**Total:** ~13-16 h (~2 días). **No mergear sin F-6.**

**Dependencias:** F-1 desbloquea F-2/F-3. F-4 paralelo. F-5 paralelo. F-6 al final.

**Decisión de scope explícita:** quedan FUERA de S2.0 (se difieren a su sprint correspondiente):
- Polish dashboard, skeletons, density tablas → con S7 (Balance) o ad-hoc.
- Polish agenda (línea "ahora", colores por estado) → con S2 (toca agenda igual).
- Polish hero pública, wizard reserva, mobile sticky CTA → con S2 (Reserva pública).
- Sidebar polish, dialogs refactor → con cualquier sprint que toque esa zona.

Ver inventario completo de quick wins en `frontend/CLAUDE.md` § "Polish backlog incremental".

---

### Sprint 2 — Disponibilidad admin + Reserva sin login (~2 semanas)

**Motivación:** cambio de enfoque — el admin siempre confirma, por lo tanto el cliente no necesita login para reservar. Esto simplifica todo el funnel de venta y hace el producto vendible antes.

**Deuda técnica resuelta (2026-05-15):** ✅ `clinic.json` / `docker-entrypoint.sh` / vars `CLINIC_*` eliminados. El core `ClinicConfigService` ahora llama a `/api/public/config` igual que la página pública. Una sola fuente de verdad en Spring Boot.

**Decisiones de diseño:**
- La duración de cada cita la define el **servicio**, no el paciente. Cada `Servicio` tiene un campo `duracionMinutos` que el admin configura al crear/editar el servicio.
- Cuando el paciente elige un servicio, el backend usa esa duración para calcular los slots disponibles. El paciente nunca ve ni elige la duración.
- El **email es obligatorio** en la reserva pública — necesario para enviar confirmación (S3) y recordatorio 24h antes (S3).
- Al confirmar una cita `PENDIENTE`, el admin ve la duración pre-cargada desde el servicio pero **puede editarla** antes de confirmar (flexibilidad clínica). La agenda bloquea la duración final confirmada.

✅**Paso 1 — Backend: duración en Servicio + modelo de disponibilidad persistente**
- Agregar `duracionMinutos` a la entidad `Servicio` (si no existe) + migración
- Nueva entidad `DisponibilidadSemanal` (día de semana → hora apertura/cierre + pausa opcional)
- Nueva entidad `FechaBloqueada` (fecha específica bloqueada por el admin)
- Repository + DTO + Service + Controller (`/api/admin/disponibilidad`, `/api/admin/fechas-bloqueadas`)
- Refactor `CitaService.getDisponibilidad()` para usar estas entidades en vez de valores hardcodeados

✅**Paso 2 — Backend: endpoints públicos de slots y reserva**
- `GET /api/public/slots?fecha=...&servicioId=...` — slots disponibles para un servicio en una fecha (sin auth). La duración se deriva del servicio.
- `POST /api/public/reservas` — crea `Cita` en estado `PENDIENTE` con: nombre, teléfono, email (obligatorio), servicioId, fechaHoraInicio. La duración se toma del servicio.

✅**Paso 3 — Frontend admin: configuración de horarios**
- Nueva ruta `/admin/disponibilidad`
- Tabla por día de semana: toggle activo/inactivo + hora apertura + hora cierre + pausa (opcional)
- Sección de bloqueo de fechas específicas (feriados, vacaciones) con calendario

✅**Paso 4 — Frontend admin: gestión de pendientes**
- Badge en sidebar con count de citas `PENDIENTE`
- Sección en Dashboard o Agenda para confirmar/rechazar reservas pendientes
- Modal de confirmación: muestra datos del paciente + servicio + duración pre-cargada (editable) + hora solicitada

✅**Paso 5 — Frontend público: formulario de reserva**
- Nueva ruta `/reservar` en la página pública
- Flujo: (1) elige servicio → (2) elige fecha → (3) elige slot (backend calcula con duración del servicio) → (4) completa nombre + teléfono + email → (5) envía
- Confirmación visual: "Tu solicitud fue enviada, te contactaremos para confirmar"
- Email obligatorio, validado en frontend y backend

✅**Preparación Google Calendar (incluida en S2):**
- Columna `google_event_id VARCHAR(255) NULL` en tabla `citas` (migración V9)
- Campo en entidad `Cita` — sin exponer en DTOs por ahora
- Permite futura sincronización bidireccional sin migración en producción con datos reales

### Sprint 2.5 — Hardening urgente (~1.5 días)

> **Origen:** auditoría técnica 2026-05-19, prioridad P0. Riesgos existenciales antes de seguir agregando features sobre 2 instancias en producción.

- **[P0-1] Probar restore de backup en VM limpia (2-3 h).** Restaurar el dump más reciente en una VM Hetzner descartable, validar arranque + integridad de datos. Sin esto un disco corrupto termina el negocio. Documentar el procedimiento en `scripts/` o `WORKFLOWS.md`.
- **[P0-2] Uptime monitor externo (30 min).** Better Stack / UptimeRobot free tier sobre las 2 instancias live, alertas a mail + Telegram.
- **[P0-3] Sentry para backend + frontend (3-4 h).** Free tier, `release` por commit SHA, `environment` por subdominio. Reemplaza la opacidad actual sobre 500s en producción.
- **[P0-4] Headers de seguridad en Caddy (1 h).** `Strict-Transport-Security`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`. Revisar también que `allowed-origins` CORS no tenga wildcards.
- **[P0-5] Auditar `@Query` y `Specification` por SQL injection (1-2 h).** Grep en backend de concatenación de strings en queries y Specification builders, especialmente filtros de búsqueda.
- **[P0-6] Verificar Sprint 2 contra spam/DoS (1-2 h).** Confirmar rate limit por IP + validación email + honeypot/captcha en endpoint público de reserva. Sin esto una botnet llena la agenda.

**Total:** ~10-13 h. Hacer antes de S3 para no acumular riesgo mientras crece la superficie del producto.

---

### Sprint 3 — Diagnósticos + Presupuestos + Archivos + Hardening seguridad (~2 semanas)

**Motivación:** el admin necesita poder armar presupuestos antes de iniciar tratamientos, enviarlos al paciente por mail, y adjuntar archivos clínicos (radiografías, fotos, consentimientos) a la ficha. El envío por mail depende de la infraestructura de S4, pero las entidades y la UI se construyen aquí. Se integran además los items P1 de la auditoría 2026-05-19 — hardening de seguridad que no puede esperar más sprints.

**Hardening de seguridad integrado (P1 auditoría, ~6-8 días):**

- **[P1-1] Migrar JWT de localStorage a cookie HttpOnly + SameSite=Strict (1-1.5 días).** Adelantado desde S6. XSS roba localStorage trivialmente. Habilitar Spring Security CSRF para endpoints state-changing y ajustar Angular. Aplica a JWT admin actual y al futuro JWT paciente (S6).
- **[P1-2] Rate limit + lockout en `/auth/login` (2-3 h).** Bucket4j, 5 intentos fallidos en 15 min → lockout temporal. Caddy rate-limit por IP es insuficiente contra botnet.
- **[P1-3] Paginación `Pageable` en listados (1 día).** Promovida de "deuda aceptada" a deuda activa. Listado de pacientes sin paginar explota con 3000+ pacientes reales.
- **[P1-4] CSP — Content Security Policy (4-6 h).** Empezar en modo `Report-Only`, iterar con Angular Material (inline styles requieren `style-src 'self' 'unsafe-inline'`), luego enforce. Bloquea XSS aunque se filtre sanitización.
- **[P1-5] Endurecer file upload (2-3 h).** `Content-Disposition: attachment` siempre + `X-Content-Type-Options: nosniff` en el endpoint de archivos clínicos. Subdominio dedicado `files.turnosuy.com` queda para S3.5 si requiere DNS.
- **[P1-6] Audit log de acciones admin (1 día).** Tabla `audit_log` con `@EntityListeners` o AOP. Quién modificó qué entidad y cuándo. Sin UI todavía — modelarlo ya antes de multi-usuario.
- **[P1-7] Recuperación de contraseña admin (4-6 h).** Hoy sin reset = SSH a producción. Adelantar la pieza mínima de SMTP (Brevo API directa) solo para este flujo. Resto de mails en S4.
- **[P1-8] Logs estructurados JSON + correlationId (2-3 h).** Adelantado desde S10. Logback `logstash-encoder` + filtro Spring que propaga `X-Correlation-Id`. Sin esto el primer bug raro es un suplicio de debugging.
- **[P1-9] Revisar `GET /api/public/config` (30 min).** Confirmar que no expone versiones de libs, URLs internas o paths. Solo flags + branding público.
- **[P1-10] Verificar Spring Actuator en :8081 (30 min).** Bindear a `127.0.0.1:8081` o red Docker interna dedicada. Si está en bridge default lo alcanza otro contenedor comprometido.

**Producto (Presupuestos + Archivos, ~5-8 días):**

**Presupuestos:**
- Entidad `Presupuesto`: paciente, fecha, descripción libre (texto), monto total, estado (`BORRADOR` / `ENVIADO`)
- CRUD de presupuestos desde la ficha del paciente en el admin
- Sin vinculación obligatoria a tratamientos ni aranceles del sistema — descripción libre
- Vista de presupuestos por paciente en el dashboard admin
- El envío por mail se implementa en Sprint 4

**Storage de archivos clínicos:**
- Entidad `ArchivoClinico`: paciente, nombre original, path en disco, tipo MIME, tamaño, fecha subida, referencia opcional a presupuesto
- `FileStorageService` (interfaz) + `LocalFileStorageServiceImpl`: guarda en `/app/uploads/pacientes/{id}/`, retorna path relativo para BD
- Docker volume en `docker-compose.prod.yml`: `/opt/consultorio/uploads:/app/uploads` — archivos sobreviven redeploys
- Endpoint `POST /api/admin/pacientes/{id}/archivos` (multipart) + `GET` para servir el archivo
- UI: sección "Archivos" en ficha del paciente — upload drag&drop + lista con nombre, fecha, botón descarga/borrar
- Backlog: migrar `LocalFileStorageServiceImpl` → `BackblazeStorageServiceImpl` cuando escale (sin cambiar el resto del código)

> **Operaciones críticas al agregar S3:**
> - Agregar al cron de backup: `rsync` o `tar` de `/opt/consultorio/uploads` además del dump MySQL. Los archivos **no** están en BD — si no se backupean se pierden.
> - Monitorear espacio en disco Hetzner (radiografías clínicas: 5-20 MB c/u).

### Sprint 3.5 — Pre-cliente #3 (~2 semanas)

> **Origen:** auditoría técnica 2026-05-19, prioridad P2. Bloqueantes comerciales antes de firmar el tercer cliente. Cosas que un cliente serio va a pedir y que hoy no podés responder.

- **[P2-1] Test E2E happy path con Playwright (1-1.5 días).** Login → crear paciente → crear turno → ver agenda. Corriendo en CI tras cada deploy. Más valor que tests unitarios frontend dispersos.
- **[P2-2] Export de datos del paciente JSON/CSV (4-6 h).** Endpoint admin que dumpea todo. Primera pregunta de cualquier cliente serio: "¿cómo me llevo mis datos si me voy?".
- **[P2-3] Soft delete en pacientes y turnos (3-4 h).** Flag `deleted_at`. Hoy borrar por error = ir al backup. No escala.
- **[P2-4] Multi-usuario admin — secretaria + dentista (2-3 días).** Repensar el modelo "un solo admin" que justifica varias decisiones actuales (sin refresh tokens, etc.). El primer cliente con secretaria lo pide; mejor antes que después de firmar.
- **[P2-5] Healthcheck que valide bundle frontend (2 h).** Caddy chequea HTML, no que Angular bootee y conecte al backend. Endpoint `/health` que verifique ambos extremos.
- **[P2-6] Script de provisioning de nueva instancia + `docs/ONBOARDING_CLIENTE.md` (1-2 días + 2-3 h).** Adelantado de Sprint 11. Ansible / shell idempotente. Cliente #3 no se monta a mano. El documento es la **fuente única** del checklist por rubro: datos de clínica, branding (`BRAND_*`), feature flags (`FEATURE_*`), servicios iniciales seed, DNS + Caddy, backup cron, SPF/DKIM, smoke test post-deploy. Hoy esa información está fragmentada entre `CLAUDE.md`, `ClinicProperties` y S2.0 — consolidarla acá evita errores al onboardar nuevos rubros (estética, veterinaria, kinesiología, etc.).
- **[P2-7] Definir SLA explícito y documentarlo (4 h).** 99% mensual = 7h downtime permitidas. Necesario para firmar con expectativas alineadas.
- **[P2-8] Página de status manual (2 h).** Estática editable cuando algo falla — "sabemos, ETA X". El cliente necesita saber que vos sabés.

**Total:** ~8-12 días. Bloquea la venta del cliente #3.

---

### Sprint 4 — Mails transaccionales (5-8 días)

**Scope:**
- `spring-boot-starter-mail` + Brevo SMTP (free tier: 300 mails/día) en `application-prod.properties`
- Templates Thymeleaf base con header/footer compartido (nombre + logo de clínica desde `ClinicProperties`)

**3 tipos de notificación al paciente (email ahora, WhatsApp en S5):**

1. **Cita:**
   - Confirmación cuando admin confirma la reserva
   - Cancelación / reagendamiento
   - Recordatorio 24h antes (cron job)

2. **Presupuesto:**
   - Admin envía el presupuesto (S3) por mail al paciente: descripción libre + monto total
   - Estado del presupuesto cambia de `BORRADOR` → `ENVIADO`

3. **Pago y deuda pendiente:**
   - Cuando admin registra un pago: mail al paciente con monto abonado y saldo pendiente

**Infraestructura adicional:**
- ~~Mail password reset~~ — adelantado a S3 (item P1-7 auditoría)
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

### Sprint 8 — Demo instance + README pro (2-4 días)

**Por qué después de Balance+Gastos:** el producto está completo — tiene todo lo que mostrar. Hacerlo antes implica actualizar los datos demo con cada sprint.

- `docker-compose.demo.yml` con datos fake realistas
- Subdominio `demo.turnosuy.com` con usuario/contraseña públicos en README
- README pro: badges, screenshot/GIF del admin, arquitectura de deploy, decisiones de diseño, link live + demo
- Sirve doble: portfolio personal + demo comercial para clientes potenciales
- La demo puede usar `BRAND_PRIMARY` neutral (gris oscuro) para que se entienda como "preview genérico" antes de que el cliente elija su paleta

> Theming dinámico ya está implementado desde Sprint 2.0 — acá solo se setean env vars de la instancia demo.

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

---

## Decisiones estratégicas pendientes (P3 auditoría 2026-05-19)

No urgentes pero caras si no se deciden ahora. Revisar al planificar el sprint relevante.

- **[P3-1] Reevaluar trigger de multi-tenancy real (análisis ~1 día).** Hoy "post 5 clientes". El costo operativo (deploys, parches, migraciones x N instancias) probablemente hace que el trigger real sea 3. Modelar costo operacional vs costo de refactor antes de cliente #3.
- **[P3-2] Feature flags: env vars → tabla DB (1-2 días).** Hoy 3 booleanos via env. Al primer pedido tipo "portal sí, reserva no" se vuelve incómodo. Tabla `clinic_features` con flags granulares. **Decidir antes de S8.**
- ~~**[P3-3] Estrategia theming Angular Material**~~ — ✅ resuelto en Sprint 2.0: opción (b) Material neutro + override de tokens MDC vía CSS vars. Ver `frontend/CLAUDE.md` § Theming.
- **[P3-4] Costo por mensaje WhatsApp y modelo "X incluidos/mes" (4 h).** Twilio ~$0.005/msg UY. UltraMsg con número personal = riesgo de ban. Modelar antes de prometer "WhatsApp ilimitado". **Antes de S5.**
- **[P3-5] Unificar JWT ADMIN + JWT PACIENTE con `roles` claim (1 día en S6).** Dos sistemas de auth en paralelo es complejidad real. Más barato unificar al implementar S6 que después.
- **[P3-6] Reservar 20% de cada sprint para incidentes/soporte (replanificación).** Con 2 clientes en producción el roadmap lineal sin buffer es ilusorio. Ajustar estimaciones de S3+ al planificar.
- **[P3-7] SPF/DKIM/DMARC en onboarding de subdominio (2 h en S4).** Sin esto los mails de S4 van a spam. Parte del checklist de provisioning (sinergia con P2-6).
- **[P3-8] Rotación de secrets documentada (2 h).** Procedimiento escrito para rotar `JWT_SECRET`, `DB_PASSWORD`. Sin blacklist JWT, rotación = logout masivo — documentar ventana.
- **[P3-9] Docker secrets en lugar de env vars (1 día a partir de 3+ clientes).** Hoy `docker inspect` revela secrets. Aceptable con 1-2 instancias, no con 5.
- **[P3-10] Backup off-site verificado (4-6 h).** Asumiendo que `backup.sh` ya manda fuera del host (si no, sube a P0). Verificar retención, encriptación en reposo, y que el restore (P0-1) funcione desde la copia off-site.

---

## Backlog medio plazo

- ~~Soft delete + auditoría acceso historial clínico~~ — soft delete en P2-3 (S3.5), audit log en P1-6 (S3). Falta solo el matiz Ley 18.331 (acceso histórico).
- ~~Paginación con `Pageable` en endpoints de listado~~ — movida a S3 como P1-3.
- Export PDF historial clínico
- Dark mode toggle
- Cobertura tests backend ~50% / frontend ~40%
- `FileStorageService` → `BackblazeStorageServiceImpl` cuando uploads escalen

## Fase Premium UX — diferido post primer cliente WhatsApp (antes de landing SaaS)

> **Motivación:** S2.0 + polish incremental llevan el producto a "profesional y vendible" (~percentil 75 del SaaS B2B regional). Para "alto valor percibido" — el tipo de producto que un cliente exigente paga con orgullo y referencia — falta una capa de delight + diseño visual profesional que no se construye con tokens, se construye con criterio de diseñador y trabajo dedicado.
>
> **Cuándo:** post S5 (paquete WhatsApp vendible) + 1-2 clientes pagando. Antes de invertir en `consultorio-landing`, porque la landing tiene que mostrar este nivel de producto, no el actual. Financiado por ingresos reales del producto, no pre-revenue.
>
> **No incluido:** features funcionales (eso es roadmap normal). Solo capa de percepción y delight.

**Bloque A — Diseño visual profesional (1 semana diseñador externo + 3-5 días integración):**
- Contratar diseñador UI senior (Dribbble/Behance regional, ~USD 800-1500 por proyecto definido) o usar plantilla curada premium (Untitled UI, Tailwind UI, Cult UI) adaptada
- Entregables esperados: paleta refinada con personalidad por rubro (no solo navy), sistema de ilustraciones propias para empty states, jerarquía tipográfica dramática (hero 56-64px, stats 48px), imagery curada con tratamiento brand consistente, iconografía custom (mover de Material Icons genéricos a set propio o Lucide/Phosphor)
- Auditoría visual completa: contraste, espaciado, alineación pixel-perfect, dark mode opcional

**Bloque B — Delight & interacciones premium (~1 sprint, 1-2 sem):**
- Command palette (Cmd+K) — búsqueda global cross-módulo: pacientes, citas, tratamientos, navegación
- Atajos de teclado documentados (`?` muestra cheatsheet): nuevo paciente, nueva cita, buscar, guardar
- Autoguardado con indicador discreto ("Guardado hace 2s") en forms largos
- Undo/redo en acciones destructivas (snackbar con "Deshacer" 5s, no solo confirmación)
- Multi-selección en listados (checkbox + bulk actions: archivar, exportar)
- Shared element transitions: paciente del listado → ficha (Angular animations o View Transitions API)
- Micro-feedback: check verde animado al guardar (no solo snackbar), números que cuentan al cargar dashboard (CountUp), skeletons con shimmer real
- Empty states con personalidad: ilustración + microcopy cálido + CTA contextual
- Loading states diferenciados: skeleton para listas, spinner para acciones, optimistic UI donde aplique

**Bloque C — Detalles de pulido fino (~3-5 días):**
- Sonido sutil opcional (toggle): confirmación de guardado, error
- Modo "focus" en agenda: oculta navegación, maximiza calendario (presentación al paciente en consulta)
- Print stylesheets: imprimir presupuesto, historial, agenda del día con branding
- Animaciones de éxito post-acción (confetti minimalista al cerrar primer presupuesto del mes, etc. — opcional, easter eggs)

**Total estimado:** 3-4 semanas calendario incluyendo ida-vuelta con diseñador. **Costo:** USD 800-1500 diseñador + tu tiempo de integración.

**Trigger explícito:** "tengo al menos 1 cliente WhatsApp pagando cuota mensual hace 2+ meses sin churn". Antes de eso, es over-engineering pre-revenue — la estética premium sin features premium no vende (Mercado Libre, Despegar, casi todo el SaaS B2B regional opera con UI funcional sin delight, y venden).

**Alternativa pragmática si no hay presupuesto para diseñador:** comprar Untitled UI Angular kit (USD 349 one-time) y hacer adaptación propia. Pierde personalidad pero sube el piso visual significativamente con 3-5 días de trabajo.

---

## Backlog largo plazo (post 5 clientes activos)

- Multi-tenancy real (schema MySQL por cliente)
- i18n (es/pt/en)
- App móvil PWA
- Búsqueda global cross-módulo

## Landing SaaS proveedor ⏳ (repo `consultorio-landing`)

Post primer cliente con caso de éxito. Stack: Astro + Tailwind v4, deploy Cloudflare Pages.
