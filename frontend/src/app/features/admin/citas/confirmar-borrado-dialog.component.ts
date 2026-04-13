import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { Cita } from '../../../core/models/cita.model';

export interface ConfirmarBorradoData {
  cita: Cita;
}

@Component({
  selector: 'app-confirmar-borrado-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Confirmar eliminación</h2>
    <mat-dialog-content>
      <p>¿Está seguro que desea eliminar la cita de <strong>{{ data.cita.nombre_paciente }} {{ data.cita.apellido_paciente }}</strong>?</p>
      <p class="detalle">{{ data.cita.fecha_hora_inicio }} · {{ data.cita.duracion_minutos }} min</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancelar()">Cancelar</button>
      <button mat-raised-button color="warn" (click)="confirmar()">Eliminar</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .detalle { color: #666; font-size: 0.9rem; margin-top: 0.5rem; }
    mat-dialog-content { min-width: 280px; }
  `]
})
export class ConfirmarBorradoDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmarBorradoDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmarBorradoData
  ) {}

  cancelar(): void {
    this.dialogRef.close(false);
  }

  confirmar(): void {
    this.dialogRef.close(true);
  }
}
