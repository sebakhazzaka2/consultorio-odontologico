import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
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
import { EmailNotificationService } from '../../services/email-notification.service';
import { CitaService } from '../../services/cita.service';
import { PacienteService } from '../../services/paciente.service';
import { Cita } from '../../models/cita.model';
import { Paciente } from '../../models/paciente.model';
import { ResultadoCita } from '../../services/cita.service';
import { PacienteFormDialogComponent } from '../pacientes/paciente-form-dialog.component';
import type { PacientePayload } from '../../models/paciente.model';
import type { CitaPayload } from '../../models/cita.model';

@Component({
  selector: 'app-registro',
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
    MatDialogModule
  ],
  templateUrl: './registro.component.html',
  styleUrl: './registro.component.scss'
})
export class RegistroComponent implements OnInit {
  registroForm: FormGroup;
  idEdicion: number | null = null;
  enviando = false;
  pacientes: Paciente[] = [];

  horarios = [
    '09:00', '09:30', '10:00', '10:30', '11:00', '11:30',
    '14:00', '14:30', '15:00', '15:30', '16:00', '16:30', '17:00', '17:30'
  ];

  minDate = new Date();

  constructor(
    private fb: FormBuilder,
    private snackBar: MatSnackBar,
    private router: Router,
    private route: ActivatedRoute,
    private emailService: EmailNotificationService,
    private citaService: CitaService,
    private pacienteService: PacienteService,
    private dialog: MatDialog
  ) {
    this.registroForm = this.fb.group({
      paciente_id: ['', Validators.required],
      fecha: ['', Validators.required],
      horario: ['', Validators.required],
      motivo: ['']
    });
  }

  get esEdicion(): boolean {
    return this.idEdicion != null;
  }

  ngOnInit(): void {
    this.cargarPacientes();
    this.route.params.subscribe((params) => {
      const id = params['id'];
      if (id) {
        this.idEdicion = parseInt(id, 10);
        if (!isNaN(this.idEdicion)) {
          this.cargarCitaParaEdicion();
        }
      } else {
        this.idEdicion = null;
      }
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
        const fechaDate = cita.fecha ? new Date(cita.fecha + 'T12:00:00') : null;
        this.registroForm.patchValue({
          paciente_id: cita.paciente_id,
          fecha: fechaDate,
          horario: cita.hora || '',
          motivo: cita.motivo || ''
        });
      },
      error: (res: ResultadoCita) => {
        const msg = res.detalles?.length ? res.mensaje + ' ' + res.detalles.join('. ') : res.mensaje;
        this.snackBar.open(msg || 'Error al cargar la cita', 'Cerrar', { duration: 6000 });
        this.router.navigate(['/citas']);
      }
    });
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
      this.snackBar.open('Selecciona un paciente, fecha y horario.', 'Cerrar', { duration: 4000 });
      return;
    }

    this.enviando = true;
    const v = this.registroForm.value;
    const fechaStr = typeof v.fecha === 'string' ? v.fecha.slice(0, 10) : (v.fecha as Date).toISOString().slice(0, 10);
    const payload: CitaPayload = {
      paciente_id: Number(v.paciente_id),
      fecha: fechaStr,
      hora: v.horario,
      motivo: v.motivo || null,
      estado: 'pendiente'
    };

    const request = this.idEdicion
      ? this.citaService.actualizarCita(this.idEdicion, payload)
      : this.citaService.guardarCita(payload);

    request.subscribe({
      next: (res) => {
        this.enviando = false;
        this.snackBar.open(res.mensaje || 'Cita guardada correctamente', 'Cerrar', { duration: 4000 });
        if (!this.idEdicion) {
          const pac = this.pacientes.find((p) => p.id === Number(v.paciente_id));
          if (pac) {
            this.emailService.enviarNotificacionCita({
              nombre: pac.nombre,
              apellido: pac.apellido,
              email: pac.email,
              telefono: pac.telefono,
              servicio: '1',
              fecha: fechaStr,
              horario: v.horario,
              motivo: v.motivo || ''
            }).then(() => {});
          }
        }
        this.registroForm.reset();
        this.router.navigate(['/citas']);
      },
      error: (res: ResultadoCita) => {
        this.enviando = false;
        // Mostrar mensaje y detalles del backend cuando rechace (validación, horario ocupado, etc.)
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
