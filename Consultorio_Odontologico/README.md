# Consultorio Odontológico

Web y API para un consultorio odontológico. Angular 19 + Material en el front, Node + Express + MySQL en el back.

---

## Arquitectura

```
Consultorio_Odontologico/
├── src/                         # Frontend Angular
│   ├── app/
│   │   ├── components/          # Home, Servicios, Registro, Chat, Citas, Pacientes
│   │   ├── models/               # Cita, Paciente
│   │   └── services/             # CitaService, PacienteService, EmailNotification, Chat
│   └── environments/            # apiUrl (dev: localhost:3000)
├── backend/
│   ├── src/
│   │   ├── server.js
│   │   ├── config/db.js
│   │   ├── routes/               # citas.routes, pacientes.routes
│   │   ├── controllers/          # citas.controller, pacientes.controller
│   │   └── validators/           # citas.validator, pacientes.validator
│   └── scripts/
│       └── migracion-pacientes.sql   # Schema de la DB
├── package.json
└── backend/package.json
```

### API

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/citas` | Lista citas |
| GET | `/api/citas/:id` | Una cita |
| POST | `/api/citas` | Crear cita |
| PUT | `/api/citas/:id` | Actualizar cita |
| DELETE | `/api/citas/:id` | Eliminar cita |
| GET | `/api/pacientes` | Lista pacientes |
| GET | `/api/pacientes/:id` | Un paciente |
| POST | `/api/pacientes` | Crear paciente |
| PUT | `/api/pacientes/:id` | Actualizar paciente |
| DELETE | `/api/pacientes/:id` | Eliminar paciente |

Citas usan `paciente_id` (FK a pacientes). Body de cita: `{ paciente_id, fecha, hora, motivo?, estado? }`.

---

## Cómo correr el proyecto

Dos terminales: una para el back, otra para el front.

### 1. Base de datos

MySQL instalado y corriendo. Crear DB y tablas con el script de migración:

```bash
cd backend/scripts
mysql -u root -p consultorio_db < migracion-pacientes.sql
```

O abrir `migracion-pacientes.sql` en MySQL Workbench y ejecutarlo. Crea `pacientes` y `citas` con sus constraints e índices.

### 2. Backend

```bash
cd backend
cp .env.example .env
# Editar .env con tu password de MySQL
npm install
npm run dev
```

Queda en http://localhost:3000

### 3. Frontend

```bash
npm install
npm start
```

Queda en http://localhost:4200

### Tests

Backend:

```bash
cd backend
npm test
```

Prueba que GET / responde bien. Frontend tiene `app.component.spec.ts`; para correrlo hace falta configurar el target test en angular.json (Karma o Jest).

### Resumen

| Dónde | Comando | URL |
|-------|---------|-----|
| Raíz | `npm start` | :4200 |
| backend/ | `npm run dev` | :3000 |
| backend/ | `npm test` | tests |

---

## Configurar la DB

1. Crear la DB: `CREATE DATABASE IF NOT EXISTS consultorio_db;`
2. Ejecutar `backend/scripts/migracion-pacientes.sql` (crea `pacientes` y `citas`)
3. Poner credenciales en `backend/.env`
4. No subir `.env` al repo

---

## .gitignore

Ignora node_modules, .env, dist, logs. Todo bien.

---

## Estado actual

- CRUD citas y pacientes (backend + front)
- Validaciones en back (campos obligatorios, fecha, no doble turno)
- Front: listado, editar, eliminar con confirmación, mensajes de error del back
- Sin auth todavía
