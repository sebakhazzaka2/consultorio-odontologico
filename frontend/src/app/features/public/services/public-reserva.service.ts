import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface PublicServicio {
  id: number;
  nombre: string;
  descripcion: string | null;
  precio: number;
  foto_url: string | null;
  duracion_minutos: number;
}

export interface ReservaPayload {
  nombre: string;
  apellido: string;
  telefono: string;
  email: string;
  servicio_id: number;
  fecha_hora_inicio: string;
}

export interface ReservaResponse {
  cita_id: number;
  nombre_paciente: string;
  apellido_paciente: string;
  servicio: string;
  fecha_hora_inicio: string;
  duracion_minutos: number;
  estado: string;
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class PublicReservaService {

  constructor(private http: HttpClient) {}

  getServicios(): Observable<PublicServicio[]> {
    return this.http.get<PublicServicio[]>(`${environment.apiUrl}/api/public/servicios`);
  }

  getSlots(fecha: string, servicioId: number): Observable<string[]> {
    return this.http.get<string[]>(`${environment.apiUrl}/api/public/slots`, {
      params: { fecha, servicioId }
    });
  }

  reservar(payload: ReservaPayload): Observable<ReservaResponse> {
    return this.http.post<ReservaResponse>(`${environment.apiUrl}/api/public/reservas`, payload);
  }
}
