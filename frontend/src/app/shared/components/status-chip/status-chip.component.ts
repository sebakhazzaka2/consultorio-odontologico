import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-status-chip',
  standalone: true,
  imports: [],
  template: `<span class="chip chip-{{ variant }}">{{ label }}</span>`,
})
export class StatusChipComponent {
  @Input() estado: string = '';

  get variant(): string {
    switch (this.estado) {
      case 'CONFIRMADA': return 'success';
      case 'PENDIENTE':  return 'warning';
      case 'CANCELADA':  return 'neutral';
      case 'COMPLETADA': return 'info';
      default:           return 'neutral';
    }
  }

  get label(): string {
    switch (this.estado) {
      case 'CONFIRMADA': return 'Confirmada';
      case 'PENDIENTE':  return 'Pendiente';
      case 'CANCELADA':  return 'Cancelada';
      case 'COMPLETADA': return 'Completada';
      default:           return this.estado;
    }
  }
}
