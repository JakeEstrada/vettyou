import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { Account, AuthApi, Credentials } from './auth-api';

@Injectable({ providedIn: 'root' })
export class Session {
  private readonly api = inject(AuthApi);

  readonly account = signal<Account | null | undefined>(undefined);

  readonly ready = computed(() => this.account() !== undefined);

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.api.me().subscribe({
      next: (account) => this.account.set(account),
      error: () => this.account.set(null),
    });
  }

  register(body: Credentials): Observable<Account> {
    return this.api.register(body).pipe(tap(() => this.refresh()));
  }

  login(body: Credentials): Observable<Account> {
    return this.api.login(body).pipe(tap(() => this.refresh()));
  }

  signOut(): Observable<void> {
    return this.api.logout().pipe(tap(() => this.account.set(null)));
  }
}

/** True when there is no API at all (local process down, or the Vercel UI with no Java host). */
export function apiUnreachable(error: unknown): boolean {
  if (!(error instanceof HttpErrorResponse)) {
    return false;
  }
  if (error.status === 0 || error.status === 404 || error.status === 502 || error.status === 503) {
    return true;
  }
  const body = error.error as { text?: string } | string | null;
  const text = typeof body === 'string' ? body : body?.text;
  return typeof text === 'string' && /<!doctype|<html/i.test(text);
}

export function messageOf(error: unknown): string {
  if (apiUnreachable(error)) {
    return 'Could not reach the Vettyou API.';
  }
  if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
    return error.error.message;
  }
  return 'Something went wrong. Try again.';
}

export function fieldErrorsOf(error: unknown): { field: string; message: string }[] {
  if (error instanceof HttpErrorResponse && Array.isArray(error.error?.fieldErrors)) {
    return error.error.fieldErrors;
  }
  return [];
}
