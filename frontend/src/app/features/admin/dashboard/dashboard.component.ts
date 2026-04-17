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
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { CitaService } from '../citas/cita.service';
import { PacienteService } from '../pacientes/paciente.service';
import { Cita } from '../../../core/models/cita.model';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule, MatProgressSpinnerModule, MatButtonModule, StatusChipComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  animations: [
    trigger('staggerList', [
      transition(':enter', [
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
  citasPendientes = signal<Cita[]>([]);
  totalPacientes = signal<number>(0);
  estaSemana = signal<number>(0);
  canceladasMes = signal<number>(0);
  completadasMes = signal<number>(0);
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
        const hoyStr = ahora.toISOString().slice(0, 10);

        // Inicio y fin de la semana actual (lunes–domingo)
        const lunes = new Date(ahora);
        lunes.setDate(ahora.getDate() - ((ahora.getDay() + 6) % 7));
        lunes.setHours(0, 0, 0, 0);
        const domingo = new Date(lunes);
        domingo.setDate(lunes.getDate() + 6);
        domingo.setHours(23, 59, 59, 999);

        // Inicio y fin del mes actual
        const inicioMes = new Date(ahora.getFullYear(), ahora.getMonth(), 1);
        const finMes = new Date(ahora.getFullYear(), ahora.getMonth() + 1, 0, 23, 59, 59, 999);

        this.turnosHoy.set(
          citas
            .filter(c => c.fecha_hora_inicio.startsWith(hoyStr))
            .sort((a, b) => a.fecha_hora_inicio.localeCompare(b.fecha_hora_inicio))
        );

        this.estaSemana.set(
          citas.filter(c => {
            const f = new Date(c.fecha_hora_inicio);
            return (c.estado === 'CONFIRMADA' || c.estado === 'PENDIENTE') && f >= lunes && f <= domingo;
          }).length
        );

        const futuras = citas.filter(
          c => (c.estado === 'CONFIRMADA' || c.estado === 'PENDIENTE') &&
               new Date(c.fecha_hora_inicio) > ahora
        ).sort((a, b) => a.fecha_hora_inicio.localeCompare(b.fecha_hora_inicio));

        this.proximos5.set(futuras.slice(0, 5));

        this.citasPendientes.set(
          futuras.filter(c => c.estado === 'PENDIENTE').slice(0, 5)
        );

        this.canceladasMes.set(
          citas.filter(c => {
            const f = new Date(c.fecha_hora_inicio);
            return c.estado === 'CANCELADA' && f >= inicioMes && f <= finMes;
          }).length
        );

        this.completadasMes.set(
          citas.filter(c => {
            const f = new Date(c.fecha_hora_inicio);
            return c.estado === 'COMPLETADA' && f >= inicioMes && f <= finMes;
          }).length
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
