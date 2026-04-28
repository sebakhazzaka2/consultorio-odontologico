export interface ClinicFeature {
  titulo: string;
  texto: string;
  icono: string;
}

export interface ClinicConfig {
  nombre: string;
  tagline: string;
  direccion: string;
  ciudad: string;
  horario: string;
  horario_apertura: string;
  horario_cierre: string;
  dias_laborales: string;
  telefono: string;
  whatsapp: string;
  email: string;
  nosotros: string;
  foto_ubicacion_url: string;
  reviews_enabled: boolean;
  stats_pacientes: string;
  stats_anos_experiencia: string;
  stats_calificacion: string;
  hero_imagenes: string[];
  features: ClinicFeature[];
}
