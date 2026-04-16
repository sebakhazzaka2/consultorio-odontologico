import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PublicTratamientoService } from './services/public-tratamiento.service';
import { PublicTratamiento } from './models/public-tratamiento.model';

@Component({
  selector: 'app-public',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './public.component.html',
  styleUrl: './public.component.scss'
})
export class PublicComponent implements OnInit {
  tratamientos: PublicTratamiento[] = [];

  constructor(private readonly tratamientoService: PublicTratamientoService) {}

  ngOnInit(): void {
    this.tratamientoService.getActivos().subscribe(t => this.tratamientos = t);
  }
}
