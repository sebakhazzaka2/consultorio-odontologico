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
  hero_eyebrow: '',
  label_servicio: 'Servicio',
  label_servicio_plural: 'Servicios',
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

  describe('applyBranding', () => {
    const VARS = [
      '--brand-primary', '--brand-primary-hover', '--brand-primary-contrast', '--brand-accent',
      '--brand-navy-900', '--brand-navy-700', '--brand-navy-500', '--brand-navy-100', '--brand-navy-50',
    ];
    const get = (k: string) => document.documentElement.style.getPropertyValue(k);
    const clear = () => VARS.forEach(k => document.documentElement.style.removeProperty(k));
    beforeEach(clear);
    afterEach(clear);

    it('sets brand vars for valid hex colors', () => {
      service.applyBranding({ primary: '#8A2BE2', primary_hover: '#6A1FB5', accent: '#FF69B4' });
      expect(get('--brand-primary')).toBe('#8A2BE2');
      expect(get('--brand-primary-hover')).toBe('#6A1FB5');
      expect(get('--brand-accent')).toBe('#FF69B4');
      expect(get('--brand-primary-contrast')).toBe('#FFFFFF');
      expect(get('--brand-navy-900')).toMatch(/^#[0-9a-f]{6}$/);
    });

    it('derives hover and accent from primary when missing', () => {
      service.applyBranding({ primary: '#000000' });
      expect(get('--brand-primary-hover')).toBe('#262626');
      expect(get('--brand-accent')).toBe('#000000');
    });

    it('picks a dark contrast color for light primaries', () => {
      service.applyBranding({ primary: '#FFD700' });
      expect(get('--brand-primary-contrast')).toBe('#0F172A');
    });

    it('ignores invalid values', () => {
      service.applyBranding({
        primary: 'red', primary_hover: '#12', accent: '#000000;background:url(x)',
      });
      VARS.forEach(k => expect(get(k)).toBe(''));
    });

    it('ignores only the invalid field when others are valid', () => {
      service.applyBranding({ primary: '#8A2BE2', primary_hover: 'nope' });
      expect(get('--brand-primary')).toBe('#8A2BE2');
      expect(get('--brand-primary-hover')).toBe('#9c4be6');
    });

    it('does nothing without branding', () => {
      service.applyBranding(undefined);
      service.applyBranding({});
      VARS.forEach(k => expect(get(k)).toBe(''));
    });

    it('is applied from load() and logoUrl is exposed', () => {
      service.load().subscribe();
      httpMock.expectOne(API_URL).flush({
        ...MOCK_CONFIG,
        branding: { primary: '#8A2BE2', logo_url: '/api/uploads/branding/logo.png' },
      });
      expect(get('--brand-primary')).toBe('#8A2BE2');
      expect(service.logoUrl).toBe('/api/uploads/branding/logo.png');
    });

    it('load() without branding leaves vars untouched', () => {
      service.load().subscribe();
      httpMock.expectOne(API_URL).flush(MOCK_CONFIG);
      VARS.forEach(k => expect(get(k)).toBe(''));
      expect(service.logoUrl).toBe('');
    });
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
