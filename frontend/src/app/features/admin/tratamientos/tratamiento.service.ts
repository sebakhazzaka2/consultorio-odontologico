import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { Tratamiento, TratamientoPayload } from '../../../core/models/tratamiento.model';

export interface ResultadoTratamiento {
  ok: boolean;
  mensaje?: string;
  detalles?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class TratamientoService {
  private readonly apiUrl = `${environment.apiUrl}/api/tratamientos`;

  constructor(private http: HttpClient) {}

  private extraerError(err: HttpErrorResponse): ResultadoTratamiento {
    const body = err.error as { error?: string; detalles?: string[] } | undefined;
    return {
      ok: false,
      mensaje: body?.error || err.message || 'Error de conexión',
      detalles: body?.detalles
    };
  }

  listar(): Observable<Tratamiento[]> {
    return this.http.get<Tratamiento[]>(this.apiUrl).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  listarActivos(): Observable<Tratamiento[]> {
    return this.http.get<Tratamiento[]>(`${this.apiUrl}/activos`).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  obtener(id: number): Observable<Tratamiento> {
    return this.http.get<Tratamiento>(`${this.apiUrl}/${id}`).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  crear(payload: TratamientoPayload): Observable<Tratamiento> {
    return this.http.post<Tratamiento>(this.apiUrl, payload).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  actualizar(id: number, payload: TratamientoPayload): Observable<Tratamiento> {
    return this.http.put<Tratamiento>(`${this.apiUrl}/${id}`, payload).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  uploadFoto(id: number, file: File): Observable<Tratamiento> {
    const form = new FormData();
    form.append('foto', file);
    return this.http.patch<Tratamiento>(`${this.apiUrl}/${id}/foto`, form).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  toggleActivo(id: number): Observable<Tratamiento> {
    return this.http.patch<Tratamiento>(`${this.apiUrl}/${id}/toggle`, {}).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.extraerError(err)))
    );
  }
}
