import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { Paciente, PacientePayload } from '../../models/paciente.model';

export interface PacienteFormDialogData {
  paciente?: Paciente | null;
}

@Component({
  selector: 'app-paciente-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  template: `
    <h2 mat-dialog-title>{{ data.paciente ? 'Editar paciente' : 'Nuevo paciente' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="nombre" />
          <mat-error *ngIf="form.get('nombre')?.hasError('required')">Requerido</mat-error>
          <mat-error *ngIf="form.get('nombre')?.hasError('minlength')">Mínimo 2 caracteres</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Apellido</mat-label>
          <input matInput formControlName="apellido" />
          <mat-error *ngIf="form.get('apellido')?.hasError('required')">Requerido</mat-error>
          <mat-error *ngIf="form.get('apellido')?.hasError('minlength')">Mínimo 2 caracteres</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Teléfono</mat-label>
          <input matInput type="tel" formControlName="telefono" />
          <mat-error *ngIf="form.get('telefono')?.hasError('required')">Requerido</mat-error>
          <mat-error *ngIf="form.get('telefono')?.hasError('pattern')">10 dígitos</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Email</mat-label>
          <input matInput type="email" formControlName="email" />
          <mat-error *ngIf="form.get('email')?.hasError('required')">Requerido</mat-error>
          <mat-error *ngIf="form.get('email')?.hasError('email')">Email inválido</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Fecha de nacimiento (opcional)</mat-label>
          <input matInput [matDatepicker]="picker" formControlName="fecha_nacimiento" />
          <mat-datepicker-toggle matSuffix [for]="picker"></mat-datepicker-toggle>
          <mat-datepicker #picker></mat-datepicker>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancelar()">Cancelar</button>
      <button mat-raised-button color="primary" (click)="guardar()" [disabled]="form.invalid">
        {{ data.paciente ? 'Guardar' : 'Crear' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .form { display: flex; flex-direction: column; min-width: 320px; padding-top: 8px; }
    .full-width { width: 100%; }
  `]
})
export class PacienteFormDialogComponent {
  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<PacienteFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PacienteFormDialogData
  ) {
    const p = data.paciente;
    this.form = this.fb.group({
      nombre: [p?.nombre ?? '', [Validators.required, Validators.minLength(2)]],
      apellido: [p?.apellido ?? '', [Validators.required, Validators.minLength(2)]],
      telefono: [p?.telefono ?? '', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
      email: [p?.email ?? '', [Validators.required, Validators.email]],
      fecha_nacimiento: [p?.fecha_nacimiento ? new Date(p.fecha_nacimiento + 'T12:00:00') : null]
    });
  }

  cancelar(): void {
    this.dialogRef.close(null);
  }

  guardar(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    const payload: PacientePayload = {
      nombre: v.nombre.trim(),
      apellido: v.apellido.trim(),
      telefono: v.telefono.trim(),
      email: v.email.trim(),
      fecha_nacimiento: v.fecha_nacimiento ? (v.fecha_nacimiento as Date).toISOString().slice(0, 10) : null
    };
    this.dialogRef.close(payload);
  }
}
