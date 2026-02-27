import type { Paciente } from './paciente.model';

/**
 * Cita tal como la devuelve el backend (con paciente anidado).
 */
export interface Cita {
  id: number;
  paciente_id: number;
  fecha: string;
  hora: string;
  motivo: string | null;
  estado: string;
  created_at?: string;
  paciente?: Paciente;
}

/**
 * Payload para crear/actualizar cita en el API.
 */
export interface CitaPayload {
  paciente_id: number;
  fecha: string;
  hora: string;
  motivo?: string | null;
  estado?: string;
}
