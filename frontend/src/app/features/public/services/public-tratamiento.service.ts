import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PublicTratamiento } from '../models/public-tratamiento.model';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PublicTratamientoService {
  constructor(private readonly http: HttpClient) {}

  getActivos(): Observable<PublicTratamiento[]> {
    return this.http.get<PublicTratamiento[]>(`${environment.apiUrl}/api/public/tratamientos`);
  }
}
