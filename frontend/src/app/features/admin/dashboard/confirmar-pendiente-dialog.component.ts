import { Component, Inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CitaService } from '../citas/cita.service';
import { Cita } from '../../../core/models/cita.model';

export interface ConfirmarPendienteDialogData {
  cita: Cita;
}

@Component({
  selector: 'app-confirmar-pendiente-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    DatePipe
  ],
  template: `
    <h2 mat-dialog-title>Confirmar reserva</h2>

    <mat-dialog-content>
      <div class="info-grid">
        <div class="info-row">
          <span class="info-label">Paciente</span>
          <span class="info-value">{{ data.cita.nombre_paciente }} {{ data.cita.apellido_paciente }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">Servicio</span>
          <span class="info-value">{{ data.cita.motivo || '—' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">Fecha y hora</span>
          <span class="info-value">{{ data.cita.fecha_hora_inicio | date:'d/MM/yyyy HH:mm' }}</span>
        </div>
      </div>

      <form [formGroup]="form" class="duracion-form">
        <mat-form-field appearance="outline" class="duracion-field">
          <mat-label>Duración (minutos)</mat-label>
          <input matInput type="number" formControlName="duracionMinutos" min="5">
          <mat-hint>Pre-cargado desde el servicio. Podés ajustarlo.</mat-hint>
        </mat-form-field>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cerrar()">Cancelar</button>
      <button mat-stroked-button color="warn" (click)="confirmar('rechazar')" [disabled]="guardando">
        Rechazar
      </button>
      <button mat-flat-button color="primary" (click)="confirmar('aceptar')" [disabled]="form.invalid || guardando">
        @if (guardando) { <mat-spinner diameter="18"></mat-spinner> }
        @else { <mat-icon>check_circle</mat-icon> }
        Confirmar
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .info-grid { display: flex; flex-direction: column; gap: 8px; margin-bottom: 20px; }
    .info-row { display: flex; gap: 12px; }
    .info-label { font-size: 13px; color: var(--color-text-muted); min-width: 90px; }
    .info-value { font-size: 14px; font-weight: 500; color: var(--color-text); }
    .duracion-form { margin-top: 4px; }
    .duracion-field { width: 100%; }
    mat-dialog-actions button { display: flex; align-items: center; gap: 4px; }
  `]
})
export class ConfirmarPendienteDialogComponent {
  form: FormGroup;
  guardando = false;

  constructor(
    private dialogRef: MatDialogRef<ConfirmarPendienteDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmarPendienteDialogData,
    private citaService: CitaService,
    private snackBar: MatSnackBar,
    private fb: FormBuilder
  ) {
    this.form = this.fb.group({
      duracionMinutos: [data.cita.duracion_minutos, [Validators.required, Validators.min(5)]]
    });
  }

  confirmar(accion: 'aceptar' | 'rechazar'): void {
    this.guardando = true;
    const cita = this.data.cita;

    if (accion === 'rechazar') {
      this.citaService.cancelarCita(cita.id).subscribe({
        next: () => {
          this.snackBar.open('Reserva rechazada', 'Cerrar', { duration: 3000 });
          this.dialogRef.close('rechazada');
        },
        error: () => {
          this.guardando = false;
          this.snackBar.open('Error al rechazar', 'Cerrar', { duration: 4000 });
        }
      });
    } else {
      const duracion = this.form.value.duracionMinutos;
      this.citaService.confirmarCita(cita.id, duracion).subscribe({
        next: () => {
          this.snackBar.open('Reserva confirmada', 'Cerrar', { duration: 3000 });
          this.dialogRef.close('confirmada');
        },
        error: () => {
          this.guardando = false;
          this.snackBar.open('Error al confirmar', 'Cerrar', { duration: 4000 });
        }
      });
    }
  }

  cerrar(): void {
    this.dialogRef.close(null);
  }
}
