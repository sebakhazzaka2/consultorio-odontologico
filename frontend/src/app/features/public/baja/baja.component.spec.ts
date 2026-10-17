import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { Meta } from '@angular/platform-browser';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { BajaComponent } from './baja.component';

describe('BajaComponent', () => {
  const URL = '/api/public/notificaciones/baja';
  let httpMock: HttpTestingController;

  function crear(token: string | null): ComponentFixture<BajaComponent> {
    TestBed.configureTestingModule({
      imports: [BajaComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(token ? { token } : {}) } }
        }
      ]
    });
    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(BajaComponent);
    fixture.detectChanges();
    return fixture;
  }

  function texto(f: ComponentFixture<BajaComponent>): string {
    f.detectChanges();
    return (f.nativeElement as HTMLElement).textContent ?? '';
  }

  afterEach(() => httpMock.verify());

  it('sin token muestra enlace inválido y no hace requests', () => {
    const f = crear(null);
    expect(f.componentInstance.vista).toBe('invalido');
    expect(texto(f)).toContain('El enlace no es válido');
  });

  it('token inválido (400) muestra enlace inválido o expirado', () => {
    const f = crear('mal');
    const req = httpMock.expectOne(r => r.url === URL && r.params.get('token') === 'mal');
    req.flush({ error: 'x', message: 'x' }, { status: 400, statusText: 'Bad Request' });
    expect(texto(f)).toContain('El enlace no es válido o expiró');
  });

  it('error de red muestra mensaje genérico', () => {
    const f = crear('t');
    httpMock.expectOne(r => r.url === URL).flush(null, { status: 500, statusText: 'Server Error' });
    expect(f.componentInstance.vista).toBe('error');
    expect(texto(f)).toContain('Algo salió mal');
  });

  it('estado BAJA muestra que ya no recibe recordatorios', () => {
    const f = crear('t');
    httpMock.expectOne(r => r.url === URL).flush({ clinica: 'Neo Dental', estado: 'BAJA' });
    expect(texto(f)).toContain('Ya no recibís recordatorios de Neo Dental');
  });

  it('estado ACTIVO permite confirmar la baja y muestra éxito', () => {
    const f = crear('t');
    httpMock.expectOne(r => r.url === URL).flush({ clinica: 'Neo Dental', estado: 'ACTIVO' });
    expect(texto(f)).toContain('Dejar de recibir recordatorios');

    (f.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.btn-baja')!.click();
    const post = httpMock.expectOne(r => r.url === URL && r.method === 'POST');
    expect(post.request.body).toEqual({ token: 't' });
    post.flush(null, { status: 204, statusText: 'No Content' });

    expect(texto(f)).toContain('no vas a recibir más recordatorios');
    expect(texto(f)).toContain('pedile a Neo Dental que los reactive');
  });

  it('error al confirmar mantiene el botón y muestra el error', () => {
    const f = crear('t');
    httpMock.expectOne(r => r.url === URL).flush({ clinica: 'Neo Dental', estado: 'ACTIVO' });
    f.componentInstance.confirmar();
    httpMock.expectOne(r => r.url === URL && r.method === 'POST')
      .flush(null, { status: 500, statusText: 'Server Error' });
    expect(f.componentInstance.vista).toBe('confirmar');
    expect(f.componentInstance.enviando).toBeFalse();
    expect(texto(f)).toContain('No pudimos procesar tu solicitud');
  });

  it('agrega meta noindex y lo quita al destruir', () => {
    const f = crear(null);
    const meta = TestBed.inject(Meta);
    expect(meta.getTag('name="robots"')?.content).toBe('noindex');
    f.destroy();
    expect(meta.getTag('name="robots"')).toBeNull();
  });
});
