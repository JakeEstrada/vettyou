import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { JobPostingApi } from '../../core/api/job-posting-api';
import {
  ResumeLine,
  TailoredResume,
  TailoredResumeApi,
  TailoredResumeSummary,
} from '../../core/api/tailored-resume-api';
import { messageOf } from '../../core/auth/session';
import { ResumeSheet, SheetHighlight, SheetModel, sheetFromDocument } from '../resume/resume-sheet';

export interface ScoreRow {
  postingId: string;
  title: string;
  company: string;
  createdAt: string;
  resumeId: string | null;
  version: number | null;
  total: number | null;
  when: string;
}

@Component({
  selector: 'app-scores-page',
  imports: [RouterLink, ResumeSheet],
  templateUrl: './scores-page.html',
  styleUrl: './scores-page.scss',
})
export class ScoresPage {
  private readonly postings = inject(JobPostingApi);

  private readonly resumes = inject(TailoredResumeApi);

  readonly loading = signal(true);

  readonly missing = signal(false);

  readonly error = signal('');

  readonly rows = signal<ScoreRow[]>([]);

  readonly confirming = signal('');

  readonly deleting = signal(false);

  readonly opened = signal<TailoredResume | null>(null);

  readonly opening = signal(false);

  readonly picked = signal('');

  readonly sheet = computed<SheetModel | null>(() => {
    const resume = this.opened();
    return resume ? sheetFromDocument(resume.document) : null;
  });

  readonly highlights = computed<SheetHighlight[]>(() =>
    (this.opened()?.lines ?? []).filter((line) => line.backs.length > 0).map((line) => ({
      text: line.text,
      why: whyOf(line),
    })),
  );

  readonly why = computed(() => this.highlights().find((item) => item.text === this.picked())?.why ?? '');

  constructor() {
    this.load();
  }

  open(row: ScoreRow): void {
    if (!row.resumeId) {
      return;
    }
    this.picked.set('');
    this.opening.set(true);
    this.resumes.get(row.postingId, row.resumeId).subscribe({
      next: (resume) => {
        this.opening.set(false);
        this.opened.set(resume);
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
        if (this.opened()?.postingId === id) {
          this.opened.set(null);
        }
      },
      error: (error: unknown) => {
        this.deleting.set(false);
        this.error.set(messageOf(error));
      },
    });
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

function rowOf(
  postingId: string,
  title: string,
  company: string,
  createdAt: string,
  latest?: TailoredResumeSummary,
): ScoreRow {
  return {
    postingId,
    title: title,
    company,
    createdAt,
    resumeId: latest?.id ?? null,
    version: latest?.version ?? null,
    total: latest?.total ?? null,
    when: latest?.createdAt ?? createdAt,
  };
}

function whyOf(line: ResumeLine): string {
  return line.backs
    .map((back) => `${back.part} — ${back.requirement}${back.support === 'transferable' ? ' (transferable)' : ''}`)
    .join('; ');
}
