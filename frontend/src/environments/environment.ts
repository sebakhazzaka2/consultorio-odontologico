/**
 * Configuración de producción / Docker.
 * apiUrl vacío: Nginx proxea /api al backend dentro del compose network.
 */
export const environment = {
  production: true,
  apiUrl: '',
  businessName: 'Nombre del Negocio'
};
