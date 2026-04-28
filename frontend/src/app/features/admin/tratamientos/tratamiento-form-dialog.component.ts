import { Component, Inject, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Tratamiento, TratamientoPayload } from '../../../core/models/tratamiento.model';
import { TratamientoService } from './tratamiento.service';
import { environment } from '../../../../environments/environment';

export interface TratamientoFormDialogData {
  tratamiento: Tratamiento | null;
}

export interface TratamientoFormDialogResult {
  payload: TratamientoPayload;
  fotoFile: File | null;
}

@Component({
  selector: 'app-tratamiento-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.tratamiento ? 'Editar tratamiento' : 'Nuevo tratamiento' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="nombre" />
          <mat-error *ngIf="form.get('nombre')?.hasError('required')">Requerido</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Descripción (opcional)</mat-label>
          <textarea matInput formControlName="descripcion" rows="3"></textarea>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Precio (UY$)</mat-label>
          <input matInput type="number" formControlName="precio" min="0.01" step="0.01" />
          <mat-error *ngIf="form.get('precio')?.hasError('required')">Requerido</mat-error>
          <mat-error *ngIf="form.get('precio')?.hasError('min')">Debe ser mayor a 0</mat-error>
        </mat-form-field>
        <mat-checkbox formControlName="activo" class="activo-check">Activo</mat-checkbox>
      </form>

      <div class="foto-section">
        <p class="foto-label">Foto del tratamiento</p>
        <div class="foto-preview">
          @if (previewSrc()) {
            <img [src]="previewSrc()!" alt="Foto del tratamiento">
          } @else {
            <div class="foto-placeholder">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M21 19V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2z"/>
                <circle cx="8.5" cy="8.5" r="1.5"/>
                <path d="M21 15l-5-5L5 21"/>
              </svg>
              <span>Sin foto</span>
            </div>
          }
        </div>
        <input #fotoInput type="file" accept="image/jpeg,image/png,image/webp" style="display:none"
               (change)="onFotoChange($event)">
        <button mat-stroked-button type="button" class="foto-btn"
                (click)="fotoInput.click()" [disabled]="uploading()">
          @if (uploading()) {
            <mat-spinner diameter="16"></mat-spinner>
          }
          {{ previewSrc() ? 'Cambiar foto' : 'Subir foto' }}
        </button>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancelar()">Cancelar</button>
      <button mat-raised-button color="primary" (click)="guardar()" [disabled]="form.invalid">
        {{ data.tratamiento ? 'Guardar' : 'Crear' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .form { display: flex; flex-direction: column; min-width: 380px; padding-top: 8px; gap: 4px; }
    .full-width { width: 100%; }
    .activo-check { margin-top: 4px; margin-bottom: 8px; }
    .foto-section { margin-top: 16px; border-top: 1px solid #e2e8f0; padding-top: 16px; }
    .foto-label { font-size: 0.8rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.08em; color: #64748b; margin: 0 0 10px; }
    .foto-preview { width: 100%; height: 160px; border-radius: 10px; overflow: hidden; background: #f8fafc; border: 1px solid #e2e8f0; margin-bottom: 10px; }
    .foto-preview img { width: 100%; height: 100%; object-fit: cover; display: block; }
    .foto-placeholder { height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; color: #94a3b8; }
    .foto-placeholder svg { width: 36px; height: 36px; }
    .foto-placeholder span { font-size: 0.85rem; }
    .foto-btn { width: 100%; display: flex; align-items: center; justify-content: center; gap: 8px; }
    mat-spinner { display: inline-block; }
  `]
})
export class TratamientoFormDialogComponent implements OnDestroy {
  form: FormGroup;
  previewSrc = signal<string | null>(null);
  uploading = signal(false);

  private selectedFile: File | null = null;
  private objectUrl: string | null = null;

  constructor(
    private fb: FormBuilder,
    private tratamientoService: TratamientoService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<TratamientoFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: TratamientoFormDialogData
  ) {
    const t = data.tratamiento;
    if (t?.foto_url) {
      this.previewSrc.set(environment.apiUrl + t.foto_url);
    }
    this.form = this.fb.group({
      nombre: [t?.nombre ?? '', [Validators.required]],
      descripcion: [t?.descripcion ?? ''],
      precio: [t?.precio ?? null, [Validators.required, Validators.min(0.01)]],
      activo: [t?.activo ?? true]
    });
  }

  onFotoChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];

    if (this.data.tratamiento) {
      // Edición — sube inmediatamente
      this.uploading.set(true);
      this.tratamientoService.uploadFoto(this.data.tratamiento.id, file).subscribe({
        next: (t) => {
          this.previewSrc.set(environment.apiUrl + t.foto_url);
          this.uploading.set(false);
          this.snackBar.open('Foto actualizada', 'Cerrar', { duration: 3000 });
        },
        error: () => {
          this.uploading.set(false);
          this.snackBar.open('Error al subir la foto', 'Cerrar', { duration: 4000 });
        }
      });
    } else {
      // Creación — guarda localmente y muestra preview
      this.revokeObjectUrl();
      this.selectedFile = file;
      this.objectUrl = URL.createObjectURL(file);
      this.previewSrc.set(this.objectUrl);
    }
  }

  ngOnDestroy(): void {
    this.revokeObjectUrl();
  }

  cancelar(): void {
    this.dialogRef.close(null);
  }

  guardar(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    const payload: TratamientoPayload = {
      nombre: (v.nombre as string).trim(),
      descripcion: v.descripcion ? (v.descripcion as string).trim() : undefined,
      precio: v.precio as number,
      activo: v.activo as boolean
    };
    const result: TratamientoFormDialogResult = {
      payload,
      fotoFile: this.selectedFile,
    };
    this.dialogRef.close(result);
  }

  private revokeObjectUrl(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
  }
}
