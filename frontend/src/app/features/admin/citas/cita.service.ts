import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { Cita, CitaPayload } from '../../../core/models/cita.model';

export interface RespuestaBackend {
  message?: string;
  error?: string;
  detalles?: string[];
}

export interface ResultadoCita {
  ok: boolean;
  mensaje?: string;
  detalles?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class CitaService {
  private readonly apiUrl = `${environment.apiUrl}/api/citas`;

  constructor(private http: HttpClient) {}

  private extraerMensajeError(err: HttpErrorResponse): ResultadoCita {
    const body = err.error as RespuestaBackend | undefined;
    const mensaje = body?.error || err.message || 'Error de conexión';
    const detalles = body?.detalles;
    return { ok: false, mensaje, detalles };
  }

  listarCitas(): Observable<Cita[]> {
    return this.http.get<Cita[]>(this.apiUrl).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerMensajeError(err)))
    );
  }

  obtenerCita(id: number): Observable<Cita> {
    return this.http.get<Cita>(`${this.apiUrl}/${id}`).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerMensajeError(err)))
    );
  }

  guardarCita(payload: CitaPayload): Observable<ResultadoCita> {
    return this.http.post<{ message?: string }>(this.apiUrl, payload).pipe(
      map((res) => ({ ok: true, mensaje: res.message })),
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerMensajeError(err)))
    );
  }

  actualizarCita(id: number, payload: CitaPayload): Observable<ResultadoCita> {
    return this.http.put<{ message?: string }>(`${this.apiUrl}/${id}`, payload).pipe(
      map((res) => ({ ok: true, mensaje: res.message })),
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerMensajeError(err)))
    );
  }

  eliminarCita(id: number): Observable<ResultadoCita> {
    return this.http.delete<{ message?: string } | null>(`${this.apiUrl}/${id}`).pipe(
      map((res) => ({ ok: true, mensaje: res?.message ?? 'Cita eliminada correctamente' })),
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerMensajeError(err)))
    );
  }

  cancelarCita(id: number): Observable<Cita> {
    return this.http.patch<Cita>(`${this.apiUrl}/${id}/cancelar`, {}).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerMensajeError(err)))
    );
  }
}
