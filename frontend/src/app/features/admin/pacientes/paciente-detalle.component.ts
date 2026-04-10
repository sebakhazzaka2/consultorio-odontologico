import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTableModule } from '@angular/material/table';
import { PacienteService } from './paciente.service';
import { HistorialService } from './historial.service';
import { PagoService } from './pago.service';
import { Paciente } from '../../../core/models/paciente.model';
import { HistorialClinico } from '../../../core/models/historial.model';
import { Pago, SaldoPaciente } from '../../../core/models/pago.model';

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
    MatTableModule
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

  readonly historialColumnas: string[] = ['fecha_hora', 'procedimiento', 'tratamiento', 'precio_aplicado', 'notas'];
  readonly pagosColumnas: string[] = ['fecha', 'monto', 'concepto'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private pacienteService: PacienteService,
    private historialService: HistorialService,
    private pagoService: PagoService
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

  volver(): void {
    this.router.navigate(['/admin/pacientes']);
  }
}
