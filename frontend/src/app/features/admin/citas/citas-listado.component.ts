import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CitaService, ResultadoCita } from './cita.service';
import { Cita } from '../../../core/models/cita.model';
import { ConfirmarBorradoDialogComponent } from './confirmar-borrado-dialog.component';

@Component({
  selector: 'app-citas-listado',
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
    MatDialogModule
  ],
  templateUrl: './citas-listado.component.html',
  styleUrl: './citas-listado.component.scss'
})
export class CitasListadoComponent implements OnInit {
  citas: Cita[] = [];
  cargando = true;
  errorMensaje: string | null = null;

  constructor(
    private citaService: CitaService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.cargarCitas();
  }

  cargarCitas(): void {
    this.cargando = true;
    this.errorMensaje = null;
    this.citaService.listarCitas().subscribe({
      next: (lista) => {
        this.citas = lista;
        this.cargando = false;
      },
      error: (res: ResultadoCita) => {
        this.errorMensaje = res.mensaje || 'Error al cargar las citas';
        if (res.detalles?.length) {
          this.errorMensaje += ': ' + res.detalles.join('. ');
        }
        this.cargando = false;
      }
    });
  }

  eliminarCita(cita: Cita): void {
    const dialogRef = this.dialog.open(ConfirmarBorradoDialogComponent, {
      width: '400px',
      data: { cita }
    });

    dialogRef.afterClosed().subscribe((confirmado: boolean) => {
      if (!confirmado) return;
      this.citaService.eliminarCita(cita.id).subscribe({
        next: (res) => {
          this.snackBar.open(res.mensaje || 'Cita eliminada correctamente', 'Cerrar', {
            duration: 4000,
            horizontalPosition: 'center',
            verticalPosition: 'top'
          });
          this.cargarCitas();
        },
        error: (res: ResultadoCita) => {
          // Mostrar mensaje y detalles del backend cuando rechace la operación
          const msg = res.detalles?.length ? res.mensaje + ': ' + res.detalles.join('. ') : res.mensaje;
          this.snackBar.open(msg || 'Error al eliminar', 'Cerrar', {
            duration: 6000,
            horizontalPosition: 'center',
            verticalPosition: 'top'
          });
        }
      });
    });
  }

  formatearFecha(fechaHoraInicio: string): string {
    if (!fechaHoraInicio) return '';
    const d = new Date(fechaHoraInicio);
    return isNaN(d.getTime()) ? fechaHoraInicio :
      d.toLocaleDateString('es-ES', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })
      + ' ' + d.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
  }
}
