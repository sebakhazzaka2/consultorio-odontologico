import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { ClinicConfig } from './clinic-config.model';

@Injectable({ providedIn: 'root' })
export class ClinicConfigService {
  config = signal<ClinicConfig | null>(null);

  constructor(private http: HttpClient) {}

  load(): Observable<ClinicConfig> {
    return this.http.get<ClinicConfig>('/assets/config/clinic.json').pipe(
      tap(cfg => this.config.set(cfg))
    );
  }

  get name(): string { return this.config()?.name ?? ''; }
  get tagline(): string { return this.config()?.tagline ?? ''; }
  get address(): string { return this.config()?.address ?? ''; }
  get phone(): string { return this.config()?.phone ?? ''; }
  get whatsapp(): string { return this.config()?.whatsapp ?? ''; }
  get email(): string { return this.config()?.email ?? ''; }
  get hours(): string { return this.config()?.hours ?? ''; }
}
