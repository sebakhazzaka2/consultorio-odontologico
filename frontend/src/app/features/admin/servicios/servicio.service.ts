import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { Servicio, ServicioPayload } from '../../../core/models/servicio.model';

export interface ResultadoServicio {
  ok: boolean;
  mensaje?: string;
  detalles?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class ServicioService {
  private readonly apiUrl = `${environment.apiUrl}/api/servicios`;

  constructor(private http: HttpClient) {}

  private extraerError(err: HttpErrorResponse): ResultadoServicio {
    const body = err.error as { error?: string; detalles?: string[] } | undefined;
    return {
      ok: false,
      mensaje: body?.error || err.message || 'Error de conexión',
      detalles: body?.detalles
    };
  }

  listar(): Observable<Servicio[]> {
    return this.http.get<Servicio[]>(this.apiUrl).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  listarActivos(): Observable<Servicio[]> {
    return this.http.get<Servicio[]>(`${this.apiUrl}/activos`).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  obtener(id: number): Observable<Servicio> {
    return this.http.get<Servicio>(`${this.apiUrl}/${id}`).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  crear(payload: ServicioPayload): Observable<Servicio> {
    return this.http.post<Servicio>(this.apiUrl, payload).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  actualizar(id: number, payload: ServicioPayload): Observable<Servicio> {
    return this.http.put<Servicio>(`${this.apiUrl}/${id}`, payload).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  uploadFoto(id: number, file: File): Observable<Servicio> {
    const form = new FormData();
    form.append('foto', file);
    return this.http.patch<Servicio>(`${this.apiUrl}/${id}/foto`, form).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  toggleActivo(id: number): Observable<Servicio> {
    return this.http.patch<Servicio>(`${this.apiUrl}/${id}/toggle`, {}).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }
}
