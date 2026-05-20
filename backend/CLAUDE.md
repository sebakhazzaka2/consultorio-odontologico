# Backend — Consultorio Odontológico

## Stack
- Spring Boot 3.2.5, Java 17, Spring Security, jjwt 0.12.5
- Puerto: 8080, context path `/api`
- Base de datos: MySQL (`consultorio_db`, `localhost:3306`)

## Estructura
```
backend/src/main/java/com/consultorio/
├── controller/
├── service/
├── repository/
├── model/
├── dto/
├── exception/
└── config/
```

---

## Convenciones obligatorias — NUNCA violar

1. **Constructor injection siempre.** Nunca `@Autowired` en campo.
2. **DTOs con `@JsonNaming(SnakeCaseStrategy.class)`** — serialización automática a snake_case.
3. **Mapping manual en el Service.** Nunca exponer entidades JPA desde el Controller.
4. **Entidades sin lógica de negocio.** Solo getters, setters y anotaciones JPA.
5. **`@Valid` en el parámetro del Controller** para activar validaciones del DTO.
6. **Flag `-parameters` en maven-compiler-plugin** — ya está en pom.xml, no tocar.
7. **Fechas en UTC.** `Instant` para timestamps de auditoría (`created_at`/`updated_at`), `LocalDateTime` para fechas de negocio. En MySQL: `Instant` ↔ `TIMESTAMP`, `LocalDateTime` ↔ `DATETIME`.
8. **`application-local.properties`** para credenciales (gitignored). Variables sensibles: `${DB_PASSWORD}`, `${JWT_SECRET}`.
9. **Path variable constraints:** usar `/{id:[0-9]+}` en controllers para evitar conflictos de routing con endpoints como `/disponibilidad`.

### SOLID aplicado
- **SRP:** Controller solo recibe request y devuelve response. Sin lógica.
- **DIP:** Services dependen de interfaces de Repository (Spring Data las implementa).

---

## Estado MVP1 (✅ mergeado a main)

- **Auth:** registro + login JWT, `JwtAuthFilter` stateless, rutas públicas: `POST /api/auth/login`
- **Pacientes:** CRUD completo, validación email único
- **Citas:** CRUD + `PATCH /cancelar` + `PATCH /confirmar` + `GET /disponibilidad?fecha&duracion`
  - `fechaHoraInicio` como `LocalDateTime` (columna migrada desde `fecha`+`hora` separados)
  - Slots de 15 min, validación solapamiento solo contra CONFIRMADA/PENDIENTE
  - `/{id:[0-9]+}` para evitar conflicto con `/disponibilidad`
- **Historial Clínico:** CRUD, scoped a paciente, `precio_aplicado` copiado del tratamiento al crear (snapshot pattern)
- **Tratamientos:** CRUD + `PATCH /toggle` activo/inactivo
- **Pagos:** POST + DELETE (sin PUT), `GET /pacientes/{id}/saldo` calcula dinámicamente
- **GlobalExceptionHandler:** maneja 404, 400, 422, 500

### P1 — Hardening (✅ mergeado a main)
- **P1.1 Bean Validation** — todos los DTOs tienen `@NotBlank`, `@NotNull`, `@Email`, `@DecimalMin`, etc.
- **P1.2 Logging SLF4J** — todos los services loguean operaciones (INFO) y conflictos de negocio (WARN)
- **P1.3 Flyway** — `flyway-core` + `flyway-mysql`, `ddl-auto=validate`, V1 baseline + V2 fix INT→BIGINT
- **P1.4 Tests** — `@SpringBootTest` + `@AutoConfigureMockMvc` en endpoints críticos

### P3 Fase B — Deploy (✅ mergeado a main)
- CORS desde `${app.cors.allowed-origins}` — `SecurityConfig.java` usa `@Value`, no hardcodeado
- `jwt.expiration=7200000` (2h) en `application-prod.properties`
- Spring Actuator en puerto interno 8081 (`management.server.port=8081`), bloqueado en nginx
- `USER appuser` en `backend/Dockerfile` — container no corre como root

---

## Base de datos

### Tablas
- `users` — admin hardcodeado (MVP2 agrega pacientes)
- `pacientes` — FK nullable `user_id` a `users` (reservado MVP2)
- `citas` — `fecha_hora_inicio DATETIME`, FK a `pacientes`
- `historial_clinico` — FK a `pacientes`, FK nullable a `citas`, FK nullable a `tratamientos`
- `tratamientos`
- `pagos` — FK a `pacientes`

### Normalización
- Esquema en 3NF — verificado, no requiere cambios.
- **`precio_aplicado` en `historial_clinico`** es desnormalización intencional (snapshot/ledger): preserva el precio del tratamiento al momento de registrar.

### Convenciones SQL
- Snake_case en columnas
- `created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP`
- `updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`
- FKs con `ON DELETE RESTRICT` salvo excepción justificada

---

## Decisiones de diseño

- **Snapshot/ledger pattern:** `precio_aplicado` en historial se copia del tratamiento al momento de crear — preserva precio histórico aunque cambie el tratamiento.
- **Saldo dinámico:** nunca almacenado, siempre calculado: `SUM(historial.precio_aplicado) - SUM(pagos.monto)`.
- **Solapamiento de citas:** valida solo contra CONFIRMADA y PENDIENTE — CANCELADA libera el slot.
- **Disponibilidad de slots:** backend genera slots 09:00–17:45 cada 15 min y excluye los ocupados dada una fecha y duración.

### Storage de archivos clínicos (implementado en S3)

**Nombres en disco:** siempre `{uuid}.{ext}`, nunca el nombre original. El nombre original se guarda solo en BD (`nombre_original`). Razones: evita colisiones, caracteres especiales que rompen el filesystem, y path traversal.

**Paths construidos server-side:** el path en disco se arma en `LocalFileStorageServiceImpl` como `pacientes/{pacienteId}/{uuid}.{ext}`. Nunca se acepta un path del cliente.

**Archivos servidos siempre por endpoint autenticado:** `GET /api/admin/archivos/{id}` verifica auth antes de leer el disco y devuelve el contenido como `Resource`. Los archivos nunca son URLs públicas directas — son datos clínicos de pacientes.

**Validación server-side:** verificar MIME type real del contenido (no solo extensión). Whitelist: `image/jpeg`, `image/png`, `image/webp`, `application/pdf`. Límite configurable via `spring.servlet.multipart.max-file-size` (default propuesto: 15 MB).

**Soft delete:** borrar un `ArchivoClinico` marca `deleted_at = now()` en BD. Un cron job borra físicamente los archivos con `deleted_at` de más de 30 días. Previene pérdida por borrado accidental sin complejidad de papelera.

**Backup:** el cron de backup en el servidor debe incluir un `rsync` o `tar` de `/opt/consultorio/uploads` además del dump MySQL. Los archivos **no están en BD** — si no se backupean se pierden independientemente del backup de base de datos.

**Monitorear espacio en disco:** radiografías clínicas pesan 5-20 MB c/u. Con crecimiento real, monitorear uso del volumen de Hetzner.

**Migración a Backblaze B2:** cuando el volumen justifique el costo, escribir `BackblazeStorageServiceImpl` implementando la misma interfaz `FileStorageService`. Script de migración: leer paths de BD, subir archivos a B2, actualizar paths en BD. El resto del código no cambia.

## Decisiones de infraestructura

- **Hetzner sobre DigitalOcean:** CAX11 ARM €3.79/mes vs DO Basic $6/mes. 12-factor compatible → migración a AWS straightforward cuando corresponda.
- **Rate limiting en Nginx, no en Spring:** pertenece a la capa de infraestructura (SRP arquitectónico). `limit_req_zone` en `nginx.conf` es cero cambios en la lógica de negocio.
- **JWT expiration corta (≤ 2h) como mitigante de revocación:** sin blacklist ni refresh tokens (MVP1, un solo admin). Refresh tokens con rotación va en MVP2.
- **MySQL nunca expuesto en `ports` en docker-compose.prod.yml:** solo accesible desde la Docker network interna.
- **FileStorageService interface (Strategy Pattern):** `LocalFileStorageServiceImpl` ahora (sin costo). Migrar a `BackblazeStorageServiceImpl` (Backblaze B2, API S3-compatible) cuando el volumen de archivos justifique el gasto. El código que usa la interfaz no cambia. Ver decisiones de diseño de storage más abajo.
- **DB por tenant para multi-tenant (V5):** un schema MySQL diferente por cliente — más simple, seguro y fácil de migrar que Row-Level Security compartida.
