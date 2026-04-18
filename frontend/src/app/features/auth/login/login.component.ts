import { Component } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../../core/auth/auth.service';
import { fadeInUp } from '../../../shared/animations/fade.animations';
import { BrandLogoComponent } from '../../../shared/components/brand-logo/brand-logo.component';
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
    BrandLogoComponent,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  animations: [fadeInUp]
})
export class LoginComponent {
  loginForm;
  cargando = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) return;

    this.cargando = true;
    const { email, password } = this.loginForm.value;

    this.authService.login(email!, password!).subscribe({
      next: () => {
        this.cargando = false;
        this.router.navigate(['/admin']);
      },
      error: () => {
        this.cargando = false;
        this.snackBar.open('Credenciales inválidas', 'Cerrar', { duration: 4000 });
      }
    });
  }
}
