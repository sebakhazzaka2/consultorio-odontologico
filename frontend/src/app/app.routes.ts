import { Routes } from '@angular/router';
import { HomeComponent } from './features/public/home/home.component';
import { ServiciosComponent } from './features/public/servicios/servicios.component';
import { LoginComponent } from './features/auth/login/login.component';
import { CitasListadoComponent } from './features/admin/citas/citas-listado.component';
import { PacientesListadoComponent } from './features/admin/pacientes/pacientes-listado.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'servicios', component: ServiciosComponent },
  { path: 'registro', component: LoginComponent },
  { path: 'registro/editar/:id', component: LoginComponent },
  { path: 'citas', component: CitasListadoComponent },
  { path: 'pacientes', component: PacientesListadoComponent },
  { path: '**', redirectTo: '' }
];

