import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ProfileApi } from '../../core/api/profile-api';
import { fieldErrorsOf, messageOf, Session } from '../../core/auth/session';

@Component({
  selector: 'app-profile-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss',
})
export class ProfilePage {
  protected readonly session = inject(Session);

  private readonly api = inject(ProfileApi);

  readonly form = new FormGroup({
    displayName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(120)] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.email, Validators.maxLength(200)] }),
    phone: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(40)] }),
    location: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(120)] }),
    website: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(200)] }),
    linkedin: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(200)] }),
  });

  readonly formError = signal('');

  readonly saved = signal(false);

  readonly missing = signal(false);

  constructor() {
    this.api.get().subscribe({
      next: (profile) => this.form.setValue(profile),
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.missing.set(true);
        }
      },
    });
  }

  submit(): void {
    this.formError.set('');
    this.saved.set(false);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.api.save(this.form.getRawValue()).subscribe({
      next: (profile) => {
        this.form.setValue(profile);
        this.saved.set(true);
      },
      error: (error: unknown) => {
        for (const fieldError of fieldErrorsOf(error)) {
          this.form.get(fieldError.field)?.setErrors({ server: fieldError.message });
        }
        if (fieldErrorsOf(error).length === 0) {
          this.formError.set(messageOf(error));
        }
      },
    });
  }
}
