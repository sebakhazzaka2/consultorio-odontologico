import { ApplicationConfig, LOCALE_ID, APP_INITIALIZER } from '@angular/core';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideNativeDateAdapter } from '@angular/material/core';
import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { firstValueFrom } from 'rxjs';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { httpErrorInterceptor } from './core/http-error.interceptor';
import { ClinicConfigService } from './core/config/clinic-config.service';

registerLocaleData(localeEs);

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor, httpErrorInterceptor])),
    provideAnimations(),
    provideNativeDateAdapter(),
    { provide: LOCALE_ID, useValue: 'es' },
    provideCharts(withDefaultRegisterables()),
    {
      provide: APP_INITIALIZER,
      useFactory: (service: ClinicConfigService) => () => firstValueFrom(service.load()),
      deps: [ClinicConfigService],
      multi: true
    }
  ]
};
