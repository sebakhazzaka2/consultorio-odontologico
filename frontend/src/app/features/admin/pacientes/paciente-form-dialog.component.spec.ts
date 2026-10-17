import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { PacienteFormDialogComponent, PacienteFormDialogData } from './paciente-form-dialog.component';
import { Paciente } from '../../../core/models/paciente.model';

describe('PacienteFormDialogComponent', () => {
  let dialogRef: jasmine.SpyObj<MatDialogRef<PacienteFormDialogComponent>>;

  function crear(data: PacienteFormDialogData): PacienteFormDialogComponent {
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    TestBed.configureTestingModule({
      imports: [PacienteFormDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    });
    const fixture = TestBed.createComponent(PacienteFormDialogComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  const sinEmail: Paciente = {
    id: 1, nombre: 'Ana', apellido: 'Pérez', telefono: '099123456', email: null, fecha_nacimiento: null
  };

  it('permite editar un paciente sin email y manda email null', () => {
    const c = crear({ paciente: sinEmail });
    expect(c.form.valid).toBeTrue();
    c.guardar();
    expect(dialogRef.close).toHaveBeenCalledWith(jasmine.objectContaining({ email: null }));
  });

  it('crea un paciente con email vacío como null', () => {
    const c = crear({});
    c.form.patchValue({ nombre: 'Juan', apellido: 'García', telefono: '099123456', email: '' });
    c.guardar();
    expect(dialogRef.close).toHaveBeenCalledWith(jasmine.objectContaining({ email: null }));
  });

  it('manda el email cuando se informa', () => {
    const c = crear({});
    c.form.patchValue({ nombre: 'Juan', apellido: 'García', telefono: '099123456', email: 'juan@test.com' });
    c.guardar();
    expect(dialogRef.close).toHaveBeenCalledWith(jasmine.objectContaining({ email: 'juan@test.com' }));
  });

  it('rechaza un email con formato inválido', () => {
    const c = crear({});
    c.form.patchValue({ nombre: 'Juan', apellido: 'García', telefono: '099123456', email: 'no-es-email' });
    expect(c.isFormSubmittable).toBeFalse();
    c.guardar();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });
});
