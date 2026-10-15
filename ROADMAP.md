# Roadmap — TurnosUy (Consultorio)

> **Última actualización:** 2026-10-15 — Replanificación por fases F0–F5 contrastada contra el código de `main`. Entra Guille Blanchet Beauty (Uruguay) como piloto de belleza; la clínica de fisioterapia (Brasil) pasa a ser un paquete complementario de Vedius, condicionado. Reemplaza la secuencia por sprints del 2026-09-24 (S3→S6→S10…); los sprints viejos que siguen vigentes quedan en "Backlog posterior".

## Restricciones

- **Capacidad:** 15 a 20 h por semana. Las semanas de abajo asumen ~17 h.
- **Un solo código** con módulos y presets por configuración; sin ramas ni repos por vertical o paquete.
- **Una instancia Docker por cliente** hasta ~5 clientes; después se evalúa multi-tenancy.
- **La infraestructura de Guille va antes que flags y presets** (F1 antes que F3).
- **Guille no migra hasta tener recordatorios por email.** Su instancia se levanta en paralelo y ella sigue en Ágora hasta que F2a esté en producción. Sin recordatorios, el producto es inferior a Ágora.

## Secuencia

```
F0 Higiene (12–18 h)                                   ← PRÓXIMO
→ F1 Instancia Guille en paralelo + importación CSV (42–63 h) — Guille sigue en Ágora
→ F2a Recordatorios por email (20–28 h)
→ Corte Guille: importar sus clientas y salir en vivo (3–5 h)
→ F2b WhatsApp (23–33 h)
→ F3 Flags y presets (20–30 h)
→ F3b pt-BR (30–45 h)                     — solo si firma un cliente de Brasil
→ F4 Fisio como complemento de Vedius     — condicionada a la reunión con la clínica
→ F5 Agente (95–135 h)
→ Backlog posterior (S5, S7, S8, S9, S11–S14)
```

**Hasta Guille en vivo con recordatorios por email** (F0 + F1 + F2a + corte): **77–114 h, unas 5 a 7 semanas.**

**En paralelo y fuera de este repo:** FrontPet (entregar lo comprometido al cliente cerrado). Consume horas de la misma bolsa.

---

## Estado por fase

| Fase | Estado |
|------|--------|
| MVP1, P1 hardening, P2 infra, P3 Fase A/B, página pública, admin polish | ✅ Completo |
| Deploy real | ✅ Live desde 2026-04-21. Hoy en DigitalOcean (migrado desde Hetzner 2026-10-15) |
| Sprint 1 — Multi-rubro (labels `labelServicio` / `labelHistorial`) | ✅ (PR #21, 2026-05-11) |
| Sprint 2 — Disponibilidad + reserva sin login | ✅ (2026-05-20) |
| Sprint 3 — Hardening urgente (register cerrado, headers Caddy, rate limit nginx, honeypot) | ✅ Mergeado (PR #26). Restore probado y alertas de Kuma pasan a F0 |
| Race condition en reserva pública + PII en logs de reserva | ✅ (PR #32–#35, 2026-10-15). Falta test de concurrencia contra MySQL real dentro del repo |
| Demo en Render (seed + login precargado) | ✅ (2026-08-19) |
| **F0 — Higiene** | ⏳ Próximo |
| **F1 — Instancia Guille + importación CSV** | ⏳ |
| **F2a — Recordatorios por email** | ⏳ |
| **Corte Guille** | ⏳ Tras F2a |
| **F2b — WhatsApp** | ⏳ |
| **F3 — Flags y presets** | ⏳ |
| **F3b — pt-BR** | ⏸️ Condicionada a un cliente de Brasil |
| **F4 — Fisio (complemento de Vedius)** | ⏸️ Condicionada a la reunión con la clínica |
| **F5 — Agente** | ⏳ Tras F2 |

---

## Hechos verificados en el código (2026-10-15)

Base sobre la que se estimaron las fases. Si alguno cambia, revisar las horas.

| Tema | Estado real | Evidencia |
|------|-------------|-----------|
| Tests | 26 `@Test` en backend (7 clases), 6 specs de frontend | `backend/src/test`, `*.spec.ts` |
| Login erróneo | Responde 500 | `AuthService` lanza `RuntimeException`; `AuthControllerTest.login_passwordIncorrecta_retorna500` |
| Defaults del primer cliente | Datos reales de Dental Montecaseros en 5 lugares | `ClinicProperties:27-37`, `application.properties:45-52`, `index.html:5,9`, `clinic.config.ts` (código muerto), `.env.example` |
| Validación de arranque | `StartupEnvValidator` no valida `CLINIC_*` | Solo JWT, CORS y credenciales admin |
| Vista previa de links | `index.html` tiene `<title>` y `og:title` estáticos de Montecaseros; WhatsApp no ejecuta JS | `index.html:5,9`; `app.component.ts` cambia el título solo en runtime |
| Feature flags | **No existen.** `ClinicProperties.Feature` son tarjetas de marketing | `ClinicProperties.java` |
| Branding | Colores en SCSS, sin `BRAND_*` ni inyección en runtime | `_design-system.scss` |
| Texto por rubro | Solo `labelServicio` y `labelHistorial` configurables; texto dental fijo en `home`, `public`, `servicios` | grep "sonrisa", "odontol" |
| i18n | Ninguna biblioteca. 17 templates HTML + 15 inline en español; `LOCALE_ID: 'es'` | `app.config.ts` |
| Moneda y locale | ~20 apariciones de `UY$` / `UYU` / `es-UY` / `es-ES` en 9 archivos | grep |
| Agenda | Una sola agenda. `disponibilidad_semanal` con `UNIQUE (dia_semana)`; lock por día de semana | `V7`, `CitaService.lockDiaParaReserva` |
| Alta automática de pacientes | Existe desde la reserva pública (`PacienteOrigen.AGENDA`) | `PublicReservaService:123` |
| Pacientes: email | `NOT NULL`, `@NotBlank @Email`; sin índice único en la base, unicidad solo en la app (`existsByEmail`) | `V1`, `Paciente.java`, `PacienteService:38` |
| Pacientes: teléfono | `VARCHAR(15) NOT NULL`, solo dígitos `^[0-9]{7,15}$`, sin código de país | `V1`, `Paciente.java` |
| Pacientes: apellido | `NOT NULL` + `@NotBlank` | `V1`, `Paciente.java` |
| Pacientes: origen | `creado_por ENUM('ADMIN','AGENDA')` en MySQL | `V8` |
| PII en logs | `PacienteService:39,63` loguea el email en los warn | — |
| Notificaciones | Ninguna. Sin `spring-boot-starter-mail` ni Twilio; solo links `wa.me`. `@EnableScheduling` existe (un cron de disponibilidad) | `pom.xml`, `ConsultorioApplication` |
| Opt-in / consentimiento | No hay columnas | `Paciente.java` |
| Roles | Rol único `ADMIN` | `User.java` |
| Dashboard | Calculado en el cliente: descarga todas las citas, pacientes y pagos sin paginar | `dashboard.component.ts:132` |
| Archivos clínicos | **No existen.** `FileStorageService` solo maneja imágenes subidas | No hay `ArchivoClinico` |
| CI/CD | Despliega a un solo host (`DO_HOST`), tag `:latest`, sin rollback | `.github/workflows/ci.yml` |
| Logging | `spring.jpa.show-sql=true` en la configuración base, sin override en prod | `application.properties:15` |
| Backups | Uploads + copia off-site a B2 implementados en cambios locales, **todavía sin commitear** | `scripts/backup.sh` (working tree) |

---

## Detalle por fase

### F0 — Higiene (12–18 h, semana 1)

**Tareas**
- [ ] Login fallido responde 401 con test (1–2 h).
- [ ] Quitar los defaults de Montecaseros de `ClinicProperties`, `application.properties` y `.env.example`; borrar `clinic.config.ts`; `StartupEnvValidator` falla si falta un `CLINIC_*` de marca (3–4 h).
- [ ] `index.html` con `<title>` y `og:*` por instancia, con envsubst en el arranque del contenedor de nginx (3–5 h).
- [ ] Commitear los cambios de `backup.sh` / `restore.sh` (uploads + B2) y probar el restore con uploads en una VM limpia, cronometrado (4–6 h). Seguir `docs/RUNBOOK_S3.md`.
- [ ] Confirmar las alertas de Uptime Kuma.
- [ ] Contrastar el SHA de prod con `main` y apagar `show-sql` (1 h).

**Entregables:** PR de higiene; restore documentado con tiempos.

**Criterio de salida:** restore probado en máquina limpia; login devuelve 401; `grep Montecaseros` sin resultados en el código; prod en el mismo SHA que `main`.

**Riesgo:** que prod difiera del repositorio.

### F1 — Instancia de Guille en paralelo + importación CSV (42–63 h, semanas 2–4)

Guille **sigue en Ágora** durante toda esta fase. La instancia se levanta y se valida, pero no se le migra ni se le anuncia a sus clientas.

**Tareas — instancia (25–38 h)**
- [ ] VPS nuevo, Docker Compose, `.env` con secretos propios (JWT, DB, admin distintos de los de prod), registro A del subdominio de turnosuy.com, Caddy y monitor en Kuma (6–8 h).
- [ ] Deploy multi-host en CI: un merge a `main` llega a los dos hosts (3–5 h).
- [ ] Texto del rubro desde la configuración: sacar el texto dental fijo de `home`, `public` y `servicios` (6–10 h).
- [ ] Colores y logo en runtime con variables CSS `--brand-*`, el subconjunto mínimo de F-2/F-3/F-4 del viejo S4 (6–10 h).
- [ ] Backups propios a B2 y restore probado de esta instancia (2–3 h).
- [ ] Cargar servicios y horarios, smoke test y checklist de aislamiento (2–4 h).

**Tareas — importación CSV (17–25 h)**

El esquema actual tiene cuatro límites que la importación tiene que resolver: email obligatorio, teléfono de hasta 15 dígitos sin formato, sin unicidad de email en la base y apellido obligatorio.

- [ ] Migración `V10` (4–6 h):
  - `email` pasa a `NULL` y se normaliza con `lower(trim(email))`.
  - Se agrega `UNIQUE(email)`; MySQL admite varios `NULL`.
  - `creado_por` suma el valor `IMPORT`.
  - La reserva pública sigue exigiendo email; se relaja solo en admin e importación. Ajustar el frontend para email nulo.
  - **Prerrequisito en cada base de prod:** `SELECT lower(trim(email)) e, COUNT(*) FROM pacientes GROUP BY e HAVING COUNT(*) > 1;` debe devolver 0.
- [ ] `PhoneNormalizer` por país de la instancia (`CLINIC_COUNTRY`: 598 o 55): deja solo dígitos y agrega el prefijo. F2b lo reutiliza (2–3 h).
- [ ] Importador (8–12 h):
  - Paso de simulación con estado por fila: `ok`, `error` con motivo, `duplicado en el archivo`, `ya existe` (email normalizado; si no hay email, teléfono normalizado).
  - Paso de confirmación que inserta solo las filas `ok`; correr el mismo archivo dos veces no duplica nada.
  - Nunca fusiona por nombre (error revertido en el PR #34).
  - Nombre completo en una columna: separar en el último espacio; un solo token queda para revisión.
  - Filas sin teléfono van al reporte de errores.
  - Acepta UTF-8 y Latin-1, separador `;` o `,`, fechas `dd/MM/yyyy`.
  - No usar emails de relleno.
- [ ] Tests de integración con duplicados, filas sin email, teléfonos sucios y archivos Latin-1 (3–4 h).
- [ ] Sacar el email de los logs de `PacienteService` (0.5 h).
- v1 sin interfaz: un endpoint de admin y una plantilla CSV que se corre en el onboarding. La pantalla con vista previa (8–12 h) queda para después.

**Entregables:** instancia de Guille en línea con su marca; importador probado con un CSV real de Ágora si se puede exportar.

**Criterio de salida:** ninguna respuesta pública ni vista previa de WhatsApp menciona a Montecaseros; un merge a `main` llega a los dos hosts; simulación de importación del CSV de Guille sin errores no explicados.

**Riesgos:** que una variable faltante publique datos ajenos; que Ágora no exporte clientas; que el VPS chico no aguante JVM + MySQL + Kuma.

### F2a — Recordatorios por email (20–28 h, semanas 5–6)

**Tareas**
- [ ] `NotificationService` con interfaz por canal (`NotificationChannel` → `EmailChannel`), para que F2b solo agregue `WhatsAppChannel`.
- [ ] `spring-boot-starter-mail` + Brevo SMTP y plantillas Thymeleaf con nombre y logo de la instancia.
- [ ] Tabla `notificaciones_enviadas` con `UNIQUE (cita_id, tipo)`: el cron es idempotente.
- [ ] Zona horaria por instancia (`CLINIC_TIMEZONE`) para el cron de recordatorio 24 h antes.
- [ ] Confirmación, cancelación o reagendamiento, y recordatorio 24 h.
- [ ] Link de baja en cada mail.
- [ ] SPF, DKIM y DMARC en los subdominios de turnosuy.com.
- [ ] Password reset de admin (P1-7) si entra en las horas; si no, pasa a F2b.

**Criterio de salida:** recordatorios en producción en la instancia dental y en la de Guille, sin duplicados, y mails que no caen en spam.

**Riesgo:** reputación del dominio de envío.

### Corte de Guille (3–5 h, tras F2a)

- [ ] Exportar sus clientas de Ágora, simular, corregir y confirmar la importación.
- [ ] Cargar los turnos futuros que ya tiene agendados en Ágora.
- [ ] Publicar su link de reserva y avisar a sus clientas.
- [ ] Definir con ella una ventana de convivencia con Ágora y la fecha de baja.

**Criterio de salida:** Guille agenda sus turnos reales en la instancia durante 2 semanas y recibe los recordatorios por email.

### F2b — WhatsApp (23–33 h)

**Tareas**
- [ ] Modelar el costo por mensaje en UY y BR y cuántos mensajes incluye la cuota (P3-4), antes de construir.
- [ ] Enviar las plantillas a Meta el primer día de la fase (español; portugués si F3b está en marcha).
- [ ] Twilio WhatsApp Business API como `WhatsAppChannel` sobre los mismos triggers de F2a.
- [ ] Columna de opt-in en `pacientes` + checkbox en la reserva pública; sin opt-in no se envía.
- [ ] Normalización E.164 con el `PhoneNormalizer` de F1, incluida la migración de los teléfonos existentes sin código de país.

**Criterio de salida:** 2 semanas en las dos instancias sin duplicados; ausencias medidas antes y después en la instancia dental.

**Riesgo:** la aprobación de Meta lleva tiempo de calendario, no horas.

### F3 — Flags y presets (20–30 h)

**Tareas**
- [ ] `FEATURE_*` reales en `ClinicProperties`, expuestos en `GET /api/public/config` y aplicados con guards de rutas en el frontend (6–10 h).
- [ ] Moneda y locale por configuración en los ~20 lugares fijos, incluido `LOCALE_ID` y los `toLocale*String('es-ES')` de la agenda (8–12 h).
- [ ] Presets por rubro (odontología, belleza, fisioterapia) como conjuntos de variables, y `docs/ONBOARDING_CLIENTE.md` (6–8 h).

**Criterio de salida:** una instancia nueva se crea cambiando solo `.env`; no queda ningún `UY$` fijo.

**Riesgo:** rediseñar de más antes de que un cliente lo pida.

### F3b — pt-BR (30–45 h, condicionada)

Solo si firma un cliente de Brasil. Introducir una biblioteca de i18n, extraer el texto de los 32 templates y traducir los mensajes del backend.

### F4 — Fisioterapia como complemento de Vedius (condicionada)

El jefe de la clínica no piensa dejar Vedius, y lo que más les molesta es el trabajo manual dentro de Vedius. Nuestra alta automática registra pacientes en nuestra base, no en la de Vedius, así que una segunda agenda duplicaría la carga.

- **4a. Timer de intervalos como PWA independiente (12–16 h).** No depende del núcleo; puede adelantarse si aceptan pagar un piloto.
- **4b. Web institucional sin reserva propia (6–10 h).** Botón a la reserva online de Vedius o a WhatsApp. Requiere F3 y F3b.
- **4c. Integración con Vedius (0 h hasta saber si tiene API).** Hipótesis de 30–50 h si la tiene. Es lo único que les reduciría tareas en Vedius.
- **Profesional: no se construye** mientras Vedius tenga la agenda. Se construye (55–85 h) cuando firme un cliente que adopte nuestra agenda con más de un profesional: esquema, disponibilidad y lock por profesional, slots, paso en la reserva pública, agenda y tests.
- **Importación CSV:** se reutiliza la de F1 si Vedius exporta.

**Criterio de salida:** usan el timer 4 semanas seguidas y pagan algo.

### F5 — Agente (95–135 h, tras F2)

- Prerrequisitos (15–25 h): credencial de servicio con permisos acotados (hoy hay un solo rol `ADMIN`), idempotencia en reservas, token de cancelación, límite propio para el gateway (el de nginx es 6 req/min por IP).
- Agente con confirmación humana, primero sobre una instancia que use nuestra agenda.
- Con la fisio y sin API de Vedius, el agente solo responde consultas y deriva al link de reserva de Vedius.

**Criterio de salida:** el agente crea solicitudes durante 4 semanas sin incidentes; ningún turno se confirma sin una persona.

**Riesgo:** competir con Sofia de AgendaPro sin un diferencial claro.

---

## Preguntas abiertas (solo se responden con clientes o producción)

**Guille**
- [ ] ¿Qué funciones le faltan en Ágora?
- [ ] ¿Usa el cobro adelantado de Mercado Pago?
- [ ] ¿Los recordatorios le llegan por WhatsApp o por mail?
- [ ] ¿Cuántos turnos tiene por semana?
- [ ] ¿Ágora exporta clientas a CSV o Excel, con qué columnas, y cuántas no tienen email?
- [ ] ¿Tiene logo y colores definidos?
- [ ] ¿Cuánto pagaría, y acepta pagar los dos sistemas durante la transición?

**Clínica de fisioterapia**
- [ ] ¿Vedius tiene API, exportación o importación?
- [ ] ¿Qué tareas manuales exactas les molestan: alta de pacientes, confirmaciones, evoluciones, cobros?
- [ ] ¿Usan el agendamiento online de Vedius?
- [ ] ¿Las rutinas de ejercicios salen de Vedius?
- [ ] ¿El timer lo usan en celular o en pantalla, y cuántos a la vez?
- [ ] ¿Pagarían además de Vedius, y cuánto?
- [ ] ¿Tienen dominio propio?

**Cliente dental**
- [ ] ¿Quién paga hoy? La instancia viva es `neodentalmaster`; `dentalmontecaseros` figura dada de baja.
- [ ] ¿Qué tasa de ausencias tienen hoy?
- [ ] ¿Atiende más de un profesional?

**Producción**
- [ ] ¿Qué SHA corre?
- [ ] ¿Los `CLINIC_*` están definidos en `.env.prod`?
- [ ] ¿Hay emails duplicados al normalizar mayúsculas y espacios? (prerrequisito de `V10`)
- [ ] ¿Cuánta RAM y disco usa el droplet?
- [ ] ¿Están activos los backups del droplet y el cron, y `rclone` contra B2?
- [ ] ¿Llegan las alertas de Kuma?
- [ ] ¿La cuenta de Meta Business está verificada para Twilio?

---

## Completados — decisiones clave

### Sprint 2 — Disponibilidad + Reserva sin login ✅ (2026-05-20)

**Decisiones de diseño que no deben revertirse:**
- La duración de cada cita la define el **servicio** (`duracionMinutos`), no el paciente ni el admin al reservar. El backend calcula slots usando esa duración.
- Al confirmar una cita `PENDIENTE`, el admin puede editar la duración antes de confirmar (flexibilidad clínica). La agenda bloquea la duración final confirmada.
- El **email es obligatorio** en la reserva pública — necesario para confirmaciones y recordatorios. (Desde `V10` se relaja solo en admin e importación.)
- Columna `google_event_id VARCHAR(255) NULL` en tabla `citas` (migración V9) para futura sincronización sin migración en prod.
- `clinic.json` / `docker-entrypoint.sh` / vars `CLINIC_*` del frontend eliminados. `ClinicConfigService` llama a `/api/public/config` — una sola fuente de verdad.

### Reserva pública concurrente ✅ (PR #32–#35, 2026-10-15)

- `SELECT ... FOR UPDATE` sobre `disponibilidad_semanal` como **primera sentencia** de la transacción, antes de cualquier lectura normal (bajo REPEATABLE READ el snapshot se fija en la primera lectura normal).
- Dedupe por email en la reserva pública, sin comparar nombres. La respuesta devuelve el nombre que mandó quien reservó, nunca el guardado.

---

## Backlog posterior (post F5, o antes si un cliente lo pide)

### Agenda polish (~1 día)

- Línea "ahora" roja horizontal cruzando el día actual (estilo Google Calendar)
- Color del bloque por estado: confirmada `--color-primary`, pendiente outline `--color-warning`, cancelada ink-300 tachada
- Tipografía bloque: hora 11px bold / paciente 13px regular / tratamiento 11px muted

### Theming completo (resto del viejo S4, ~1 día)

F1 cubre colores y logo en runtime. Queda la migración de Angular Material prebuilt → `@use mat` con override de tokens MDC (F-1), los tokens base nuevos (F-5) y QA visual en 2 marcas (F-6).

### S5 — Presupuestos + Archivos (~1 semana)

**Presupuestos:**
- Entidad `Presupuesto`: paciente, fecha, descripción libre (texto), monto total, estado (`BORRADOR` / `ENVIADO`)
- CRUD de presupuestos desde la ficha del paciente en el admin
- Sin vinculación obligatoria a tratamientos ni aranceles — descripción libre
- El envío por mail usa el `NotificationService` de F2a

**Archivos clínicos** (diseño en `backend/CLAUDE.md`, no implementado):
- Entidad `ArchivoClinico`: paciente, nombre original, path en disco, tipo MIME, tamaño, fecha subida, referencia opcional a presupuesto
- `FileStorageService` (interfaz) + `LocalFileStorageServiceImpl`: guarda en `/app/uploads/pacientes/{id}/`
- Endpoint `POST /api/admin/pacientes/{id}/archivos` (multipart) + `GET` para servir
- UI: sección "Archivos" en ficha del paciente — upload drag&drop + lista + descarga/borrar

### S7 — Portal paciente opcional (~2-3 semanas)

- Registro/login opcional — el cliente puede "reclamar" su historial de reservas
- Ver citas agendadas (próximas + historial), cancelar (hasta 72h antes), reagendar
- Ver procedimientos realizados (historial clínico resumido)
- JWT con rol PACIENTE + RBAC (`@PreAuthorize` + route guard)
- Refresh tokens con rotación
- JWT en cookie HttpOnly + SameSite=Strict
- Consentimiento explícito de datos (Ley 18.331)

> **Decisión P3-5:** unificar JWT ADMIN + JWT PACIENTE con `roles` claim al implementar este sprint.

### S8 — Balance + Gastos (~1-2 semanas)

- CRUD de gastos con categorías
- Balance mensual: Σ cobros − Σ gastos del período
- Vista resumen en dashboard admin (calculada en backend, no en el cliente)
- Export básico (CSV)

### S9 — Hardening consolidado + Pre-cliente #3 (~2-3 semanas)

**Seguridad P1:**
- **[P1-1] JWT de localStorage → cookie HttpOnly + SameSite=Strict (1-1.5 días).** Habilitar CSRF para endpoints state-changing.
- **[P1-2] Rate limit + lockout en `/auth/login` (2-3 h).** nginx ya limita 5 req/min por IP; falta lockout por cuenta.
- **[P1-3] Paginación `Pageable` en listados (1 día).** Incluye el dashboard, que hoy descarga todo.
- **[P1-4] CSP — Content Security Policy (4-6 h).** Modo `Report-Only` primero.
- **[P1-5] Endurecer file upload (2-3 h).**
- **[P1-6] Audit log de acciones admin (1 día).**
- **[P1-7] Password reset admin** — si no entró en F2a.
- **[P1-8] Logs JSON + correlationId (2-3 h).**
- **[P1-9] Revisar `GET /api/public/config` (30 min).**
- **[P1-10] Spring Actuator en :8081 (30 min).**
- Test de concurrencia de la reserva pública contra MySQL real (Testcontainers) dentro del repo.

**Pre-cliente #3 P2:**
- **[P2-1] E2E happy path con Playwright (1-1.5 días).**
- **[P2-2] Export datos del paciente JSON/CSV (4-6 h).**
- **[P2-3] Soft delete en pacientes y turnos (3-4 h).**
- **[P2-4] Multi-usuario admin — secretaria + profesional (2-3 días).**
- **[P2-5] Healthcheck que valide bundle frontend (2 h).**
- **[P2-6] Script de provisioning (1-2 días).** El checklist `ONBOARDING_CLIENTE.md` sale en F3.
- **[P2-7] SLA documentado (4 h).**
- **[P2-8] Página de status manual (2 h).**

### S-Tests restante

- JUnit + Mockito sobre services con lógica de negocio; `@DataJpaTest` para queries custom
- ESLint + Prettier en frontend; Spotless + Checkstyle en backend
- GitHub Actions: job de tests de frontend en el CI
- Lighthouse, bundle size (1.2MB > budget 1MB), load test (k6) en `/api/public/reservas`

### S11 — Demo + README (1-2 días restantes)

Demo en Render lista. Falta subdominio `demo.turnosuy.com` y README con GIF.

### S12 — SEO multi-subdominio (3-5 días)

- Prerender con `@angular/ssr`, meta tags dinámicos, JSON-LD `LocalBusiness`, `/sitemap.xml` por subdominio, `robots.txt`

### S13 — Observability + rollback (1 semana)

- Micrometer + `actuator/prometheus`
- `scripts/rollback.sh` y tag de imagen por SHA en lugar de `:latest`
- Backup automático pre-deploy en CI/CD
- Notificación de deploy a Telegram/Discord; reglas de Sentry con alertas

### S14 — Google Calendar sync (diferido)

Columna `google_event_id` ya existe (V9).

### Pre-cliente #2+ — infra como código

- Terraform módulo DigitalOcean + Cloudflare
- Prometheus + Grafana (self-hosted)

---

## Decisiones estratégicas pendientes

- **[P3-1] Reevaluar trigger de multi-tenancy real.** Hoy "~5 clientes". Con F1 ya hay dos hosts; medir el costo operativo real antes del cliente #3.
- **[P3-2] Feature flags: env vars → tabla DB.** Se crean como env vars en F3; pasar a tabla al primer pedido de toggle en runtime.
- **[P3-4] Costo por mensaje WhatsApp y modelo "X incluidos/mes".** Antes de F2b.
- **[P3-5] Unificar JWT ADMIN + JWT PACIENTE con `roles` claim.** En S7; F5 necesita al menos un rol de servicio antes.
- **[P3-6] Reservar 20% de cada fase para incidentes/soporte.** Con dos instancias en producción, el roadmap lineal sin buffer es ilusorio.
- **[P3-7] SPF/DKIM/DMARC en onboarding de subdominio.** En F2a.
- **[P3-8] Rotación de secrets documentada (2 h).**
- **[P3-9] Docker secrets en lugar de env vars (1 día a partir de 3+ clientes).**
- **[P3-10] Backup off-site verificado.** Implementado con B2 en cambios locales; se cierra con el restore desde B2 en F0.
- **Vertical bandera:** elegir en 30 días la que primero cierre un segundo cliente pago.

## Fase Premium UX — diferido post primer cliente WhatsApp (antes de landing SaaS)

> **Trigger explícito:** al menos 1 cliente con WhatsApp pagando cuota mensual hace 2+ meses sin churn.

- **Bloque A — Diseño visual profesional:** diseñador UI senior (~USD 800-1500) o plantilla premium; paleta por rubro, ilustraciones para empty states, iconografía custom.
- **Bloque B — Delight & interacciones:** command palette, atajos, autoguardado, undo/redo, shared transitions, empty states con personalidad.
- **Bloque C — Pulido fino:** modo "focus" en agenda, print stylesheets, animaciones de éxito.

## Backlog largo plazo (post 5 clientes activos)

- Multi-tenancy real (schema MySQL por cliente)
- App móvil PWA
- Búsqueda global cross-módulo

## Landing SaaS proveedor ⏳ (repo `consultorio-landing`)

Post primer cliente con caso de éxito. Stack: Astro + Tailwind v4, deploy Cloudflare Pages.
