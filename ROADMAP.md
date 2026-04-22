# Roadmap — Consultorio Odontológico

## Secuencia recomendada
```
Deploy real ✅ → post-deploy hardening → MVP2 → WhatsApp → primer cliente → P3C/P4 con feedback real
```

## Estado por fase

| Fase | Estado |
|------|--------|---------------|
| MVP1 — Backend | ✅ Completo (mergeado a main) |
| MVP1 — Frontend | ✅ Completo (mergeado a main) |
| P1 — Hardening pre-producción | ✅ Completo (mergeado a main) |
| P2 — Infraestructura profesional | ✅ Completo (mergeado a main) |
| P3 Fase A — Producto usable | ✅ Completo (mergeado a main) |
| **P3 Fase B — código deploy** | ✅ Completo (mergeado a main) |
| **Página pública clínica** | ✅ Completo (mergeado a main) |
| **Admin polish** | ✅ Completo (mergeado a main) |
| **Deploy real** | ✅ Live en dentalmontecaseros.turnosuy.com (2026-04-21) | 
| **Post-deploy hardening** | ⏳ En curso (`feat/post-deploy-hardening`) | — |
| **MVP2 — Rol paciente** | ⏳ Próximo ciclo grande | — |
| **WhatsApp automatizado** | ⏳ Después de MVP2 | — |
| 🎯 Primer cliente | ⏳ Meta inmediata | — |
| P3 Fase C — Producto premium | ⏳ Con feedback real | — |
| V3 — Historial avanzado + Gastos | ⏳ Futuro | — |
| V4 — Notificaciones email/WhatsApp | ⏳ Futuro | — |
| P4 — Producción hardened | ⏳ Intercalado con V3/V4 | — |
| Landing SaaS proveedor | ⏳ Futuro (repo separado, post primer cliente) | 
| V5 — Multi-tenant SaaS | ⏳ Futuro | — |

---

## Detalle por fase

### P3 Fase B — código ✅ (`feat/production-deploy`, mergeado)
- ✅ CORS, rate limiting, JWT 2h, MySQL aislado, healthchecks, appuser, backup.sh, actuator
- ✅ Flip `push: true` en CI + secrets GHCR — hacer cuando haya server

### Página pública clínica ⏳ (`feat/public-page`)
- ✅`''` → `PublicComponent` — muestra tratamientos activos vía `GET /api/tratamientos`
- ✅Botón "Iniciar sesión" en navbar → `/login`
- ✅Base para MVP2 donde pacientes pedirán turno desde esta misma página

### Admin polish ⏳ (`feat/admin-polish`)
- ✅Login oscuro, sidebar oscuro (`#0F172A`), Inter font
- ✅Dashboard component en `/admin` con cards de turnos/pacientes
- ✅Tablas: íconos de acción, empty states, chips semánticos por estado de cita

### Deploy real ✅ (live 2026-04-21)
- ✅ Hetzner CX-23, Ubuntu 22.04 — `167.235.134.150`
- ✅ `dentalmontecaseros.turnosuy.com` — Caddy + TLS Let's Encrypt
- ✅ Admin seeding desde env vars, nginx proxy fix, CORS configurado

### Post-deploy hardening ⏳ (`feat/post-deploy-hardening`)
- ✅ Cron backup diario 2am — `scripts/backup.sh` + `scripts/restore.sh`
- ✅ Endpoint + UI cambio de password del admin
- ✅ CI flip `push: true` + secrets GHCR
- ✅ Sentry + Uptime monitoring
- ✅ Validación fail-fast de env vars al startup

### MVP2 scope
- Registro y login de pacientes
- Paciente elige slot → cita queda PENDIENTE
- Admin confirma (asigna duración), cancela o reagenda
- HTTPS, rate limiting, passwords fuertes
- `pacientes.user_id` FK ya está en la DB

### P3 Fase C — Producto premium (con feedback real)
- ✅Dashboard avanzado (ingresos, gráfico 30 días)
- Historial UX mejorado (cards, colores por tratamiento)
- Dark mode toggle
- Pageable cuando haya volumen
- Email automático (Fase 1 notificaciones)

### P4 — Producción hardened (intercalado con V3/V4)
- Paginación con `Pageable` en endpoints de listado
- Refresh tokens con rotación + blacklist en DB
- `FileStorageService` + `BackblazeStorageServiceImpl` para attachments (V3)

### Landing SaaS proveedor ⏳ (repo `consultorio-landing`, post primer cliente)
- No urgente — es la página de ventas del software para atraer nuevas clínicas
- Stack: Astro + Tailwind v4, deploy en Cloudflare Pages (gratis)
- Formulario demo vía EmailJS

Auditoría del proyecto y priorización pre/post deploy                                                                         Context                                                       

 Estado: rama feat/Admin-Polish-v2, ROADMAP dice que falta el  
 deploy real (Hetzner + Cloudflare + dominio, requiere pagar). 
  El usuario pide una revisión del estado actual del proyecto, 
  mejoras posibles y una tabla de prioridad antes y después    
 del deploy, identificando qué debería estar y no está.        

 Este documento no modifica código. Es el entregable de        
 planificación: un snapshot de hallazgos más tablas de
 prioridad accionables. Cada ítem listado después puede        
 convertirse en una tarea o PR independiente.

 ---
 Snapshot de estado

 Backend (Spring Boot 3.2.5 + Java 17 + MySQL): CRUD completo  
 para Pacientes, Citas, Tratamientos, Historial y Pagos (Pagos 
  sin PUT). JWT + BCrypt + CORS + validación @Valid
 funcionales. Flyway V1-V4, Actuator habilitado en 8081        
 interno, Dockerfile multi-stage con usuario non-root,
 application-prod.properties con envs externalizados. Tests de 
  integración básicos (~20% cobertura estimada).

 Frontend (Angular 19 + Material): Standalone components,      
 signals, lazy loading, locale es-UY, Design System en
 _design-system.scss con tokens CSS. Auth con JWT en
 localStorage + authInterceptor + authGuard +
 httpErrorInterceptor (401 → logout). Dashboard con stats y    
 gráfico, agenda semanal, detalle paciente con pagos y saldo,  
 página pública con tratamientos activos, búsqueda de
 pacientes, pagos en flujo de historial.

 Infra: docker-compose.prod.yml con MySQL no expuesto, nginx   
 en contenedor frontend con limit_req_zone para login y proxy  
 /api/, actuator bloqueado. CI (.github/workflows/ci.yml)      
 corre tests + build pero push: false (GHCR no activado).      
 scripts/backup.sh existe pero sin cron.

 ---
 Hallazgos críticos (paths de referencia)

 Bloqueantes de deploy

 - Sin HTTPS en el stack: frontend/nginx.conf solo HTTP. No    
 hay reverse proxy con Cloudflare Origin CA ni Let's Encrypt   
 configurado.
 - CI no publica imágenes: .github/workflows/ci.yml tiene      
 push: false. El server Hetzner no podrá pullear nada.
 - Sin runbook de deploy: ni DEPLOY.md ni RUNBOOK.md. El       
 ROADMAP menciona pasos pero no hay secuencia ejecutable.      
 - Backup manual: scripts/backup.sh existe pero sin cron en    
 servidor ni test de restore documentado.
 - Sin reset/cambio de password para admin: si el único        
 usuario olvida la contraseña queda locked-out
 (frontend/src/app/core/auth/auth.service.ts, sin endpoint en  
 AuthController).
 - Sin validación fail-fast de envs: si falta JWT_SECRET o     
 DB_PASSWORD, la app arranca con defaults silenciosos.
 - Sin páginas 404/500 en frontend ni error_page en nginx.     

 Gaps de producto / UX

 - Sin perfil de usuario: el admin no tiene pantalla para      
 cambiar su propia contraseña.
 - Logout sin confirmación: un clic accidental en la toolbar   
 desloguea.
 - JWT en localStorage: riesgo XSS conocido; aceptable para    
 MVP1 admin-only, replantear en MVP2.
 - Sin refresh token: sesión muere a las 2h sin aviso
 (interceptor la cierra, pero no hay renovación).
 - Sin paginación en listados (Pacientes, Citas). OK hoy (1    
 clínica), rompe a escala.
 - Sin índice compuesto citas(paciente_id, fecha_hora_inicio)  
 (consulta de disponibilidad).

 Gaps legales / compliance (Uruguay — Ley 18.331)

 - Sin política de privacidad ni términos en la página pública 
  — obligatorio al manejar datos sensibles de salud.
 - Sin consentimiento de datos al registrar pacientes.
 - Sin auditoría de acceso a historial clínico (quién vio qué  
 y cuándo).

 Gaps de observabilidad

 - Sin error tracking (Sentry o similar).
 - Sin uptime monitoring (Uptime Kuma / UptimeRobot).
 - Sin métricas de app (Micrometer/Prometheus).
 - Sin logs estructurados (JSON con correlationId).

 ---
 Tabla 1 — ANTES del deploy (bloqueantes y alta prioridad)     

 #: 1
 Ítem: Reverse proxy con HTTPS (Caddy o Traefik delante del    
   stack, Cloudflare Origin CA)
 Categoría: Infra
 Criticidad: 🔴 Bloqueante
 Esfuerzo: M
 Razón: No se puede servir un consultorio por HTTP en 2026     
 ────────────────────────────────────────
 #: 2
 Ítem: Flip push: true en CI + secrets GHCR + workflow de      
   deploy (pull + compose up en Hetzner)
 Categoría: CI/CD
 Criticidad: 🔴 Bloqueante
 Esfuerzo: M
 Razón: Sin esto no hay forma repetible de deployar
 ────────────────────────────────────────
 #: 3
 Ítem: DEPLOY.md / RUNBOOK con pasos Hetzner + Cloudflare +    
   dominio + primer deploy
 Categoría: Docs
 Criticidad: 🔴 Bloqueante
 Esfuerzo: S
 Razón: Evita errores la noche del deploy
 ────────────────────────────────────────
 #: 4
 Ítem: Validación fail-fast de envs críticas (JWT_SECRET,      
   DB_PASSWORD, CORS_ALLOWED_ORIGINS) al startup
 Categoría: Backend
 Criticidad: 🔴 Alta
 Esfuerzo: XS
 Razón: Evita prod con defaults inseguros
 ────────────────────────────────────────
 #: 5
 Ítem: Endpoint + UI de cambio de password del admin
   autenticado
 Categoría: Fullstack
 Criticidad: 🔴 Alta
 Esfuerzo: S
 Razón: Sin esto no se puede rotar la contraseña entregada al  
   cliente
 ────────────────────────────────────────
 #: 6
 Ítem: Cron de backup diario + test de restore manual
   documentado
 Categoría: Infra
 Criticidad: 🔴 Alta
 Esfuerzo: S
 Razón: Datos de salud — sin backup funcional no se despliega  
 ────────────────────────────────────────
 #: 7
 Ítem: Página 404 + 500 en Angular y error_page en nginx       
 Categoría: Frontend
 Criticidad: 🟡 Media
 Esfuerzo: XS
 Razón: Rutas inválidas o errores quedan en blanco
 ────────────────────────────────────────
 #: 8
 Ítem: Política de privacidad + términos + aviso legal básico  
   (Ley 18.331)
 Categoría: Legal
 Criticidad: 🔴 Alta
 Esfuerzo: S
 Razón: Obligación legal al tratar datos de salud
 ────────────────────────────────────────
 #: 9
 Ítem: Confirmación de logout + mostrar email del admin en     
   toolbar
 Categoría: UX
 Criticidad: 🟡 Media
 Esfuerzo: XS
 Razón: Evita desloguear por error; mejora claridad
 ────────────────────────────────────────
 #: 10
 Ítem: Título dinámico por ruta (Title service de Angular)     
 Categoría: SEO/UX
 Criticidad: 🟡 Media
 Esfuerzo: XS
 Razón: Hoy todas las pestañas dicen lo mismo
 ────────────────────────────────────────
 #: 11
 Ítem: Índice compuesto citas(paciente_id, fecha_hora_inicio)  
   en nueva migración Flyway
 Categoría: Backend
 Criticidad: 🟡 Media
 Esfuerzo: XS
 Razón: Consulta de disponibilidad se degrada rápido
 ────────────────────────────────────────
 #: 12
 Ítem: Verificar .env.example completo + .env fuera del repo + 

   doc de rotación de secretos
 Categoría: Seguridad
 Criticidad: 🔴 Alta
 Esfuerzo: XS
 Razón: Riesgo de commitear secrets accidental
 ────────────────────────────────────────
 #: 13
 Ítem: robots.txt (allow público, disallow /admin y /api) +    
   favicon revisado
 Categoría: SEO
 Criticidad: 🟢 Baja
 Esfuerzo: XS
 Razón: Higiene básica SEO
 ────────────────────────────────────────
 #: 14
 Ítem: Smoke test manual completo en staging (Hetzner antes de 

   apuntar DNS)
 Categoría: QA
 Criticidad: 🔴 Alta
 Esfuerzo: S
 Razón: Primera vez en prod ≠ primera vez en Docker

 Esfuerzo total aprox pre-deploy: 2–4 días de trabajo
 focalizado.

 ---
 Tabla 2 — DESPUÉS del deploy (Semana 1–2 en producción)       

 #: 1
 Ítem: Sentry (plan free) en Angular y Spring Boot
 Categoría: Observabilidad
 Criticidad: 🔴 Alta
 Esfuerzo: S
 Razón: Detectar errores del cliente sin que él los reporte    
 ────────────────────────────────────────
 #: 2
 Ítem: Uptime Kuma auto-hosteado o UptimeRobot free
 Categoría: Observabilidad
 Criticidad: 🔴 Alta
 Esfuerzo: XS
 Razón: Aviso si el server se cae
 ────────────────────────────────────────
 #: 3
 Ítem: Analytics ligero (Plausible self-hosted o GA4)
 Categoría: Producto
 Criticidad: 🟡 Media
 Esfuerzo: S
 Razón: Saber si entra tráfico a la página pública
 ────────────────────────────────────────
 #: 4
 Ítem: Logs estructurados JSON + correlationId + rotación de   
   logs
 Categoría: Observabilidad
 Criticidad: 🟡 Media
 Esfuerzo: S
 Razón: Debugging serio post primer usuario
 ────────────────────────────────────────
 #: 5
 Ítem: Email transaccional (welcome admin, recordatorio cita)  
 —
   provider gratis tier (Brevo/Resend)
 Categoría: Producto
 Criticidad: 🟡 Media
 Esfuerzo: M
 Razón: Paso hacia MVP2
 ────────────────────────────────────────
 #: 6
 Ítem: Password reset flow con token por email
 Categoría: Seguridad
 Criticidad: 🟡 Media
 Esfuerzo: M
 Razón: Una vez haya email funcionando
 ────────────────────────────────────────
 #: 7
 Ítem: Paginación en PacienteController y CitaController con   
   Pageable + soporte en frontend
 Categoría: Performance
 Criticidad: 🟡 Media
 Esfuerzo: M
 Razón: Cuando crezca el volumen
 ────────────────────────────────────────
 #: 8
 Ítem: Soft delete en Paciente + createdBy/updatedBy
 auditables
 Categoría: Compliance
 Criticidad: 🟡 Media
 Esfuerzo: M
 Razón: Requisito serio de historias clínicas
 ────────────────────────────────────────
 #: 9
 Ítem: Auditoría de acceso al historial clínico (tabla
   audit_log)
 Categoría: Compliance
 Criticidad: 🟡 Media
 Esfuerzo: M
 Razón: Ley 18.331 + buenas prácticas
 ────────────────────────────────────────
 #: 10
 Ítem: Dark mode toggle
 Categoría: UX
 Criticidad: 🟢 Baja
 Esfuerzo: S
 Razón: Ya está el design system; es barato sumarlo
 ────────────────────────────────────────
 #: 11
 Ítem: Export a PDF de historial clínico + impresión de        
 recetas
 Categoría: Producto
 Criticidad: 🟡 Media
 Esfuerzo: M
 Razón: Pedido muy probable del cliente
 ────────────────────────────────────────
 #: 12
 Ítem: Subir cobertura de tests backend a ~50% y frontend a    
   ~40% (componentes clave)
 Categoría: Calidad
 Criticidad: 🟡 Media
 Esfuerzo: M
 Razón: Antes de que MVP2 agregue más superficie
 ────────────────────────────────────────
 #: 13
 Ítem: Meta tags OG dinámicos + structured data JSON-LD en     
   página pública
 Categoría: SEO
 Criticidad: 🟢 Baja
 Esfuerzo: S
 Razón: Google Business Profile + compartir en WhatsApp        

 ---
 Tabla 3 — MVP2 (rol paciente, siguiente ciclo grande)

 ┌─────┬────────────────────────────┬───────────┬──────────┐   
 │  #  │            Ítem            │ Categoría │ Esfuerzo │   
 ├─────┼────────────────────────────┼───────────┼──────────┤   
 │     │ Registro y login de        │           │          │   
 │ 1   │ pacientes (JWT con rol     │ Fullstack │ L        │   
 │     │ PACIENTE)                  │           │          │   
 ├─────┼────────────────────────────┼───────────┼──────────┤   
 │     │ RBAC efectivo              │           │          │   
 │ 2   │ (@PreAuthorize en          │ Seguridad │ M        │   
 │     │ controllers + roleGuard en │           │          │   
 │     │  frontend)                 │           │          │   
 ├─────┼────────────────────────────┼───────────┼──────────┤   
 │     │ Paciente elige slot en la  │           │          │   
 │ 3   │ página pública → cita      │ Producto  │ L        │   
 │     │ PENDIENTE; admin confirma  │           │          │   
 ├─────┼────────────────────────────┼───────────┼──────────┤   
 │ 4   │ Refresh tokens con         │ Seguridad │ M        │   
 │     │ rotación + revocación      │           │          │   
 ├─────┼────────────────────────────┼───────────┼──────────┤   
 │ 5   │ Passwords fuertes + rate   │ Seguridad │ S        │   
 │     │ limit login reforzado      │           │          │   
 ├─────┼────────────────────────────┼───────────┼──────────┤   
 │ 6   │ PWA (manifest + service    │ Frontend  │ M        │   
 │     │ worker + instalable)       │           │          │   
 ├─────┼────────────────────────────┼───────────┼──────────┤   
 │     │ Página "Mi cuenta" del     │           │          │   
 │ 7   │ paciente (ver citas,       │ Frontend  │ M        │   
 │     │ historial resumido)        │           │          │   
 ├─────┼────────────────────────────┼───────────┼──────────┤   
 │ 8   │ Consentimiento explícito   │ Legal     │ S        │   
 │     │ de datos al registrarse    │           │          │   
 └─────┴────────────────────────────┴───────────┴──────────┘   

 ---
 Tabla 4 — Nice-to-have / futuro (V3–V5)

 ┌────────────────────────────────────┬───────────────────┐    
 │                Ítem                │      Cuándo       │    
 ├────────────────────────────────────┼───────────────────┤    
 │ Historial avanzado con fotos       │ V3                │    
 │ (FileStorage + B2/S3)              │                   │    
 ├────────────────────────────────────┼───────────────────┤    
 │ Notificaciones WhatsApp            │ V4                │    
 │ automatizadas                      │                   │    
 ├────────────────────────────────────┼───────────────────┤    
 │ Búsqueda global cross-módulo       │ Cuando moleste no │    
 │                                    │  tenerla          │    
 ├────────────────────────────────────┼───────────────────┤    
 │ Multi-tenant SaaS                  │ V5, post primer   │    
 │                                    │ cliente pagando   │    
 ├────────────────────────────────────┼───────────────────┤    
 │ Landing comercial                  │ Post primer       │    
 │ (consultorio-landing)              │ cliente           │    
 ├────────────────────────────────────┼───────────────────┤    
 │ Terraform/Ansible para Hetzner     │ Cuando haya       │    
 │                                    │ segundo server    │    
 ├────────────────────────────────────┼───────────────────┤    
 │ Grafana + Prometheus dashboards    │ Cuando haya >1    │    
 │                                    │ cliente           │    
 ├────────────────────────────────────┼───────────────────┤    
 │ i18n (es/pt/en)                    │ Cuando expanda de │    
 │                                    │  Uruguay          │    
 └────────────────────────────────────┴───────────────────┘    

 ---
 Secuencia recomendada

 1. Pre-deploy polish (3–5 días): ítems 1–14 de Tabla 1. Foco  
 en HTTPS + CI push + runbook + cambio de password + backup    
 cron + legal básico.
 2. Deploy real: pagar Hetzner + dominio + Cloudflare,
 ejecutar runbook, smoke test, apuntar DNS.
 3. Semana 1 en prod: Sentry + Uptime monitoring + primer      
 backup verificado + observar uso real.
 4. Semana 2–4: paginación, email, password reset, soft        
 delete/auditoría según feedback del cliente.
 5. MVP2: abrir rama feat/patient-portal con los ítems de      
 Tabla 3.

 ---
 Archivos clave para futuras tareas

 - backend/src/main/resources/application-prod.properties —    
 envs y perfiles
 - backend/src/main/resources/db/migration/ — próximas
 migraciones (índices, auditoría, soft delete)
 - backend/src/main/java/.../auth/AuthController.java +        
 JwtService.java — password change, refresh tokens
 - backend/src/main/java/.../config/SecurityConfig.java — RBAC 
  con @PreAuthorize
 - frontend/nginx.conf — reemplazar/ampliar con proxy a Caddy  
 o configurar TLS upstream
 - frontend/src/app/core/auth/auth.service.ts — refresh token, 
  cambio de password
 - frontend/src/app/app.routes.ts — rutas 404, /mi-cuenta,     
 reset password
 - docker-compose.prod.yml — agregar servicio Caddy/Traefik +  
 volumen de certs
 - .github/workflows/ci.yml — flip push: true, nuevo workflow  
 de deploy
 - scripts/backup.sh — cron + script de restore complementario 
 - ROADMAP.md — actualizar estado post-deploy
 - tasks/lessons.md — registrar lo aprendido del deploy real   
 - Nuevo: DEPLOY.md (runbook), PRIVACY.md + TERMS.md (o        
 componentes Angular equivalentes)

 ---
 Verificación del plan

 Este plan es un documento de análisis; no hay "ejecución"     
 única. La verificación consiste en:

 1. El usuario revisa las 4 tablas y confirma/reordena
 prioridades.
 2. Cada ítem bloqueante de la Tabla 1 se convierte en una     
 tarea concreta (rama + PR).
 3. Al completar la Tabla 1, ROADMAP.md se actualiza y arranca 
  el deploy real.
 4. Tras el deploy, se itera sobre Tabla 2 según dolor real    
 observado.

 Preguntas abiertas que pueden refinar esto:
 - ¿Hay deadline del cliente para deploy (urgente vs. con      
 polish)?
 - ¿Se acepta Caddy como reverse proxy (más simple) o
 preferencia por Traefik/Nginx?
 - ¿El cliente ya tiene dominio o se compra ahora?
 - ¿Analytics self-hosted (Plausible) o OK con GA4?