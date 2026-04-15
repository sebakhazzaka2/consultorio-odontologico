import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Cita } from '../../../core/models/cita.model';

@Component({
  selector: 'app-reagendar-cita-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="dialog-dark">
      <h2 mat-dialog-title class="dialog-title">
        <mat-icon>event_repeat</mat-icon>
        Reagendar cita
      </h2>

      <mat-dialog-content class="dialog-content">
        <div class="cita-info">
          <div class="info-row">
            <mat-icon>person</mat-icon>
            <span>{{ data.cita.nombre_paciente }} {{ data.cita.apellido_paciente }}</span>
          </div>
          <div class="info-row">
            <mat-icon>schedule</mat-icon>
            <span>{{ formatFecha(data.cita.fecha_hora_inicio) }}</span>
          </div>
          <div class="info-row">
            <mat-icon>timelapse</mat-icon>
            <span>{{ data.cita.duracion_minutos }} minutos</span>
          </div>
          @if (data.cita.motivo) {
            <div class="info-row">
              <mat-icon>description</mat-icon>
              <span>{{ data.cita.motivo }}</span>
            </div>
          }
        </div>
        <p class="dialog-question">¿Querés reagendar esta cita?</p>
      </mat-dialog-content>

      <mat-dialog-actions class="dialog-actions">
        <button mat-stroked-button (click)="dialogRef.close(false)">Cancelar</button>
        <button mat-raised-button color="primary" (click)="dialogRef.close(true)">
          <mat-icon>event_repeat</mat-icon>
          Reagendar
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .dialog-dark {
      background: #1e1e1e;
      color: #f0f0f0;
      border-radius: 8px;
      min-width: 320px;
    }
    .dialog-title {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 18px;
      font-weight: 500;
      color: #f0f0f0;
      padding: 20px 24px 0;
      margin: 0;
      mat-icon { color: #90caf9; }
    }
    .dialog-content {
      padding: 16px 24px;
      max-height: none;
    }
    .cita-info {
      background: #2a2a2a;
      border-radius: 6px;
      padding: 12px 16px;
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin-bottom: 16px;
    }
    .info-row {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 14px;
      color: #d0d0d0;
      mat-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
        color: #6b6b6b;
      }
    }
    .dialog-question {
      margin: 0;
      font-size: 14px;
      color: #9e9e9e;
    }
    .dialog-actions {
      padding: 8px 24px 20px;
      gap: 10px;
      justify-content: flex-end;
      button[mat-stroked-button] {
        color: #9e9e9e;
        border-color: #444;
      }
    }
  `]
})
export class ReagendarCitaDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ReagendarCitaDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { cita: Cita }
  ) {}

  formatFecha(fechaHoraInicio: string): string {
    const d = new Date(fechaHoraInicio);
    return d.toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'long' })
      + ' — ' + d.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
  }
}
