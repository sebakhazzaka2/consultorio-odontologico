import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface Mensaje {
  id: number;
  texto: string;
  esUsuario: boolean;
  timestamp: Date;
  nombre?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private mensajesSubject = new BehaviorSubject<Mensaje[]>([]);
  public mensajes$ = this.mensajesSubject.asObservable();

  private mensajesIniciales: Mensaje[] = [
    {
      id: 1,
      texto: '¡Hola! 👋 Bienvenido a nuestro consultorio odontológico. ¿En qué puedo ayudarte hoy?',
      esUsuario: false,
      timestamp: new Date(),
      nombre: 'Consultorio'
    }
  ];

  constructor() {
    this.mensajesSubject.next(this.mensajesIniciales);
  }

  agregarMensaje(texto: string, esUsuario: boolean = true): void {
    const nuevoMensaje: Mensaje = {
      id: Date.now(),
      texto,
      esUsuario,
      timestamp: new Date(),
      nombre: esUsuario ? 'Tú' : 'Consultorio'
    };

    const mensajesActuales = this.mensajesSubject.value;
    this.mensajesSubject.next([...mensajesActuales, nuevoMensaje]);

    // Simular respuesta automática después de 1 segundo
    if (esUsuario) {
      setTimeout(() => {
        this.responderMensaje(texto);
      }, 1000);
    }
  }

  private responderMensaje(mensajeUsuario: string): void {
    const mensajeLower = mensajeUsuario.toLowerCase();
    let respuesta = '';

    if (mensajeLower.includes('horario') || mensajeLower.includes('hora')) {
      respuesta = 'Nuestro horario de atención es de Lunes a Viernes de 9:00 AM a 6:00 PM. ¿Te gustaría agendar una cita?';
    } else if (mensajeLower.includes('precio') || mensajeLower.includes('costo') || mensajeLower.includes('cuanto')) {
      respuesta = 'Los precios varían según el tratamiento. Te recomiendo agendar una consulta para una evaluación personalizada. ¿Quieres que te ayude a agendar?';
    } else if (mensajeLower.includes('cita') || mensajeLower.includes('agendar') || mensajeLower.includes('turno')) {
      respuesta = '¡Perfecto! Puedes agendar tu cita desde el botón "Agendar Cita" en el menú, o puedo ayudarte con más información. ¿Qué servicio te interesa?';
    } else if (mensajeLower.includes('servicio') || mensajeLower.includes('tratamiento')) {
      respuesta = 'Ofrecemos limpieza dental, blanqueamiento, ortodoncia, implantes, endodoncia, odontopediatría, prótesis y cirugía oral. ¿Cuál te interesa?';
    } else if (mensajeLower.includes('direccion') || mensajeLower.includes('ubicacion') || mensajeLower.includes('donde')) {
      respuesta = 'Estamos ubicados en el centro de la ciudad. Puedes contactarnos al (555) 123-4567 o por email a info@consultorio.com para más detalles.';
    } else if (mensajeLower.includes('hola') || mensajeLower.includes('buenos dias') || mensajeLower.includes('buenas tardes')) {
      respuesta = '¡Hola! 😊 ¿En qué puedo ayudarte hoy? Puedo ayudarte con información sobre nuestros servicios, horarios, precios o agendar una cita.';
    } else {
      respuesta = 'Gracias por tu mensaje. Nuestro equipo se pondrá en contacto contigo pronto. Mientras tanto, ¿hay algo más en lo que pueda ayudarte? Puedo ayudarte con información sobre servicios, horarios o agendar una cita.';
    }

    this.agregarMensaje(respuesta, false);
  }

  limpiarChat(): void {
    this.mensajesSubject.next(this.mensajesIniciales);
  }
}
