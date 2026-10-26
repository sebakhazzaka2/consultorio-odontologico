/**
 * Paciente tal como lo devuelve el backend.
 */
export interface Paciente {
  id: number;
  nombre: string;
  apellido: string;
  telefono: string;
  /** Opcional desde V10: null si el paciente no tiene email. */
  email: string | null;
  fecha_nacimiento: string | null;
  /** false si el paciente se dio de baja de los recordatorios por email. */
  recordatorios_email: boolean;
  created_at?: string;
}

/**
 * Payload para crear o actualizar paciente.
 */
export interface PacientePayload {
  nombre: string;
  apellido: string;
  telefono: string;
  email: string | null;
  fecha_nacimiento?: string | null;
  /** Opcional: si se omite, el backend aplica su valor por defecto (true al crear). */
  recordatorios_email?: boolean;
}
