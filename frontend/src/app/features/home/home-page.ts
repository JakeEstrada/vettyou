import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { ApplicationsApi, JobApplication } from '../../core/api/applications-api';
import { Education, EducationApi } from '../../core/api/education-api';
import { JobPosting, JobPostingApi, JobPostingSummary } from '../../core/api/job-posting-api';
import { Experience, Profile, ProfileApi } from '../../core/api/profile-api';
import { Project, ProjectApi } from '../../core/api/project-api';
import { SkillItem, SkillsApi, useLink } from '../../core/api/skills-api';
import { TailoredResumeApi, TailoredResumeSummary } from '../../core/api/tailored-resume-api';
import { messageOf } from '../../core/auth/session';
import { dateRange, lines } from '../../shared/format/dates';
import { missingKeywords, noteLike, parseKeywords, seedKeywords } from '../../shared/format/keywords';
import { ApplicationTracker } from '../applications/application-tracker';
import { EMPTY_SHEET, ResumeSheet, SheetModel, sheetText } from '../resume/resume-sheet';

const CATEGORIES: Record<string, string> = {
  language: 'Languages',
  framework: 'Frameworks',
  database: 'Databases',
  platform: 'Cloud and platforms',
  practice: 'Practices',
  tool: 'Tools',
};

export interface SkillRow {
  name: string;
  places: number;
  sources: { label: string; link: string[] }[];
}

export interface RecentJob {
  postingId: string;
  title: string;
  company: string;
  pay: string;
  preview: string;
  createdAt: string;
  resumeId: string | null;
  total: number | null;
}

@Component({
  selector: 'app-home-page',
  imports: [RouterLink, ReactiveFormsModule, ResumeSheet, ApplicationTracker],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss',
})
export class HomePage {
  private readonly profiles = inject(ProfileApi);

  private readonly educationApi = inject(EducationApi);

  private readonly projectApi = inject(ProjectApi);

  private readonly skillsApi = inject(SkillsApi);

  private readonly postings = inject(JobPostingApi);

  private readonly resumes = inject(TailoredResumeApi);

  private readonly applicationsApi = inject(ApplicationsApi);

  readonly loading = signal(true);

  readonly missing = signal(false);

  readonly error = signal('');

  readonly profile = signal<Profile | null>(null);

  readonly jobs = signal<Experience[]>([]);

  readonly schools = signal<Education[]>([]);

  readonly projects = signal<Project[]>([]);

  readonly skills = signal<SkillItem[]>([]);

  readonly recents = signal<RecentJob[]>([]);

  readonly applications = signal<JobApplication[]>([]);

  readonly selected = signal<JobPosting | null>(null);

  readonly selectedTotal = signal<number | null>(null);

  readonly keywords = new FormControl('', { nonNullable: true });

  readonly keywordTick = signal(0);

  readonly sheet = computed(() => this.buildSheet());

  readonly role = computed(() => {
    const jobs = this.jobs();
    const current = jobs.find((job) => job.current);
    return (current ?? jobs[0])?.title ?? '';
  });

  readonly topSkills = computed<SkillRow[]>(() =>
    this.skills()
      .filter((skill) => skill.status === 'kept')
      .slice()
      .sort((a, b) => b.places - a.places || a.name.localeCompare(b.name))
      .slice(0, 8)
      .map((skill) => ({
        name: skill.name,
        places: skill.places,
        sources: skill.uses.slice(0, 3).map((use) => ({ label: use.label, link: useLink(use) })),
      })),
  );

  readonly marks = computed(() => {
    this.keywordTick();
    return parseKeywords(this.keywords.value);
  });

  readonly missingWords = computed(() => missingKeywords(sheetText(this.sheet()), this.marks()));

  constructor() {
    this.keywords.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.keywordTick.update((n) => n + 1));
    this.load();
  }

  openJob(item: RecentJob): void {
    this.error.set('');
    this.postings.get(item.postingId).subscribe({
      next: (posting) => {
        this.selected.set(posting);
        this.selectedTotal.set(item.total);
        this.keywords.setValue(seedKeywords(posting.requirements.map((requirement) => requirement.skill)).join(', '));
      },
      error: (error: unknown) => this.error.set(messageOf(error)),
    });
  }

  private load(): void {
    this.profiles
      .get()
      .pipe(
        switchMap((profile) => {
          this.profile.set(profile);
          return forkJoin({
            summaries: this.profiles.experiences(),
            schools: this.educationApi.list(),
            projects: this.projectApi.list(),
            skills: this.skillsApi.list(),
            postings: this.postings.list(),
            applications: this.applicationsApi.list().pipe(catchError(() => of([] as JobApplication[]))),
          });
        }),
        switchMap((library) =>
          forkJoin({
            jobs:
              library.summaries.length === 0
                ? of([] as Experience[])
                : forkJoin(library.summaries.map((item) => this.profiles.experience(item.id))),
            projects:
              library.projects.length === 0
                ? of([] as Project[])
                : forkJoin(library.projects.map((item) => this.projectApi.get(item.id))),
            recents:
              library.postings.length === 0
                ? of([] as RecentJob[])
                : forkJoin(library.postings.map((posting) => this.recentOf(posting))),
            schools: of(library.schools),
            skills: of(library.skills),
            applications: of(library.applications),
          }),
        ),
      )
      .subscribe({
        next: (library) => {
          this.jobs.set(library.jobs);
          this.schools.set(library.schools);
          this.projects.set(library.projects);
          this.skills.set(library.skills);
          this.applications.set(library.applications);
          this.recents.set([...library.recents].sort((a, b) => b.createdAt.localeCompare(a.createdAt)));
          this.loading.set(false);
          const first = this.recents()[0];
          if (first) {
            this.openJob(first);
          }
        },
        error: (error: unknown) => {
          this.loading.set(false);
          if (error instanceof HttpErrorResponse && error.status === 401) {
            this.missing.set(true);
          } else {
            this.error.set(messageOf(error));
          }
        },
      });
  }

  private recentOf(posting: JobPostingSummary): Observable<RecentJob> {
    const fallback: RecentJob = {
      postingId: posting.id,
      title: posting.title,
      company: posting.company,
      pay: posting.pay ?? '',
      preview: posting.preview ?? '',
      createdAt: posting.createdAt,
      resumeId: null,
      total: null,
    };
    return this.resumes.list(posting.id).pipe(
      map((list: TailoredResumeSummary[]): RecentJob => {
        const latest = list[0];
        return {
          ...fallback,
          createdAt: latest?.createdAt ?? posting.createdAt,
          resumeId: latest?.id ?? null,
          total: latest?.total ?? null,
        };
      }),
      catchError(() => of(fallback)),
    );
  }

  private buildSheet(): SheetModel {
    const profile = this.profile();
    if (!profile) {
      return EMPTY_SHEET;
    }
    const groups = new Map<string, string[]>();
    for (const skill of this.skills()) {
      if (skill.status !== 'kept') {
        continue;
      }
      const label = CATEGORIES[skill.category] ?? skill.category;
      groups.set(label, [...(groups.get(label) ?? []), skill.name]);
    }
    return {
      name: profile.displayName,
      tagline: this.role(),
      contact: [profile.phone, profile.location, profile.email, profile.website, profile.linkedin].filter(
        (part) => part !== '',
      ),
      skills: [...groups].map(([label, names]) => ({ label, text: names.join(', ') })),
      jobs: this.jobs().map((job) => ({
        title: job.title,
        org: job.employer,
        subtitle: job.location,
        dates: dateRange(job.startDate, job.endDate, job.current),
        bullets: lines(job.body).filter((line) => !noteLike(line)).slice(0, 4),
      })),
      projects: this.projects()
        .map((project) => ({
          title: project.name,
          org: '',
          subtitle: '',
          dates: '',
          bullets: project.claims
            .filter((claim) => claim.kind === 'feature' && claim.status === 'approved' && !claim.planned)
            .map((claim) => claim.name),
        }))
        .filter((project) => project.bullets.length > 0),
      schools: this.schools().map((school) => ({
        title: [school.credential, school.fieldOfStudy].filter((part) => part !== '').join(' '),
        subtitle: school.institution,
        dates: dateRange(school.startDate, school.endDate),
      })),
    };
  }
}
