import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import {
  JobPosting,
  JobPostingApi,
  postingMarkdownUrl,
  RequirementMatch,
} from '../../core/api/job-posting-api';
import { TailoredResume, TailoredResumeApi, TailoredResumeSummary } from '../../core/api/tailored-resume-api';
import { messageOf } from '../../core/auth/session';
import { ResumePreview } from '../resume/resume-preview';

/** Personal apply line on the plot. Not an employer cutoff. */
export const APPLY_LINE = 78;

export interface ScoreRow {
  postingId: string;
  title: string;
  company: string;
  createdAt: string;
  resumeId: string | null;
  version: number | null;
  total: number | null;
  toAnswer: number;
  when: string;
}

export interface CoverageBuckets {
  matched: string[];
  partial: string[];
  missing: string[];
}

@Component({
  selector: 'app-scores-page',
  imports: [RouterLink, DatePipe, ResumePreview],
  templateUrl: './scores-page.html',
  styleUrl: './scores-page.scss',
})
export class ScoresPage {
  private readonly postings = inject(JobPostingApi);

  private readonly resumes = inject(TailoredResumeApi);

  readonly applyLine = APPLY_LINE;

  readonly markdownUrl = postingMarkdownUrl;

  readonly loading = signal(true);

  readonly missing = signal(false);

  readonly error = signal('');

  readonly rows = signal<ScoreRow[]>([]);

  readonly confirming = signal('');

  readonly deleting = signal(false);

  readonly opened = signal<TailoredResume | null>(null);

  readonly openedPosting = signal<JobPosting | null>(null);

  readonly opening = signal(false);

  readonly picked = signal('');

  readonly hover = signal('');

  readonly highlights = computed(() => this.opened()?.highlights ?? []);

  readonly buckets = computed(() => coverageBuckets(this.openedPosting()?.requirements ?? []));

  readonly banner = computed(() => lineOf(this.openedRow()));

  readonly plot = computed(() => buildPlot(this.rows()));

  constructor() {
    this.load();
  }

  lineOf = lineOf;

  openById(id: string): void {
    const row = this.rows().find((item) => item.postingId === id);
    if (row) {
      this.open(row);
    }
  }

  open(row: ScoreRow): void {
    this.picked.set('');
    this.opening.set(true);
    this.error.set('');
    forkJoin({
      posting: this.postings.get(row.postingId),
      resume: row.resumeId ? this.resumes.get(row.postingId, row.resumeId) : of(null),
    }).subscribe({
      next: ({ posting, resume }) => {
        this.opening.set(false);
        this.openedPosting.set(posting);
        this.opened.set(resume);
        this.rows.update((rows) =>
          rows.map((item) => (item.postingId === row.postingId ? { ...item, toAnswer: posting.score.toAnswer } : item)),
        );
      },
      error: (error: unknown) => {
        this.opening.set(false);
        this.error.set(messageOf(error));
      },
    });
  }

  choose(text: string): void {
    this.picked.update((current) => (current === text ? '' : text));
  }

  remove(id: string): void {
    this.deleting.set(true);
    this.error.set('');
    this.postings.remove(id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.confirming.set('');
        this.rows.update((rows) => rows.filter((row) => row.postingId !== id));
        if (this.opened()?.postingId === id || this.openedPosting()?.id === id) {
          this.opened.set(null);
          this.openedPosting.set(null);
        }
      },
      error: (error: unknown) => {
        this.deleting.set(false);
        this.error.set(messageOf(error));
      },
    });
  }

  private openedRow(): ScoreRow | null {
    const posting = this.openedPosting();
    if (!posting) {
      return null;
    }
    return this.rows().find((row) => row.postingId === posting.id) ?? null;
  }

  private load(): void {
    this.postings
      .list()
      .pipe(
        switchMap((postings) =>
          postings.length === 0
            ? of([] as ScoreRow[])
            : forkJoin(
                postings.map((posting) =>
                  this.resumes.list(posting.id).pipe(
                    map((list) => rowOf(posting.id, posting.title, posting.company, posting.createdAt, list[0])),
                    catchError(() => of(rowOf(posting.id, posting.title, posting.company, posting.createdAt))),
                  ),
                ),
              ),
        ),
      )
      .subscribe({
        next: (rows) => {
          this.rows.set(rows.sort((a, b) => b.when.localeCompare(a.when)));
          this.loading.set(false);
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
}

export function coverageBuckets(requirements: RequirementMatch[]): CoverageBuckets {
  const matched: string[] = [];
  const partial: string[] = [];
  const missing: string[] = [];
  for (const item of requirements) {
    if (item.status === 'direct' || item.status === 'met') {
      matched.push(item.skill);
    } else if (item.status === 'partial' || item.status === 'unconfirmed') {
      partial.push(item.skill);
    } else if (item.status !== 'hidden') {
      missing.push(item.skill);
    }
  }
  return { matched, partial, missing };
}

export function lineOf(row: ScoreRow | null): string {
  if (!row || row.total === null) {
    return 'No estimate yet';
  }
  if (row.total < APPLY_LINE) {
    return 'Under 78';
  }
  if (row.toAnswer > 0) {
    return '78 or above, hard requirement open';
  }
  return '78 or above';
}

function rowOf(
  postingId: string,
  title: string,
  company: string,
  createdAt: string,
  latest?: TailoredResumeSummary,
): ScoreRow {
  return {
    postingId,
    title,
    company,
    createdAt,
    resumeId: latest?.id ?? null,
    version: latest?.version ?? null,
    total: latest?.total ?? null,
    toAnswer: 0,
    when: latest?.createdAt ?? createdAt,
  };
}

export interface PlotDot {
  postingId: string;
  x: number;
  y: number;
  tone: 'over' | 'under' | 'none';
  label: string;
}

export interface PlotLayout {
  width: number;
  height: number;
  left: number;
  right: number;
  top: number;
  bottom: number;
  applyY: number;
  y0: number;
  y50: number;
  y100: number;
  minLabel: string;
  maxLabel: string;
  dots: PlotDot[];
}

function buildPlot(rows: ScoreRow[]): PlotLayout {
  const left = 56;
  const right = 820;
  const top = 24;
  const bottom = 360;
  const yAt = (total: number) => bottom - (total / 100) * (bottom - top);
  const scored = rows.filter((row) => row.total !== null);
  const times = scored.map((row) => Date.parse(row.createdAt)).filter((value) => !Number.isNaN(value));
  const minT = times.length > 0 ? Math.min(...times) : Date.now();
  const maxT = times.length > 0 ? Math.max(...times) : minT;
  const span = Math.max(maxT - minT, 86_400_000);
  const xAt = (iso: string) => {
    const time = Date.parse(iso);
    if (Number.isNaN(time) || times.length <= 1) {
      return (left + right) / 2;
    }
    return left + ((time - minT) / span) * (right - left);
  };
  return {
    width: 860,
    height: 420,
    left,
    right,
    top,
    bottom,
    applyY: yAt(APPLY_LINE),
    y0: yAt(0),
    y50: yAt(50),
    y100: yAt(100),
    minLabel: dayLabel(minT),
    maxLabel: dayLabel(maxT),
    dots: scored.map((row) => ({
      postingId: row.postingId,
      x: xAt(row.createdAt),
      y: yAt(row.total ?? 0),
      tone: (row.total ?? 0) >= APPLY_LINE ? 'over' : 'under',
      label: `${row.title}${row.company ? ' at ' + row.company : ''}: Vettyou estimate ${row.total}/100`,
    })),
  };
}

function dayLabel(time: number): string {
  return new Date(time).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}
