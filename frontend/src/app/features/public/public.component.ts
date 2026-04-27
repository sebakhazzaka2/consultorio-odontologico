import { Component, ElementRef, OnInit, ViewChild, computed, effect, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PublicTratamientoService } from './services/public-tratamiento.service';
import { ClinicConfigService } from './services/clinic-config.service';
import { ReviewsService } from './services/reviews.service';
import { PublicTratamiento } from './models/public-tratamiento.model';
import { ClinicConfig } from './models/clinic-config.model';
import { Review } from './models/review.model';
import { fadeInUp, staggerList } from '../../shared/animations/fade.animations';
import { environment } from '../../../environments/environment';
import { BrandLogoComponent } from '../../shared/components/brand-logo/brand-logo.component';

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
  animations: [fadeInUp, staggerList],
})
export class PublicComponent implements OnInit {
  tratamientos = signal<PublicTratamiento[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  mobileMenuOpen = signal(false);
  clinica = signal<ClinicConfig>(FALLBACK);
  reviews = signal<Review[]>([]);
  reviewsLoading = signal(false);
  reviewIndex = signal(0);

  @ViewChild('carouselTrack') carouselTrack!: ElementRef<HTMLElement>;

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

  starArray(rating: number): number[] {
    return Array.from({ length: 5 }, (_, i) => i + 1);
  }

  prevReview(): void {
    if (this.reviewIndex() > 0) {
      this.reviewIndex.update(i => i - 1);
      this.scrollCarouselTo(this.reviewIndex());
    }
  }

  nextReview(): void {
    if (this.reviewIndex() < this.reviews().length - 1) {
      this.reviewIndex.update(i => i + 1);
      this.scrollCarouselTo(this.reviewIndex());
    }
  }

  private scrollCarouselTo(index: number): void {
    const track = this.carouselTrack?.nativeElement;
    if (!track) return;
    const card = track.children[index] as HTMLElement;
    card?.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'start' });
  }

  private loadReviews(): void {
    this.reviewsLoading.set(true);
    this.reviewsService.getReviews().subscribe({
      next: (r) => {
        this.reviews.set(r);
        this.reviewsLoading.set(false);
        this.reviewIndex.set(0);
      },
      error: () => this.reviewsLoading.set(false),
    });
  }
}
