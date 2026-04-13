import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

interface Servicio {
  id: number;
  nombre: string;
  descripcion: string;
  icono: string;
  imagen: string;
  precio?: string;
}

@Component({
  selector: 'app-servicios',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    RouterModule
  ],
  templateUrl: './servicios.component.html',
  styleUrl: './servicios.component.scss'
})
export class ServiciosComponent {
  servicios: Servicio[] = [
    {
      id: 1,
      nombre: 'Limpieza Dental',
      descripcion: 'Limpieza profesional profunda para mantener tus dientes saludables y brillantes. Incluye eliminación de placa y sarro.',
      icono: 'cleaning_services',
      imagen: 'https://images.unsplash.com/photo-1606811971618-4486c4e32d55?w=400&h=300&fit=crop',
      precio: 'Desde $50'
    },
    {
      id: 2,
      nombre: 'Blanqueamiento Dental',
      descripcion: 'Tratamiento profesional para blanquear y aclarar el color de tus dientes, dándote una sonrisa más brillante.',
      icono: 'auto_awesome',
      imagen: 'https://images.unsplash.com/photo-1609840114035-3c981b7820f17?w=400&h=300&fit=crop',
      precio: 'Desde $200'
    },
    {
      id: 3,
      nombre: 'Ortodoncia',
      descripcion: 'Tratamiento de ortodoncia con brackets tradicionales o invisibles para corregir la alineación dental.',
      icono: 'straighten',
      imagen: 'https://images.unsplash.com/photo-1629909613654-28e377c37b09?w=400&h=300&fit=crop',
      precio: 'Consulta gratuita'
    },
    {
      id: 4,
      nombre: 'Implantes Dentales',
      descripcion: 'Solución permanente para reemplazar dientes faltantes con implantes de titanio de alta calidad.',
      icono: 'medical_services',
      imagen: 'https://images.unsplash.com/photo-1551601651-2a8555f1a136?w=400&h=300&fit=crop',
      precio: 'Desde $800'
    },
    {
      id: 5,
      nombre: 'Endodoncia',
      descripcion: 'Tratamiento de conducto para salvar dientes infectados o dañados, eliminando el dolor y preservando la pieza dental.',
      icono: 'healing',
      imagen: 'https://images.unsplash.com/photo-1609840114035-3c981b7820f17?w=400&h=300&fit=crop',
      precio: 'Desde $300'
    },
    {
      id: 6,
      nombre: 'Odontopediatría',
      descripcion: 'Atención dental especializada para niños, creando un ambiente amigable y cómodo para los más pequeños.',
      icono: 'child_care',
      imagen: 'https://images.unsplash.com/photo-1551601651-2a8555f1a136?w=400&h=300&fit=crop',
      precio: 'Desde $60'
    },
    {
      id: 7,
      nombre: 'Prótesis Dentales',
      descripcion: 'Diseño y colocación de prótesis dentales removibles o fijas para restaurar la funcionalidad y estética.',
      icono: 'precision_manufacturing',
      imagen: 'https://images.unsplash.com/photo-1629909613654-28e9909613654?w=400&h=300&fit=crop',
      precio: 'Desde $400'
    },
    {
      id: 8,
      nombre: 'Cirugía Oral',
      descripcion: 'Procedimientos quirúrgicos dentales incluyendo extracciones, cirugía de muelas del juicio y más.',
      icono: 'local_hospital',
      imagen: 'https://images.unsplash.com/photo-1606811971618-4486c4e32d55?w=400&h=300&fit=crop',
      precio: 'Consulta personalizada'
    }
  ];
}

