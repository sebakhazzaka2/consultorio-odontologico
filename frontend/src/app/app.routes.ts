import { Routes } from '@angular/router';
import { HomeComponent } from './features/public/home/home.component';
import { ServiciosComponent } from './features/public/servicios/servicios.component';
import { LoginComponent } from './features/auth/login/login.component';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'servicios', component: ServiciosComponent },
  { path: 'login', component: LoginComponent },
  {
    path: 'admin',
    canActivate: [authGuard],
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.adminRoutes)
  },
  { path: '**', redirectTo: '' }
];
