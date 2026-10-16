import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/public/public.component').then(m => m.PublicComponent)
  },
  { path: 'servicios', redirectTo: '', pathMatch: 'full' },
  {
    path: 'reservar',
    loadComponent: () => import('./features/public/reserva/reserva.component').then(m => m.ReservaComponent)
  },
  { path: 'login', component: LoginComponent },
  {
    path: 'admin',
    canActivate: [authGuard],
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.adminRoutes)
  },
  { path: '**', redirectTo: '' }
];
