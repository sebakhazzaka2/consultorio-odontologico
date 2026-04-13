import { Routes } from '@angular/router';
import { AdminLayoutComponent } from './admin-layout/admin-layout.component';
import { PacientesListadoComponent } from './pacientes/pacientes-listado.component';
import { CitasListadoComponent } from './citas/citas-listado.component';
import { CitaFormComponent } from './citas/cita-form.component';

export const adminRoutes: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
      { path: 'pacientes', component: PacientesListadoComponent },
      { path: 'citas', component: CitasListadoComponent },
      { path: 'citas/nueva', component: CitaFormComponent },
      { path: 'citas/editar/:id', component: CitaFormComponent },
      { path: 'citas/reagendar/:id', component: CitaFormComponent },
      {
        path: 'pacientes/:id',
        loadComponent: () => import('./pacientes/paciente-detalle.component').then(m => m.PacienteDetalleComponent)
      },
      {
        path: 'tratamientos',
        loadComponent: () => import('./tratamientos/tratamientos-listado.component').then(m => m.TratamientosListadoComponent)
      },
      { path: '', redirectTo: 'pacientes', pathMatch: 'full' }
    ]
  }
];
