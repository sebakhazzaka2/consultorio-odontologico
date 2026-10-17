import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';
import { PacienteDetalleComponent } from './paciente-detalle.component';
import { PacienteService, ResultadoPaciente } from './paciente.service';
import { HistorialService } from './historial.service';
import { PagoService } from './pago.service';
import { Paciente } from '../../../core/models/paciente.model';

describe('PacienteDetalleComponent - recordatorios por email', () => {
  let pacienteSvc: jasmine.SpyObj<PacienteService>;

  const base: Paciente = {
    id: 7, nombre: 'Ana', apellido: 'Pérez', telefono: '099123456',
    email: 'ana@test.com', fecha_nacimiento: '1990-05-01', recordatorios_email: true
  };

  function crear(paciente: Paciente): ComponentFixture<PacienteDetalleComponent> {
    pacienteSvc = jasmine.createSpyObj('PacienteService', ['obtener', 'actualizar']);
    pacienteSvc.obtener.and.returnValue(of({ ...paciente }));
    const historialSvc = jasmine.createSpyObj('HistorialService', ['listarPorPaciente']);
    historialSvc.listarPorPaciente.and.returnValue(of([]));
    const pagoSvc = jasmine.createSpyObj('PagoService', ['listarPorPaciente', 'getSaldo']);
    pagoSvc.listarPorPaciente.and.returnValue(of([]));
    pagoSvc.getSaldo.and.returnValue(of(null));

    TestBed.configureTestingModule({
      imports: [PacienteDetalleComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: PacienteService, useValue: pacienteSvc },
        { provide: HistorialService, useValue: historialSvc },
        { provide: PagoService, useValue: pagoSvc },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } } }
      ]
    });
    const fixture = TestBed.createComponent(PacienteDetalleComponent);
    fixture.detectChanges();
    return fixture;
  }

  function toggle(f: ComponentFixture<PacienteDetalleComponent>): MatSlideToggle {
    f.detectChanges();
    return f.debugElement.query(By.directive(MatSlideToggle)).componentInstance as MatSlideToggle;
  }

  function clickToggle(f: ComponentFixture<PacienteDetalleComponent>): void {
    f.detectChanges();
    (f.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('mat-slide-toggle button')!.click();
    f.detectChanges();
  }

  it('deshabilita el toggle y muestra el hint si no hay email', () => {
    const f = crear({ ...base, email: null });
    expect(toggle(f).disabled).toBeTrue();
    expect((f.nativeElement as HTMLElement).textContent).toContain('Sin email cargado');
  });

  it('habilita el toggle con email y refleja el valor', () => {
    const f = crear(base);
    expect(toggle(f).disabled).toBeFalse();
    expect(toggle(f).checked).toBeTrue();
  });

  it('al cambiar envía el payload completo con recordatorios_email', () => {
    const f = crear(base);
    pacienteSvc.actualizar.and.returnValue(of({ ok: true } as ResultadoPaciente));
    clickToggle(f);
    expect(pacienteSvc.actualizar).toHaveBeenCalledWith(7, {
      nombre: 'Ana', apellido: 'Pérez', telefono: '099123456',
      email: 'ana@test.com', fecha_nacimiento: '1990-05-01', recordatorios_email: false
    });
    expect(f.componentInstance.paciente?.recordatorios_email).toBeFalse();
  });

  it('si falla revierte el toggle', () => {
    const f = crear(base);
    pacienteSvc.actualizar.and.returnValue(throwError(() => ({ ok: false, mensaje: 'boom' })));
    clickToggle(f);
    expect(f.componentInstance.paciente?.recordatorios_email).toBeTrue();
    expect(toggle(f).checked).toBeTrue();
    expect(f.componentInstance.guardandoRecordatorios).toBeFalse();
  });
});
