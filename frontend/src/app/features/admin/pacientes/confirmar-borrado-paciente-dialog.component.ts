import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { Paciente } from '../../../core/models/paciente.model';

export interface ConfirmarBorradoPacienteData {
  paciente: Paciente;
}

@Component({
  selector: 'app-confirmar-borrado-paciente-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Eliminar paciente</h2>
    <mat-dialog-content>
      <p>¿Eliminar a <strong>{{ data.paciente.nombre }} {{ data.paciente.apellido }}</strong>?</p>
      <p class="detalle">Si tiene citas asociadas, no se podrá eliminar.</p>
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
export class ConfirmarBorradoPacienteDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmarBorradoPacienteDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmarBorradoPacienteData
  ) {}

  cancelar(): void {
    this.dialogRef.close(false);
  }

  confirmar(): void {
    this.dialogRef.close(true);
  }
}
