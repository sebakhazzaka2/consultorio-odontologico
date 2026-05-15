import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';

export interface DisponibilidadSemanal {
  id: number;
  dia_semana: number;
  activo: boolean;
  hora_apertura: string;
  hora_cierre: string;
  pausa_inicio: string | null;
  pausa_fin: string | null;
}

export interface DisponibilidadSemanalPayload {
  dia_semana: number;
  activo: boolean;
  hora_apertura: string;
  hora_cierre: string;
  pausa_inicio: string | null;
  pausa_fin: string | null;
}

export interface FechaBloqueada {
  id: number;
  fecha: string;
  motivo: string | null;
  created_at: string;
}

export interface FechaBloqueadaPayload {
  fecha: string;
  motivo: string | null;
}

interface ApiError {
  ok: false;
  mensaje: string;
  detalles?: string[];
}

@Injectable({ providedIn: 'root' })
export class DisponibilidadService {

  private readonly baseUrl = `${environment.apiUrl}/api/admin/disponibilidad`;

  constructor(private http: HttpClient) {}

  private extraerError(err: HttpErrorResponse): ApiError {
    const body = err.error as { error?: string; message?: string; detalles?: string[] } | undefined;
    return {
      ok: false,
      mensaje: body?.message || body?.error || err.message || 'Error de conexión',
      detalles: body?.detalles
    };
  }

  getSemanal(): Observable<DisponibilidadSemanal[]> {
    return this.http.get<DisponibilidadSemanal[]>(`${this.baseUrl}/semanal`).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  upsertDia(payload: DisponibilidadSemanalPayload): Observable<DisponibilidadSemanal> {
    return this.http.put<DisponibilidadSemanal>(`${this.baseUrl}/semanal`, payload).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  getFechasBloqueadas(): Observable<FechaBloqueada[]> {
    return this.http.get<FechaBloqueada[]>(`${this.baseUrl}/fechas-bloqueadas`).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  bloquearFecha(payload: FechaBloqueadaPayload): Observable<FechaBloqueada> {
    return this.http.post<FechaBloqueada>(`${this.baseUrl}/fechas-bloqueadas`, payload).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  desbloquearFecha(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/fechas-bloqueadas/${id}`).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }
}
