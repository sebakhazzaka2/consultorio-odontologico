import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <header class="page-header">
      <div class="page-header__left">
        @if (icon) {
          <mat-icon class="page-header__icon">{{ icon }}</mat-icon>
        }
        <div>
          <h1 class="page-header__title">{{ title }}</h1>
          @if (subtitle) {
            <p class="page-header__sub">{{ subtitle }}</p>
          }
        </div>
      </div>
      <div class="page-header__actions">
        <ng-content />
      </div>
    </header>
  `,
  styleUrl: './page-header.component.scss'
})
export class PageHeaderComponent {
  @Input() title: string = '';
  @Input() subtitle?: string;
  @Input() icon?: string;
}
