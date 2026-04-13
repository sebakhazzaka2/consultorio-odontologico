import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { TratamientoService } from '../tratamientos/tratamiento.service';
import { Tratamiento } from '../../../core/models/tratamiento.model';
import { HistorialClinico, HistorialPayload } from '../../../core/models/historial.model';

export interface HistorialFormDialogData {
  historial: HistorialClinico | null;
  pacienteId: number;
}

@Component({
  selector: 'app-historial-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  templateUrl: './historial-form-dialog.component.html'
})
export class HistorialFormDialogComponent implements OnInit {
  form: FormGroup;
  tratamientos: Tratamiento[] = [];
  tratamientoSeleccionado: Tratamiento | null = null;

  readonly horariosDisponibles: string[] = (() => {
    const slots: string[] = [];
    for (let h = 9; h <= 17; h++) {
      for (let m = 0; m < 60; m += 15) {
        if (h === 17 && m > 45) break;
        slots.push(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`);
      }
    }
    return slots;
  })();

  constructor(
    private fb: FormBuilder,
    private tratamientoService: TratamientoService,
    public dialogRef: MatDialogRef<HistorialFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: HistorialFormDialogData
  ) {
    this.form = this.fb.group({
      fecha: [null, Validators.required],
      hora: ['', Validators.required],
      procedimiento: ['', Validators.required],
      tratamiento_id: [null],
      notas: ['']
    });
  }

  ngOnInit(): void {
    this.tratamientoService.listarActivos().subscribe({
      next: (lista) => {
        this.tratamientos = lista;
        if (this.data.historial) {
          this.poblarFormulario(this.data.historial);
        }
      },
      error: () => {
        if (this.data.historial) {
          this.poblarFormulario(this.data.historial);
        }
      }
    });

    this.form.get('tratamiento_id')!.valueChanges.subscribe((id: number | null) => {
      this.tratamientoSeleccionado = id
        ? (this.tratamientos.find(t => t.id === id) ?? null)
        : null;
    });
  }

  private poblarFormulario(h: HistorialClinico): void {
    this.form.patchValue({
      fecha: new Date(h.fecha_hora),
      hora: h.fecha_hora.slice(11, 16),
      procedimiento: h.procedimiento,
      tratamiento_id: h.tratamiento_id ?? null,
      notas: h.notas ?? ''
    });
    if (h.tratamiento_id) {
      this.tratamientoSeleccionado = this.tratamientos.find(t => t.id === h.tratamiento_id) ?? null;
    }
  }

  guardar(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    const fechaISO = (v.fecha as Date).toISOString().slice(0, 10);
    const payload: HistorialPayload = {
      paciente_id: this.data.pacienteId,
      fecha_hora: `${fechaISO}T${v.hora}:00`,
      procedimiento: v.procedimiento,
      tratamiento_id: v.tratamiento_id ?? null,
      notas: v.notas || null
    };
    this.dialogRef.close(payload);
  }

  cancelar(): void {
    this.dialogRef.close(null);
  }
}
