/**
 * Paciente tal como lo devuelve el backend.
 */
export interface Paciente {
  id: number;
  nombre: string;
  apellido: string;
  telefono: string;
  email: string;
  fecha_nacimiento: string | null;
  created_at?: string;
}

/**
 * Payload para crear o actualizar paciente.
 */
export interface PacientePayload {
  nombre: string;
  apellido: string;
  telefono: string;
  email: string;
  fecha_nacimiento?: string | null;
}
