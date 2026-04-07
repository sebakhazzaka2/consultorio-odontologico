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
import { PacienteService } from './paciente.service';
import { Paciente } from '../../../core/models/paciente.model';
import { PacienteFormDialogComponent } from './paciente-form-dialog.component';
import { ConfirmarBorradoPacienteDialogComponent } from './confirmar-borrado-paciente-dialog.component';
import type { PacientePayload } from '../../../core/models/paciente.model';

@Component({
  selector: 'app-pacientes-listado',
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
  templateUrl: './pacientes-listado.component.html',
  styleUrl: './pacientes-listado.component.scss'
})
export class PacientesListadoComponent implements OnInit {
  pacientes: Paciente[] = [];
  cargando = true;
  errorMensaje: string | null = null;

  constructor(
    private pacienteService: PacienteService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.errorMensaje = null;
    this.pacienteService.listar().subscribe({
      next: (lista) => {
        this.pacientes = lista;
        this.cargando = false;
      },
      error: (res) => {
        this.errorMensaje = res.detalles?.length ? res.mensaje + ': ' + res.detalles.join('. ') : res.mensaje;
        this.cargando = false;
      }
    });
  }

  abrirNuevo(): void {
    const ref = this.dialog.open(PacienteFormDialogComponent, {
      width: '400px',
      data: { paciente: null }
    });
    ref.afterClosed().subscribe((payload: PacientePayload | null) => {
      if (!payload) return;
      this.pacienteService.crear(payload).subscribe({
        next: (r) => {
          this.snackBar.open(r.mensaje || 'Paciente creado', 'Cerrar', { duration: 4000 });
          this.cargar();
        },
        error: (res) => {
          this.snackBar.open(res.detalles?.length ? res.mensaje + ' ' + res.detalles.join('. ') : res.mensaje, 'Cerrar', { duration: 6000 });
        }
      });
    });
  }

  abrirEditar(p: Paciente): void {
    const ref = this.dialog.open(PacienteFormDialogComponent, {
      width: '400px',
      data: { paciente: p }
    });
    ref.afterClosed().subscribe((payload: PacientePayload | null) => {
      if (!payload) return;
      this.pacienteService.actualizar(p.id, payload).subscribe({
        next: (r) => {
          this.snackBar.open(r.mensaje || 'Paciente actualizado', 'Cerrar', { duration: 4000 });
          this.cargar();
        },
        error: (res) => {
          this.snackBar.open(res.detalles?.length ? res.mensaje + ' ' + res.detalles.join('. ') : res.mensaje, 'Cerrar', { duration: 6000 });
        }
      });
    });
  }

  eliminar(p: Paciente): void {
    const ref = this.dialog.open(ConfirmarBorradoPacienteDialogComponent, {
      width: '380px',
      data: { paciente: p }
    });
    ref.afterClosed().subscribe((confirmado: boolean) => {
      if (!confirmado) return;
      this.pacienteService.eliminar(p.id).subscribe({
        next: (r) => {
          this.snackBar.open(r.mensaje || 'Paciente eliminado', 'Cerrar', { duration: 4000 });
          this.cargar();
        },
        error: (res) => {
          this.snackBar.open(res.detalles?.length ? res.mensaje + ' ' + res.detalles.join('. ') : res.mensaje, 'Cerrar', { duration: 6000 });
        }
      });
    });
  }
}
