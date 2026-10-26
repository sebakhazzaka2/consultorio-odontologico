import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface BajaEstado {
  clinica: string;
  estado: 'ACTIVO' | 'BAJA';
}

@Injectable({ providedIn: 'root' })
export class BajaService {
  private readonly apiUrl = `${environment.apiUrl}/api/public/notificaciones/baja`;

  constructor(private http: HttpClient) {}

  consultar(token: string): Observable<BajaEstado> {
    return this.http.get<BajaEstado>(this.apiUrl, { params: new HttpParams().set('token', token) });
  }

  confirmar(token: string): Observable<void> {
    return this.http.post<void>(this.apiUrl, { token });
  }
}
