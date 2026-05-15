import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ClinicConfigService } from './clinic-config.service';
import { ClinicConfig } from './clinic-config.model';
import { environment } from '../../../environments/environment';

const MOCK_CONFIG: ClinicConfig = {
  nombre: 'Test Clinic',
  tagline: 'Testing tagline',
  direccion: 'Test Address 123',
  ciudad: 'Montevideo',
  horario: 'Lun–Vie 09:00–18:00',
  horario_apertura: '09:00',
  horario_cierre: '18:00',
  dias_laborales: '1,2,3,4,5',
  telefono: '+000000000',
  whatsapp: '+000000001',
  email: 'test@test.com',
  nosotros: '',
  foto_ubicacion_url: '',
  reviews_enabled: false,
  stats_pacientes: '',
  stats_anos_experiencia: '',
  stats_calificacion: '',
  hero_imagenes: [],
  features: [],
  label_servicio: 'Servicio',
  label_historial: 'Historial clínico',
  features_title: '',
  features_subtitle: '',
};

const API_URL = `${environment.apiUrl}/api/public/config`;

describe('ClinicConfigService', () => {
  let service: ClinicConfigService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ClinicConfigService]
    });
    service = TestBed.inject(ClinicConfigService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('starts with null config signal', () => {
    expect(service.config()).toBeNull();
  });

  it('populates signal after load()', () => {
    service.load().subscribe();
    httpMock.expectOne(API_URL).flush(MOCK_CONFIG);
    expect(service.config()).toEqual(MOCK_CONFIG);
  });

  it('exposes name getter', () => {
    service.load().subscribe();
    httpMock.expectOne(API_URL).flush(MOCK_CONFIG);
    expect(service.name).toBe('Test Clinic');
  });

  it('exposes tagline getter', () => {
    service.load().subscribe();
    httpMock.expectOne(API_URL).flush(MOCK_CONFIG);
    expect(service.tagline).toBe('Testing tagline');
  });

  it('getters return empty string before load', () => {
    expect(service.name).toBe('');
    expect(service.tagline).toBe('');
    expect(service.address).toBe('');
    expect(service.phone).toBe('');
    expect(service.whatsapp).toBe('');
    expect(service.email).toBe('');
    expect(service.hours).toBe('');
  });
});
