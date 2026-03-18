import { Injectable } from '@angular/core';

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

@Injectable({
  providedIn: 'root'
})
export class EmailNotificationService {

  async enviarNotificacionCita(datos: DatosCita): Promise<boolean> {
    console.warn('EmailJS no configurado. Datos:', datos);
    return false;
  }

  async enviarNotificacionCitaBackend(datos: DatosCita): Promise<boolean> {
    return false;
  }
}