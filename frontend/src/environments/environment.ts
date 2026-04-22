/**
 * Configuración de producción / Docker.
 * apiUrl vacío: Nginx proxea /api al backend dentro del compose network.
 */
export const environment = {
  production: true,
  apiUrl: '',
  businessName: 'Nombre del Negocio',
  sentryDsn: 'https://3abe41975179aa8480f1ae7464e005c4@o4511261305208832.ingest.us.sentry.io/4511261321527296'
};
