import { Component, Input, Output, EventEmitter, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

interface SlotItem {
  label: string;       // "09:30"
  disponible: boolean;
}

interface HourGroup {
  etiqueta: string;    // "09h"
  slots: SlotItem[];
}

// Slots generados por el backend: 09:00 a 17:45 cada 15 min
const HORA_INICIO = 9;
const HORA_FIN    = 17;

@Component({
  selector: 'app-slot-picker',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="slot-picker">

      <div class="slot-picker-label">
        <mat-icon>schedule</mat-icon>
        <span>Horario</span>
        @if (horaSeleccionada) {
          <span class="hora-elegida">{{ horaSeleccionada }}</span>
        }
      </div>

      @if (cargando) {
        <div class="slot-loading">
          <mat-spinner diameter="28"></mat-spinner>
          <span>Cargando horarios...</span>
        </div>

      } @else if (!disponibles.length && !horaSeleccionada) {
        <div class="slot-empty">
          <mat-icon>event_available</mat-icon>
          <span>Elegí una fecha y duración para ver los horarios disponibles</span>
        </div>

      } @else {
        <div class="slot-grid">
          @for (grupo of grupos; track grupo.etiqueta) {
            <div class="slot-row">
              <span class="hour-label">{{ grupo.etiqueta }}</span>
              <div class="slot-chips">
                @for (slot of grupo.slots; track slot.label) {
                  <button
                    type="button"
                    class="slot-btn"
                    [class.disponible]="slot.disponible"
                    [class.ocupado]="!slot.disponible"
                    [class.seleccionado]="slot.label === horaSeleccionada"
                    [disabled]="!slot.disponible"
                    (click)="seleccionar(slot)"
                    [title]="slot.disponible ? slot.label : slot.label + ' — ocupado'"
                  >
                    @if (slot.label === horaSeleccionada) {
                      <mat-icon class="check-icon">check</mat-icon>
                    }
                    {{ slot.label }}
                  </button>
                }
              </div>
            </div>
          }
        </div>
      }

    </div>
  `,
  styles: [`
    .slot-picker {
      border: 1px solid rgba(0,0,0,0.12);
      border-radius: 6px;
      padding: 12px 16px;
      background: #fafafa;
    }

    .slot-picker-label {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      font-weight: 500;
      color: rgba(0,0,0,0.6);
      margin-bottom: 12px;

      mat-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
      }

      .hora-elegida {
        margin-left: auto;
        font-size: 13px;
        font-weight: 700;
        color: #1976d2;
        background: #e3f2fd;
        padding: 2px 10px;
        border-radius: 12px;
      }
    }

    .slot-loading {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px 0;
      color: rgba(0,0,0,0.5);
      font-size: 13px;
    }

    .slot-empty {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 16px 0;
      color: rgba(0,0,0,0.4);
      font-size: 13px;
      font-style: italic;

      mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
        opacity: 0.5;
      }
    }

    .slot-grid {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .slot-row {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .hour-label {
      width: 30px;
      flex-shrink: 0;
      font-size: 12px;
      font-weight: 600;
      color: #9e9e9e;
      text-align: right;
    }

    .slot-chips {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }

    .slot-btn {
      display: inline-flex;
      align-items: center;
      gap: 3px;
      padding: 4px 10px;
      border-radius: 16px;
      border: 1.5px solid transparent;
      font-size: 12px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.15s;
      line-height: 1.4;
      font-family: inherit;
      white-space: nowrap;

      .check-icon {
        font-size: 14px;
        width: 14px;
        height: 14px;
      }
    }

    .slot-btn.disponible {
      background: #e8f5e9;
      border-color: #a5d6a7;
      color: #2e7d32;

      &:hover:not(.seleccionado) {
        background: #c8e6c9;
        border-color: #66bb6a;
        transform: translateY(-1px);
        box-shadow: 0 2px 4px rgba(0,0,0,0.12);
      }
    }

    .slot-btn.seleccionado {
      background: #1976d2;
      border-color: #1565c0;
      color: #fff;
      box-shadow: 0 2px 6px rgba(25,118,210,0.4);
    }

    .slot-btn.ocupado {
      background: #f5f5f5;
      border-color: #e0e0e0;
      color: #bdbdbd;
      cursor: not-allowed;
    }
  `]
})
export class SlotPickerComponent implements OnChanges {
  @Input() disponibles: string[] = [];
  @Input() cargando = false;
  @Input() horaSeleccionada: string | null = null;
  @Output() slotElegido = new EventEmitter<string>();

  grupos: HourGroup[] = [];

  ngOnChanges(): void {
    this.buildGrupos();
  }

  private buildGrupos(): void {
    const disponiblesSet = new Set(this.disponibles);
    this.grupos = [];
    for (let h = HORA_INICIO; h <= HORA_FIN; h++) {
      const slots: SlotItem[] = [];
      for (let m = 0; m <= 45; m += 15) {
        const label = `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}`;
        slots.push({ label, disponible: disponiblesSet.has(label) });
      }
      this.grupos.push({ etiqueta: `${h.toString().padStart(2, '0')}h`, slots });
    }
  }

  seleccionar(slot: SlotItem): void {
    if (!slot.disponible) return;
    this.slotElegido.emit(slot.label);
  }
}
