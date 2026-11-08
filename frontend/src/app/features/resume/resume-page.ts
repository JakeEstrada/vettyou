import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, FormRecord, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, merge, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { Education, EducationApi } from '../../core/api/education-api';
import { JobPostingApi, JobPostingSummary } from '../../core/api/job-posting-api';
import { ProfileApi } from '../../core/api/profile-api';
import { Project, ProjectApi } from '../../core/api/project-api';
import { ResumeApi, ResumeImport } from '../../core/api/resume-api';
import { SkillItem, SkillsApi } from '../../core/api/skills-api';
import { TailoredResume, TailoredResumeApi, TailoredResumeSummary } from '../../core/api/tailored-resume-api';
import { fieldErrorsOf, messageOf } from '../../core/auth/session';
import { dateRange, lines } from '../../shared/format/dates';
import { noteLike } from '../../shared/format/keywords';
import { ResumeDrop, ResumeFile } from '../../shared/resume-drop/resume-drop';
import { EMPTY_SHEET, ResumeSheet, SheetJob, SheetModel, sheetFromDocument } from './resume-sheet';

interface DraftJob {
  id: string;
  employer: string;
  title: string;
  location: string;
  startDate: string | null;
  endDate: string | null;
  current: boolean;
}

interface ProjectBlock {
  id: string;
  name: string;
  bullets: string[];
}

export interface RecentItem {
  postingId: string;
  resumeId: string;
  title: string;
  company: string;
  version: number;
  createdAt: string;
  total: number | null;
}

const CATEGORIES: Record<string, string> = {
  language: 'Languages',
  framework: 'Frameworks',
  database: 'Databases',
  platform: 'Cloud and platforms',
  practice: 'Practices',
  tool: 'Tools',
};

@Component({
  selector: 'app-resume-page',
  imports: [RouterLink, ReactiveFormsModule, ResumeDrop, ResumeSheet],
  templateUrl: './resume-page.html',
  styleUrl: './resume-page.scss',
})
export class ResumePage {
  private readonly profiles = inject(ProfileApi);

  private readonly educationApi = inject(EducationApi);

  private readonly projectApi = inject(ProjectApi);

  private readonly skillsApi = inject(SkillsApi);

  private readonly postings = inject(JobPostingApi);

  private readonly tailored = inject(TailoredResumeApi);

  private readonly resumes = inject(ResumeApi);

  private readonly route = inject(ActivatedRoute);

  private readonly router = inject(Router);

  readonly loading = signal(true);

  readonly missing = signal(false);

  readonly loadError = signal('');

  readonly importing = signal(false);

  readonly importError = signal('');

  readonly pasting = signal(false);

  readonly result = signal<ResumeImport | null>(null);

  readonly view = signal<'master' | 'recent'>('master');

  readonly jobs = signal<DraftJob[]>([]);

  readonly schools = signal<Education[]>([]);

  readonly projectBlocks = signal<ProjectBlock[]>([]);

  readonly skillItems = signal<SkillItem[]>([]);

  readonly recents = signal<RecentItem[]>([]);

  readonly opened = signal<TailoredResume | null>(null);

  readonly opening = signal(false);

  readonly saving = signal(false);

  readonly saveError = signal('');

  readonly savedNote = signal('');

  private readonly savedContact = signal('');

  private readonly savedBodies = signal('');

  private readonly tick = signal(0);

  private readonly recentMeta = signal<SheetModel>(EMPTY_SHEET);

  private wantedPosting = '';

  private wantedResume = '';

  readonly contact = new FormGroup({
    displayName: new FormControl('', { nonNullable: true }),
    email: new FormControl('', { nonNullable: true }),
    phone: new FormControl('', { nonNullable: true }),
    location: new FormControl('', { nonNullable: true }),
    website: new FormControl('', { nonNullable: true }),
    linkedin: new FormControl('', { nonNullable: true }),
  });

  readonly bodies = new FormRecord<FormControl<string>>({});

  readonly recentForm = new FormRecord<FormControl<string>>({});

  readonly paste = new FormControl('', { nonNullable: true, validators: [Validators.required] });

  readonly dirty = computed(() => {
    this.tick();
    return this.contactKey() !== this.savedContact() || this.bodyKey() !== this.savedBodies();
  });

  readonly shown = computed(() => {
    this.tick();
    return this.view() === 'recent' ? this.recentPreview() : this.masterSheet();
  });

  constructor() {
    merge(this.contact.valueChanges, this.bodies.valueChanges, this.recentForm.valueChanges)
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
      this.tick.update((n) => n + 1);
      this.savedNote.set('');
    });
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const view = params.get('view');
      if (view === 'recent' || view === 'master') {
        this.view.set(view);
      }
      this.wantedPosting = params.get('posting') ?? '';
      this.wantedResume = params.get('resume') ?? '';
    });
    this.load();
  }

  setView(view: 'master' | 'recent'): void {
    this.view.set(view);
    void this.router.navigate([], { queryParams: { view }, queryParamsHandling: 'merge', replaceUrl: true });
    if (view === 'recent') {
      this.openDefaultRecent();
    }
  }

  bodyControl(id: string): FormControl<string> {
    return this.bodies.controls[id];
  }

  recentControl(key: string): FormControl<string> {
    return this.recentForm.controls[key];
  }

  openRecent(item: RecentItem): void {
    this.opening.set(true);
    this.tailored.get(item.postingId, item.resumeId).subscribe({
      next: (resume) => {
        this.opening.set(false);
        this.opened.set(resume);
        this.fillRecent(sheetFromDocument(resume.document));
      },
      error: (error: unknown) => {
        this.opening.set(false);
        this.loadError.set(messageOf(error));
      },
    });
  }

  save(): void {
    this.saveError.set('');
    this.savedNote.set('');
    const calls = [];
    if (this.contactKey() !== this.savedContact()) {
      calls.push(this.profiles.save(this.contact.getRawValue()));
    }
    const saved = new Map(JSON.parse(this.savedBodies() || '[]') as [string, string][]);
    for (const job of this.jobs()) {
      const body = this.bodies.controls[job.id]?.value ?? '';
      if (body !== saved.get(job.id)) {
        calls.push(
          this.profiles.saveExperience(job.id, {
            employer: job.employer,
            title: job.title,
            location: job.location,
            startDate: job.startDate,
            endDate: job.current ? null : job.endDate,
            current: job.current,
            body,
          }),
        );
      }
    }
    if (calls.length === 0) {
      return;
    }
    this.saving.set(true);
    forkJoin(calls).subscribe({
      next: () => {
        this.saving.set(false);
        this.savedContact.set(this.contactKey());
        this.savedBodies.set(this.bodyKey());
        this.savedNote.set('Saved. Tailored resumes you already built stay as they were.');
      },
      error: (error: unknown) => {
        this.saving.set(false);
        const fieldErrors = fieldErrorsOf(error);
        this.saveError.set(fieldErrors.length > 0 ? fieldErrors[0].message : messageOf(error));
      },
    });
  }

  importResume(file: ResumeFile): void {
    this.importError.set('');
    this.result.set(null);
    this.importing.set(true);
    this.resumes.importResume(file.fileName, file.body, file.encoding).subscribe({
      next: (imported) => {
        this.importing.set(false);
        this.result.set(imported);
        this.pasting.set(false);
        this.load();
      },
      error: (error: unknown) => {
        this.importing.set(false);
        const fieldErrors = fieldErrorsOf(error);
        this.importError.set(fieldErrors.length > 0 ? fieldErrors[0].message : messageOf(error));
      },
    });
  }

  pasteResume(): void {
    this.paste.markAsTouched();
    if (this.paste.invalid) {
      return;
    }
    this.importResume({ fileName: 'pasted-resume.txt', body: this.paste.value, encoding: 'text' });
  }

  private load(): void {
    this.loadError.set('');
    this.profiles
      .get()
      .pipe(
        switchMap((profile) => {
          this.contact.setValue(profile, { emitEvent: false });
          this.savedContact.set(this.contactKey());
          return forkJoin({
            summaries: this.profiles.experiences(),
            schools: this.educationApi.list(),
            projects: this.projectApi.list(),
            skills: this.skillsApi.list(),
            postings: this.postings.list(),
          });
        }),
        switchMap((library) =>
          forkJoin({
            jobs:
              library.summaries.length === 0
                ? of([])
                : forkJoin(library.summaries.map((item) => this.profiles.experience(item.id))),
            projects:
              library.projects.length === 0
                ? of([] as Project[])
                : forkJoin(library.projects.map((item) => this.projectApi.get(item.id))),
            recents:
              library.postings.length === 0
                ? of([] as RecentItem[])
                : forkJoin(library.postings.map((posting) => this.recentOf(posting))),
            schools: of(library.schools),
            skills: of(library.skills),
          }),
        ),
      )
      .subscribe({
        next: (library) => {
          this.schools.set(library.schools);
          this.skillItems.set(library.skills);
          this.projectBlocks.set(
            library.projects.map((project) => ({
              id: project.id,
              name: project.name,
              bullets: project.claims
                .filter((claim) => claim.kind === 'feature' && claim.status === 'approved' && !claim.planned)
                .map((claim) => claim.name),
            })),
          );
          for (const name of Object.keys(this.bodies.controls)) {
            this.bodies.removeControl(name, { emitEvent: false });
          }
          const drafts: DraftJob[] = library.jobs.map((job) => {
            this.bodies.addControl(job.id, new FormControl(job.body, { nonNullable: true }), { emitEvent: false });
            return {
              id: job.id,
              employer: job.employer,
              title: job.title,
              location: job.location,
              startDate: job.startDate,
              endDate: job.endDate,
              current: job.current,
            };
          });
          this.jobs.set(drafts);
          this.savedBodies.set(this.bodyKey());
          const recents = library.recents
            .filter((item): item is RecentItem => item !== null)
            .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
          this.recents.set(recents);
          this.loading.set(false);
          if (this.view() === 'recent') {
            this.openDefaultRecent();
          }
        },
        error: (error: unknown) => {
          this.loading.set(false);
          if (error instanceof HttpErrorResponse && error.status === 401) {
            this.missing.set(true);
          } else {
            this.loadError.set(messageOf(error));
          }
        },
      });
  }

  private recentOf(posting: JobPostingSummary) {
    return this.tailored.list(posting.id).pipe(
      map((list: TailoredResumeSummary[]): RecentItem | null => {
        const latest = list[0];
        if (!latest) {
          return null;
        }
        return {
          postingId: posting.id,
          resumeId: latest.id,
          title: posting.title,
          company: posting.company,
          version: latest.version,
          createdAt: latest.createdAt,
          total: latest.total,
        };
      }),
      catchError(() => of(null)),
    );
  }

  private openDefaultRecent(): void {
    const items = this.recents();
    if (items.length === 0 || this.opened()) {
      return;
    }
    const wanted = items.find(
      (item) => item.postingId === this.wantedPosting && item.resumeId === this.wantedResume,
    );
    this.openRecent(wanted ?? items[0]);
  }

  private fillRecent(sheet: SheetModel): void {
    for (const name of Object.keys(this.recentForm.controls)) {
      this.recentForm.removeControl(name, { emitEvent: false });
    }
    sheet.jobs.forEach((job, jobIndex) => {
      job.bullets.forEach((bullet, bulletIndex) => {
        this.recentForm.addControl(`job-${jobIndex}-${bulletIndex}`, new FormControl(bullet, { nonNullable: true }), {
          emitEvent: false,
        });
      });
    });
    sheet.projects.forEach((project, projectIndex) => {
      project.bullets.forEach((bullet, bulletIndex) => {
        this.recentForm.addControl(
          `project-${projectIndex}-${bulletIndex}`,
          new FormControl(bullet, { nonNullable: true }),
          { emitEvent: false },
        );
      });
    });
    this.recentMeta.set(sheet);
    this.tick.update((n) => n + 1);
  }

  private masterSheet(): SheetModel {
    const contact = this.contact.getRawValue();
    const groups = new Map<string, string[]>();
    for (const skill of this.skillItems()) {
      if (skill.status !== 'kept') {
        continue;
      }
      const label = CATEGORIES[skill.category] ?? skill.category;
      groups.set(label, [...(groups.get(label) ?? []), skill.name]);
    }
    const current = this.jobs().find((job) => job.current);
    return {
      name: contact.displayName,
      tagline: (current ?? this.jobs()[0])?.title ?? '',
      contact: [contact.phone, contact.location, contact.email, contact.website, contact.linkedin].filter(
        (part) => part !== '',
      ),
      skills: [...groups].map(([label, names]) => ({ label, text: names.join(', ') })),
      jobs: this.jobs().map((job) => ({
        title: job.title,
        org: job.employer,
        subtitle: job.location,
        dates: dateRange(job.startDate, job.endDate, job.current),
        bullets: lines(this.bodies.controls[job.id]?.value ?? '').filter((line) => !noteLike(line)),
      })),
      projects: this.projectBlocks().map((project) => ({
        title: project.name,
        org: '',
        subtitle: '',
        dates: '',
        bullets: project.bullets,
      })),
      schools: this.schools().map((school) => ({
        title: [school.credential, school.fieldOfStudy].filter((part) => part !== '').join(' '),
        subtitle: school.institution,
        dates: dateRange(school.startDate, school.endDate),
      })),
    };
  }

  private recentPreview(): SheetModel {
    const sheet = this.recentMeta();
    const bullets = (kind: 'job' | 'project', entry: SheetJob, index: number): string[] =>
      entry.bullets.map((_, bulletIndex) => this.recentForm.controls[`${kind}-${index}-${bulletIndex}`]?.value ?? '');
    return {
      ...sheet,
      jobs: sheet.jobs.map((job, index) => ({ ...job, bullets: bullets('job', job, index) })),
      projects: sheet.projects.map((project, index) => ({
        ...project,
        bullets: bullets('project', project, index),
      })),
    };
  }

  private contactKey(): string {
    return JSON.stringify(this.contact.getRawValue());
  }

  private bodyKey(): string {
    return JSON.stringify(this.jobs().map((job) => [job.id, this.bodies.controls[job.id]?.value ?? '']));
  }
}
