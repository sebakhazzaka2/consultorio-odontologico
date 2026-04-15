import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CitaService, ResultadoCita } from './cita.service';
import { Cita, CitaPayload } from '../../../core/models/cita.model';

describe('CitaService', () => {
  let service: CitaService;
  let httpMock: HttpTestingController;

  const API_URL = 'https://api.tu-consultorio.com/api/citas';

  const mockCita: Cita = {
    id: 1,
    paciente_id: 1,
    nombre_paciente: 'Juan',
    apellido_paciente: 'García',
    fecha_hora_inicio: '2026-06-01T09:00:00',
    duracion_minutos: 30,
    estado: 'CONFIRMADA',
    motivo: 'Control',
    notas: null
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CitaService]
    });

    service = TestBed.inject(CitaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listarCitas() debería hacer GET y retornar lista de citas', () => {
    service.listarCitas().subscribe(citas => {
      expect(citas.length).toBe(1);
      expect(citas[0]).toEqual(mockCita);
    });

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('GET');
    req.flush([mockCita]);
  });

  it('guardarCita() debería hacer POST con el payload correcto', () => {
    const payload: CitaPayload = {
      paciente_id: 1,
      fecha_hora_inicio: '2026-06-01T09:00:00',
      duracion_minutos: 30,
      motivo: 'Control'
    };

    service.guardarCita(payload).subscribe(resultado => {
      expect(resultado.ok).toBeTrue();
    });

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({});
  });

  it('cancelarCita() debería hacer PATCH al endpoint correcto', () => {
    service.cancelarCita(1).subscribe(cita => {
      expect(cita.estado).toBe('CANCELADA');
    });

    const req = httpMock.expectOne(`${API_URL}/1/cancelar`);
    expect(req.request.method).toBe('PATCH');
    req.flush({ ...mockCita, estado: 'CANCELADA' });
  });

  it('getDisponibilidad() debería hacer GET con los query params correctos', () => {
    const slots = ['09:00', '09:15', '09:30'];

    service.getDisponibilidad('2026-06-01', 30).subscribe(resultado => {
      expect(resultado).toEqual(slots);
    });

    const req = httpMock.expectOne(r =>
      r.url === `${API_URL}/disponibilidad` &&
      r.params.get('fecha') === '2026-06-01' &&
      r.params.get('duracion') === '30'
    );
    expect(req.request.method).toBe('GET');
    req.flush(slots);
  });

  it('listarCitas() debería propagar el error con ok: false cuando el backend falla', () => {
    service.listarCitas().subscribe({
      next: () => fail('debería haber fallado'),
      error: (err: ResultadoCita) => {
        expect(err.ok).toBeFalse();
        expect(err.mensaje).toBe('No autorizado');
      }
    });

    const req = httpMock.expectOne(API_URL);
    req.flush({ error: 'No autorizado' }, { status: 401, statusText: 'Unauthorized' });
  });
});
