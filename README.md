# Consultorio Odontológico

Sistema de gestión para consultorios odontológicos. Permite administrar pacientes, turnos, historial clínico, tratamientos y pagos.

## Stack

| Capa | Tecnología |
|------|-----------|
| Backend | Spring Boot 3.2.5 · Java 17 · Spring Security · JWT |
| Frontend | Angular 19 · Angular Material |
| Base de datos | MySQL 8 |

## Arquitectura

```
consultorio-odontologico/
├── backend/          # Spring Boot — API REST en /api, puerto 8080
│   └── src/main/java/com/consultorio/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── model/
│       ├── dto/
│       ├── exception/
│       └── config/
└── frontend/         # Angular 19 — SPA en puerto 4200
    └── src/app/
        ├── core/         # auth (service, interceptor, guard)
        ├── features/     # módulos de funcionalidad (admin layout, ABMs)
        └── shared/       # componentes reutilizables
```

## Funcionalidades (MVP1)

- **Autenticación:** login JWT stateless, rutas protegidas con guard
- **Pacientes:** CRUD completo con validación de email único
- **Citas:** CRUD + confirmar + cancelar + disponibilidad de slots por fecha y duración
- **Historial clínico:** scoped a paciente, snapshot del precio del tratamiento al registrar
- **Tratamientos:** CRUD + toggle activo/inactivo
- **Pagos:** registro + saldo calculado dinámicamente (`Σ historial - Σ pagos`)

## Decisiones de diseño

- **Snapshot/ledger en historial:** `precio_aplicado` se copia del tratamiento al registrar — preserva el precio histórico aunque el tratamiento cambie luego.
- **Saldo dinámico:** nunca almacenado, siempre calculado en consulta.
- **Solapamiento de citas:** valida solo contra CONFIRMADA y PENDIENTE — CANCELADA libera el slot.
- **Fechas:** `Instant` para auditoría (UTC), `LocalDateTime` para negocio. Flyway para migraciones versionadas.
- **Esquema en 3NF**, salvo `precio_aplicado` que es desnormalización intencional.

## Cómo correr el proyecto

### Requisitos

- Java 17+
- Maven 3.8+
- Node.js 20+ / npm
- MySQL 8 corriendo en `localhost:3306`

### Base de datos

```sql
CREATE DATABASE IF NOT EXISTS consultorio_db;
```

Flyway aplica las migraciones automáticamente al iniciar el backend.

### Backend

```bash
cd backend
# Crear src/main/resources/application-local.properties con:
# spring.datasource.password=TU_PASSWORD
# jwt.secret=TU_SECRET_BASE64
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Queda en `http://localhost:8080/api`

### Frontend

```bash
cd frontend
npm install
npm start
```

Queda en `http://localhost:4200`

## Estado

| Fase | Estado |
|------|--------|
| MVP1 Backend | Completo |
| MVP1 Frontend | Completo |
| P1 Hardening (Bean Validation, Logging, Flyway, Tests) | En curso |
| P2 Docker + CI/CD + Paginación | Pendiente |
| MVP2 Rol paciente | Pendiente |
