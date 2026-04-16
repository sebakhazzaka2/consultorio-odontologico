# Roadmap — Consultorio Odontológico

## Secuencia recomendada
```
P3B código ✅ → public-page → admin-polish → [pagar] deploy → 🎯 primer cliente → MVP2 → P3C/P4 intercalados con V3-V5
```

## Estado por fase

| Fase | Estado | Requiere pagar |
|------|--------|---------------|
| MVP1 — Backend | ✅ Completo (mergeado a main) | No |
| MVP1 — Frontend | ✅ Completo (mergeado a main) | No |
| P1 — Hardening pre-producción | ✅ Completo (mergeado a main) | No |
| P2 — Infraestructura profesional | ✅ Completo (mergeado a main) | No |
| P3 Fase A — Producto usable | ✅ Completo (mergeado a main) | No |
| **P3 Fase B — código deploy** | ✅ Completo (mergeado a main) | No |
| **Página pública clínica** | ⏳ Próxima (`feat/public-page`) | No |
| **Admin polish** | ⏳ Próximo (`feat/admin-polish`) | No |
| **Deploy real** | ⏳ Al final (Hetzner + Cloudflare + dominio) | **Sí** |
| 🎯 Primer cliente | ⏳ Meta inmediata post-deploy | — |
| MVP2 — Rol paciente | ⏳ Después del primer cliente | — |
| P3 Fase C — Producto premium | ⏳ Con feedback real | — |
| V3 — Historial avanzado + Gastos | ⏳ Futuro | — |
| V4 — Notificaciones email/WhatsApp | ⏳ Futuro | — |
| P4 — Producción hardened | ⏳ Intercalado con V3/V4 | — |
| Landing SaaS proveedor | ⏳ Futuro (repo separado, post primer cliente) | No |
| V5 — Multi-tenant SaaS | ⏳ Futuro | — |

---

## Detalle por fase

### P3 Fase B — código ✅ (`feat/production-deploy`, mergeado)
- ✅ CORS, rate limiting, JWT 2h, MySQL aislado, healthchecks, appuser, backup.sh, actuator
- ⏳ Flip `push: true` en CI + secrets GHCR — hacer cuando haya server

### Página pública clínica ⏳ (`feat/public-page`)
- `''` → `PublicComponent` — muestra tratamientos activos vía `GET /api/tratamientos`
- Botón "Iniciar sesión" en navbar → `/login`
- Base para MVP2 donde pacientes pedirán turno desde esta misma página

### Admin polish ⏳ (`feat/admin-polish`)
- Login oscuro, sidebar oscuro (`#0F172A`), Inter font
- Dashboard component en `/admin` con cards de turnos/pacientes
- Tablas: íconos de acción, empty states, chips semánticos por estado de cita

### Deploy real ⏳ (requiere pagar)
- Comprar dominio + Hetzner CAX11 (€3.79/mes) + Cloudflare Full strict SSL
- Firewall Hetzner: 80, 443, 22 únicamente
- Flip `push: true` en CI, configurar secrets GHCR

### MVP2 scope
- Registro y login de pacientes
- Paciente elige slot → cita queda PENDIENTE
- Admin confirma (asigna duración), cancela o reagenda
- HTTPS, rate limiting, passwords fuertes
- `pacientes.user_id` FK ya está en la DB

### P3 Fase C — Producto premium (con feedback real)
- Dashboard avanzado (ingresos, gráfico 30 días)
- Historial UX mejorado (cards, colores por tratamiento)
- Agenda drag & drop
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
