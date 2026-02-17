import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { DatosCita } from '../models/cita.model';

/**
 * Servicio para gestionar citas contra el backend.
 * Cuando el backend exista, las llamadas se harán a environment.apiUrl.
 * Mientras no haya backend, devuelve éxito simulado para no romper el flujo.
 */
@Injectable({
  providedIn: 'root'
})
export class CitaService {
  private readonly apiUrl = `${environment.apiUrl}/api`;

  constructor(private http: HttpClient) {}

  /**
   * Guarda una cita en el backend.
   * Cuando tengas el API: POST {apiUrl}/api/citas
   */
  guardarCita(datos: DatosCita): Observable<{ ok: boolean; mensaje?: string }> {
    const url = `${this.apiUrl}/citas`;
    return this.http.post<{ ok: boolean; mensaje?: string }>(url, datos).pipe(
      map(() => ({ ok: true })),
      catchError((err) => {
        console.warn('Backend no disponible o error:', err?.message || err);
        // Mientras no haya backend, no fallar: el registro puede seguir usando EmailJS
        return of({ ok: false, mensaje: 'Servicio temporalmente no disponible' });
      })
    );
  }
}
