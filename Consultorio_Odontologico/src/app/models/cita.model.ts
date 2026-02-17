/**
 * Modelo de cita para reutilizar en formularios y servicios.
 */
export interface DatosCita {
  nombre: string;
  apellido: string;
  email: string;
  telefono: string;
  servicio: string;
  fecha: string;
  horario: string;
  motivo?: string;
}
