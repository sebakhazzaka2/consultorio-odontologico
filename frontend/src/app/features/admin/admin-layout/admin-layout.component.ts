import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { RouterOutlet, RouterModule, Router, NavigationEnd } from '@angular/router';
import { MatSidenavModule, MatSidenav } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { MatBadgeModule } from '@angular/material/badge';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../../core/auth/auth.service';
import { ChangePasswordDialogComponent } from './change-password-dialog.component';
import { ClinicConfigService } from '../../../core/config/clinic-config.service';
import { CitaService } from '../citas/cita.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterModule,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatBadgeModule
  ],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss'
})
export class AdminLayoutComponent implements OnInit, OnDestroy {
  @ViewChild('sidenav') sidenav!: MatSidenav;

  isMobile = false;
  businessName = '';
  pendientesCount = 0;
  private subs = new Subscription();

  constructor(
    private authService: AuthService,
    private breakpointObserver: BreakpointObserver,
    private router: Router,
    private dialog: MatDialog,
    private clinicConfigService: ClinicConfigService,
    private citaService: CitaService
  ) {}

  ngOnInit(): void {
    this.businessName = this.clinicConfigService.name;

    this.subs.add(
      this.breakpointObserver.observe([Breakpoints.XSmall, Breakpoints.Small]).subscribe(result => {
        this.isMobile = result.matches;
      })
    );

    // Cerrar sidenav automáticamente al navegar en mobile
    this.subs.add(
      this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(() => {
        if (this.isMobile && this.sidenav?.opened) {
          this.sidenav.close();
        }
      })
    );

    this.subs.add(
      this.citaService.pendientesCount$.subscribe(count => this.pendientesCount = count)
    );
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  onChangePassword(): void {
    this.dialog.open(ChangePasswordDialogComponent, { disableClose: true });
  }

  onLogout(): void {
    this.authService.logout();
  }
}
