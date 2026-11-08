import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ExperienceList } from '../../core/api/experience-list';
import { Experience, ExperienceInput, ProfileApi } from '../../core/api/profile-api';
import { fieldErrorsOf, messageOf } from '../../core/auth/session';
import { dateRange, lines } from '../../shared/format/dates';

@Component({
  selector: 'app-job-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './job-page.html',
  styleUrl: './job-page.scss',
})
export class JobPage {
  private readonly api = inject(ProfileApi);

  private readonly jobList = inject(ExperienceList);

  private readonly router = inject(Router);

  private id = '';

  readonly job = signal<Experience | null>(null);

  readonly loadError = signal('');

  readonly formError = signal('');

  readonly saved = signal(false);

  readonly confirming = signal(false);

  readonly deleting = signal(false);

  readonly bullets = computed(() => lines(this.job()?.body ?? ''));

  readonly when = computed(() => {
    const job = this.job();
    return job ? dateRange(job.startDate, job.endDate, job.current) : '';
  });

  readonly form = new FormGroup({
    employer: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(160)] }),
    title: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(160)] }),
    location: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(120)] }),
    startDate: new FormControl('', { nonNullable: true }),
    endDate: new FormControl('', { nonNullable: true }),
    current: new FormControl(false, { nonNullable: true }),
    body: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(20000)] }),
  });

  constructor() {
    inject(ActivatedRoute)
      .paramMap.pipe(takeUntilDestroyed())
      .subscribe((params) => this.load(params.get('id') ?? ''));
  }

  submit(): void {
    this.formError.set('');
    this.saved.set(false);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const body: ExperienceInput = {
      ...value,
      startDate: value.startDate || null,
      endDate: value.current ? null : value.endDate || null,
    };
    this.api.saveExperience(this.id, body).subscribe({
      next: (job) => {
        this.show(job);
        this.saved.set(true);
        this.jobList.refresh();
      },
      error: (error: unknown) => {
        const fieldErrors = fieldErrorsOf(error);
        for (const fieldError of fieldErrors) {
          this.form.get(fieldError.field)?.setErrors({ server: fieldError.message });
        }
        if (fieldErrors.length === 0) {
          this.formError.set(messageOf(error));
        }
      },
    });
  }

  remove(): void {
    this.deleting.set(true);
    this.formError.set('');
    this.api.deleteExperience(this.id).subscribe({
      next: () => {
        this.jobList.refresh();
        void this.router.navigate(['/experience']);
      },
      error: (error: unknown) => {
        this.deleting.set(false);
        this.confirming.set(false);
        this.formError.set(messageOf(error));
      },
    });
  }

  private load(id: string): void {
    this.id = id;
    this.job.set(null);
    this.loadError.set('');
    this.formError.set('');
    this.saved.set(false);
    this.confirming.set(false);
    this.deleting.set(false);
    this.api.experience(id).subscribe({
      next: (job) => this.show(job),
      error: (error: unknown) =>
        this.loadError.set(
          error instanceof HttpErrorResponse && error.status === 404 ? 'That job was not found.' : messageOf(error),
        ),
    });
  }

  private show(job: Experience): void {
    this.job.set(job);
    this.form.setValue({
      employer: job.employer,
      title: job.title,
      location: job.location,
      startDate: job.startDate ?? '',
      endDate: job.endDate ?? '',
      current: job.current,
      body: job.body,
    });
  }
}
