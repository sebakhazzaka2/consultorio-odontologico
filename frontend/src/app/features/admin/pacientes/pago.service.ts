import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { Pago, SaldoPaciente } from '../../../core/models/pago.model';

export interface ResultadoPago {
  ok: boolean;
  mensaje?: string;
  detalles?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class PagoService {
  private readonly apiUrl = `${environment.apiUrl}/api/pagos`;
  private readonly pacientesUrl = `${environment.apiUrl}/api/pacientes`;

  constructor(private http: HttpClient) {}

  private extraerError(err: HttpErrorResponse): ResultadoPago {
    const body = err.error as { error?: string; detalles?: string[] } | undefined;
    return {
      ok: false,
      mensaje: body?.error || err.message || 'Error de conexión',
      detalles: body?.detalles
    };
  }

  listarPorPaciente(pacienteId: number): Observable<Pago[]> {
    return this.http.get<Pago[]>(`${this.apiUrl}/paciente/${pacienteId}`).pipe(
      catchError((err) => throwError(() => this.extraerError(err)))
    );
  }

  getSaldo(pacienteId: number): Observable<SaldoPaciente> {
    return this.http.get<SaldoPaciente>(`${this.pacientesUrl}/${pacienteId}/saldo`).pipe(
      catchError((err) => throwError(() => this.extraerError(err)))
    );
  }
}
