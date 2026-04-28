import { Component, Input, HostBinding } from '@angular/core';
import { CommonModule } from '@angular/common';

type LogoSize = 'sm' | 'md' | 'lg' | 'xl';

const SIZE_PX: Record<LogoSize, string> = {
  sm: '20px',
  md: '28px',
  lg: '40px',
  xl: '64px',
};

@Component({
  selector: 'app-brand-logo',
  standalone: true,
  imports: [CommonModule],
  template: `
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 28"
      fill="none"
      [style.width]="sizePx"
      [style.height]="sizePx"
      aria-hidden="true">
      <path d="M12 1L2 4v10c0 6.5 4.5 11.5 10 13 5.5-1.5 10-6.5 10-13V4L12 1z" fill="currentColor"/>
      <path d="M10.5 9h3v3h3v3h-3v3h-3v-3h-3v-3h3V9z" fill="#fff"/>
    </svg>
    <span *ngIf="showWordmark" class="wordmark" aria-label="TurnosUy">
      <span class="wordmark-nexa">Turnos</span><span class="wordmark-clinic">Uy</span>
    </span>
  `,
  styleUrl: './brand-logo.component.scss'
})
export class BrandLogoComponent {
  @Input() size: LogoSize = 'md';
  @Input() showWordmark: boolean = false;

  get sizePx(): string {
    return SIZE_PX[this.size];
  }
}
