import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { HistorialProcedimientos, HistorialProcedimientosPayload } from '../../../core/models/historial.model';

export interface ResultadoHistorial {
  ok: boolean;
  mensaje?: string;
  detalles?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class HistorialService {
  private readonly apiUrl = `${environment.apiUrl}/api/historial-procedimientos`;

  constructor(private http: HttpClient) {}

  private extraerError(err: HttpErrorResponse): ResultadoHistorial {
    const body = err.error as { error?: string; detalles?: string[] } | undefined;
    return {
      ok: false,
      mensaje: body?.error || err.message || 'Error de conexión',
      detalles: body?.detalles
    };
  }

  listarPorPaciente(pacienteId: number): Observable<HistorialProcedimientos[]> {
    return this.http.get<HistorialProcedimientos[]>(`${this.apiUrl}/paciente/${pacienteId}`).pipe(
      catchError((err) => throwError(() => this.extraerError(err)))
    );
  }

  crear(payload: HistorialProcedimientosPayload): Observable<HistorialProcedimientos> {
    return this.http.post<HistorialProcedimientos>(this.apiUrl, payload).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  actualizar(id: number, payload: HistorialProcedimientosPayload): Observable<HistorialProcedimientos> {
    return this.http.put<HistorialProcedimientos>(`${this.apiUrl}/${id}`, payload).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }
}
