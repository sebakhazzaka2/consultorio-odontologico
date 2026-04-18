import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { switchMap } from 'rxjs/operators';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CitaService, ResultadoCita } from './cita.service';
import { PacienteService } from '../pacientes/paciente.service';
import { Cita } from '../../../core/models/cita.model';
import { Paciente } from '../../../core/models/paciente.model';
import { PacienteFormDialogComponent } from '../pacientes/paciente-form-dialog.component';
import { SlotPickerComponent } from './slot-picker.component';
import type { PacientePayload } from '../../../core/models/paciente.model';
import type { CitaPayload } from '../../../core/models/cita.model';

@Component({
  selector: 'app-cita-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatIconModule,
    MatCardModule,
    MatSnackBarModule,
    MatDialogModule,
    SlotPickerComponent
  ],
  templateUrl: './cita-form.component.html',
  styleUrl: './cita-form.component.scss'
})
export class CitaFormComponent implements OnInit {
  registroForm: FormGroup;
  idEdicion: number | null = null;
  enviando = false;
  esReagendar = false;
  pacientes: Paciente[] = [];
  horariosDisponibles: string[] = [];
  cargandoHorarios = false;

  duraciones = [15, 30, 45, 60, 90];

  minDate = new Date();

  constructor(
    private fb: FormBuilder,
    private snackBar: MatSnackBar,
    private router: Router,
    private route: ActivatedRoute,
    private citaService: CitaService,
    private pacienteService: PacienteService,
    private dialog: MatDialog
  ) {
    this.registroForm = this.fb.group({
      paciente_id: ['', Validators.required],
      fecha: [new Date(), Validators.required],
      hora: ['', Validators.required],
      duracion_minutos: [15, Validators.required],
      motivo: ['', Validators.required],
      notas: ['']
    });
  }

  get esEdicion(): boolean {
    return this.idEdicion != null;
  }

  ngOnInit(): void {
    this.esReagendar = this.route.snapshot.url.some(s => s.path === 'reagendar');
    this.cargarPacientes();
    this.registroForm.get('fecha')!.valueChanges.subscribe(() => this.cargarDisponibilidad());
    this.registroForm.get('duracion_minutos')!.valueChanges.subscribe(() => this.cargarDisponibilidad());
    this.route.params.subscribe((params) => {
      const id = params['id'];
      if (id) {
        this.idEdicion = parseInt(id, 10);
        if (!isNaN(this.idEdicion)) {
          this.cargarCitaParaEdicion();
        }
      } else {
        this.idEdicion = null;
        this.cargarDisponibilidad(); // carga con fecha=hoy y duracion=15 por defecto
      }
    });
  }

  cargarDisponibilidad(): void {
    const fecha: Date | null = this.registroForm.get('fecha')!.value;
    const duracion: number | null = this.registroForm.get('duracion_minutos')!.value;
    if (!fecha || !duracion) {
      this.horariosDisponibles = [];
      return;
    }
    this.cargandoHorarios = true;
    const fechaIso = fecha.toISOString().slice(0, 10);
    this.citaService.getDisponibilidad(fechaIso, duracion).subscribe({
      next: (slots: string[]) => {
        this.horariosDisponibles = this.filtrarSlotsPasados(fecha, slots);
        this.cargandoHorarios = false;
      },
      error: () => {
        this.snackBar.open('Error al cargar horarios disponibles', 'Cerrar', { duration: 4000 });
        this.cargandoHorarios = false;
      }
    });
  }

  private filtrarSlotsPasados(fecha: Date, slots: string[]): string[] {
    const hoy = new Date();
    const esHoy =
      fecha.getFullYear() === hoy.getFullYear() &&
      fecha.getMonth() === hoy.getMonth() &&
      fecha.getDate() === hoy.getDate();
    if (!esHoy) return slots;
    const minutosAhora = hoy.getHours() * 60 + hoy.getMinutes();
    return slots.filter((s) => {
      const [h, m] = s.split(':').map(Number);
      return h * 60 + m > minutosAhora;
    });
  }

  private cargarPacientes(): void {
    this.pacienteService.listar().subscribe({
      next: (lista) => {
        this.pacientes = lista;
      },
      error: () => {
        this.snackBar.open('Error al cargar la lista de pacientes', 'Cerrar', { duration: 4000 });
      }
    });
  }

  private cargarCitaParaEdicion(): void {
    if (this.idEdicion == null) return;
    this.citaService.obtenerCita(this.idEdicion).subscribe({
      next: (cita: Cita) => {
        this.registroForm.patchValue({
          paciente_id: cita.paciente_id,
          fecha: new Date(cita.fecha_hora_inicio),
          hora: cita.fecha_hora_inicio.slice(11, 16),
          duracion_minutos: cita.duracion_minutos,
          motivo: cita.motivo || '',
          notas: cita.notas || ''
        });
      },
      error: (res: ResultadoCita) => {
        const msg = res.detalles?.length ? res.mensaje + ' ' + res.detalles.join('. ') : res.mensaje;
        this.snackBar.open(msg || 'Error al cargar la cita', 'Cerrar', { duration: 6000 });
        this.router.navigate(['/admin/citas']);
      }
    });
  }

  onSlotElegido(hora: string): void {
    this.registroForm.patchValue({ hora });
    this.registroForm.get('hora')!.markAsTouched();
  }

  abrirNuevoPaciente(): void {
    const ref = this.dialog.open(PacienteFormDialogComponent, {
      width: '400px',
      data: { paciente: null }
    });
    ref.afterClosed().subscribe((payload: PacientePayload | null) => {
      if (!payload) return;
      this.pacienteService.crear(payload).subscribe({
        next: (r) => {
          this.snackBar.open(r.mensaje || 'Paciente creado', 'Cerrar', { duration: 3000 });
          if (r.id != null) {
            this.pacienteService.obtener(r.id).subscribe({
              next: (nuevo) => {
                this.pacientes = [...this.pacientes, nuevo];
                this.registroForm.patchValue({ paciente_id: nuevo.id });
              }
            });
          }
        },
        error: (res) => {
          this.snackBar.open(res.detalles?.length ? res.mensaje + ' ' + res.detalles.join('. ') : res.mensaje, 'Cerrar', { duration: 6000 });
        }
      });
    });
  }

  onSubmit(): void {
    if (!this.registroForm.valid) {
      this.snackBar.open('Completá todos los campos requeridos.', 'Cerrar', { duration: 4000 });
      return;
    }

    this.enviando = true;
    const v = this.registroForm.value;
    const fechaStr = (v.fecha as Date).toISOString().slice(0, 10);
    const fechaHoraInicio = `${fechaStr}T${v.hora}:00`;
    const payload: CitaPayload = {
      paciente_id: Number(v.paciente_id),
      fecha_hora_inicio: fechaHoraInicio,
      duracion_minutos: Number(v.duracion_minutos),
      motivo: v.motivo,
      notas: v.notas || null
    };

    if (this.esReagendar && this.idEdicion != null) {
      const id = this.idEdicion;
      this.citaService.actualizarCita(id, payload).pipe(
        switchMap(() => this.citaService.confirmarCita(id))
      ).subscribe({
        next: () => {
          this.enviando = false;
          this.snackBar.open('Cita reagendada correctamente', 'Cerrar', { duration: 4000 });
          this.registroForm.reset();
          this.router.navigate(['/admin/citas']);
        },
        error: (res: ResultadoCita) => {
          this.enviando = false;
          const msg = res.detalles?.length ? res.mensaje + ': ' + res.detalles.join('. ') : res.mensaje;
          this.snackBar.open(msg || 'Error al reagendar la cita', 'Cerrar', { duration: 6000 });
        }
      });
      return;
    }

    const request = this.idEdicion
      ? this.citaService.actualizarCita(this.idEdicion, payload)
      : this.citaService.guardarCita(payload);

    request.subscribe({
      next: (res) => {
        this.enviando = false;
        this.snackBar.open(res.mensaje || 'Cita guardada correctamente', 'Cerrar', { duration: 4000 });
        this.registroForm.reset();
        this.router.navigate(['/admin/citas']);
      },
      error: (res: ResultadoCita) => {
        this.enviando = false;
        const msg = res.detalles?.length ? res.mensaje + ': ' + res.detalles.join('. ') : res.mensaje;
        this.snackBar.open(msg || 'Error al guardar la cita', 'Cerrar', { duration: 6000 });
      }
    });
  }

  getErrorMessage(fieldName: string): string {
    const field = this.registroForm.get(fieldName);
    if (field?.hasError('required')) return 'Este campo es requerido';
    return '';
  }
}
