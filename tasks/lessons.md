# Lessons Learned

Patrones de errores y correcciones. Actualizar después de cada corrección del usuario.

---

## Convenciones Angular

_Ninguna lección registrada aún._

## Convenciones Spring Boot

_Ninguna lección registrada aún._

## Arquitectura y diseño

### [2026-06-11] Imágenes servidas por el backend salen con `no-store`
**Contexto:** se migraron las fotos del hero de imgur a self-hosting en el volumen de uploads, servidas por el backend vía `/api/uploads/**`.
**Error:** Spring Security agrega `Cache-Control: no-cache, no-store` a TODAS las respuestas, así que el navegador re-descargaba cada imagen en cada vista → carrusel "trabado" (mostraba la misma mientras bajaba la siguiente) y fotos lentas. Con imgur no pasaba porque venían de su CDN con caché propia.
**Regla:** las imágenes y assets estáticos se cachean en la capa de proxy (Caddy), no en Spring — mismo criterio que el rate limiting en nginx. Cache-busting = cambiar el nombre del archivo. La caché va acotada a `/api/uploads/*` y assets con hash; NUNCA al JSON de la API (debe seguir `no-store`).

## Workflow y proceso

### [2026-06-11] El repo del server queda "sucio" y rompe el deploy
**Contexto:** se aplicó un cambio al `Caddyfile` directo en el server por `scp` (sin commit). El deploy CI corría `git pull origin main`.
**Error:** un archivo versionado modificado localmente en el server (sin commitear) que también cambia en main hace abortar `git pull` → el deploy falla. El repo del server además arrastraba otros mods locales inertes (`docker-compose.yml`, `frontend/src/*`).
**Regla:** el deploy usa `git fetch + git reset --hard origin/main` (idempotente; `.env.prod` gitignored y `backups/` untracked no se tocan). El server es un checkout pristino de main: todo lo de entorno vive en `.env.prod` y volúmenes. No editar archivos versionados directo en el server.

---

## Formato de entrada

Cuando se registre una lección, usar este formato:

```
### [fecha] Título corto del error
**Contexto:** qué se estaba haciendo
**Error:** qué salió mal o qué dijo el usuario
**Regla:** comportamiento correcto a seguir en el futuro
```
