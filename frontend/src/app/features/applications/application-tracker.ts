import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ApplicationInput, ApplicationsApi, JobApplication, statusLabel } from '../../core/api/applications-api';
import { jobsFromHistory } from '../../core/api/history-file';
import { JobPosting, JobPostingApi } from '../../core/api/job-posting-api';
import { messageOf } from '../../core/auth/session';
import { day } from '../../shared/format/dates';
import { postingBlocks } from '../../shared/format/posting';

export interface TrackableJob {
  postingId: string;
  title: string;
  company: string;
  pay: string;
  preview: string;
  createdAt: string;
  total: number | null;
}

export interface LedgerRow {
  key: string;
  application: JobApplication | null;
  postingId: string | null;
  title: string;
  company: string;
  pay: string;
  preview: string;
  estimate: number | null;
  date: string;
  status: JobApplication['status'] | null;
}

@Component({
  selector: 'app-application-tracker',
  imports: [RouterLink],
  templateUrl: './application-tracker.html',
  styleUrl: './application-tracker.scss',
})
export class ApplicationTracker {
  private readonly api = inject(ApplicationsApi);

  private readonly postings = inject(JobPostingApi);

  readonly items = input<JobApplication[]>([]);

  readonly openings = input<TrackableJob[]>([]);

  readonly changed = signal<JobApplication[] | null>(null);

  readonly error = signal('');

  readonly confirming = signal('');

  readonly tracking = signal('');

  readonly looking = signal('');

  readonly importing = signal(false);

  readonly importNote = signal('');

  readonly opened = signal('');

  readonly details = signal<Record<string, JobPosting>>({});

  readonly rows = computed(() => this.ledger());

  constructor() {
    effect(() => {
      for (const row of this.rows()) {
        if (row.postingId && this.scoreOf(row) === null && !this.details()[row.postingId]) {
          this.load(row.postingId);
        }
      }
    });
  }

  shown(): JobApplication[] {
    return [...(this.changed() ?? this.items())].sort((a, b) => dateOf(b).localeCompare(dateOf(a)));
  }

  when(iso: string): string {
    return day(iso);
  }

  statusOf(item: JobApplication): string {
    return item.status === 'draft' ? 'Waiting on email' : statusLabel(item.status);
  }

  scoreOf(row: LedgerRow): number | null {
    return this.detail(row.postingId)?.score.total ?? row.estimate;
  }

  detail(postingId: string | null): JobPosting | null {
    return postingId ? (this.details()[postingId] ?? null) : null;
  }

  blocks(body: string): ReturnType<typeof postingBlocks> {
    return postingBlocks(body);
  }

  openPosting(row: LedgerRow): void {
    if (!row.postingId) {
      return;
    }
    if (this.opened() === row.key) {
      this.opened.set('');
      return;
    }
    this.opened.set(row.key);
    this.load(row.postingId);
  }

  async importFile(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }
    this.error.set('');
    this.importNote.set('');
    this.importing.set(true);
    let jobs;
    try {
      jobs = jobsFromHistory(JSON.parse(await file.text()) as unknown);
    } catch {
      this.importing.set(false);
      this.error.set('That file is not a job history JSON.');
      return;
    }
    this.api.importHistory(jobs).subscribe({
      next: (result) => {
        this.importing.set(false);
        this.changed.set([...result.applications, ...this.shown()]);
        this.importNote.set(noteOf(result.imported, result.skipped));
      },
      error: (error: unknown) => {
        this.importing.set(false);
        this.error.set(messageOf(error));
      },
    });
  }

  track(job: TrackableJob): void {
    this.error.set('');
    this.tracking.set(job.postingId);
    this.api.create(this.body(job.title, job.company, job.pay, 'draft', '', job.postingId)).subscribe({
      next: (created) => {
        this.tracking.set('');
        this.changed.set([{ ...created, estimate: created.estimate ?? job.total }, ...this.shown()]);
      },
      error: (error: unknown) => {
        this.tracking.set('');
        this.error.set(messageOf(error));
      },
    });
  }

  remove(id: string): void {
    this.error.set('');
    this.api.remove(id).subscribe({
      next: () => {
        this.confirming.set('');
        this.changed.set(this.shown().filter((item) => item.id !== id));
      },
      error: (error: unknown) => this.error.set(messageOf(error)),
    });
  }

  private load(postingId: string): void {
    if (this.details()[postingId]) {
      return;
    }
    this.looking.set(postingId);
    this.postings.get(postingId).subscribe({
      next: (posting) => {
        this.details.update((map) => ({ ...map, [postingId]: posting }));
        if (this.looking() === postingId) {
          this.looking.set('');
        }
      },
      error: (error: unknown) => {
        if (this.looking() === postingId) {
          this.looking.set('');
        }
        this.error.set(messageOf(error));
      },
    });
  }

  private ledger(): LedgerRow[] {
    const apps = this.shown();
    const byPosting = new Map(this.openings().map((job) => [job.postingId, job]));
    const rows: LedgerRow[] = apps.map((item) => {
      const job = item.postingId ? byPosting.get(item.postingId) : undefined;
      return {
        key: item.id,
        application: item,
        postingId: item.postingId,
        title: named(item.title, job?.title),
        company: item.company || job?.company || '',
        pay: item.pay || job?.pay || '',
        preview: job?.preview || '',
        estimate: item.estimate ?? job?.total ?? null,
        date: dateOf(item),
        status: item.status,
      };
    });
    for (const job of this.openings()) {
      if (isTracked(apps, job)) {
        continue;
      }
      rows.push({
        key: job.postingId,
        application: null,
        postingId: job.postingId,
        title: named(job.title, ''),
        company: job.company,
        pay: job.pay,
        preview: job.preview,
        estimate: job.total,
        date: job.createdAt,
        status: null,
      });
    }
    return rows.sort((a, b) => b.date.localeCompare(a.date));
  }

  private body(
    title: string,
    company: string,
    pay: string,
    status: JobApplication['status'],
    notes: string,
    postingId: string | null,
  ): ApplicationInput {
    return {
      title: title.trim(),
      company: company.trim(),
      location: '',
      pay: (pay ?? '').trim(),
      status,
      postingId,
      appliedAt: status === 'applied' ? new Date().toISOString() : null,
      notes: notes.trim(),
    };
  }
}

function named(title: string, fallback: string | undefined): string {
  const value = title.trim();
  if (value !== '' && value.toLowerCase() !== 'untitled posting') {
    return value;
  }
  const next = (fallback ?? '').trim();
  return next !== '' && next.toLowerCase() !== 'untitled posting' ? next : value;
}

function dateOf(item: JobApplication): string {
  return item.appliedAt || item.createdAt;
}

function noteOf(imported: number, skipped: number): string {
  const saved = imported === 1 ? 'Imported 1 job.' : `Imported ${imported} jobs.`;
  if (skipped === 0) {
    return saved;
  }
  return `${saved} Skipped ${skipped} with no recoverable title, already saved, or reference only.`;
}

function isTracked(apps: JobApplication[], job: TrackableJob): boolean {
  return apps.some(
    (item) =>
      item.postingId === job.postingId ||
      (item.title === job.title && item.company === job.company),
  );
}
