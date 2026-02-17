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
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { EmailNotificationService } from '../../services/email-notification.service';

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
    MatSnackBarModule
  ],
  templateUrl: './registro.component.html',
  styleUrl: './registro.component.scss'
})
export class RegistroComponent implements OnInit {
  registroForm: FormGroup;
  servicios = [
    { id: 1, nombre: 'Limpieza Dental' },
    { id: 2, nombre: 'Blanqueamiento Dental' },
    { id: 3, nombre: 'Ortodoncia' },
    { id: 4, nombre: 'Implantes Dentales' },
    { id: 5, nombre: 'Endodoncia' },
    { id: 6, nombre: 'Odontopediatría' },
    { id: 7, nombre: 'Prótesis Dentales' },
    { id: 8, nombre: 'Cirugía Oral' }
  ];

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
    private emailService: EmailNotificationService
  ) {
    this.registroForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(2)]],
      apellido: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
      servicio: ['', Validators.required],
      fecha: ['', Validators.required],
      horario: ['', Validators.required],
      motivo: ['']
    });
  }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      if (params['servicio']) {
        this.registroForm.patchValue({
          servicio: params['servicio']
        });
      }
    });
  }

  async onSubmit() {
    if (this.registroForm.valid) {
      const formValue = this.registroForm.value;
      
      // Preparar datos para el email
      const datosCita = {
        nombre: formValue.nombre,
        apellido: formValue.apellido,
        email: formValue.email,
        telefono: formValue.telefono,
        servicio: formValue.servicio,
        fecha: formValue.fecha.toISOString(),
        horario: formValue.horario,
        motivo: formValue.motivo || ''
      };

      // Enviar notificación por email
      const emailEnviado = await this.emailService.enviarNotificacionCita(datosCita);
      
      if (emailEnviado) {
        this.snackBar.open('¡Cita agendada exitosamente! Has recibido una confirmación por email.', 'Cerrar', {
          duration: 5000,
          horizontalPosition: 'center',
          verticalPosition: 'top'
        });
      } else {
        this.snackBar.open('¡Cita agendada! Hubo un problema al enviar la confirmación por email, pero tu cita fue registrada.', 'Cerrar', {
          duration: 5000,
          horizontalPosition: 'center',
          verticalPosition: 'top'
        });
      }

      // Limpiar formulario
      this.registroForm.reset();
      
      // Opcional: redirigir después de unos segundos
      setTimeout(() => {
        this.router.navigate(['/']);
      }, 2000);
    } else {
      this.snackBar.open('Por favor, completa todos los campos requeridos correctamente.', 'Cerrar', {
        duration: 4000,
        horizontalPosition: 'center',
        verticalPosition: 'top'
      });
    }
  }

  getErrorMessage(fieldName: string): string {
    const field = this.registroForm.get(fieldName);
    if (field?.hasError('required')) {
      return 'Este campo es requerido';
    }
    if (field?.hasError('email')) {
      return 'Email inválido';
    }
    if (field?.hasError('pattern')) {
      return 'Formato inválido';
    }
    if (field?.hasError('minlength')) {
      return 'Mínimo 2 caracteres';
    }
    return '';
  }
}

