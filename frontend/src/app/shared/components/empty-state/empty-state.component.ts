import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <div class="empty-state">
      <div class="empty-state__icon-wrap">
        <mat-icon>{{ icon }}</mat-icon>
      </div>
      <h3 class="empty-state__title">{{ title }}</h3>
      @if (description) {
        <p class="empty-state__desc">{{ description }}</p>
      }
      <ng-content />
    </div>
  `,
  styleUrl: './empty-state.component.scss'
})
export class EmptyStateComponent {
  @Input() icon: string = 'inbox';
  @Input() title: string = '';
  @Input() description?: string;
}
