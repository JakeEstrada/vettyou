import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { ExperienceList } from '../../core/api/experience-list';
import { ExperienceInput, ExperienceSummary, ProfileApi } from '../../core/api/profile-api';
import { fieldErrorsOf, messageOf } from '../../core/auth/session';
import { dateRange } from '../../shared/format/dates';

@Component({
  selector: 'app-experience-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './experience-page.html',
  styleUrl: './experience-page.scss',
})
export class ExperiencePage {
  private readonly api = inject(ProfileApi);

  private readonly jobList = inject(ExperienceList);

  private readonly router = inject(Router);

  readonly jobs = signal<ExperienceSummary[]>([]);

  readonly loading = signal(true);

  readonly writing = signal(false);

  readonly formError = signal('');

  readonly missing = signal(false);

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
    this.api.experiences().subscribe({
      next: (jobs) => {
        this.jobs.set(jobs);
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

  when(job: ExperienceSummary): string {
    return dateRange(job.startDate, job.endDate, job.current);
  }

  add(): void {
    this.writing.set(true);
    this.formError.set('');
    this.form.reset({ employer: '', title: '', location: '', startDate: '', endDate: '', current: false, body: '' });
  }

  submit(): void {
    this.formError.set('');
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
    this.api.createExperience(body).subscribe({
      next: (job) => {
        this.jobList.refresh();
        void this.router.navigate(['/experience', job.id]);
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
}
