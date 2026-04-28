import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ClinicConfig } from '../models/clinic-config.model';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ClinicConfigService {
  constructor(private readonly http: HttpClient) {}

  getConfig(): Observable<ClinicConfig> {
    return this.http.get<ClinicConfig>(`${environment.apiUrl}/api/public/config`);
  }
}
