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

const pageSlide = trigger('pageSlide', [
  transition('* => *', [
    style({ opacity: 0, transform: 'translateY(16px)' }),
    animate('320ms cubic-bezier(0.4, 0, 0.2, 1)', style({ opacity: 1, transform: 'translateY(0)' })),
  ]),
]);

const FALLBACK: ClinicConfig = {
  nombre: '',
  tagline: '',
  direccion: '',
  ciudad: '',
  horario: '',
  telefono: '',
  whatsapp: '',
  email: '',
  nosotros: '',
  foto_ubicacion_url: '',
  reviews_enabled: false,
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
