import { Routes } from '@angular/router';
import { PacientesListadoComponent } from './pacientes/pacientes-listado.component';
import { CitasListadoComponent } from './citas/citas-listado.component';
import { CitaFormComponent } from './citas/cita-form.component';

export const adminRoutes: Routes = [
  { path: 'pacientes', component: PacientesListadoComponent },
  { path: 'citas', component: CitasListadoComponent },
  { path: 'citas/nueva', component: CitaFormComponent },
  { path: 'citas/editar/:id', component: CitaFormComponent }
];
