import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { ClinicConfigService } from './core/config/clinic-config.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  constructor(
    private titleService: Title,
    private clinicConfigService: ClinicConfigService
  ) {}

  ngOnInit(): void {
    const nombre = this.clinicConfigService.name;
    const tagline = this.clinicConfigService.tagline;
    if (nombre) {
      this.titleService.setTitle(tagline ? `${nombre} — ${tagline}` : nombre);
    }
  }
}
