import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { ClinicConfig } from './clinic-config.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ClinicConfigService {
  config = signal<ClinicConfig | null>(null);

  constructor(private http: HttpClient) {}

  load(): Observable<ClinicConfig | null> {
    return this.http.get<ClinicConfig>(`${environment.apiUrl}/api/public/config`).pipe(
      tap(cfg => this.config.set(cfg)),
      catchError(() => of(null))
    );
  }

  get name(): string { return this.config()?.nombre ?? ''; }
  get tagline(): string { return this.config()?.tagline ?? ''; }
  get address(): string { return this.config()?.direccion ?? ''; }
  get phone(): string { return this.config()?.telefono ?? ''; }
  get whatsapp(): string { return this.config()?.whatsapp ?? ''; }
  get email(): string { return this.config()?.email ?? ''; }
  get hours(): string { return this.config()?.horario ?? ''; }
}
