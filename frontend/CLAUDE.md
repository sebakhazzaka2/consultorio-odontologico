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

## Theming dinámico por cliente (Sprint 2.0 — foundations)

> **Status:** planificado para Sprint 2.0, antes de S2. Hasta que esté mergeado, los componentes ya deben consumir `--color-primary` (no `--color-navy-950` directo) para que la migración sea drop-in.

### Arquitectura de dos capas

El sistema separa **paleta del sistema** (estática, vive en `_design-system.scss`) de **brand del cliente** (dinámica, viene de la API):

```
PALETA SISTEMA (estática)     →  BRAND TOKENS (sobrescribibles)  →  ROLES SEMÁNTICOS (consumidos por componentes)
--color-navy-950, --color-ink-*   --brand-primary, --brand-accent     --color-primary, --color-primary-hover, --color-focus-ring
```

**Regla:** los componentes consumen siempre los **roles semánticos** (`--color-primary`, `--color-primary-hover`, `--color-primary-light`, `--color-focus-ring`). Nunca tocan `--brand-*` ni `--color-navy-*` directo. Cuando el cliente cambia de marca, solo los `--brand-*` se sobrescriben en runtime; los roles siguen apuntando a ellos.

### Tokens brand (capa intermedia)

```scss
:root {
  --brand-primary:          #0B1A3A;   // default = navy actual
  --brand-primary-hover:    #1E3A73;   // derivable: shade(primary, -12%)
  --brand-primary-contrast: #FFFFFF;   // derivable: bestContrast WCAG
  --brand-accent:           #3B5BDB;   // fallback: = primary
  --brand-surface-tint:     #F0F4FD;   // derivable: alpha(primary, 6%)
  --brand-focus-ring:       #4C6FE3;   // fallback: = accent

  --color-primary:       var(--brand-primary);
  --color-primary-hover: var(--brand-primary-hover);
  --color-primary-light: var(--brand-accent);
  --color-focus-ring:    var(--brand-focus-ring);
}
```

### Inyección en runtime

`ClinicConfigService.load()` ya corre en `APP_INITIALIZER`. Tras setear el signal, aplica el branding al `<html>`:

```ts
private applyBranding(b: ClinicBranding | undefined): void {
  if (!b) return; // fallback a defaults navy del :root
  const root = document.documentElement;
  const set = (k: string, v?: string) => v && root.style.setProperty(k, v);
  set('--brand-primary', b.primary);
  set('--brand-primary-hover', b.primaryHover ?? this.shade(b.primary, -0.12));
  set('--brand-accent', b.accent ?? b.primary);
  set('--brand-focus-ring', b.accent ?? b.primary);
  set('--brand-surface-tint', b.surfaceTint ?? this.alpha(b.primary, 0.06));
  set('--brand-primary-contrast', this.bestContrast(b.primary));
}
```

Helpers `shade` / `alpha` / `bestContrast` viven en el mismo service (~30 LOC, sin libs externas). `bestContrast` aplica WCAG 2.1 AA para decidir blanco vs `--color-ink-900` como contraste sobre el primary — evita el bug clásico "botón amarillo con texto blanco ilegible".

### Backend

`ClinicProperties` lee `BRAND_PRIMARY`, `BRAND_PRIMARY_HOVER`, `BRAND_ACCENT` del env. Los incluye en `GET /api/public/config` bajo `branding: {...}`. Si ninguna env var está seteada → no manda `branding` → frontend usa defaults. Cero regresiones para instancias existentes.

### Angular Material — opción decidida (P3-3 resuelto)

**Opción elegida (b):** Material con paleta neutra + override de tokens MDC vía CSS vars. Descartada (a) bundle por cliente porque rompe la promesa "un solo build Docker, env vars distintas".

```scss
// styles.scss — reemplaza el @import prebuilt
@use '@angular/material' as mat;

html {
  @include mat.theme((
    color: (primary: mat.$azure-palette, tertiary: mat.$blue-palette),
    typography: Inter,
    density: 0
  ));

  // Tokens MDC → siguen el brand dinámico
  --mdc-filled-button-container-color: var(--color-primary);
  --mdc-filled-button-label-text-color: var(--brand-primary-contrast);
  --mdc-protected-button-container-color: #fff;
  --mat-toolbar-container-background-color: var(--color-primary);
  --mdc-checkbox-selected-icon-color: var(--color-primary);
  --mdc-radio-selected-icon-color: var(--color-primary);
  --mdc-switch-selected-track-color: var(--color-primary);
  --mat-tab-header-active-focus-indicator-color: var(--color-primary);
  --mat-datepicker-calendar-date-selected-state-background-color: var(--color-primary);
  // ...lista completa documentada al implementar S2.0
}
```

Componentes Material a auditar al hacer F-1: button (flat/stroked/raised), form-field outline, datepicker, snackbar, dialog, menu, tab, table, chip, tooltip, paginator, checkbox, radio, switch.

### Tokens nuevos a agregar en S2.0 (F-5)

Faltantes hoy en `_design-system.scss`, necesarios para próximos componentes:

```scss
// GRADIENTES
--gradient-brand:    linear-gradient(135deg, var(--brand-primary) 0%, var(--brand-accent) 100%);
--gradient-surface:  linear-gradient(180deg, #fff 0%, var(--color-surface) 100%);

// SHADOWS brand-aware
--shadow-focus:  0 0 0 3px color-mix(in srgb, var(--brand-focus-ring) 30%, transparent);
--shadow-brand:  0 8px 24px color-mix(in srgb, var(--brand-primary) 25%, transparent);

// MOTION
--ease-out:      cubic-bezier(0.16, 1, 0.3, 1);
--ease-in-out:   cubic-bezier(0.65, 0, 0.35, 1);
--duration-instant: 100ms;
--duration-fast:    150ms;
--duration-base:    250ms;
--duration-slow:    400ms;

// Z-INDEX SCALE
--z-base: 0; --z-dropdown: 1000; --z-sticky: 1100;
--z-overlay: 1200; --z-modal: 1300; --z-toast: 1400; --z-tooltip: 1500;

// LETTER-SPACING
--tracking-tight: -0.02em; --tracking-normal: 0;
--tracking-wide: 0.05em; --tracking-wider: 0.1em;

// SURFACES extra
--surface-elevated: #FFFFFF;
--surface-overlay:  rgba(15, 23, 42, 0.45);
```

`color-mix()` requiere Chrome 111+/Safari 16.2+/Firefox 113+ — alineado con Angular 19.

---

## Polish backlog incremental

> **Regla de aplicación:** estos quick wins NO se hacen en sprint dedicado. Se aplican en el PR del sprint que toca cada zona. La sección está acá para que cuando entres a un sprint, mires qué corresponde meterle de paso.

### Por zona del admin

**Sidebar (con cualquier sprint que toque navegación):**
- Indicador activo: barra izq de 3px en `--brand-accent` + fondo `--brand-surface-tint` + ícono activo coloreado
- Footer con perfil clínica (avatar iniciales + nombre + logout discreto) en lugar de logout flotante
- Backdrop blur (`backdrop-filter: blur(4px)`) al abrir sidenav mobile, cerrar al click fuera

**Dashboard (con S7 Balance, que rediseña stats igual):**
- Jerarquía stats: label uppercase 11px gris / número 36px bold / delta opcional en chip-success
- 1 card "hero" 2-col + 3 cards 1-col en lugar de grilla 4 iguales
- `EmptyStateComponent` real con ilustración SVG en "Por confirmar"

**Tablas / listados (cualquier sprint que toque mat-table):**
- Density 48px: `--mat-table-row-item-container-height: 48px`
- Acciones inline al `:hover` de fila (íconos editar/borrar) en vez de kebab siempre visible
- Header sticky (`position: sticky; top: 0`)
- Hover con `--brand-surface-tint`

**Forms y dialogs (con S3 Presupuestos, que mete dialogs nuevos):**
- Dialogs con header/body/footer separados por borde sutil
- Errores debajo del campo con color + ícono (no solo color — a11y)
- Primary siempre a la derecha, secundario a la izquierda. Auditar consistencia
- Grep y unificar `mat-form-field appearance="outline"` en todos lados

**Agenda (con S2 Disponibilidad, que la toca toda):**
- Línea "ahora" roja horizontal cruzando el día actual (estilo Google Calendar) — ~30 min, alto impacto
- Color del bloque por estado: confirmada `--color-primary`, pendiente outline `--color-warning`, cancelada ink-300 tachada
- Tipografía bloque: hora 11px bold / paciente 13px regular / tratamiento 11px muted

**Estados de carga (transversal, cuando entres a refactorear un listado):**
- `SkeletonComponent` genérico `<app-skeleton variant="text|card|row" />` con shimmer
- Skeletons en shapes predecibles (dashboard, tablas, detalle paciente); spinners solo en acciones

**Micro-animaciones (con M1 al introducir motion tokens):**
- SÍ: fade-in 200ms al cargar rutas, hover lift de cards (existe `.card-hover`), check animado en validación
- NO: bouncy/spring en UI productiva, animaciones >300ms, parallax. Es gestión, no marketing

### Página pública (todo con S2, que rehace flujo de reserva)

**Hero:**
- Tipografía generosa: título 48-56px (clamp), tagline 18-20px, CTA primary grande
- CTA único: "Reservar turno". Secundarios "Llamar" / "WhatsApp" como ghost
- Imagen real con overlay brand (`mix-blend-mode: multiply` + `--brand-primary` 20% alpha) para unificar fotos heterogéneas

**Servicios:**
- Grid 3 col desktop / 1 col mobile. Card: nombre / descripción 2 líneas / precio destacado / botón "Reservar"
- Agrupar por categoría si backend lo soporta (limpieza / estética / urgencias)

**Wizard reserva (S2 producto, no polish):**
- 3 pasos: Servicio → Fecha+Hora → Datos contacto. Progress bar arriba
- Calendario inline + slots como chips (no dropdown)
- Confirmación con resumen + ICS descargable
- Sin login obligatorio

**Mobile (70%+ del tráfico):**
- Sticky bottom bar con CTA "Reservar" mientras scrollea
- Click-to-call y click-to-WhatsApp con íconos grandes (44px touch target)
- Hero recortado a ~70vh para que servicios asomen como señal de scroll

### Esfuerzos estimados (referencia)

| Bucket | Horas | Cuándo |
|---|---|---|
| Foundations (F-1 a F-6) | 13-16 | Sprint 2.0 (dedicado) |
| Quick wins admin (10 items) | ~11 | Incrementales por sprint |
| Tareas medianas (skeletons, dialogs, etc.) | ~20 | Incrementales por sprint |
| Wizard reserva pública | ~8 | Sprint 2 (es feature, no polish) |
| QA visual 2 brands | 4 | Incluido en F-6 + repetir tras cambios grandes |

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

### Admin polish v1 (✅ mergeado a main)
- Login page: fondo oscuro, sidebar oscuro `#0F172A`, Inter font, toolbar `#1E293B`
- Dashboard component en `/admin` — 3 stats (hoy, próximos, pacientes)
- Chips semánticos de estado de cita

### Admin polish v2 (✅ en main)
- `PageHeaderComponent` (`shared/components/page-header`) — header flat con borde inferior, título izq, acción derecha
- `EmptyStateComponent` (`shared/components/empty-state`) — ícono en círculo + título + descripción + ng-content para CTA
- StatusChip: simplificado a usar clases `.chip .chip-{variant}` del design system (sin SCSS propio)
- Citas y Pacientes: `mat-table` en lugar de card-list
- Dashboard: 4 stats, chips en próximos turnos, sección "Por confirmar" (PENDIENTE), accesos rápidos

### Página pública de la clínica (✅ mergeado a main)
- `''` → `PublicComponent` — página pública de la clínica
- Consumir `GET /api/tratamientos` (solo activos) para mostrar carta de servicios con precio
- Botón "Iniciar sesión" visible (navbar o header) → `/login`
- Datos de la clínica: nombre, descripción, horario (hardcodeados en MVP1, configurables en MVP2)
- Diseño coherente con el admin (Inter font, paleta azul/slate)
- MVP2: desde esta página los pacientes podrán elegir turno

{MARCA_SISTEMA} = "TurnosUy"
{MARCA_TAGLINE} = "Simple, integrated clinical system"
{CLINICA_NOMBRE} = "Dental Montecaseros"
{CLINICA_TAGLINE} = "Odontología integral con atención cercana"
{CLINICA_DIRECCION} = "Monte Caseros 2687 a"
{CLINICA_HORARIO} = "Lunes a Viernes 09:00 – 18:00"
{CLINICA_TELEFONO} = "+59899572537"
{CLINICA_WHATSAPP} = "+59899572537"
{CLINICA_EMAIL} = "[EMAIL_ADDRESS]"