import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Meta } from '@angular/platform-browser';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BajaService } from './baja.service';

export type BajaVista = 'cargando' | 'invalido' | 'error' | 'confirmar' | 'ya-baja' | 'listo';

@Component({
  selector: 'app-baja',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './baja.component.html',
  styleUrls: ['./baja.component.scss']
})
export class BajaComponent implements OnInit, OnDestroy {
  vista: BajaVista = 'cargando';
  clinica = '';
  enviando = false;
  errorEnvio: string | null = null;

  private token = '';
  private robotsTag: HTMLMetaElement | null = null;

  constructor(
    private route: ActivatedRoute,
    private bajaService: BajaService,
    private meta: Meta
  ) {}

  ngOnInit(): void {
    this.robotsTag = this.meta.addTag({ name: 'robots', content: 'noindex' }, true);

    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) {
      this.vista = 'invalido';
      return;
    }
    this.bajaService.consultar(this.token).subscribe({
      next: (res) => {
        this.clinica = res.clinica;
        this.vista = res.estado === 'BAJA' ? 'ya-baja' : 'confirmar';
      },
      error: (err: HttpErrorResponse) => {
        this.vista = err.status === 400 ? 'invalido' : 'error';
      }
    });
  }

  ngOnDestroy(): void {
    if (this.robotsTag) {
      this.meta.removeTagElement(this.robotsTag);
      this.robotsTag = null;
    }
  }

  confirmar(): void {
    if (this.enviando) return;
    this.enviando = true;
    this.errorEnvio = null;
    this.bajaService.confirmar(this.token).subscribe({
      next: () => {
        this.enviando = false;
        this.vista = 'listo';
      },
      error: (err: HttpErrorResponse) => {
        this.enviando = false;
        if (err.status === 400) {
          this.vista = 'invalido';
        } else {
          this.errorEnvio = 'No pudimos procesar tu solicitud. Intentá de nuevo en unos minutos.';
        }
      }
    });
  }
}
