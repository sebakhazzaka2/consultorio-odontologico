import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { PagoPayload } from '../../../core/models/pago.model';

export interface PagoFormDialogData {
  pacienteId: number;
}

@Component({
  selector: 'app-pago-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  templateUrl: './pago-form-dialog.component.html'
})
export class PagoFormDialogComponent {
  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<PagoFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PagoFormDialogData
  ) {
    this.form = this.fb.group({
      monto: [null, [Validators.required, Validators.min(0.01)]],
      fecha: [null, Validators.required],
      concepto: ['']
    });
  }

  guardar(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    const fecha_str: string = (v.fecha as Date).toISOString().slice(0, 10);
    const payload: PagoPayload = {
      paciente_id: this.data.pacienteId,
      monto: v.monto,
      fecha: fecha_str,
      concepto: v.concepto || null
    };
    this.dialogRef.close(payload);
  }

  cancelar(): void {
    this.dialogRef.close(null);
  }
}
