import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Education, EducationApi, EducationInput } from '../../core/api/education-api';
import { EducationList } from '../../core/api/education-list';
import { fieldErrorsOf, messageOf } from '../../core/auth/session';
import { dateRange } from '../../shared/format/dates';

@Component({
  selector: 'app-school-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './school-page.html',
  styleUrl: './school-page.scss',
})
export class SchoolPage {
  private readonly api = inject(EducationApi);

  private readonly schoolList = inject(EducationList);

  private readonly router = inject(Router);

  private id = '';

  readonly school = signal<Education | null>(null);

  readonly loadError = signal('');

  readonly formError = signal('');

  readonly saved = signal(false);

  readonly confirming = signal(false);

  readonly deleting = signal(false);

  readonly degree = computed(() => {
    const school = this.school();
    return school ? [school.credential, school.fieldOfStudy].filter((part) => part).join(', ') : '';
  });

  readonly when = computed(() => {
    const school = this.school();
    return school ? dateRange(school.startDate, school.endDate) : '';
  });

  readonly form = new FormGroup({
    institution: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(160)] }),
    credential: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(160)] }),
    fieldOfStudy: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(160)] }),
    startDate: new FormControl('', { nonNullable: true }),
    endDate: new FormControl('', { nonNullable: true }),
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
    const body: EducationInput = { ...value, startDate: value.startDate || null, endDate: value.endDate || null };
    this.api.save(this.id, body).subscribe({
      next: (school) => {
        this.show(school);
        this.saved.set(true);
        this.schoolList.refresh();
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
    this.api.remove(this.id).subscribe({
      next: () => {
        this.schoolList.refresh();
        void this.router.navigate(['/education']);
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
    this.school.set(null);
    this.loadError.set('');
    this.formError.set('');
    this.saved.set(false);
    this.confirming.set(false);
    this.deleting.set(false);
    this.api.list().subscribe({
      next: (schools) => {
        const school = schools.find((item) => item.id === id);
        if (school) {
          this.show(school);
        } else {
          this.loadError.set('That school was not found.');
        }
      },
      error: (error: unknown) => this.loadError.set(messageOf(error)),
    });
  }

  private show(school: Education): void {
    this.school.set(school);
    this.form.setValue({
      institution: school.institution,
      credential: school.credential,
      fieldOfStudy: school.fieldOfStudy,
      startDate: school.startDate ?? '',
      endDate: school.endDate ?? '',
    });
  }
}
