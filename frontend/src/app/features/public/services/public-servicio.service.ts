import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PublicServicio } from '../models/public-servicio.model';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PublicServicioService {
  constructor(private readonly http: HttpClient) {}

  getActivos(): Observable<PublicServicio[]> {
    return this.http.get<PublicServicio[]>(`${environment.apiUrl}/api/public/servicios`);
  }
}
