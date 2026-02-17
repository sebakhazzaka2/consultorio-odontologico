# Consultorio Odontológico

Sitio web y API para un consultorio odontológico. Frontend en Angular 19 + Angular Material; backend en Node.js + Express con MySQL.

---

## Arquitectura

### Vista general

```
Consultorio_Odontologico/
├── src/                    # Frontend (Angular)
│   ├── app/
│   │   ├── components/     # Home, Servicios, Registro, Chat
│   │   ├── models/         # Interfaces (ej. DatosCita)
│   │   ├── services/       # CitaService, EmailNotificationService, ChatService
│   │   └── app.routes.ts
│   └── environments/       # apiUrl → http://localhost:3000 (dev)
├── backend/                 # API Node.js + Express
│   └── src/
│       ├── server.js       # Entrada, CORS, rutas
│       ├── config/
│       │   └── db.js       # Conexión MySQL (variables de entorno)
│       ├── routes/
│       │   └── citas.routes.js
│       └── controllers/
│           └── citas.controller.js
├── package.json             # Frontend (Angular)
├── backend/package.json     # Backend (Express)
└── README.md
```

### Frontend (Angular 19)

| Capa        | Ubicación           | Descripción                                      |
|------------|---------------------|--------------------------------------------------|
| Vista      | `src/app/components/` | Home, Servicios, Registro, Chat (HTML + SCSS) |
| Modelo     | `src/app/models/`   | Interfaces compartidas (ej. `DatosCita`)          |
| Servicios  | `src/app/services/`| CitaService (HttpClient → API), Email, Chat      |
| Rutas      | `app.routes.ts`     | `/`, `/servicios`, `/registro`                   |
| Config     | `environments/`     | `apiUrl` para el backend (dev: `http://localhost:3000`) |

El frontend llama al backend vía **CitaService** usando `HttpClient` y `environment.apiUrl`.

### Backend (Node.js + Express)

| Capa        | Ubicación                    | Descripción                          |
|------------|------------------------------|--------------------------------------|
| Entrada    | `src/server.js`              | Express, CORS, `express.json()`, rutas |
| Config     | `src/config/db.js`           | Pool MySQL con variables de entorno  |
| Rutas      | `src/routes/citas.routes.js` | GET/POST `/api/citas`                |
| Controlador| `src/controllers/citas.controller.js` | Lógica de citas, acceso a DB   |

**Endpoints:**

- `GET /` — mensaje de estado del API.
- `GET /api/citas` — listar citas (ordenadas por fecha de creación).
- `POST /api/citas` — crear cita. Body: `{ nombre, telefono, email, fecha, hora, motivo }`.  
  (El formulario Angular usa `horario`; al conectar con este endpoint conviene mapear a `hora` en el servicio o aceptar ambos en el backend.)

---

## Cómo correr el proyecto

Se necesitan **dos terminales**: una para el backend y otra para el frontend.

### 1. Base de datos MySQL

- Tener MySQL instalado y el servicio en ejecución.
- Crear la base de datos y la tabla (si aún no existe):

```sql
CREATE DATABASE IF NOT EXISTS consultorio_db;
USE consultorio_db;

CREATE TABLE IF NOT EXISTS citas (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  telefono VARCHAR(20) NOT NULL,
  email VARCHAR(100) NOT NULL,
  fecha DATE NOT NULL,
  hora VARCHAR(10) NOT NULL,
  motivo TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 2. Configurar el backend

En la carpeta del backend, crear el archivo de entorno a partir del ejemplo:

```bash
cd backend
cp .env.example .env
```

Editar `.env` y completar con tus datos de MySQL (no subir `.env` al repositorio):

```env
PORT=3000
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=tu_password_mysql
DB_NAME=consultorio_db
DB_PORT=3306
```

Instalar dependencias y levantar el servidor en modo desarrollo:

```bash
npm install
npm run dev
```

El backend quedará en **http://localhost:3000**. Con nodemon, se reiniciará solo al cambiar archivos.

### 3. Configurar y correr el frontend

En la **raíz del proyecto** (donde está el `package.json` de Angular):

```bash
npm install
npm start
```

El frontend quedará en **http://localhost:4200**. El `environment.development.ts` ya apunta `apiUrl` a `http://localhost:3000`, así que las peticiones de citas irán al backend.

### Resumen rápido

| Dónde        | Comando       | URL              |
|-------------|---------------|------------------|
| Raíz        | `npm start`   | http://localhost:4200 (Angular) |
| backend/    | `npm run dev` | http://localhost:3000 (API)     |

---

## Cómo configurar la base de datos

1. **Crear la base de datos** (una sola vez):  
   `CREATE DATABASE IF NOT EXISTS consultorio_db;`

2. **Crear la tabla `citas`** (una sola vez), con el `CREATE TABLE` del apartado anterior.

3. **Configurar credenciales** en `backend/.env` (`DB_HOST`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`, `DB_PORT`).  
   El archivo `backend/.env.example` indica las variables necesarias.

4. No versionar nunca el archivo `.env` (está en `.gitignore`).

---

## .gitignore

El repositorio ignora de forma correcta:

- **node_modules** (raíz y `backend/`)
- **.env** y variantes (`.env.local`, `*.env`)
- **dist** (salida de build del frontend)
- **logs** y archivos `*.log`
- Carpetas de IDE, caché de Angular, etc.

Así no se suben dependencias, secretos ni artefactos de build.

---

## Estado actual (versión estable)

- Frontend: Angular 19, Material, Reactive Forms, CitaService con HttpClient, environments con `apiUrl`.
- Backend: Express, MySQL con `mysql2`, configuración por variables de entorno, CORS para `http://localhost:4200`.
- Base de datos: `consultorio_db`, tabla `citas`, conexión desde `backend/src/config/db.js`.
- Sin autenticación; listo para usar como base estable y ampliar después.
