import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CitaService } from '../citas/cita.service';
import { Cita } from '../../../core/models/cita.model';
import { ReagendarCitaDialogComponent } from './reagendar-cita-dialog.component';

const HOUR_HEIGHT   = 96;  // px por hora — 15 min = 24 px exactos
const QUARTER_HEIGHT = HOUR_HEIGHT / 4; // 24 px
const DAY_START      = 8;  // 08:00
const DAY_END        = 19; // 19:00 (exclusivo)
const TOTAL_HOURS    = DAY_END - DAY_START; // 11

@Component({
  selector: 'app-agenda-calendar',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDialogModule
  ],
  templateUrl: './agenda-calendar.component.html',
  styleUrl: './agenda-calendar.component.scss'
})
export class AgendaCalendarComponent implements OnInit {
  weekDays: Date[]    = [];
  hourLabels: string[] = [];
  citas: Cita[]       = [];
  cargando            = true;

  readonly HOUR_HEIGHT    = HOUR_HEIGHT;
  readonly QUARTER_HEIGHT = QUARTER_HEIGHT;
  readonly quarterOffsets = [1, 2, 3];
  readonly totalHeight    = TOTAL_HOURS * HOUR_HEIGHT;
  readonly DAY_NAMES      = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];

  // ── Week picker ──────────────────────────────────────────────────────────
  showPicker   = false;
  pickerMonth  = new Date();
  pickerWeeks: Date[][] = [];

  constructor(
    private citaService: CitaService,
    private router: Router,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.buildHourLabels();
    this.buildWeekDays(new Date());
    this.cargarCitas();
  }

  private buildHourLabels(): void {
    this.hourLabels = Array.from({ length: TOTAL_HOURS }, (_, i) =>
      `${(DAY_START + i).toString().padStart(2, '0')}:00`
    );
  }

  buildWeekDays(ref: Date): void {
    const dayOfWeek = ref.getDay();
    const diff      = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
    const monday    = new Date(ref);
    monday.setDate(ref.getDate() + diff);
    monday.setHours(0, 0, 0, 0);
    this.weekDays = Array.from({ length: 7 }, (_, i) => {
      const d = new Date(monday);
      d.setDate(monday.getDate() + i);
      return d;
    });
  }

  cargarCitas(): void {
    this.cargando = true;
    this.citaService.listarCitas().subscribe({
      next:  (lista) => { this.citas = lista; this.cargando = false; },
      error: ()      => { this.cargando = false; }
    });
  }

  getCitasDelDia(day: Date): Cita[] {
    const iso = this.toIsoDate(day);
    return this.citas.filter(c =>
      c.fecha_hora_inicio.startsWith(iso) && c.estado !== 'CANCELADA'
    );
  }

  private toIsoDate(d: Date): string {
    const y  = d.getFullYear();
    const m  = (d.getMonth() + 1).toString().padStart(2, '0');
    const dd = d.getDate().toString().padStart(2, '0');
    return `${y}-${m}-${dd}`;
  }

  // Posición y alto exactamente proporcionales — sin mínimo artificial
  getBlockStyle(cita: Cita): { [key: string]: string } {
    const dt           = new Date(cita.fecha_hora_inicio);
    const minSinceStart = (dt.getHours() - DAY_START) * 60 + dt.getMinutes();
    const top          = Math.max(0, (minSinceStart / 60) * HOUR_HEIGHT);
    const height       = (cita.duracion_minutos / 60) * HOUR_HEIGHT;
    return { top: `${top}px`, height: `${height}px` };
  }

  getBlockClass(estado: string): string {
    const map: Record<string, string> = {
      CONFIRMADA: 'estado-confirmada',
      PENDIENTE:  'estado-pendiente',
      COMPLETADA: 'estado-completada'
    };
    return map[estado] ?? 'estado-pendiente';
  }

  getHoraInicio(fechaHoraInicio: string): string {
    const d = new Date(fechaHoraInicio);
    return d.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
  }

  getDuracionLabel(minutos: number): string {
    if (minutos < 60) return `${minutos} min`;
    const h = Math.floor(minutos / 60);
    const m = minutos % 60;
    return m > 0 ? `${h}h ${m}m` : `${h}h`;
  }

  isToday(day: Date): boolean {
    const hoy = new Date();
    return day.getDate()     === hoy.getDate()  &&
           day.getMonth()    === hoy.getMonth() &&
           day.getFullYear() === hoy.getFullYear();
  }

  getDayName(day: Date): string {
    return day.toLocaleDateString('es-ES', { weekday: 'short' }).replace('.', '').toUpperCase();
  }

  getDayNumber(day: Date): number {
    return day.getDate();
  }

  getWeekRange(): string {
    if (!this.weekDays.length) return '';
    const s   = this.weekDays[0];
    const e   = this.weekDays[6];
    const fmt = (d: Date, opts: Intl.DateTimeFormatOptions) =>
      d.toLocaleDateString('es-ES', opts);
    return `${fmt(s, { day: 'numeric', month: 'short' })} – ${fmt(e, { day: 'numeric', month: 'short', year: 'numeric' })}`;
  }

  goToPrevWeek(): void {
    const prev = new Date(this.weekDays[0]);
    prev.setDate(prev.getDate() - 7);
    this.buildWeekDays(prev);
  }

  goToNextWeek(): void {
    const next = new Date(this.weekDays[0]);
    next.setDate(next.getDate() + 7);
    this.buildWeekDays(next);
  }

  goToToday(): void {
    this.buildWeekDays(new Date());
  }

  // ── Week picker ──────────────────────────────────────────────────────────
  togglePicker(): void {
    if (!this.showPicker) {
      this.pickerMonth = new Date(this.weekDays[0] ?? new Date());
      this.buildPickerWeeks();
    }
    this.showPicker = !this.showPicker;
  }

  closePicker(): void {
    this.showPicker = false;
  }

  prevPickerMonth(): void {
    this.pickerMonth = new Date(
      this.pickerMonth.getFullYear(),
      this.pickerMonth.getMonth() - 1,
      1
    );
    this.buildPickerWeeks();
  }

  nextPickerMonth(): void {
    this.pickerMonth = new Date(
      this.pickerMonth.getFullYear(),
      this.pickerMonth.getMonth() + 1,
      1
    );
    this.buildPickerWeeks();
  }

  private buildPickerWeeks(): void {
    const year  = this.pickerMonth.getFullYear();
    const month = this.pickerMonth.getMonth();

    const firstDay  = new Date(year, month, 1);
    const dayOfWeek = firstDay.getDay();
    const diff      = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
    const start     = new Date(firstDay);
    start.setDate(firstDay.getDate() + diff);

    this.pickerWeeks = [];
    const cur = new Date(start);

    for (let w = 0; w < 6; w++) {
      const week: Date[] = [];
      for (let d = 0; d < 7; d++) {
        week.push(new Date(cur));
        cur.setDate(cur.getDate() + 1);
      }
      this.pickerWeeks.push(week);
      // Parar cuando ya pasamos el mes y tenemos al menos 4 semanas
      if (w >= 3 && cur.getMonth() !== month) break;
    }
  }

  getPickerMonthLabel(): string {
    return this.pickerMonth.toLocaleDateString('es-ES', { month: 'long', year: 'numeric' });
  }

  isSelectedWeek(week: Date[]): boolean {
    return this.weekDays.length > 0 &&
           week[0].toDateString() === this.weekDays[0].toDateString();
  }

  isDayInCurrentMonth(day: Date): boolean {
    return day.getMonth() === this.pickerMonth.getMonth();
  }

  selectWeek(week: Date[]): void {
    this.buildWeekDays(week[0]);
    this.showPicker = false;
  }

  onCitaClick(cita: Cita): void {
    const ref = this.dialog.open(ReagendarCitaDialogComponent, {
      data: { cita },
      panelClass: 'dark-dialog'
    });
    ref.afterClosed().subscribe((confirmar: boolean) => {
      if (confirmar) {
        this.router.navigate(['/admin/citas/reagendar', cita.id]);
      }
    });
  }

  onNuevaCita(): void {
    this.router.navigate(['/admin/citas/nueva']);
  }
}
