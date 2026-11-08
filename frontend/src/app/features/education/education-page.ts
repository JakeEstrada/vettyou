import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { Education, EducationApi, EducationInput } from '../../core/api/education-api';
import { EducationList } from '../../core/api/education-list';
import { fieldErrorsOf, messageOf } from '../../core/auth/session';
import { dateRange } from '../../shared/format/dates';

@Component({
  selector: 'app-education-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './education-page.html',
  styleUrl: './education-page.scss',
})
export class EducationPage {
  private readonly api = inject(EducationApi);

  private readonly schoolList = inject(EducationList);

  private readonly router = inject(Router);

  readonly schools = signal<Education[]>([]);

  readonly loading = signal(true);

  readonly missing = signal(false);

  readonly adding = signal(false);

  readonly formError = signal('');

  readonly form = new FormGroup({
    institution: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(160)] }),
    credential: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(160)] }),
    fieldOfStudy: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(160)] }),
    startDate: new FormControl('', { nonNullable: true }),
    endDate: new FormControl('', { nonNullable: true }),
  });

  constructor() {
    this.api.list().subscribe({
      next: (schools) => {
        this.schools.set(schools);
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

  degree(school: Education): string {
    return [school.credential, school.fieldOfStudy].filter((part) => part).join(', ');
  }

  when(school: Education): string {
    return dateRange(school.startDate, school.endDate);
  }

  add(): void {
    this.adding.set(true);
    this.formError.set('');
    this.form.reset({ institution: '', credential: '', fieldOfStudy: '', startDate: '', endDate: '' });
  }

  submit(): void {
    this.formError.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const body: EducationInput = { ...value, startDate: value.startDate || null, endDate: value.endDate || null };
    this.api.create(body).subscribe({
      next: (school) => {
        this.schoolList.refresh();
        void this.router.navigate(['/education', school.id]);
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
