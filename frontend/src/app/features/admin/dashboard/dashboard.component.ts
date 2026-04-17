import { Component, OnInit, signal } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
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
import { PagoService } from '../pacientes/pago.service';
import { Cita } from '../../../core/models/cita.model';
import { Pago } from '../../../core/models/pago.model';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, RouterLink, MatIconModule, MatProgressSpinnerModule, MatButtonModule, StatusChipComponent, BaseChartDirective],
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
  ingresosHoy = signal<number>(0);
  ingresosSemana = signal<number>(0);
  ingresosMes = signal<number>(0);
  periodoFinanciero = signal<'dia' | 'semana' | 'mes'>('mes');
  loading = signal(true);

  chartData: ChartData<'bar'> = { labels: [], datasets: [] };
  readonly barLabelsPlugin = {
    id: 'barLabels',
    afterDatasetDraw(chart: any): void {
      const { ctx } = chart;
      chart.getDatasetMeta(0).data.forEach((bar: any, index: number) => {
        const value = chart.data.datasets[0].data[index] as number;
        if (value < 1) return;
        const barHeight = bar.base - bar.y;
        if (barHeight < 18) return;
        ctx.save();
        ctx.fillStyle = '#ffffff';
        ctx.font = `bold 12px Inter, sans-serif`;
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(String(value), bar.x, bar.y + barHeight / 2);
        ctx.restore();
      });
    }
  };

  chartData: ChartData<'bar'> = { labels: [], datasets: [] };
  chartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false }, tooltip: { callbacks: {
      label: ctx => ` ${ctx.parsed.y} turno${ctx.parsed.y !== 1 ? 's' : ''}`
    }}},
    scales: {
      x: { grid: { display: false }, ticks: { font: { family: 'Inter, sans-serif', size: 11 }, color: '#64748B' } },
      y: { beginAtZero: true, ticks: { stepSize: 1, font: { family: 'Inter, sans-serif', size: 11 }, color: '#64748B' }, grid: { color: '#E2E8F0' } }
    }
  };

  readonly today = new Date();

  get ingresosPeriodo(): number {
    switch (this.periodoFinanciero()) {
      case 'dia':    return this.ingresosHoy();
      case 'semana': return this.ingresosSemana();
      case 'mes':    return this.ingresosMes();
    }
  }

  constructor(
    private citaService: CitaService,
    private pacienteService: PacienteService,
    private pagoService: PagoService
  ) {}

  ngOnInit(): void {
    forkJoin({
      citas: this.citaService.listarCitas(),
      pacientes: this.pacienteService.listar(),
      pagos: this.pagoService.listarTodos()
    }).subscribe({
      next: ({ citas, pacientes, pagos }) => {
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

        const sumar = (lista: Pago[]) => lista.reduce((acc, p) => acc + p.monto, 0);
        this.ingresosHoy.set(sumar(pagos.filter(p => p.fecha.startsWith(hoyStr))));
        this.ingresosSemana.set(sumar(pagos.filter(p => {
          const f = new Date(p.fecha);
          return f >= lunes && f <= domingo;
        })));
        this.ingresosMes.set(sumar(pagos.filter(p => {
          const f = new Date(p.fecha);
          return f >= inicioMes && f <= finMes;
        })));

        // Gráfico: últimas 4 semanas (lunes→domingo)
        const semanas: { label: string; count: number }[] = [];
        for (let i = 3; i >= 0; i--) {
          const ini = new Date(lunes);
          ini.setDate(lunes.getDate() - i * 7);
          const fin = new Date(ini);
          fin.setDate(ini.getDate() + 6);
          fin.setHours(23, 59, 59, 999);
          const label = `${ini.getDate()}/${ini.getMonth() + 1}`;
          const count = citas.filter(c => {
            const f = new Date(c.fecha_hora_inicio);
            return f >= ini && f <= fin && c.estado !== 'CANCELADA';
          }).length;
          semanas.push({ label, count });
        }
        this.chartData = {
          labels: semanas.map(s => s.label),
          datasets: [{
            data: semanas.map(s => s.count),
            backgroundColor: '#3B5BDB',
            borderRadius: 6,
            borderSkipped: false
          }]
        };

        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }
}
