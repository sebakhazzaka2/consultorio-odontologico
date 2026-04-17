import { Component, OnInit, effect, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PublicTratamientoService } from './services/public-tratamiento.service';
import { PublicTratamiento } from './models/public-tratamiento.model';
import { fadeInUp, staggerList } from '../../shared/animations/fade.animations';
import { CLINIC_CONFIG } from './clinic.config';
import { BrandLogoComponent } from '../../shared/components/brand-logo/brand-logo.component';

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

  readonly clinica = CLINIC_CONFIG;
  readonly currentYear = new Date().getFullYear();

  constructor(private readonly tratamientoService: PublicTratamientoService) {
    effect(() => {
      document.body.style.overflow = this.mobileMenuOpen() ? 'hidden' : '';
    });
  }

  ngOnInit(): void {
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
}
