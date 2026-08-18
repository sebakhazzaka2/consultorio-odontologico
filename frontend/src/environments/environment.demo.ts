/**
 * Configuración para la instancia demo en Render (portfolio).
 * apiUrl apunta al backend Render como origen distinto (no hay Nginx/Caddy
 * proxeando /api en el mismo dominio como en producción real).
 * Reemplazar por la URL real asignada por Render al servicio backend.
 */
export const environment = {
  production: true,
  apiUrl: 'https://consultorio-demo-backend.onrender.com',
  businessName: 'Consultorio Demo',
  sentryDsn: ''
};
