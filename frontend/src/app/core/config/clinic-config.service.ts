import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { ClinicBranding, ClinicConfig } from './clinic-config.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ClinicConfigService {
  config = signal<ClinicConfig | null>(null);

  constructor(private http: HttpClient) {}

  load(): Observable<ClinicConfig | null> {
    return this.http.get<ClinicConfig>(`${environment.apiUrl}/api/public/config`).pipe(
      tap(cfg => {
        this.config.set(cfg);
        this.applyBranding(cfg?.branding);
      }),
      catchError(() => of(null))
    );
  }

  /**
   * Inyecta los colores de la instancia como CSS vars en <html>. Solo setea lo que viene
   * y es un hex válido (#RRGGBB): sin branding no toca nada y rige el look por defecto
   * de `_design-system.scss`. Deriva el resto de la escala (--brand-navy-*) de primary/accent.
   */
  applyBranding(b: ClinicBranding | undefined | null): void {
    if (!b) return;
    const primary = this.validHex(b.primary);
    const accent = this.validHex(b.accent);
    const hover = this.validHex(b.primary_hover);
    if (!primary && !accent) return;

    const root = document.documentElement;
    const set = (k: string, v: string) => root.style.setProperty(k, v);

    if (primary) {
      set('--brand-primary', primary);
      set('--brand-primary-hover', hover ?? this.mix(primary, '#ffffff', 0.15));
      set('--brand-primary-contrast', this.bestContrast(primary));
      set('--brand-navy-900', this.mix(primary, '#ffffff', 0.08));
      set('--brand-navy-700', this.mix(primary, '#ffffff', 0.25));
    }
    const acc = accent ?? primary;
    if (acc) {
      set('--brand-accent', acc);
      set('--brand-navy-500', this.mix(acc, '#ffffff', 0.15));
      set('--brand-navy-100', this.mix(acc, '#ffffff', 0.85));
      set('--brand-navy-50', this.mix(acc, '#ffffff', 0.95));
    }
  }

  private validHex(v: string | undefined | null): string | null {
    return typeof v === 'string' && /^#[0-9a-fA-F]{6}$/.test(v) ? v : null;
  }

  private rgb(hex: string): number[] {
    return [1, 3, 5].map(i => parseInt(hex.slice(i, i + 2), 16));
  }

  /** Mezcla `hex` con `other`; t = proporción de `other` (0..1). */
  private mix(hex: string, other: string, t: number): string {
    const a = this.rgb(hex);
    const b = this.rgb(other);
    return '#' + a.map((v, i) => Math.round(v * (1 - t) + b[i] * t).toString(16).padStart(2, '0')).join('');
  }

  private luminance(hex: string): number {
    const [r, g, b] = this.rgb(hex).map(v => {
      const c = v / 255;
      return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    });
    return 0.2126 * r + 0.7152 * g + 0.0722 * b;
  }

  /** Blanco o ink-900 según cuál da más contraste (WCAG) sobre `hex`. */
  private bestContrast(hex: string): string {
    const l = this.luminance(hex);
    const white = 1.05 / (l + 0.05);
    const dark = (l + 0.05) / (this.luminance('#0F172A') + 0.05);
    return white >= dark ? '#FFFFFF' : '#0F172A';
  }

  get logoUrl(): string { return this.config()?.branding?.logo_url ?? ''; }
  get name(): string { return this.config()?.nombre ?? ''; }
  get tagline(): string { return this.config()?.tagline ?? ''; }
  get address(): string { return this.config()?.direccion ?? ''; }
  get phone(): string { return this.config()?.telefono ?? ''; }
  get whatsapp(): string { return this.config()?.whatsapp ?? ''; }
  get email(): string { return this.config()?.email ?? ''; }
  get historialLabel(): string { return this.config()?.label_historial || 'Historial'; }
  get hours(): string { return this.config()?.horario ?? ''; }
}
