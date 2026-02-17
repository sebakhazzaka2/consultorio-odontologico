import { Injectable } from '@angular/core';
import emailjs from '@emailjs/browser';
import { DatosCita } from '../models/cita.model';

export type { DatosCita };

@Injectable({
  providedIn: 'root'
})
export class EmailNotificationService {
  // Configuración de EmailJS
  // IMPORTANTE: Necesitas crear una cuenta en https://www.emailjs.com/
  // y reemplazar estos valores con tus credenciales
  private readonly PUBLIC_KEY = 'YOUR_PUBLIC_KEY'; // Reemplazar con tu Public Key
  private readonly SERVICE_ID = 'YOUR_SERVICE_ID'; // Reemplazar con tu Service ID
  private readonly TEMPLATE_ID = 'YOUR_TEMPLATE_ID'; // Reemplazar con tu Template ID
  private readonly EMAIL_DESTINO = 'faacubp.27@hotmail.com';

  private isConfigured = false;

  constructor() {
    // Verificar si EmailJS está configurado
    if (this.PUBLIC_KEY !== 'YOUR_PUBLIC_KEY' && 
        this.SERVICE_ID !== 'YOUR_SERVICE_ID' && 
        this.TEMPLATE_ID !== 'YOUR_TEMPLATE_ID') {
      this.isConfigured = true;
      // Inicializar EmailJS con tu Public Key
      emailjs.init(this.PUBLIC_KEY);
    }
  }

  async enviarNotificacionCita(datos: DatosCita): Promise<boolean> {
    // Si no está configurado, mostrar advertencia en consola
    if (!this.isConfigured) {
      console.warn('EmailJS no está configurado. Por favor, configura tus credenciales en email-notification.service.ts');
      console.log('Datos de la cita que se enviarían:', datos);
      return false;
    }

    try {
      // Obtener nombre del servicio
      const nombreServicio = this.obtenerNombreServicio(parseInt(datos.servicio));
      
      // Formatear fecha
      const fechaFormateada = new Date(datos.fecha).toLocaleDateString('es-ES', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });

      // Preparar el template con los datos
      const templateParams = {
        to_email: this.EMAIL_DESTINO,
        paciente_nombre: `${datos.nombre} ${datos.apellido}`,
        paciente_email: datos.email,
        paciente_telefono: datos.telefono,
        servicio: nombreServicio,
        fecha: fechaFormateada,
        horario: datos.horario,
        motivo: datos.motivo || 'No especificado',
        fecha_raw: datos.fecha
      };

      // Enviar email usando EmailJS
      const response = await emailjs.send(
        this.SERVICE_ID,
        this.TEMPLATE_ID,
        templateParams
      );

      console.log('Email enviado exitosamente:', response);
      return true;
    } catch (error) {
      console.error('Error al enviar email:', error);
      return false;
    }
  }

  private obtenerNombreServicio(id: number): string {
    const servicios: { [key: number]: string } = {
      1: 'Limpieza Dental',
      2: 'Blanqueamiento Dental',
      3: 'Ortodoncia',
      4: 'Implantes Dentales',
      5: 'Endodoncia',
      6: 'Odontopediatría',
      7: 'Prótesis Dentales',
      8: 'Cirugía Oral'
    };
    return servicios[id] || 'Servicio no especificado';
  }

  // Método alternativo usando fetch (si prefieres no usar EmailJS)
  // Requiere un backend para enviar emails de forma segura
  async enviarNotificacionCitaBackend(datos: DatosCita): Promise<boolean> {
    try {
      // Aquí iría la llamada a tu backend
      // const response = await fetch('tu-backend/api/enviar-email', {
      //   method: 'POST',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify({ ...datos, emailDestino: this.EMAIL_DESTINO })
      // });
      // return response.ok;
      return false;
    } catch (error) {
      console.error('Error al enviar email:', error);
      return false;
    }
  }
}
