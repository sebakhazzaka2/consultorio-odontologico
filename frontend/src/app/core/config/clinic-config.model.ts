export interface ClinicFeature {
  titulo: string;
  texto: string;
  icono: string;
}

/** Branding por instancia (BRAND_*). Todo opcional; sin él rige el look por defecto. */
export interface ClinicBranding {
  primary?: string;
  primary_hover?: string;
  accent?: string;
  logo_url?: string;
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
  hero_eyebrow: string;
  label_servicio: string;
  label_servicio_plural: string;
  label_historial: string;
  features_title: string;
  features_subtitle: string;
  branding?: ClinicBranding;
}
