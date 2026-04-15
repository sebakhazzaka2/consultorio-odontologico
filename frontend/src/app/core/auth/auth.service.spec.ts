import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let mockRouter: jasmine.SpyObj<Router>;

  const API_LOGIN = 'https://api.tu-consultorio.com/api/auth/login';

  beforeEach(() => {
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: Router, useValue: mockRouter }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('login() debería hacer POST con las credenciales y guardar el token', () => {
    const token = 'eyJhbGciOiJIUzI1NiJ9.test-token';

    service.login('admin@test.com', 'clave123').subscribe();

    const req = httpMock.expectOne(API_LOGIN);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'admin@test.com', password: 'clave123' });
    req.flush({ token });

    expect(localStorage.getItem('auth_token')).toBe(token);
  });

  it('logout() debería eliminar el token y navegar a /login', () => {
    localStorage.setItem('auth_token', 'test-token');

    service.logout();

    expect(localStorage.getItem('auth_token')).toBeNull();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('isLoggedIn() debería retornar false cuando no hay token', () => {
    localStorage.clear();
    expect(service.isLoggedIn()).toBeFalse();
  });

  it('isLoggedIn() debería retornar true cuando hay token', () => {
    localStorage.setItem('auth_token', 'token-existente');
    expect(service.isLoggedIn()).toBeTrue();
  });
});
