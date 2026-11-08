import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { Session } from '../../core/auth/session';
import { applyServerErrors, credentialsForm } from './credentials-form';

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register-page.html',
})
export class RegisterPage {
  private readonly session = inject(Session);

  private readonly router = inject(Router);

  readonly form = credentialsForm();

  readonly formError = signal('');

  submit(): void {
    this.formError.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.session.register(this.form.getRawValue()).subscribe({
      next: () => {
        void this.router.navigateByUrl('/account');
      },
      error: (error: unknown) => applyServerErrors(this.form, error, this.formError),
    });
  }
}
