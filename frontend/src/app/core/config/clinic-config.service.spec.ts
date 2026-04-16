import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ClinicConfigService } from './clinic-config.service';
import { ClinicConfig } from './clinic-config.model';

const MOCK_CONFIG: ClinicConfig = {
  name: 'Test Clinic',
  tagline: 'Testing tagline',
  address: 'Test Address 123',
  phone: '+000000000',
  whatsapp: '+000000001',
  email: 'test@test.com',
  hours: 'Mon–Fri 09:00–18:00'
};

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
    httpMock.expectOne('/assets/config/clinic.json').flush(MOCK_CONFIG);
    expect(service.config()).toEqual(MOCK_CONFIG);
  });

  it('exposes name getter', () => {
    service.load().subscribe();
    httpMock.expectOne('/assets/config/clinic.json').flush(MOCK_CONFIG);
    expect(service.name).toBe('Test Clinic');
  });

  it('exposes tagline getter', () => {
    service.load().subscribe();
    httpMock.expectOne('/assets/config/clinic.json').flush(MOCK_CONFIG);
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
