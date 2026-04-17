# Frontend — Consultorio Odontológico

## Stack
- Angular 19 + Angular Material
- `environment.apiUrl` = `http://localhost:8080` (los services agregan `/api/...`)

## Estructura
```
frontend/src/app/
├── core/         # auth (service, interceptor, guard)
├── features/     # admin layout + feature modules
└── shared/       # componentes reutilizables
```

---

## Convenciones obligatorias — NUNCA violar

1. **Standalone components siempre.** Sin NgModules.
2. **Constructor injection.** No usar `inject()` function.
3. **Imports explícitos** en cada componente standalone.
4. **Tipos explícitos siempre.** Sin `any`.
5. **Locale es-UY** configurado globalmente en `app.config.ts`.
6. **Fechas mostradas en dd/MM/yyyy** usando DatePipe.
7. **`environment.apiUrl`** = `http://localhost:8080`, los services agregan `/api/...`.

---

## Design System — estándar obligatorio

**Fuente única:** `src/styles/_design-system.scss`  
Todos los tokens están como CSS custom properties en `:root`. Nunca hardcodear colores, fuentes, spacing ni radios directamente en componentes.

### Colores — roles semánticos (usar estos, no la paleta directa)

| Variable | Valor | Rol |
|---|---|---|
| `--color-primary` | navy-950 `#0B1A3A` | Botón fill, acción principal |
| `--color-primary-hover` | navy-800 `#1E3A73` | Hover de primary |
| `--color-primary-light` | navy-600 `#3B5BDB` | Nav activo, links, accents |
| `--color-secondary` | ink-700 `#334155` | Acción secundaria |
| `--color-focus-ring` | navy-500 `#4C6FE3` | Outline accesibilidad |
| `--color-bg` | `#FFFFFF` | Fondo de página |
| `--color-surface` | `#F8FAFC` | Fondo content area admin |
| `--color-surface-alt` | `#F1F5F9` | Hover filas, fondos alternativos |
| `--color-border` | `#E2E8F0` | Bordes normales |
| `--color-border-strong` | `#CBD5E1` | Bordes enfatizados |
| `--color-text` | ink-900 `#0F172A` | Texto principal |
| `--color-text-muted` | ink-500 `#64748B` | Texto secundario, labels |
| `--color-success` | `#059669` | Estado positivo |
| `--color-warning` | `#D97706` | Estado precaución |
| `--color-danger` | `#DC2626` | Estado error/negativo |
| `--color-info` | `#2563EB` | Estado informativo |

Paleta base disponible: `--color-navy-{50,100,500,600,700,800,900,950}` y `--color-ink-{50,100,200,300,500,700,900}`.

### Tipografía

- **Font:** siempre `var(--font-family)` → Inter + system fallbacks
- **Tamaños:** `--font-size-{xs,sm,base,lg,xl,2xl,3xl,4xl}` (12→40px)
- **Pesos:** `--font-weight-{normal,medium,semibold,bold}` (400/500/600/700)
- **Line-height:** `--line-height-{tight,snug,base,relaxed}` (1.2/1.4/1.6/1.8)

### Espaciado

Escala de 4px: `--space-{1,2,3,4,5,6,7,8,10,12,16}` = 4/8/12/16/20/24/28/32/40/48/64 px.  
No usar valores de px arbitrarios — siempre `var(--space-N)`.

### Bordes y sombras

- Radios: `--radius-{sm,md,lg,xl,full}` → 6/10/14/20/9999 px
- Sombras: `--shadow-{sm,md,lg,xl}` — usar en ese orden de elevación
- Transiciones: `--transition-{fast,base,slow}` → 150/250/400 ms

### Componentes predefinidos

| Clase | Uso |
|---|---|
| `.btn .btn-primary` | Botón fill principal |
| `.btn .btn-secondary` | Botón outline |
| `.btn .btn-ghost` | Botón fantasma |
| `.btn .btn-danger` | Acción destructiva |
| `.btn.btn-sm` / `.btn.btn-lg` | Variantes de tamaño |
| `.btn.btn-full` | Ancho completo |
| `.card` | Contenedor base |
| `.card.card-hover` | Card con hover elevado |
| `.card.card-stat` | Card para estadísticas |
| `.chip .chip-{success,warning,danger,info,neutral}` | Badge de estado |

Para **inputs** usar siempre `mat-form-field` con `appearance="outline"`. Los overrides de color ya están en el design system.

Para **botones en Material** usar `mat-flat-button` (primary fill) o `mat-stroked-button` (secondary); los colores se alinean vía el theme de Material + las variables CSS.

### Regla de aplicación

> Cada nuevo componente DEBE usar variables del design system. Si necesitás un valor que no existe, primero evaluá si encaja en la escala existente. Si realmente es nuevo, agregalo a `_design-system.scss` con comentario, no lo pongas inline en el componente.

---

## Estado MVP1 (✅ mergeado a main)

- **Auth:** `AuthService`, `authInterceptor` (functional), `authGuard` (functional), `LoginComponent`
- **Admin layout:** `AdminLayoutComponent` con sidenav Material, lazy loading
- **ABM Pacientes:** listado + form dialog + confirmar borrado + vista detalle
- **ABM Citas:** listado con chips de estado + filtro fecha (MatDatepicker) + form + cancelar + reagendar
  - Selector de horarios disponibles: elige fecha → duración → carga slots libres del backend
  - Citas CANCELADAS: solo botón reagendar. CONFIRMADAS: editar + cancelar
- **ABM Tratamientos:** tabla Material + toggle activo + form dialog
- **Detalle Paciente:** carga en paralelo con `forkJoin` — datos paciente + historial + pagos + saldo
- **Historial Clínico:** dentro de detalle paciente, selector de tratamientos activos autocompleta precio
- **Pagos:** dentro de detalle paciente, sin edición (refleja limitación del backend)

### P3 Fase A — UX (✅ mergeado a main)
- Vista semanal de citas con bloques horarios (`feat/agenda-calendar`)
- Interceptor global errores HTTP + MatSnackBar (`feat/ux-essentials`)
- Loading states/spinners en todas las operaciones async
- Responsive mobile-first (sidebar colapsable, tablas responsive)
- Saldo adeudado visible en listado pacientes

### Admin polish (⏳ rama `feat/admin-polish`)
- Login page: fondo oscuro (slate-900→blue-900), card blanca, Inter font
- Admin sidebar: fondo `#0F172A` (slate-900), nav items blancos, highlight azul activo
- Admin toolbar: `#1E293B` reemplaza indigo genérico de Material
- Inter font globalmente en el admin
- Dashboard component en `/admin` (reemplaza redirect a `/admin/agenda`)
  - Cards: turnos hoy, próximos 5 turnos, total pacientes
  - APIs: `GET /api/citas?fecha=hoy`, `GET /api/pacientes`
- Tablas: íconos en columna acciones (`edit`, `delete`, `visibility`), empty states con CTA, chips semánticos (PENDIENTE=yellow, CONFIRMADA=green, CANCELADA=gray)

### Página pública de la clínica (⏳ rama `feat/public-page`)
- `''` → `PublicComponent` — página pública de la clínica
- Consumir `GET /api/tratamientos` (solo activos) para mostrar carta de servicios con precio
- Botón "Iniciar sesión" visible (navbar o header) → `/login`
- Datos de la clínica: nombre, descripción, horario (hardcodeados en MVP1, configurables en MVP2)
- Diseño coherente con el admin (Inter font, paleta azul/slate)
- MVP2: desde esta página los pacientes podrán elegir turno

{MARCA_SISTEMA} = "NexaClinic"
{MARCA_TAGLINE} = "Simple, integrated clinical system"
{CLINICA_NOMBRE} = "Dental Montecaseros"
{CLINICA_TAGLINE} = "Odontología integral con atención cercana"
{CLINICA_DIRECCION} = "Monte Caseros 2687 a"
{CLINICA_HORARIO} = "Lunes a Viernes 09:00 – 18:00"
{CLINICA_TELEFONO} = "+59899572537"
{CLINICA_WHATSAPP} = "+59899572537"
{CLINICA_EMAIL} = "[EMAIL_ADDRESS]"