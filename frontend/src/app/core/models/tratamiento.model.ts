export interface Tratamiento {
  id: number;
  nombre: string;
  descripcion: string | null;
  precio: number;
  activo: boolean;
  foto_url: string | null;
  created_at: string;
}

export interface TratamientoPayload {
  nombre: string;
  descripcion?: string;
  precio: number;
  activo?: boolean;
}
