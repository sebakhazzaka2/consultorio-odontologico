import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { EMPTY, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from './auth/auth.service';

export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401) {
        snackBar.open('Sesión expirada. Por favor iniciá sesión nuevamente.', 'Cerrar', {
          duration: 5000,
          panelClass: ['snack-error']
        });
        authService.logout();
        return EMPTY;
      }

      if (err.status === 0) {
        snackBar.open('Sin conexión al servidor. Verificá tu red.', 'Cerrar', {
          duration: 6000,
          panelClass: ['snack-error']
        });
      }

      return throwError(() => err);
    })
  );
};
