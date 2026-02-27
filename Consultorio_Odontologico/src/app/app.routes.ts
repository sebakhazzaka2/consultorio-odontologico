import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { ServiciosComponent } from './components/servicios/servicios.component';
import { RegistroComponent } from './components/registro/registro.component';
import { CitasListadoComponent } from './components/citas/citas-listado.component';
import { PacientesListadoComponent } from './components/pacientes/pacientes-listado.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'servicios', component: ServiciosComponent },
  { path: 'registro', component: RegistroComponent },
  { path: 'registro/editar/:id', component: RegistroComponent },
  { path: 'citas', component: CitasListadoComponent },
  { path: 'pacientes', component: PacientesListadoComponent },
  { path: '**', redirectTo: '' }
];

