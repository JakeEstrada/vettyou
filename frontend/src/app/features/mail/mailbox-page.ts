import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { ApplicationsApi, JobApplication } from '../../core/api/applications-api';
import { JobPostingApi, JobPostingSummary } from '../../core/api/job-posting-api';
import { TailoredResumeApi, TailoredResumeSummary } from '../../core/api/tailored-resume-api';
import { messageOf } from '../../core/auth/session';
import { ApplicationTracker, TrackableJob } from '../applications/application-tracker';
import { MailboxConnect } from './mailbox-connect';

@Component({
  selector: 'app-mailbox-page',
  imports: [RouterLink, MailboxConnect, ApplicationTracker],
  templateUrl: './mailbox-page.html',
  styleUrl: './mailbox-page.scss',
})
export class MailboxPage {
  private readonly api = inject(ApplicationsApi);

  private readonly postings = inject(JobPostingApi);

  private readonly resumes = inject(TailoredResumeApi);

  readonly loading = signal(true);

  readonly missing = signal(false);

  readonly error = signal('');

  readonly items = signal<JobApplication[]>([]);

  readonly openings = signal<TrackableJob[]>([]);

  constructor() {
    this.load();
  }

  reload(): void {
    this.api.list().subscribe({
      next: (items) => this.items.set(items),
      error: (error: unknown) => this.error.set(messageOf(error)),
    });
  }

  private load(): void {
    this.api
      .list()
      .pipe(
        switchMap((items) =>
          forkJoin({
            items: of(items),
            postings: this.postings.list().pipe(catchError(() => of([] as JobPostingSummary[]))),
          }),
        ),
        switchMap((library) =>
          forkJoin({
            items: of(library.items),
            openings:
              library.postings.length === 0
                ? of([] as TrackableJob[])
                : forkJoin(library.postings.map((posting) => this.openingOf(posting))),
          }),
        ),
      )
      .subscribe({
        next: (library) => {
          this.items.set(library.items);
          this.openings.set(library.openings);
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

  private openingOf(posting: JobPostingSummary): Observable<TrackableJob> {
    const fallback: TrackableJob = {
      postingId: posting.id,
      title: posting.title,
      company: posting.company,
      pay: posting.pay ?? '',
      preview: posting.preview ?? '',
      createdAt: posting.createdAt,
      total: null,
    };
    return this.resumes.list(posting.id).pipe(
      map((list: TailoredResumeSummary[]): TrackableJob => ({ ...fallback, total: list[0]?.total ?? null })),
      catchError(() => of(fallback)),
    );
  }
}
