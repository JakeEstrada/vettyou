import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, switchMap, takeWhile, timer } from 'rxjs';

import {
  Answer,
  JobPosting,
  JobPostingApi,
  MatchGroup,
  postingMarkdownUrl,
  PostingStatus,
  RequirementMatch,
  ScoreFactor,
} from '../../core/api/job-posting-api';
import { fieldErrorsOf, messageOf } from '../../core/auth/session';
import { TailoredResume, TailoredResumeApi } from '../../core/api/tailored-resume-api';
import { ResumePreview } from '../resume/resume-preview';
import { RequirementItem } from './requirement-item';

const GROUPS: { id: MatchGroup; title: string }[] = [
  { id: 'required', title: 'Required qualifications' },
  { id: 'responsibility', title: 'Responsibilities' },
  { id: 'preferred', title: 'Nice to have' },
];

const BUSY: PostingStatus[] = ['reading', 'matching'];

const POLL_MS = 2000;

export interface MissingPart {
  key: string;
  text: string;
  from: string;
}

@Component({
  selector: 'app-job-match-page',
  imports: [RouterLink, DatePipe, RequirementItem, ResumePreview],
  templateUrl: './job-match-page.html',
  styleUrl: './job-match-page.scss',
})
export class JobMatchPage {
  private readonly api = inject(JobPostingApi);

  private readonly resumes = inject(TailoredResumeApi);

  private readonly router = inject(Router);

  private readonly destroyRef = inject(DestroyRef);

  private poll?: Subscription;

  private id = '';

  readonly posting = signal<JobPosting | null>(null);

  readonly loadError = signal('');

  readonly actionError = signal('');

  readonly answering = signal<string | null>(null);

  readonly confirming = signal(false);

  readonly deleting = signal(false);

  readonly resume = signal<TailoredResume | null>(null);

  readonly picked = signal('');

  readonly markdownUrl = postingMarkdownUrl;

  readonly busy = computed(() => BUSY.includes(this.posting()?.status ?? 'ready'));

  readonly groups = computed(() =>
    GROUPS.map((group) => ({
      ...group,
      items: this.posting()?.requirements.filter((item) => item.group === group.id) ?? [],
    })),
  );

  readonly toAnswer = computed(
    () => this.posting()?.requirements.filter((item) => item.canMarkMet && !item.answer) ?? [],
  );

  readonly toConfirm = computed(
    () =>
      this.posting()?.requirements.filter(
        (item) => !item.answer && item.parts.some((part) => part.status === 'unconfirmed'),
      ) ?? [],
  );

  /** Parts nothing saved shows. Alternatives of an "any one" item that is already met are not gaps. */
  readonly missingParts = computed<MissingPart[]>(() =>
    (this.posting()?.requirements ?? [])
      .filter((item) => !item.answer && !(item.logic === 'any' && item.status === 'direct'))
      .flatMap((item) =>
        item.parts
          .filter((part) => part.status === 'not_shown' || part.status === 'changed')
          .map((part) => ({ key: `${item.id}:${part.text}`, text: part.text, from: item.skill })),
      ),
  );

  readonly showIfConfirmed = computed(() => {
    const score = this.posting()?.score;
    return !!score && score.totalIfConfirmed !== null && score.totalIfConfirmed !== score.total;
  });

  constructor() {
    inject(ActivatedRoute)
      .paramMap.pipe(takeUntilDestroyed())
      .subscribe((params) => {
        this.id = params.get('id') ?? '';
        this.posting.set(null);
        this.resume.set(null);
        this.picked.set('');
        this.loadError.set('');
        this.actionError.set('');
        this.confirming.set(false);
        this.watch();
      });
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

  matcherName(posting: JobPosting): string {
    return posting.matcher.startsWith('openai:') ? posting.matcher.replace('openai:', '') : 'the keyword rules';
  }

  choose(text: string): void {
    this.picked.update((current) => (current === text ? '' : text));
  }

  showOnResume(skill: string): void {
    const hit = (this.resume()?.highlights ?? []).find((mark) =>
      mark.why.toLowerCase().includes(skill.trim().toLowerCase()),
    );
    this.picked.set(hit?.text ?? '');
  }

  answer(item: RequirementMatch, answer: Answer | null): void {
    this.actionError.set('');
    this.answering.set(item.id);
    this.api.answer(this.id, item.id, answer).subscribe({
      next: (posting) => {
        this.answering.set(null);
        this.posting.set(posting);
      },
      error: (error: unknown) => {
        this.answering.set(null);
        const fieldErrors = fieldErrorsOf(error);
        this.actionError.set(fieldErrors.length > 0 ? fieldErrors[0].message : messageOf(error));
      },
    });
  }

  reread(): void {
    this.start(this.api.reread(this.id), 'reading');
  }

  rematch(): void {
    this.start(this.api.rematch(this.id), 'matching');
  }

  removeRequirement(requirement: RequirementMatch): void {
    this.actionError.set('');
    this.api.removeRequirement(this.id, requirement.id).subscribe({
      next: (posting) => this.posting.set(posting),
      error: (error: unknown) => this.actionError.set(messageOf(error)),
    });
  }

  remove(): void {
    this.deleting.set(true);
    this.api.remove(this.id).subscribe({
      next: () => void this.router.navigate(['/jobs']),
      error: (error: unknown) => {
        this.deleting.set(false);
        this.confirming.set(false);
        this.actionError.set(messageOf(error));
      },
    });
  }

  private start(request: ReturnType<JobPostingApi['rematch']>, status: PostingStatus): void {
    this.actionError.set('');
    request.subscribe({
      next: () => {
        this.posting.update((posting) => (posting ? { ...posting, status, error: '' } : posting));
        this.watch();
      },
      error: (error: unknown) => this.actionError.set(messageOf(error)),
    });
  }

  private loadResume(postingId: string): void {
    this.resumes.list(postingId).subscribe({
      next: (versions) => {
        if (versions.length === 0) {
          this.resume.set(null);
          return;
        }
        this.resumes.get(postingId, versions[0].id).subscribe({
          next: (resume) => this.resume.set(resume),
          error: () => this.resume.set(null),
        });
      },
      error: () => this.resume.set(null),
    });
  }

  private watch(): void {
    this.poll?.unsubscribe();
    const id = this.id;
    this.poll = timer(0, POLL_MS)
      .pipe(
        switchMap(() => this.api.get(id)),
        takeWhile((posting) => BUSY.includes(posting.status), true),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (posting) => {
          this.posting.set(posting);
          if (posting.status === 'ready') {
            this.loadResume(posting.id);
          }
        },
        error: (error: unknown) =>
          this.loadError.set(
            error instanceof HttpErrorResponse && error.status === 404 ? 'That posting was not found.' : messageOf(error),
          ),
      });
  }
}
