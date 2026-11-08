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

export function messageOf(error: unknown): string {
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
