import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { Cita } from '../../../core/models/cita.model';

export interface ConfirmarCancelarData {
  cita: Cita;
}

@Component({
  selector: 'app-confirmar-cancelar-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Confirmar cancelación</h2>
    <mat-dialog-content>
      <p>¿Está seguro que desea cancelar la cita de <strong>{{ data.cita.nombre_paciente }} {{ data.cita.apellido_paciente }}</strong>?</p>
      <p class="detalle">{{ data.cita.fecha_hora_inicio }} · {{ data.cita.duracion_minutos }} min</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cerrar()">Volver</button>
      <button mat-raised-button color="warn" (click)="confirmar()">Cancelar cita</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .detalle { color: #666; font-size: 0.9rem; margin-top: 0.5rem; }
    mat-dialog-content { min-width: 280px; }
  `]
})
export class ConfirmarCancelarDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmarCancelarDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmarCancelarData
  ) {}

  cerrar(): void {
    this.dialogRef.close(false);
  }

  confirmar(): void {
    this.dialogRef.close(true);
  }
}
