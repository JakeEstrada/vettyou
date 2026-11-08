import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Claim, Project, ProjectApi } from '../../core/api/project-api';
import { ProjectList } from '../../core/api/project-list';
import { fieldErrorsOf, messageOf } from '../../core/auth/session';
import { ReadmeDrop, ReadmeFile } from '../../shared/readme-drop/readme-drop';

const MAX_README_CHARS = 200_000;

export interface BulletSection {
  section: string;
  items: Claim[];
}

@Component({
  selector: 'app-project-page',
  imports: [ReactiveFormsModule, RouterLink, ReadmeDrop],
  templateUrl: './project-page.html',
  styleUrl: './project-page.scss',
})
export class ProjectPage {
  private readonly api = inject(ProjectApi);

  private readonly projectList = inject(ProjectList);

  private readonly router = inject(Router);

  private id = '';

  readonly project = signal<Project | null>(null);

  readonly loadError = signal('');

  readonly readmeError = signal('');

  readonly claimError = signal('');

  readonly detailsError = signal('');

  readonly detailsSaved = signal(false);

  readonly uploading = signal(false);

  readonly rereading = signal(false);

  readonly pasting = signal(false);

  readonly confirming = signal(false);

  readonly deleting = signal(false);

  readonly deleteError = signal('');

  readonly toReview = computed(() => this.claimsWith('candidate'));

  readonly notMine = computed(() => this.claimsWith('rejected'));

  readonly bullets = computed(() => this.inReadmeOrder(this.ofKind('feature')));

  readonly bulletSections = computed<BulletSection[]>(() => {
    const sections: BulletSection[] = [];
    for (const claim of this.bullets()) {
      const last = sections.at(-1);
      if (last && last.section === claim.category) {
        last.items.push(claim);
      } else {
        sections.push({ section: claim.category, items: [claim] });
      }
    }
    return sections;
  });

  readonly bulletsToCheck = computed(() => this.bullets().filter((claim) => claim.status === 'candidate'));

  readonly stack = computed(() => {
    const claims = this.inReadmeOrder(this.ofKind('skill'));
    return {
      total: claims.length,
      review: claims.filter((claim) => claim.status === 'candidate'),
      kept: claims.filter((claim) => claim.status === 'approved'),
      notMine: claims.filter((claim) => claim.status === 'rejected'),
    };
  });

  readonly details = new FormGroup({
    summary: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(4000)] }),
    repoUrl: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] }),
    liveUrl: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] }),
  });

  readonly readme = new FormGroup({
    fileName: new FormControl('README.md', { nonNullable: true, validators: [Validators.required] }),
    body: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(MAX_README_CHARS)],
    }),
  });

  constructor() {
    inject(ActivatedRoute)
      .paramMap.pipe(takeUntilDestroyed())
      .subscribe((params) => this.load(params.get('id') ?? ''));
  }

  dropped(file: ReadmeFile): void {
    this.readme.setValue(file);
    this.uploadReadme();
  }

  uploadReadme(): void {
    this.readmeError.set('');
    if (this.readme.invalid) {
      this.readme.markAllAsTouched();
      if (this.readme.controls.body.errors?.['maxlength']) {
        this.readmeError.set('That README is too long. Keep it under 200,000 characters.');
      }
      return;
    }
    const { fileName, body } = this.readme.getRawValue();
    this.uploading.set(true);
    this.api.addReadme(this.id, fileName, body).subscribe({
      next: (project) => {
        this.uploading.set(false);
        this.show(project);
        this.projectList.refresh();
        this.readme.reset({ fileName: 'README.md', body: '' });
        this.pasting.set(false);
      },
      error: (error: unknown) => {
        this.uploading.set(false);
        const fieldErrors = fieldErrorsOf(error);
        this.readmeError.set(fieldErrors.length > 0 ? fieldErrors[0].message : messageOf(error));
      },
    });
  }

  reread(): void {
    this.readmeError.set('');
    this.rereading.set(true);
    this.api.reread(this.id).subscribe({
      next: (project) => {
        this.rereading.set(false);
        this.show(project);
      },
      error: (error: unknown) => {
        this.rereading.set(false);
        this.readmeError.set(messageOf(error));
      },
    });
  }

  decide(claim: Claim, decision: 'approved' | 'rejected'): void {
    this.claimError.set('');
    this.api.decide(claim.id, decision).subscribe({
      next: (updated) =>
        this.project.update((project) =>
          project
            ? { ...project, claims: project.claims.map((item) => (item.id === updated.id ? updated : item)) }
            : project,
        ),
      error: (error: unknown) => this.claimError.set(messageOf(error)),
    });
  }

  keepAll(claims: Claim[]): void {
    for (const claim of claims) {
      this.decide(claim, 'approved');
    }
  }

  saveDetails(): void {
    this.detailsError.set('');
    this.detailsSaved.set(false);
    if (this.details.invalid) {
      this.details.markAllAsTouched();
      return;
    }
    this.api.save(this.id, this.details.getRawValue()).subscribe({
      next: (project) => {
        this.show(project);
        this.detailsSaved.set(true);
      },
      error: (error: unknown) => this.detailsError.set(messageOf(error)),
    });
  }

  remove(): void {
    this.deleting.set(true);
    this.deleteError.set('');
    this.api.remove(this.id).subscribe({
      next: () => {
        this.projectList.refresh();
        void this.router.navigate(['/projects']);
      },
      error: (error: unknown) => {
        this.deleting.set(false);
        this.confirming.set(false);
        this.deleteError.set(messageOf(error));
      },
    });
  }

  versionOf(claim: Claim): number | undefined {
    return this.project()?.sources.find((source) => source.id === claim.sourceVersionId)?.versionNumber;
  }

  private load(id: string): void {
    this.id = id;
    this.project.set(null);
    this.loadError.set('');
    this.claimError.set('');
    this.readmeError.set('');
    this.detailsSaved.set(false);
    this.pasting.set(false);
    this.confirming.set(false);
    this.deleting.set(false);
    this.deleteError.set('');
    this.api.get(id).subscribe({
      next: (project) => this.show(project),
      error: (error: unknown) =>
        this.loadError.set(
          error instanceof HttpErrorResponse && error.status === 404 ? 'That project was not found.' : messageOf(error),
        ),
    });
  }

  private show(project: Project): void {
    this.project.set(project);
    this.details.setValue({
      summary: project.summary,
      repoUrl: project.repoUrl,
      liveUrl: project.liveUrl,
    });
  }

  private ofKind(kind: Claim['kind']): Claim[] {
    return this.project()?.claims.filter((claim) => claim.kind === kind) ?? [];
  }

  private inReadmeOrder(claims: Claim[]): Claim[] {
    return [...claims].sort(
      (a, b) => (this.versionOf(a) ?? 0) - (this.versionOf(b) ?? 0) || a.lineNumber - b.lineNumber,
    );
  }

  private claimsWith(status: Claim['status']): Claim[] {
    return this.project()?.claims.filter((claim) => claim.status === status) ?? [];
  }
}
