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
