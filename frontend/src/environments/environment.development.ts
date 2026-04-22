/**
 * Configuración para desarrollo.
 * El backend corre en el puerto 8080 con context-path /api.
 * Los services individuales agregan /api/... a esta URL base.
 */
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  businessName: 'Nombre del Negocio',
  sentryDsn: ''
};
