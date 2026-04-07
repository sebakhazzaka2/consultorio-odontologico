import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { Paciente, PacientePayload } from '../../../core/models/paciente.model';

export interface ResultadoPaciente {
  ok: boolean;
  mensaje?: string;
  detalles?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class PacienteService {
  private readonly apiUrl = `${environment.apiUrl}/api/pacientes`;

  constructor(private http: HttpClient) {}

  private extraerError(err: HttpErrorResponse): ResultadoPaciente {
    const body = err.error as { error?: string; detalles?: string[] } | undefined;
    return {
      ok: false,
      mensaje: body?.error || err.message || 'Error de conexión',
      detalles: body?.detalles
    };
  }

  listar(): Observable<Paciente[]> {
    return this.http.get<Paciente[]>(this.apiUrl).pipe(
      catchError((err) => throwError(() => this.extraerError(err)))
    );
  }

  obtener(id: number): Observable<Paciente> {
    return this.http.get<Paciente>(`${this.apiUrl}/${id}`).pipe(
      catchError((err) => throwError(() => this.extraerError(err)))
    );
  }

  crear(payload: PacientePayload): Observable<ResultadoPaciente & { id?: number }> {
    return this.http.post<{ message?: string; id?: number }>(this.apiUrl, payload).pipe(
      map((res) => ({ ok: true, mensaje: res.message, id: res.id })),
      catchError((err) => throwError(() => this.extraerError(err)))
    );
  }

  actualizar(id: number, payload: PacientePayload): Observable<ResultadoPaciente> {
    return this.http.put<{ message?: string }>(`${this.apiUrl}/${id}`, payload).pipe(
      map((res) => ({ ok: true, mensaje: res.message })),
      catchError((err) => throwError(() => this.extraerError(err)))
    );
  }

  eliminar(id: number): Observable<ResultadoPaciente> {
    return this.http.delete<{ message?: string }>(`${this.apiUrl}/${id}`).pipe(
      map((res) => ({ ok: true, mensaje: res.message })),
      catchError((err) => throwError(() => this.extraerError(err)))
    );
  }
}
