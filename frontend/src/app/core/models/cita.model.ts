export interface Cita {
  id: number;
  paciente_id: number;
  nombre_paciente: string;
  apellido_paciente: string;
  fecha_hora_inicio: string;
  duracion_minutos: number;
  estado: string;
  motivo: string | null;
  notas: string | null;
  created_at?: string;
}

export interface CitaPayload {
  paciente_id: number;
  fecha_hora_inicio: string;
  duracion_minutos: number;
  motivo: string;
  notas?: string | null;
}
