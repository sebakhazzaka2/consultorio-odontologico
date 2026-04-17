import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PublicTratamientoService } from './services/public-tratamiento.service';
import { PublicTratamiento } from './models/public-tratamiento.model';
import { fadeInUp, staggerList } from '../../shared/animations/fade.animations';

@Component({
  selector: 'app-public',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, MatProgressSpinnerModule],
  templateUrl: './public.component.html',
  styleUrl: './public.component.scss',
  animations: [fadeInUp, staggerList],
})
export class PublicComponent implements OnInit {
  tratamientos = signal<PublicTratamiento[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  mobileMenuOpen = signal(false);

  constructor(private readonly tratamientoService: PublicTratamientoService) {}

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
