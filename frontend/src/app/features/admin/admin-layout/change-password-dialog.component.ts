import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/auth/auth.service';

function passwordsMatchValidator(group: AbstractControl): ValidationErrors | null {
  const np = group.get('newPassword')?.value;
  const cp = group.get('confirmPassword')?.value;
  return np && cp && np !== cp ? { passwordsMismatch: true } : null;
}

@Component({
  selector: 'app-change-password-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>Cambiar contraseña</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form">
        <mat-form-field appearance="outline" class="field">
          <mat-label>Contraseña actual</mat-label>
          <input matInput [type]="showCurrent ? 'text' : 'password'" formControlName="currentPassword" autocomplete="current-password">
          <button mat-icon-button matSuffix type="button" (click)="showCurrent = !showCurrent" [attr.aria-label]="showCurrent ? 'Ocultar' : 'Mostrar'">
            <mat-icon>{{ showCurrent ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
          @if (form.get('currentPassword')?.invalid && form.get('currentPassword')?.touched) {
            <mat-error>La contraseña actual es requerida</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="field">
          <mat-label>Nueva contraseña</mat-label>
          <input matInput [type]="showNew ? 'text' : 'password'" formControlName="newPassword" autocomplete="new-password">
          <button mat-icon-button matSuffix type="button" (click)="showNew = !showNew" [attr.aria-label]="showNew ? 'Ocultar' : 'Mostrar'">
            <mat-icon>{{ showNew ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
          @if (form.get('newPassword')?.hasError('required') && form.get('newPassword')?.touched) {
            <mat-error>La nueva contraseña es requerida</mat-error>
          }
          @if (form.get('newPassword')?.hasError('minlength') && form.get('newPassword')?.touched) {
            <mat-error>Mínimo 8 caracteres</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="field">
          <mat-label>Confirmar nueva contraseña</mat-label>
          <input matInput [type]="showNew ? 'text' : 'password'" formControlName="confirmPassword" autocomplete="new-password">
          @if (form.hasError('passwordsMismatch') && form.get('confirmPassword')?.touched) {
            <mat-error>Las contraseñas no coinciden</mat-error>
          }
        </mat-form-field>

        @if (errorMessage) {
          <p class="error-msg">{{ errorMessage }}</p>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancelar()" [disabled]="loading">Cancelar</button>
      <button mat-flat-button color="primary" (click)="guardar()" [disabled]="form.invalid || loading">
        {{ loading ? 'Guardando...' : 'Cambiar contraseña' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .form { display: flex; flex-direction: column; gap: 4px; padding-top: 8px; }
    .field { width: 320px; }
    .error-msg { color: var(--color-danger, #DC2626); font-size: 0.85rem; margin: 0; }
    mat-dialog-content { overflow: visible; }
  `]
})
export class ChangePasswordDialogComponent {
  form: FormGroup;
  loading = false;
  errorMessage = '';
  showCurrent = false;
  showNew = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private dialogRef: MatDialogRef<ChangePasswordDialogComponent>,
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group(
      {
        currentPassword: ['', Validators.required],
        newPassword: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', Validators.required]
      },
      { validators: passwordsMatchValidator }
    );
  }

  guardar(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.errorMessage = '';
    const { currentPassword, newPassword } = this.form.value as { currentPassword: string; newPassword: string };
    this.authService.changePassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.loading = false;
        this.dialogRef.close(true);
        this.snackBar.open('Contraseña actualizada exitosamente', 'Cerrar', {
          duration: 4000,
          panelClass: ['snack-success']
        });
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.errorMessage = err.error?.message ?? 'No se pudo cambiar la contraseña';
      }
    });
  }

  cancelar(): void {
    this.dialogRef.close(false);
  }
}
