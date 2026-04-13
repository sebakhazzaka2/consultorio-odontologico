import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { Tratamiento, TratamientoPayload } from '../../../core/models/tratamiento.model';

export interface TratamientoFormDialogData {
  tratamiento: Tratamiento | null;
}

@Component({
  selector: 'app-tratamiento-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCheckboxModule
  ],
  template: `
    <h2 mat-dialog-title>{{ data.tratamiento ? 'Editar tratamiento' : 'Nuevo tratamiento' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="nombre" />
          <mat-error *ngIf="form.get('nombre')?.hasError('required')">Requerido</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Descripción (opcional)</mat-label>
          <textarea matInput formControlName="descripcion" rows="3"></textarea>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Precio (UY$)</mat-label>
          <input matInput type="number" formControlName="precio" min="0.01" step="0.01" />
          <mat-error *ngIf="form.get('precio')?.hasError('required')">Requerido</mat-error>
          <mat-error *ngIf="form.get('precio')?.hasError('min')">Debe ser mayor a 0</mat-error>
        </mat-form-field>
        <mat-checkbox formControlName="activo" class="activo-check">Activo</mat-checkbox>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancelar()">Cancelar</button>
      <button mat-raised-button color="primary" (click)="guardar()" [disabled]="form.invalid">
        {{ data.tratamiento ? 'Guardar' : 'Crear' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .form { display: flex; flex-direction: column; min-width: 340px; padding-top: 8px; gap: 4px; }
    .full-width { width: 100%; }
    .activo-check { margin-top: 4px; margin-bottom: 8px; }
  `]
})
export class TratamientoFormDialogComponent {
  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<TratamientoFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: TratamientoFormDialogData
  ) {
    const t = data.tratamiento;
    this.form = this.fb.group({
      nombre: [t?.nombre ?? '', [Validators.required]],
      descripcion: [t?.descripcion ?? ''],
      precio: [t?.precio ?? null, [Validators.required, Validators.min(0.01)]],
      activo: [t?.activo ?? true]
    });
  }

  cancelar(): void {
    this.dialogRef.close(null);
  }

  guardar(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    const payload: TratamientoPayload = {
      nombre: (v.nombre as string).trim(),
      descripcion: v.descripcion ? (v.descripcion as string).trim() : undefined,
      precio: v.precio as number,
      activo: v.activo as boolean
    };
    this.dialogRef.close(payload);
  }
}
