import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { JobPostingApi, JobPostingSummary } from '../../core/api/job-posting-api';
import { fieldErrorsOf, messageOf } from '../../core/auth/session';

const MAX_POSTING_CHARS = 60_000;

@Component({
  selector: 'app-jobs-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './jobs-page.html',
  styleUrl: './jobs-page.scss',
})
export class JobsPage {
  private readonly api = inject(JobPostingApi);

  private readonly router = inject(Router);

  readonly postings = signal<JobPostingSummary[]>([]);

  readonly loading = signal(true);

  readonly missing = signal(false);

  readonly sending = signal(false);

  readonly formError = signal('');

  readonly confirming = signal('');

  readonly deleting = signal(false);

  readonly form = new FormGroup({
    body: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(MAX_POSTING_CHARS)],
    }),
  });

  constructor() {
    this.api.list().subscribe({
      next: (postings) => {
        this.postings.set(postings);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.missing.set(true);
        } else {
          this.formError.set(messageOf(error));
        }
      },
    });
  }

  submit(): void {
    this.formError.set('');
    if (this.form.invalid || this.form.controls.body.value.trim() === '') {
      this.form.markAllAsTouched();
      return;
    }
    this.sending.set(true);
    this.api.create(this.form.controls.body.value).subscribe({
      next: (posting) => void this.router.navigate(['/jobs', posting.id]),
      error: (error: unknown) => {
        this.sending.set(false);
        const fieldErrors = fieldErrorsOf(error);
        this.formError.set(fieldErrors.length > 0 ? fieldErrors[0].message : messageOf(error));
      },
    });
  }

  remove(id: string): void {
    this.deleting.set(true);
    this.formError.set('');
    this.api.remove(id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.confirming.set('');
        this.postings.update((postings) => postings.filter((posting) => posting.id !== id));
      },
      error: (error: unknown) => {
        this.deleting.set(false);
        this.formError.set(messageOf(error));
      },
    });
  }
}
