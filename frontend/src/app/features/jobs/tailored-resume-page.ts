import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { JobPosting, JobPostingApi, MatchGroup, MatchStatus, ScoreFactor } from '../../core/api/job-posting-api';
import { useLink } from '../../core/api/skills-api';
import {
  LineReason,
  ResumeLine,
  resumeFileUrl,
  ResumeSuggestion,
  TailoredResume,
  TailoredResumeApi,
  TailoredResumeSummary,
  WorkOnItem,
} from '../../core/api/tailored-resume-api';
import { messageOf } from '../../core/auth/session';
import { STATUS_LABELS } from './requirement-item';

const GROUP_NAMES: Record<MatchGroup, string> = {
  required: 'required',
  responsibility: 'responsibility',
  preferred: 'nice to have',
};

const REASONS: Record<Exclude<LineReason, ''>, string> = {
  space: "Didn't fit on one page",
  not_needed: "Doesn't cover anything this posting asks for",
  too_long: 'Too long to print as one bullet',
  note: 'Reads as a note to yourself, so it is never printed',
  confirm: 'Not confirmed yet',
};

/** Best first. */
const STATUS_ORDER: MatchStatus[] = ['direct', 'partial', 'met', 'hidden', 'unconfirmed', 'unclear', 'not_shown', 'gap'];

/** One requirement a line backs. `parts` is empty when the line backs the requirement as a whole. */
export interface Backed {
  key: string;
  requirement: string;
  group: string;
  parts: string;
}

function same(a: string, b: string): boolean {
  return a.trim().toLowerCase() === b.trim().toLowerCase();
}

@Component({
  selector: 'app-tailored-resume-page',
  imports: [RouterLink, DatePipe, ReactiveFormsModule],
  templateUrl: './tailored-resume-page.html',
  styleUrl: './tailored-resume-page.scss',
})
export class TailoredResumePage {
  private readonly api = inject(TailoredResumeApi);

  private readonly postings = inject(JobPostingApi);

  readonly postingId = signal('');

  readonly posting = signal<JobPosting | null>(null);

  readonly versions = signal<TailoredResumeSummary[]>([]);

  readonly resume = signal<TailoredResume | null>(null);

  readonly loaded = signal(false);

  readonly opening = signal(false);

  readonly building = signal(false);

  readonly loadError = signal('');

  readonly actionError = signal('');

  readonly version = new FormControl('', { nonNullable: true });

  readonly link = useLink;

  readonly canBuild = computed(() => this.posting()?.status === 'ready');

  readonly pdfUrl = computed(() => this.fileUrl('resume.pdf'));

  readonly previewUrl = computed(() => this.fileUrl('preview.png'));

  readonly passed = computed(() => this.resume()?.checks.filter((check) => check.passed).length ?? 0);

  readonly coverage = computed(() => {
    const requirements = this.resume()?.requirements ?? [];
    return STATUS_ORDER.map((status) => ({
      status,
      label: STATUS_LABELS[status],
      items: requirements.filter((item) => item.status === status),
    })).filter((group) => group.items.length > 0);
  });

  constructor() {
    inject(ActivatedRoute)
      .paramMap.pipe(takeUntilDestroyed())
      .subscribe((params) => {
        this.postingId.set(params.get('id') ?? '');
        this.load();
      });
    this.version.valueChanges.pipe(takeUntilDestroyed()).subscribe((id) => {
      if (id && id !== this.resume()?.id) {
        this.open(id);
      }
    });
  }

  build(): void {
    this.actionError.set('');
    this.building.set(true);
    this.api.build(this.postingId()).subscribe({
      next: (resume) => {
        this.building.set(false);
        this.resume.set(resume);
        this.versions.update((versions) => [
          { id: resume.id, version: resume.version, createdAt: resume.createdAt, total: resume.score.total },
          ...versions,
        ]);
        this.version.setValue(resume.id, { emitEvent: false });
      },
      error: (error: unknown) => {
        this.building.set(false);
        this.actionError.set(messageOf(error));
      },
    });
  }

  reason(line: ResumeLine): string {
    if (line.reason === 'not_needed' && line.backs.length > 0) {
      return 'Already shown by other lines';
    }
    return line.reason ? REASONS[line.reason] : '';
  }

  /**
   * The requirements a line backs, each once, with the parts it shows. A line can back one part of a requirement, so
   * "Git (required): unit testing" must not read as showing Git.
   */
  backs(line: ResumeLine): Backed[] {
    const backed = new Map<string, { requirement: string; group: string; parts: string[] }>();
    for (const back of line.backs) {
      const key = `${back.group}|${back.requirement}`;
      const entry = backed.get(key) ?? { requirement: back.requirement, group: GROUP_NAMES[back.group], parts: [] };
      if (!entry.parts.includes(back.part)) {
        entry.parts.push(back.part);
      }
      backed.set(key, entry);
    }
    return [...backed.entries()].map(([key, entry]) => ({
      key,
      requirement: entry.requirement,
      group: entry.group,
      parts: entry.parts.length === 1 && same(entry.parts[0], entry.requirement) ? '' : entry.parts.join(', '),
    }));
  }

  /** The requirement a missing part belongs to, unless the part is the whole requirement. */
  partOf(item: WorkOnItem): string {
    return item.part && !same(item.part, item.requirement) ? item.requirement : '';
  }

  suggestionLink(suggestion: ResumeSuggestion): string[] | null {
    return suggestion.sourceId ? useLink({ kind: suggestion.sourceKind, id: suggestion.sourceId }) : null;
  }

  percent(value: number | null): string {
    return value === null ? 'n/a' : `${Math.round(value * 100)}%`;
  }

  weight(factor: ScoreFactor): string {
    if (factor.activeWeight === null || factor.activeWeight === factor.weight) {
      return `weight ${factor.weight}`;
    }
    return `weight ${factor.weight} × ${this.percent(factor.activeWeight / factor.weight)} coverage = ${factor.activeWeight.toFixed(1)}`;
  }

  points(factor: ScoreFactor): string {
    if (factor.value === null || factor.activeWeight === null) {
      return 'left out';
    }
    return `${(factor.activeWeight * factor.value).toFixed(1)} of ${Number(factor.activeWeight.toFixed(1))}`;
  }

  private fileUrl(file: 'resume.pdf' | 'preview.png'): string {
    const resume = this.resume();
    return resume ? resumeFileUrl(resume.postingId, resume.id, file) : '';
  }

  private load(): void {
    const id = this.postingId();
    this.loaded.set(false);
    this.loadError.set('');
    this.actionError.set('');
    this.resume.set(null);
    this.versions.set([]);
    forkJoin({ posting: this.postings.get(id), versions: this.api.list(id) }).subscribe({
      next: ({ posting, versions }) => {
        this.posting.set(posting);
        this.versions.set(versions);
        this.loaded.set(true);
        if (versions.length > 0) {
          this.open(versions[0].id);
        }
      },
      error: (error: unknown) =>
        this.loadError.set(
          error instanceof HttpErrorResponse && error.status === 404 ? 'That posting was not found.' : messageOf(error),
        ),
    });
  }

  private open(id: string): void {
    this.version.setValue(id, { emitEvent: false });
    this.actionError.set('');
    this.opening.set(true);
    this.api.get(this.postingId(), id).subscribe({
      next: (resume) => {
        this.opening.set(false);
        this.resume.set(resume);
      },
      error: (error: unknown) => {
        this.opening.set(false);
        this.actionError.set(messageOf(error));
      },
    });
  }
}
