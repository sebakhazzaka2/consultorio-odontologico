import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { TratamientoService, ResultadoTratamiento } from './tratamiento.service';
import { Tratamiento, TratamientoPayload } from '../../../core/models/tratamiento.model';
import { TratamientoFormDialogComponent } from './tratamiento-form-dialog.component';

// ---- Dialog de confirmación de borrado (inline, sin archivo separado) ----
@Component({
  selector: 'app-confirmar-borrado-tratamiento-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Confirmar eliminación</h2>
    <mat-dialog-content>
      <p>¿Está seguro que desea eliminar el tratamiento <strong>{{ data.nombre }}</strong>?</p>
      <p class="advertencia">Esta acción no se puede deshacer.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancelar()">Cancelar</button>
      <button mat-raised-button color="warn" (click)="confirmar()">Eliminar</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .advertencia { color: #999; font-size: 0.85rem; margin-top: 0.25rem; }
    mat-dialog-content { min-width: 280px; }
  `]
})
export class ConfirmarBorradoTratamientoDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmarBorradoTratamientoDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { nombre: string }
  ) {}

  cancelar(): void { this.dialogRef.close(false); }
  confirmar(): void { this.dialogRef.close(true); }
}
// ---- Fin dialog confirmación ----

@Component({
  selector: 'app-tratamientos-listado',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatDialogModule,
    MatTableModule
  ],
  templateUrl: './tratamientos-listado.component.html',
  styleUrl: './tratamientos-listado.component.scss'
})
export class TratamientosListadoComponent implements OnInit {
  tratamientos: Tratamiento[] = [];
  cargando = true;
  errorMensaje: string | null = null;
  readonly displayedColumns: string[] = ['nombre', 'descripcion', 'precio', 'estado', 'acciones'];

  constructor(
    private tratamientoService: TratamientoService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.errorMensaje = null;
    this.tratamientoService.listar().subscribe({
      next: (lista) => {
        this.tratamientos = lista;
        this.cargando = false;
      },
      error: (res: ResultadoTratamiento) => {
        this.errorMensaje = (res.detalles?.length ? res.mensaje + ' ' + res.detalles.join('. ') : res.mensaje) ?? null;
        this.cargando = false;
      }
    });
  }

  abrirNuevo(): void {
    const ref = this.dialog.open(TratamientoFormDialogComponent, {
      width: '420px',
      data: { tratamiento: null }
    });
    ref.afterClosed().subscribe((payload: TratamientoPayload | null) => {
      if (!payload) return;
      this.tratamientoService.crear(payload).subscribe({
        next: (t: Tratamiento) => {
          this.snackBar.open(`Tratamiento "${t.nombre}" creado`, 'Cerrar', { duration: 4000 });
          this.cargar();
        },
        error: (res: ResultadoTratamiento) => {
          this.snackBar.open((res.detalles?.length ? res.mensaje + ' ' + res.detalles.join('. ') : res.mensaje) ?? '', 'Cerrar', { duration: 6000 });
        }
      });
    });
  }

  abrirEditar(t: Tratamiento): void {
    const ref = this.dialog.open(TratamientoFormDialogComponent, {
      width: '420px',
      data: { tratamiento: t }
    });
    ref.afterClosed().subscribe((payload: TratamientoPayload | null) => {
      if (!payload) return;
      this.tratamientoService.actualizar(t.id, payload).subscribe({
        next: (updated: Tratamiento) => {
          this.snackBar.open(`Tratamiento "${updated.nombre}" actualizado`, 'Cerrar', { duration: 4000 });
          this.cargar();
        },
        error: (res: ResultadoTratamiento) => {
          this.snackBar.open((res.detalles?.length ? res.mensaje + ' ' + res.detalles.join('. ') : res.mensaje) ?? '', 'Cerrar', { duration: 6000 });
        }
      });
    });
  }

  toggleActivo(t: Tratamiento): void {
    this.tratamientoService.toggleActivo(t.id).subscribe({
      next: (updated: Tratamiento) => {
        const estado = updated.activo ? 'activado' : 'desactivado';
        this.snackBar.open(`"${updated.nombre}" ${estado}`, 'Cerrar', { duration: 4000 });
        this.cargar();
      },
      error: (res: ResultadoTratamiento) => {
        this.snackBar.open(res.detalles?.length ? res.mensaje + ' ' + res.detalles.join('. ') : res.mensaje ?? '', 'Cerrar', { duration: 6000 });
      }
    });
  }

  eliminar(t: Tratamiento): void {
    const ref = this.dialog.open(ConfirmarBorradoTratamientoDialogComponent, {
      width: '380px',
      data: { nombre: t.nombre }
    });
    ref.afterClosed().subscribe((confirmado: boolean) => {
      if (!confirmado) return;
      this.tratamientoService.eliminar(t.id).subscribe({
        next: () => {
          this.snackBar.open(`Tratamiento "${t.nombre}" eliminado`, 'Cerrar', { duration: 4000 });
          this.cargar();
        },
        error: (res: ResultadoTratamiento) => {
          this.snackBar.open((res.detalles?.length ? res.mensaje + ' ' + res.detalles.join('. ') : res.mensaje) ?? '', 'Cerrar', { duration: 6000 });
        }
      });
    });
  }
}
