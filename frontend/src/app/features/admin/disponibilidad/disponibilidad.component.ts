import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import {
  DisponibilidadService,
  DisponibilidadSemanal,
  FechaBloqueada
} from './disponibilidad.service';

interface DiaRow {
  diaSemana: number;
  nombre: string;
  form: FormGroup;
  guardando: boolean;
}

@Component({
  selector: 'app-disponibilidad',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatSnackBarModule,
    MatDatepickerModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    PageHeaderComponent
  ],
  templateUrl: './disponibilidad.component.html',
  styleUrls: ['./disponibilidad.component.scss']
})
export class DisponibilidadComponent implements OnInit {

  readonly DIAS_NOMBRES: Record<number, string> = {
    1: 'Lunes', 2: 'Martes', 3: 'Miércoles',
    4: 'Jueves', 5: 'Viernes', 6: 'Sábado', 0: 'Domingo'
  };

  readonly ORDEN_DIAS = [1, 2, 3, 4, 5, 6, 0];

  dias: DiaRow[] = [];
  fechasBloqueadas: FechaBloqueada[] = [];
  cargando = true;
  fechaForm: FormGroup;
  bloqueando = false;

  constructor(
    private disponibilidadService: DisponibilidadService,
    private fb: FormBuilder,
    private snackBar: MatSnackBar
  ) {
    this.fechaForm = this.fb.group({
      fecha: [null, Validators.required],
      motivo: ['']
    });
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.disponibilidadService.getSemanal().subscribe({
      next: (configs) => {
        this.dias = this.ORDEN_DIAS.map(dia => {
          const config = configs.find(c => c.dia_semana === dia);
          return {
            diaSemana: dia,
            nombre: this.DIAS_NOMBRES[dia],
            guardando: false,
            form: this.crearFormDia(dia, config)
          };
        });
        this.cargando = false;
      },
      error: () => {
        this.snackBar.open('Error al cargar la disponibilidad', 'Cerrar', { duration: 4000 });
        this.cargando = false;
      }
    });

    this.disponibilidadService.getFechasBloqueadas().subscribe({
      next: (fechas) => this.fechasBloqueadas = fechas,
      error: () => {}
    });
  }

  guardarDia(row: DiaRow): void {
    if (row.form.invalid) return;
    row.guardando = true;
    const v = row.form.value;
    this.disponibilidadService.upsertDia({
      dia_semana: row.diaSemana,
      activo: v.activo,
      hora_apertura: v.hora_apertura || '09:00',
      hora_cierre: v.hora_cierre || '18:00',
      pausa_inicio: v.pausa_inicio || null,
      pausa_fin: v.pausa_fin || null
    }).subscribe({
      next: () => {
        row.guardando = false;
        this.snackBar.open(`${row.nombre} actualizado`, 'Cerrar', { duration: 3000 });
      },
      error: () => {
        row.guardando = false;
        this.snackBar.open('Error al guardar', 'Cerrar', { duration: 4000 });
      }
    });
  }

  bloquearFecha(): void {
    if (this.fechaForm.invalid) return;
    this.bloqueando = true;
    const fecha: Date = this.fechaForm.value.fecha;
    const isoFecha = this.toIsoDate(fecha);
    this.disponibilidadService.bloquearFecha({
      fecha: isoFecha,
      motivo: this.fechaForm.value.motivo || null
    }).subscribe({
      next: (nueva) => {
        this.fechasBloqueadas = [...this.fechasBloqueadas, nueva].sort((a, b) => a.fecha.localeCompare(b.fecha));
        this.fechaForm.reset();
        this.bloqueando = false;
        this.snackBar.open(`Fecha ${isoFecha} bloqueada`, 'Cerrar', { duration: 3000 });
      },
      error: (err) => {
        this.bloqueando = false;
        this.snackBar.open(err?.mensaje || 'Error al bloquear fecha', 'Cerrar', { duration: 4000 });
      }
    });
  }

  desbloquear(fecha: FechaBloqueada): void {
    this.disponibilidadService.desbloquearFecha(fecha.id).subscribe({
      next: () => {
        this.fechasBloqueadas = this.fechasBloqueadas.filter(f => f.id !== fecha.id);
        this.snackBar.open(`Fecha ${fecha.fecha} desbloqueada`, 'Cerrar', { duration: 3000 });
      },
      error: () => this.snackBar.open('Error al desbloquear', 'Cerrar', { duration: 4000 })
    });
  }

  formatearFecha(isoDate: string): string {
    const [y, m, d] = isoDate.split('-');
    return `${d}/${m}/${y}`;
  }

  private crearFormDia(dia: number, config?: DisponibilidadSemanal): FormGroup {
    return this.fb.group({
      activo: [config?.activo ?? false],
      hora_apertura: [config?.hora_apertura?.substring(0, 5) ?? '09:00'],
      hora_cierre: [config?.hora_cierre?.substring(0, 5) ?? '18:00'],
      pausa_inicio: [config?.pausa_inicio?.substring(0, 5) ?? ''],
      pausa_fin: [config?.pausa_fin?.substring(0, 5) ?? '']
    });
  }

  private toIsoDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
