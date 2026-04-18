import { Component, OnInit, signal, effect } from '@angular/core';
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
import { MatTooltipModule } from '@angular/material/tooltip';
import { CitaService } from '../citas/cita.service';
import { PacienteService } from '../pacientes/paciente.service';
import { PagoService } from '../pacientes/pago.service';
import { Cita } from '../../../core/models/cita.model';
import { Pago } from '../../../core/models/pago.model';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';

type ChartVista = 'semana' | 'mes';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, RouterLink, MatIconModule, MatProgressSpinnerModule, MatButtonModule, MatTooltipModule, StatusChipComponent, BaseChartDirective],
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

  citas = signal<Cita[]>([]);
  chartVista = signal<ChartVista>('semana');
  chartOffset = signal<number>(0);
  chartRangeLabel = signal<string>('');

  private static readonly SEMANAS_VISIBLES = 8;
  private static readonly MESES_VISIBLES = 6;

  readonly today = new Date();

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
        ctx.font = 'bold 12px Inter, sans-serif';
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
      y: { beginAtZero: true, ticks: { stepSize: 5, font: { family: 'Inter, sans-serif', size: 11 }, color: '#64748B' }, grid: { color: '#E2E8F0' } }
    }
  };

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
  ) {
    effect(() => {
      this.citas();
      this.chartVista();
      this.chartOffset();
      this.rebuildChart();
    });
  }

  ngOnInit(): void {
    forkJoin({
      citas: this.citaService.listarCitas(),
      pacientes: this.pacienteService.listar(),
      pagos: this.pagoService.listarTodos()
    }).subscribe({
      next: ({ citas, pacientes, pagos }) => {
        const ahora = new Date();
        const hoyStr = ahora.toISOString().slice(0, 10);

        const lunes = new Date(ahora);
        lunes.setDate(ahora.getDate() - ((ahora.getDay() + 6) % 7));
        lunes.setHours(0, 0, 0, 0);
        const domingo = new Date(lunes);
        domingo.setDate(lunes.getDate() + 6);
        domingo.setHours(23, 59, 59, 999);

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

        this.citas.set(citas);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  setVista(v: ChartVista): void {
    if (this.chartVista() === v) return;
    this.chartVista.set(v);
    this.chartOffset.set(0);
  }

  shiftChart(delta: number): void {
    this.chartOffset.update(o => o + delta);
  }

  resetChart(): void {
    this.chartOffset.set(0);
  }

  private rebuildChart(): void {
    const citas = this.citas();
    if (this.chartVista() === 'semana') {
      this.buildWeeklyChart(citas);
    } else {
      this.buildMonthlyChart(citas);
    }
  }

  private buildWeeklyChart(citas: Cita[]): void {
    const ahora = new Date();
    const lunes = new Date(ahora);
    lunes.setDate(ahora.getDate() - ((ahora.getDay() + 6) % 7));
    lunes.setHours(0, 0, 0, 0);

    const offset = this.chartOffset();
    const total = DashboardComponent.SEMANAS_VISIBLES;

    const semanas: { label: string; count: number }[] = [];
    let primerInicio: Date | null = null;
    let ultimoFin: Date | null = null;

    for (let i = total - 1; i >= 0; i--) {
      const ini = new Date(lunes);
      ini.setDate(lunes.getDate() + (offset - i) * 7);
      const fin = new Date(ini);
      fin.setDate(ini.getDate() + 6);
      fin.setHours(23, 59, 59, 999);
      if (!primerInicio) primerInicio = ini;
      ultimoFin = fin;
      const count = citas.filter(c => {
        const f = new Date(c.fecha_hora_inicio);
        return f >= ini && f <= fin && c.estado !== 'CANCELADA';
      }).length;
      semanas.push({ label: `${ini.getDate()}/${ini.getMonth() + 1}`, count });
    }

    this.chartData = {
      labels: semanas.map(s => s.label),
      datasets: [{ data: semanas.map(s => s.count), backgroundColor: '#3B5BDB', borderRadius: 6, borderSkipped: false }]
    };

    if (primerInicio && ultimoFin) {
      this.chartRangeLabel.set(
        `${this.formatFecha(primerInicio)} – ${this.formatFecha(ultimoFin)}`
      );
    }
  }

  private buildMonthlyChart(citas: Cita[]): void {
    const ahora = new Date();
    const offset = this.chartOffset();
    const total = DashboardComponent.MESES_VISIBLES;
    const nombres = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

    const meses: { label: string; count: number }[] = [];
    let primerInicio: Date | null = null;
    let ultimoFin: Date | null = null;

    for (let i = total - 1; i >= 0; i--) {
      const base = new Date(ahora.getFullYear(), ahora.getMonth() + (offset - i), 1);
      const ini = new Date(base.getFullYear(), base.getMonth(), 1, 0, 0, 0, 0);
      const fin = new Date(base.getFullYear(), base.getMonth() + 1, 0, 23, 59, 59, 999);
      if (!primerInicio) primerInicio = ini;
      ultimoFin = fin;
      const count = citas.filter(c => {
        const f = new Date(c.fecha_hora_inicio);
        return f >= ini && f <= fin && c.estado !== 'CANCELADA';
      }).length;
      const yy = String(ini.getFullYear()).slice(2);
      meses.push({ label: `${nombres[ini.getMonth()]} ${yy}`, count });
    }

    this.chartData = {
      labels: meses.map(m => m.label),
      datasets: [{ data: meses.map(m => m.count), backgroundColor: '#3B5BDB', borderRadius: 6, borderSkipped: false }]
    };

    if (primerInicio && ultimoFin) {
      const yyIni = String(primerInicio.getFullYear()).slice(2);
      const yyFin = String(ultimoFin.getFullYear()).slice(2);
      this.chartRangeLabel.set(
        `${nombres[primerInicio.getMonth()]} ${yyIni} – ${nombres[ultimoFin.getMonth()]} ${yyFin}`
      );
    }
  }

  private formatFecha(d: Date): string {
    return `${d.getDate()}/${d.getMonth() + 1}`;
  }
}
