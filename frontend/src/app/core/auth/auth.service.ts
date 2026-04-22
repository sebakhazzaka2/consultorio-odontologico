import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private http: HttpClient, private router: Router) {}

  login(email: string, password: string): Observable<void> {
    return this.http
      .post<{ token: string }>(environment.apiUrl + '/api/auth/login', { email, password })
      .pipe(
        map((res) => {
          localStorage.setItem('auth_token', res.token);
          return undefined;
        })
      );
  }

  logout(): void {
    localStorage.removeItem('auth_token');
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem('auth_token');
  }

  isLoggedIn(): boolean {
    return this.getToken() !== null;
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http
      .patch<{ message: string }>(environment.apiUrl + '/api/auth/change-password', {
        currentPassword,
        newPassword
      })
      .pipe(map(() => undefined));
  }
}
