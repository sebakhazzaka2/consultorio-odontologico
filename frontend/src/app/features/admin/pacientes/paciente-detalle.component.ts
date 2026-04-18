import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { FormBuilder, FormControl, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTableModule } from '@angular/material/table';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { PacienteService } from './paciente.service';
import { HistorialService } from './historial.service';
import { PagoService } from './pago.service';
import { Paciente } from '../../../core/models/paciente.model';
import { HistorialClinico, HistorialPayload } from '../../../core/models/historial.model';
import { Pago, PagoPayload, SaldoPaciente } from '../../../core/models/pago.model';
import { HistorialFormDialogComponent } from './historial-form-dialog.component';
import { PagoFormDialogComponent } from './pago-form-dialog.component';

@Component({
  selector: 'app-confirmar-eliminar-historial-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Eliminar entrada</h2>
    <mat-dialog-content>
      <p>¿Eliminar la entrada <strong>{{ data.procedimiento }}</strong>?</p>
      <p class="detalle">Esta acción no se puede deshacer.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close(false)">Cancelar</button>
      <button mat-raised-button color="warn" (click)="dialogRef.close(true)">Eliminar</button>
    </mat-dialog-actions>
  `,
  styles: [`.detalle { color: #666; font-size: 0.9rem; margin-top: 0.5rem; } mat-dialog-content { min-width: 280px; }`]
})
export class ConfirmarEliminarHistorialDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmarEliminarHistorialDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { procedimiento: string }
  ) {}
}

@Component({
  selector: 'app-confirmar-eliminar-pago-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, CommonModule],
  template: `
    <h2 mat-dialog-title>Eliminar pago</h2>
    <mat-dialog-content>
      <p>¿Eliminar el pago de <strong>UY$ {{ data.monto | number:'1.2-2' }}</strong> del <strong>{{ data.fecha | date:'dd/MM/yyyy' }}</strong>?</p>
      <p class="detalle">Esta acción no se puede deshacer.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close(false)">Cancelar</button>
      <button mat-raised-button color="warn" (click)="dialogRef.close(true)">Eliminar</button>
    </mat-dialog-actions>
  `,
  styles: [`.detalle { color: #666; font-size: 0.9rem; margin-top: 0.5rem; } mat-dialog-content { min-width: 280px; }`]
})
export class ConfirmarEliminarPagoDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmarEliminarPagoDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { monto: number; fecha: string }
  ) {}
}

export interface PagoPromptData {
  pacienteId: number;
  precio: number;
  nombreTratamiento: string;
}

@Component({
  selector: 'app-pago-prompt-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>¿Registrar pago?</h2>
    <mat-dialog-content>
      <p class="tratamiento-nombre">{{ data.nombreTratamiento }}</p>
      <p class="precio-ref">Precio de referencia: <strong>UY$ {{ data.precio | number:'1.0-0' }}</strong></p>
      <div class="opciones">
        <button class="opcion-btn" [class.selected]="modo === 'total'" (click)="seleccionarTotal()">
          <span class="opcion-label">Pago total</span>
          <span class="opcion-monto">UY$ {{ data.precio | number:'1.0-0' }}</span>
        </button>
        <button class="opcion-btn" [class.selected]="modo === 'parcial'" (click)="seleccionarParcial()">
          <span class="opcion-label">Pago parcial</span>
          <span class="opcion-monto">Ingresá el monto</span>
        </button>
      </div>
      @if (modo === 'parcial') {
        <mat-form-field appearance="outline" class="monto-field">
          <mat-label>Monto a registrar</mat-label>
          <input matInput type="number" min="1" [max]="data.precio" [formControl]="montoControl" placeholder="0">
          <span matTextPrefix>UY$&nbsp;</span>
        </mat-form-field>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close(null)">Ahora no</button>
      <button mat-flat-button color="primary" [disabled]="!puedeConfirmar()" (click)="confirmar()">Registrar pago</button>
    </mat-dialog-actions>
  `,
  styles: [`
    mat-dialog-content { min-width: 340px; padding-top: 8px; }
    .tratamiento-nombre { font-weight: 600; margin: 0 0 4px; }
    .precio-ref { color: #64748B; font-size: 0.875rem; margin: 0 0 16px; }
    .opciones { display: flex; gap: 10px; margin-bottom: 16px; }
    .opcion-btn {
      flex: 1; display: flex; flex-direction: column; align-items: flex-start;
      padding: 12px 14px; border: 1.5px solid #E2E8F0; border-radius: 10px;
      background: #fff; cursor: pointer; transition: border-color 150ms, background 150ms;
    }
    .opcion-btn.selected { border-color: #3B5BDB; background: #EEF2FF; }
    .opcion-label { font-size: 0.8rem; color: #64748B; font-weight: 500; text-transform: uppercase; letter-spacing: 0.05em; }
    .opcion-monto { font-size: 1rem; font-weight: 700; color: #0F172A; margin-top: 4px; }
    .monto-field { width: 100%; }
  `]
})
export class PagoPromptDialogComponent {
  modo: 'total' | 'parcial' | null = null;
  montoControl: FormControl;

  constructor(
    public dialogRef: MatDialogRef<PagoPromptDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PagoPromptData,
    private fb: FormBuilder
  ) {
    this.montoControl = this.fb.control(null, [Validators.required, Validators.min(1)]);
  }

  seleccionarTotal(): void { this.modo = 'total'; }
  seleccionarParcial(): void { this.modo = 'parcial'; this.montoControl.reset(); }

  puedeConfirmar(): boolean {
    if (this.modo === 'total') return true;
    if (this.modo === 'parcial') return this.montoControl.valid && this.montoControl.value > 0;
    return false;
  }

  confirmar(): void {
    const monto = this.modo === 'total' ? this.data.precio : Number(this.montoControl.value);
    this.dialogRef.close(monto);
  }
}

@Component({
  selector: 'app-paciente-detalle',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatTableModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './paciente-detalle.component.html',
  styleUrl: './paciente-detalle.component.scss'
})
export class PacienteDetalleComponent implements OnInit {
  paciente: Paciente | null = null;
  historial: HistorialClinico[] = [];
  pagos: Pago[] = [];
  saldo: SaldoPaciente | null = null;
  cargando = true;
  errorMensaje: string | null = null;

  readonly historialColumnas: string[] = ['fecha_hora', 'procedimiento', 'tratamiento', 'precio_aplicado', 'notas', 'acciones'];
  readonly pagosColumnas: string[] = ['fecha', 'monto', 'concepto', 'acciones'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private pacienteService: PacienteService,
    private historialService: HistorialService,
    private pagoService: PagoService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.cargar(id);
  }

  cargar(id: number): void {
    this.cargando = true;
    this.errorMensaje = null;

    forkJoin({
      paciente: this.pacienteService.obtener(id),
      historial: this.historialService.listarPorPaciente(id),
      pagos: this.pagoService.listarPorPaciente(id),
      saldo: this.pagoService.getSaldo(id)
    }).subscribe({
      next: ({ paciente, historial, pagos, saldo }) => {
        this.paciente = paciente;
        this.historial = historial;
        this.pagos = pagos;
        this.saldo = saldo;
        this.cargando = false;
      },
      error: (res) => {
        this.errorMensaje = res.detalles?.length
          ? res.mensaje + ': ' + res.detalles.join('. ')
          : (res.mensaje || 'Error al cargar el detalle del paciente');
        this.cargando = false;
      }
    });
  }

  private recargarPagosYSaldo(): void {
    if (!this.paciente) return;
    const id = this.paciente.id;
    forkJoin({
      pagos: this.pagoService.listarPorPaciente(id),
      saldo: this.pagoService.getSaldo(id)
    }).subscribe({
      next: ({ pagos, saldo }) => {
        this.pagos = pagos;
        this.saldo = saldo;
      }
    });
  }

  private recargarHistorialYSaldo(): void {
    if (!this.paciente) return;
    const id = this.paciente.id;
    forkJoin({
      historial: this.historialService.listarPorPaciente(id),
      saldo: this.pagoService.getSaldo(id)
    }).subscribe({
      next: ({ historial, saldo }) => {
        this.historial = historial;
        this.saldo = saldo;
      }
    });
  }

  abrirNuevoHistorial(): void {
    if (!this.paciente) return;
    const ref = this.dialog.open(HistorialFormDialogComponent, {
      width: '480px',
      data: { historial: null, pacienteId: this.paciente.id }
    });
    ref.afterClosed().subscribe((payload: HistorialPayload | null) => {
      if (!payload) return;
      this.historialService.crear(payload).subscribe({
        next: (creado) => {
          this.snackBar.open('Entrada creada correctamente', 'Cerrar', { duration: 3000 });
          this.recargarHistorialYSaldo();
          if (creado.precio_aplicado && creado.precio_aplicado > 0) {
            this.abrirPagoPrompt(creado.precio_aplicado, creado.nombre_tratamiento ?? 'Tratamiento');
          }
        },
        error: (res) => {
          const msg = res.detalles?.length ? res.mensaje + ': ' + res.detalles.join('. ') : res.mensaje;
          this.snackBar.open(msg || 'Error al crear la entrada', 'Cerrar', { duration: 5000 });
        }
      });
    });
  }

  abrirPagoPrompt(precio: number, nombreTratamiento: string): void {
    if (!this.paciente) return;
    const ref = this.dialog.open(PagoPromptDialogComponent, {
      width: '400px',
      data: { pacienteId: this.paciente.id, precio, nombreTratamiento } satisfies PagoPromptData
    });
    ref.afterClosed().subscribe((monto: number | null) => {
      if (!monto) return;
      const hoy = new Date().toISOString().slice(0, 10);
      this.pagoService.crear({ paciente_id: this.paciente!.id, monto, fecha: hoy }).subscribe({
        next: () => {
          this.snackBar.open('Pago registrado', 'Cerrar', { duration: 3000 });
          this.recargarPagosYSaldo();
        },
        error: () => this.snackBar.open('Error al registrar el pago', 'Cerrar', { duration: 4000 })
      });
    });
  }

  abrirEditarHistorial(h: HistorialClinico): void {
    if (!this.paciente) return;
    const ref = this.dialog.open(HistorialFormDialogComponent, {
      width: '480px',
      data: { historial: h, pacienteId: this.paciente.id }
    });
    ref.afterClosed().subscribe((payload: HistorialPayload | null) => {
      if (!payload) return;
      this.historialService.actualizar(h.id, payload).subscribe({
        next: () => {
          this.snackBar.open('Entrada actualizada correctamente', 'Cerrar', { duration: 3000 });
          this.recargarHistorialYSaldo();
        },
        error: (res) => {
          const msg = res.detalles?.length ? res.mensaje + ': ' + res.detalles.join('. ') : res.mensaje;
          this.snackBar.open(msg || 'Error al actualizar la entrada', 'Cerrar', { duration: 5000 });
        }
      });
    });
  }

  eliminarHistorial(h: HistorialClinico): void {
    const ref = this.dialog.open(ConfirmarEliminarHistorialDialogComponent, {
      data: { procedimiento: h.procedimiento }
    });
    ref.afterClosed().subscribe((confirmado: boolean) => {
      if (!confirmado) return;
      this.historialService.eliminar(h.id).subscribe({
        next: () => {
          this.snackBar.open('Entrada eliminada', 'Cerrar', { duration: 3000 });
          this.recargarHistorialYSaldo();
        },
        error: (res) => {
          const msg = res.detalles?.length ? res.mensaje + ': ' + res.detalles.join('. ') : res.mensaje;
          this.snackBar.open(msg || 'Error al eliminar la entrada', 'Cerrar', { duration: 5000 });
        }
      });
    });
  }

  abrirNuevoPago(): void {
    if (!this.paciente) return;
    const ref = this.dialog.open(PagoFormDialogComponent, {
      width: '400px',
      data: { pacienteId: this.paciente.id }
    });
    ref.afterClosed().subscribe((payload: PagoPayload | null) => {
      if (!payload) return;
      this.pagoService.crear(payload).subscribe({
        next: () => {
          this.snackBar.open('Pago registrado', 'Cerrar', { duration: 3000 });
          this.recargarPagosYSaldo();
        },
        error: (res) => {
          const msg = res.detalles?.length ? res.mensaje + ': ' + res.detalles.join('. ') : res.mensaje;
          this.snackBar.open(msg || 'Error al registrar el pago', 'Cerrar', { duration: 5000 });
        }
      });
    });
  }

  eliminarPago(p: Pago): void {
    const ref = this.dialog.open(ConfirmarEliminarPagoDialogComponent, {
      data: { monto: p.monto, fecha: p.fecha }
    });
    ref.afterClosed().subscribe((confirmado: boolean) => {
      if (!confirmado) return;
      this.pagoService.eliminar(p.id).subscribe({
        next: () => {
          this.snackBar.open('Pago eliminado', 'Cerrar', { duration: 3000 });
          this.recargarPagosYSaldo();
        },
        error: (res) => {
          const msg = res.detalles?.length ? res.mensaje + ': ' + res.detalles.join('. ') : res.mensaje;
          this.snackBar.open(msg || 'Error al eliminar el pago', 'Cerrar', { duration: 5000 });
        }
      });
    });
  }

  volver(): void {
    this.router.navigate(['/admin/pacientes']);
  }
}
