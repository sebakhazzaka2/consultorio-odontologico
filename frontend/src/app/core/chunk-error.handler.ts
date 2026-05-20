import { ErrorHandler, Injectable } from '@angular/core';
import * as Sentry from '@sentry/angular';

const CHUNK_ERROR_PATTERNS: RegExp[] = [
  /Failed to fetch dynamically imported module/i,
  /Importing a module script failed/i,
  /ChunkLoadError/i,
  /Loading chunk [\w-]+ failed/i,
  /error loading dynamically imported module/i
];

const RELOAD_FLAG_KEY = 'chunkReloadAt';
const RELOAD_DEBOUNCE_MS = 10_000;

@Injectable()
export class ChunkErrorHandler implements ErrorHandler {
  private readonly sentryHandler: ErrorHandler = Sentry.createErrorHandler();

  handleError(error: unknown): void {
    if (this.isChunkLoadError(error) && this.shouldReload()) {
      sessionStorage.setItem(RELOAD_FLAG_KEY, String(Date.now()));
      window.location.reload();
      return;
    }
    this.sentryHandler.handleError(error);
  }

  private isChunkLoadError(error: unknown): boolean {
    const message = this.extractMessage(error);
    if (!message) return false;
    return CHUNK_ERROR_PATTERNS.some(re => re.test(message));
  }

  private extractMessage(error: unknown): string {
    if (!error) return '';
    if (typeof error === 'string') return error;
    if (error instanceof Error) return `${error.name}: ${error.message}`;
    const anyErr = error as { message?: string; rejection?: { message?: string } };
    return anyErr.message ?? anyErr.rejection?.message ?? '';
  }

  private shouldReload(): boolean {
    const last = Number(sessionStorage.getItem(RELOAD_FLAG_KEY) ?? '0');
    return Date.now() - last > RELOAD_DEBOUNCE_MS;
  }
}
