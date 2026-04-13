export interface HistorialClinico {
  id: number;
  paciente_id: number;
  nombre_paciente: string;
  apellido_paciente: string;
  cita_id: number | null;
  fecha_hora: string;
  procedimiento: string;
  notas: string | null;
  tratamiento_id: number | null;
  nombre_tratamiento: string | null;
  precio_aplicado: number | null;
  foto_url: string | null;
  created_at: string;
}

export interface HistorialPayload {
  paciente_id: number;
  cita_id?: number | null;
  fecha_hora: string;
  procedimiento: string;
  notas?: string | null;
  tratamiento_id?: number | null;
  precio_aplicado?: number | null;
}
