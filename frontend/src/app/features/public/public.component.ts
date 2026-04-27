import { Component, OnInit, effect, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PublicTratamientoService } from './services/public-tratamiento.service';
import { ClinicConfigService } from './services/clinic-config.service';
import { PublicTratamiento } from './models/public-tratamiento.model';
import { ClinicConfig } from './models/clinic-config.model';
import { fadeInUp, staggerList } from '../../shared/animations/fade.animations';
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

  readonly currentYear = new Date().getFullYear();

  constructor(
    private readonly tratamientoService: PublicTratamientoService,
    private readonly clinicConfigService: ClinicConfigService,
  ) {
    effect(() => {
      document.body.style.overflow = this.mobileMenuOpen() ? 'hidden' : '';
    });
  }

  ngOnInit(): void {
    this.clinicConfigService.getConfig().subscribe({
      next: (config) => this.clinica.set(config),
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
}
