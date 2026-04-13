import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
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
    MatSnackBarModule,
    PagoFormDialogComponent,
    ConfirmarEliminarPagoDialogComponent
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
        next: () => {
          this.snackBar.open('Entrada creada correctamente', 'Cerrar', { duration: 3000 });
          this.recargarHistorialYSaldo();
        },
        error: (res) => {
          const msg = res.detalles?.length ? res.mensaje + ': ' + res.detalles.join('. ') : res.mensaje;
          this.snackBar.open(msg || 'Error al crear la entrada', 'Cerrar', { duration: 5000 });
        }
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
