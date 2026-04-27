import { Component, OnInit, computed, effect, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { animate, style, transition, trigger } from '@angular/animations';
import { PublicTratamientoService } from './services/public-tratamiento.service';
import { ClinicConfigService } from './services/clinic-config.service';
import { ReviewsService } from './services/reviews.service';
import { PublicTratamiento } from './models/public-tratamiento.model';
import { ClinicConfig } from './models/clinic-config.model';
import { Review } from './models/review.model';
import { fadeInUp, staggerList } from '../../shared/animations/fade.animations';
import { environment } from '../../../environments/environment';
import { BrandLogoComponent } from '../../shared/components/brand-logo/brand-logo.component';
import { ClinicFeature } from './models/clinic-config.model';

const pageSlide = trigger('pageSlide', [
  transition('* => *', [
    style({ opacity: 0, transform: 'translateY(16px)' }),
    animate('320ms cubic-bezier(0.4, 0, 0.2, 1)', style({ opacity: 1, transform: 'translateY(0)' })),
  ]),
]);

const FALLBACK: ClinicConfig = {
  nombre: '', tagline: '', direccion: '', ciudad: '',
  horario: '', horario_apertura: '', horario_cierre: '', dias_laborales: '',
  telefono: '', whatsapp: '', email: '', nosotros: '', foto_ubicacion_url: '',
  reviews_enabled: false,
  stats_pacientes: '', stats_anos_experiencia: '', stats_calificacion: '',
  features: [],
};

@Component({
  selector: 'app-public',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, MatProgressSpinnerModule, BrandLogoComponent],
  templateUrl: './public.component.html',
  styleUrl: './public.component.scss',
  animations: [fadeInUp, staggerList, pageSlide],
})
export class PublicComponent implements OnInit {
  tratamientos = signal<PublicTratamiento[]>([]);
  tratamientosPage = signal(0);
  readonly tratamientosPageSize = 6;
  loading = signal(true);
  error = signal<string | null>(null);
  mobileMenuOpen = signal(false);
  clinica = signal<ClinicConfig>(FALLBACK);
  reviews = signal<Review[]>([]);
  reviewsLoading = signal(false);
  reviewsPage = signal(0);
  readonly reviewsPageSize = 3;

  readonly visibleTratamientos = computed(() => {
    const start = this.tratamientosPage() * this.tratamientosPageSize;
    return this.tratamientos().slice(start, start + this.tratamientosPageSize);
  });

  readonly canPrevTratamientos = computed(() => this.tratamientosPage() > 0);

  readonly canNextTratamientos = computed(() =>
    (this.tratamientosPage() + 1) * this.tratamientosPageSize < this.tratamientos().length,
  );

  readonly tratamientosTotalPages = computed(() =>
    Math.ceil(this.tratamientos().length / this.tratamientosPageSize),
  );

  readonly tratamientosDots = computed(() =>
    Array.from({ length: this.tratamientosTotalPages() }, (_, i) => i),
  );

  readonly visibleReviews = computed(() => {
    const start = this.reviewsPage() * this.reviewsPageSize;
    return this.reviews().slice(start, start + this.reviewsPageSize);
  });

  readonly canPrevReviews = computed(() => this.reviewsPage() > 0);

  readonly canNextReviews = computed(() =>
    (this.reviewsPage() + 1) * this.reviewsPageSize < this.reviews().length,
  );

  readonly reviewsDots = computed(() =>
    Array.from({ length: Math.ceil(this.reviews().length / this.reviewsPageSize) }, (_, i) => i),
  );

  readonly mapsUrl = computed<SafeResourceUrl>(() => {
    const c = this.clinica();
    const q = encodeURIComponent(`${c.direccion}, ${c.ciudad}`);
    return this.sanitizer.bypassSecurityTrustResourceUrl(
      `https://maps.google.com/maps?q=${q}&output=embed&hl=es&z=16`,
    );
  });

  readonly clinicStatus = computed(() => {
    const c = this.clinica();
    if (!c.horario_apertura || !c.horario_cierre || !c.dias_laborales) {
      return { label: 'Abierto hoy', type: 'open' as const };
    }
    const now = new Date();
    const isoDay = now.getDay() === 0 ? 7 : now.getDay();
    const workDays = c.dias_laborales.split(',').map(d => parseInt(d.trim(), 10));
    if (!workDays.includes(isoDay)) {
      return { label: 'Cerrado hoy', type: 'closed' as const };
    }
    const [oh, om] = c.horario_apertura.split(':').map(Number);
    const [ch, cm] = c.horario_cierre.split(':').map(Number);
    const nowMin = now.getHours() * 60 + now.getMinutes();
    const openMin = oh * 60 + om;
    const closeMin = ch * 60 + cm;
    if (nowMin < openMin) return { label: `Abre a las ${c.horario_apertura}`, type: 'closed' as const };
    if (nowMin >= closeMin) return { label: 'Cerrado', type: 'closed' as const };
    if (closeMin - nowMin <= 30) return { label: `Cierra a las ${c.horario_cierre}`, type: 'closing-soon' as const };
    return { label: 'Abierto ahora', type: 'open' as const };
  });

  featureIconPath(icono: string): string {
    const icons: Record<string, string> = {
      calendario: 'M3 4h18v18H3z M16 2v4 M8 2v4 M3 10h18',
      documento: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z M14 2v6h6 M16 13H8 M16 17H8 M10 9H8',
      usuario: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z',
      tecnologia: 'M12 2L2 7l10 5 10-5-10-5z M2 17l10 5 10-5 M2 12l10 5 10-5',
      estrella: 'M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z',
      corazon: 'M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z',
      escudo: 'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z',
      reloj: 'M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z M12 6v6l4 2',
    };
    return icons[icono] ?? icons['tecnologia'];
  }

  visibleFeatures(): ClinicFeature[] {
    const f = this.clinica().features;
    return f?.length ? f : [];
  }

  readonly currentYear = new Date().getFullYear();
  readonly apiUrl = environment.apiUrl;

  constructor(
    private readonly tratamientoService: PublicTratamientoService,
    private readonly clinicConfigService: ClinicConfigService,
    private readonly reviewsService: ReviewsService,
    private readonly sanitizer: DomSanitizer,
  ) {
    effect(() => {
      document.body.style.overflow = this.mobileMenuOpen() ? 'hidden' : '';
    });
  }

  ngOnInit(): void {
    this.clinicConfigService.getConfig().subscribe({
      next: (config) => {
        this.clinica.set(config);
        if (config.reviews_enabled) {
          this.loadReviews();
        }
      },
      error: () => {},
    });

    this.tratamientoService.getActivos().subscribe({
      next: (t) => {
        this.tratamientos.set(t);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No pudimos cargar los tratamientos.');
        this.loading.set(false);
      },
    });
  }

  prevTratamientos(): void {
    if (this.canPrevTratamientos()) this.tratamientosPage.update(p => p - 1);
  }

  nextTratamientos(): void {
    if (this.canNextTratamientos()) this.tratamientosPage.update(p => p + 1);
  }

  starArray(rating: number): number[] {
    return Array.from({ length: 5 }, (_, i) => i + 1);
  }

  prevReviews(): void {
    if (this.canPrevReviews()) this.reviewsPage.update(p => p - 1);
  }

  nextReviews(): void {
    if (this.canNextReviews()) this.reviewsPage.update(p => p + 1);
  }

  private loadReviews(): void {
    this.reviewsLoading.set(true);
    this.reviewsService.getReviews().subscribe({
      next: (r) => {
        this.reviews.set(r);
        this.reviewsLoading.set(false);
        this.reviewsPage.set(0);
      },
      error: () => this.reviewsLoading.set(false),
    });
  }
}
