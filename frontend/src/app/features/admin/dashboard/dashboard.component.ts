import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import {
  trigger,
  transition,
  style,
  animate,
  query,
  stagger
} from '@angular/animations';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CitaService } from '../citas/cita.service';
import { PacienteService } from '../pacientes/paciente.service';
import { Cita } from '../../../core/models/cita.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  animations: [
    trigger('staggerList', [
      transition('* => *', [
        query('.stat-card', [
          style({ opacity: 0, transform: 'translateY(16px)' }),
          stagger(80, [
            animate('300ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
          ])
        ], { optional: true })
      ])
    ])
  ]
})
export class DashboardComponent implements OnInit {
  turnosHoy = signal<Cita[]>([]);
  proximos5 = signal<Cita[]>([]);
  totalPacientes = signal<number>(0);
  loading = signal(true);

  readonly today = new Date();

  constructor(
    private citaService: CitaService,
    private pacienteService: PacienteService
  ) {}

  ngOnInit(): void {
    forkJoin({
      citas: this.citaService.listarCitas(),
      pacientes: this.pacienteService.listar()
    }).subscribe({
      next: ({ citas, pacientes }) => {
        const ahora = new Date();
        const hoyStr = ahora.toISOString().slice(0, 10); // YYYY-MM-DD

        this.turnosHoy.set(
          citas.filter(c => c.fecha_hora_inicio.startsWith(hoyStr))
        );

        this.proximos5.set(
          citas
            .filter(
              c =>
                (c.estado === 'CONFIRMADA' || c.estado === 'PENDIENTE') &&
                new Date(c.fecha_hora_inicio) > ahora
            )
            .sort((a, b) => a.fecha_hora_inicio.localeCompare(b.fecha_hora_inicio))
            .slice(0, 5)
        );

        this.totalPacientes.set(pacientes.length);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }
}
