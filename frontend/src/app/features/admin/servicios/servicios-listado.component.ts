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
import { ServicioService, ResultadoServicio } from './servicio.service';
import { Servicio } from '../../../core/models/servicio.model';
import { ServicioFormDialogComponent, ServicioFormDialogResult } from './servicio-form-dialog.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

@Component({
  selector: 'app-confirmar-borrado-servicio-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Confirmar eliminación</h2>
    <mat-dialog-content>
      <p>¿Está seguro que desea eliminar el servicio <strong>{{ data.nombre }}</strong>?</p>
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
export class ConfirmarBorradoServicioDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmarBorradoServicioDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { nombre: string }
  ) {}

  cancelar(): void { this.dialogRef.close(false); }
  confirmar(): void { this.dialogRef.close(true); }
}

@Component({
  selector: 'app-servicios-listado',
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
    MatTableModule,
    PageHeaderComponent,
    EmptyStateComponent
  ],
  templateUrl: './servicios-listado.component.html',
  styleUrl: './servicios-listado.component.scss'
})
export class ServiciosListadoComponent implements OnInit {
  servicios: Servicio[] = [];
  cargando = true;
  errorMensaje: string | null = null;
  readonly displayedColumns: string[] = ['nombre', 'descripcion', 'precio', 'estado', 'acciones'];

  constructor(
    private servicioService: ServicioService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.errorMensaje = null;
    this.servicioService.listar().subscribe({
      next: (lista) => {
        this.servicios = lista;
        this.cargando = false;
      },
      error: (res: ResultadoServicio) => {
        this.errorMensaje = (res.detalles?.length ? res.mensaje + ' ' + res.detalles.join('. ') : res.mensaje) ?? null;
        this.cargando = false;
      }
    });
  }

  abrirNuevo(): void {
    const ref = this.dialog.open(ServicioFormDialogComponent, {
      width: '440px',
      data: { servicio: null }
    });
    ref.afterClosed().subscribe((result: ServicioFormDialogResult | null) => {
      if (!result) return;
      this.servicioService.crear(result.payload).subscribe({
        next: (s: Servicio) => {
          this.snackBar.open(`Servicio "${s.nombre}" creado`, 'Cerrar', { duration: 4000 });
          if (result.fotoFile) {
            this.servicioService.uploadFoto(s.id, result.fotoFile).subscribe({
              next: () => this.cargar(),
              error: () => {
                this.snackBar.open('Servicio creado pero no se pudo subir la foto', 'Cerrar', { duration: 5000 });
                this.cargar();
              }
            });
          } else {
            this.cargar();
          }
        },
        error: (res: ResultadoServicio) => {
          this.snackBar.open((res.detalles?.length ? res.mensaje + ' ' + res.detalles.join('. ') : res.mensaje) ?? '', 'Cerrar', { duration: 6000 });
        }
      });
    });
  }

  abrirEditar(s: Servicio): void {
    const ref = this.dialog.open(ServicioFormDialogComponent, {
      width: '440px',
      data: { servicio: s }
    });
    ref.afterClosed().subscribe((result: ServicioFormDialogResult | null) => {
      if (!result) return;
      this.servicioService.actualizar(s.id, result.payload).subscribe({
        next: (updated: Servicio) => {
          this.snackBar.open(`Servicio "${updated.nombre}" actualizado`, 'Cerrar', { duration: 4000 });
          this.cargar();
        },
        error: (res: ResultadoServicio) => {
          this.snackBar.open((res.detalles?.length ? res.mensaje + ' ' + res.detalles.join('. ') : res.mensaje) ?? '', 'Cerrar', { duration: 6000 });
        }
      });
    });
  }

  toggleActivo(s: Servicio): void {
    this.servicioService.toggleActivo(s.id).subscribe({
      next: (updated: Servicio) => {
        const estado = updated.activo ? 'activado' : 'desactivado';
        this.snackBar.open(`"${updated.nombre}" ${estado}`, 'Cerrar', { duration: 4000 });
        this.cargar();
      },
      error: (res: ResultadoServicio) => {
        this.snackBar.open(res.detalles?.length ? res.mensaje + ' ' + res.detalles.join('. ') : res.mensaje ?? '', 'Cerrar', { duration: 6000 });
      }
    });
  }

  eliminar(s: Servicio): void {
    const ref = this.dialog.open(ConfirmarBorradoServicioDialogComponent, {
      width: '380px',
      data: { nombre: s.nombre }
    });
    ref.afterClosed().subscribe((confirmado: boolean) => {
      if (!confirmado) return;
      this.servicioService.eliminar(s.id).subscribe({
        next: () => {
          this.snackBar.open(`Servicio "${s.nombre}" eliminado`, 'Cerrar', { duration: 4000 });
          this.cargar();
        },
        error: (res: ResultadoServicio) => {
          this.snackBar.open((res.detalles?.length ? res.mensaje + ' ' + res.detalles.join('. ') : res.mensaje) ?? '', 'Cerrar', { duration: 6000 });
        }
      });
    });
  }
}
