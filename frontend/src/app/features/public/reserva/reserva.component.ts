import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatStepperModule } from '@angular/material/stepper';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import {
  PublicReservaService,
  PublicServicio,
  ReservaResponse
} from '../services/public-reserva.service';
import { ClinicConfigService } from '../../../core/config/clinic-config.service';

@Component({
  selector: 'app-reserva',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatStepperModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatDatepickerModule,
    MatProgressSpinnerModule,
    MatCardModule
  ],
  templateUrl: './reserva.component.html',
  styleUrls: ['./reserva.component.scss']
})
export class ReservaComponent implements OnInit {

  servicios: PublicServicio[] = [];
  slots: string[] = [];
  cargandoServicios = true;
  cargandoSlots = false;
  enviando = false;
  error: string | null = null;
  reservaExitosa: ReservaResponse | null = null;

  servicioSeleccionado: PublicServicio | null = null;
  slotSeleccionado: string | null = null;

  readonly minFecha = (() => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return d;
  })();

  servicioForm: FormGroup;
  fechaForm: FormGroup;
  slotForm: FormGroup;
  datosForm: FormGroup;

  clinicName = '';

  constructor(
    private reservaService: PublicReservaService,
    private clinicConfig: ClinicConfigService,
    private fb: FormBuilder
  ) {
    this.servicioForm = this.fb.group({ servicioId: [null, Validators.required] });
    this.fechaForm = this.fb.group({ fecha: [null, Validators.required] });
    this.slotForm = this.fb.group({ slot: [null, Validators.required] });
    this.datosForm = this.fb.group({
      nombre: ['', Validators.required],
      apellido: ['', Validators.required],
      telefono: ['', [Validators.required, Validators.pattern('^[0-9]{7,15}$')]],
      email: ['', [Validators.required, Validators.email]]
    });
  }

  ngOnInit(): void {
    this.clinicName = this.clinicConfig.name;
    this.reservaService.getServicios().subscribe({
      next: (s) => { this.servicios = s; this.cargandoServicios = false; },
      error: () => { this.cargandoServicios = false; this.error = 'No se pudieron cargar los servicios.'; }
    });
  }

  seleccionarServicio(servicio: PublicServicio): void {
    this.servicioSeleccionado = servicio;
    this.servicioForm.setValue({ servicioId: servicio.id });
    this.slotSeleccionado = null;
    this.slotForm.reset();
    this.slots = [];
  }

  onFechaChange(): void {
    const fecha = this.fechaForm.value.fecha as Date | null;
    if (!fecha || !this.servicioSeleccionado) return;
    this.slotSeleccionado = null;
    this.slotForm.reset();
    this.slots = [];
    this.cargandoSlots = true;
    const isoFecha = this.toIsoDate(fecha);
    this.reservaService.getSlots(isoFecha, this.servicioSeleccionado.id).subscribe({
      next: (s) => { this.slots = s; this.cargandoSlots = false; },
      error: () => { this.cargandoSlots = false; this.error = 'Error al cargar horarios disponibles.'; }
    });
  }

  seleccionarSlot(slot: string): void {
    this.slotSeleccionado = slot;
    this.slotForm.setValue({ slot });
  }

  enviar(): void {
    if (this.datosForm.invalid || !this.servicioSeleccionado || !this.slotSeleccionado) return;
    this.enviando = true;
    this.error = null;

    const fecha = this.fechaForm.value.fecha as Date;
    const fechaHoraInicio = `${this.toIsoDate(fecha)}T${this.slotSeleccionado}:00`;

    this.reservaService.reservar({
      nombre: this.datosForm.value.nombre,
      apellido: this.datosForm.value.apellido,
      telefono: this.datosForm.value.telefono,
      email: this.datosForm.value.email,
      servicio_id: this.servicioSeleccionado.id,
      fecha_hora_inicio: fechaHoraInicio
    }).subscribe({
      next: (res) => { this.reservaExitosa = res; this.enviando = false; },
      error: (err) => {
        this.enviando = false;
        this.error = err?.error?.message || 'Error al enviar la reserva. Intentá de nuevo.';
      }
    });
  }

  private toIsoDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
