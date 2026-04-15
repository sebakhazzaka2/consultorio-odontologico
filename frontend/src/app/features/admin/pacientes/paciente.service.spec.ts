import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PacienteService, ResultadoPaciente } from './paciente.service';
import { Paciente, PacientePayload } from '../../../core/models/paciente.model';

describe('PacienteService', () => {
  let service: PacienteService;
  let httpMock: HttpTestingController;

  const API_URL = 'https://api.tu-consultorio.com/api/pacientes';

  const mockPaciente: Paciente = {
    id: 1,
    nombre: 'Juan',
    apellido: 'García',
    telefono: '099123456',
    email: 'juan@test.com',
    fecha_nacimiento: null
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PacienteService]
    });

    service = TestBed.inject(PacienteService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listar() debería hacer GET y retornar lista de pacientes', () => {
    service.listar().subscribe(pacientes => {
      expect(pacientes.length).toBe(1);
      expect(pacientes[0]).toEqual(mockPaciente);
    });

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('GET');
    req.flush([mockPaciente]);
  });

  it('obtener() debería hacer GET al endpoint correcto', () => {
    service.obtener(1).subscribe(paciente => {
      expect(paciente).toEqual(mockPaciente);
    });

    const req = httpMock.expectOne(`${API_URL}/1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockPaciente);
  });

  it('crear() debería hacer POST y retornar ok: true', () => {
    const payload: PacientePayload = {
      nombre: 'Juan', apellido: 'García',
      telefono: '099123456', email: 'juan@test.com'
    };

    service.crear(payload).subscribe(resultado => {
      expect(resultado.ok).toBeTrue();
    });

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(mockPaciente);
  });

  it('listar() debería propagar el error con ok: false cuando el backend falla', () => {
    service.listar().subscribe({
      next: () => fail('debería haber fallado'),
      error: (err: ResultadoPaciente) => {
        expect(err.ok).toBeFalse();
        expect(err.mensaje).toBe('No autorizado');
      }
    });

    const req = httpMock.expectOne(API_URL);
    req.flush({ error: 'No autorizado' }, { status: 401, statusText: 'Unauthorized' });
  });
});
