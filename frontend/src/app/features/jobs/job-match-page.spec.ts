import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import {
  EvidenceCite,
  JobPosting,
  PartMatch,
  PartStatus,
  RequirementMatch,
  ScoreFactor,
} from '../../core/api/job-posting-api';
import { JobMatchPage } from './job-match-page';

function factor(
  key: string,
  label: string,
  weight: number,
  activeWeight: number | null,
  activeWeightIfConfirmed: number | null,
  value: number | null,
  valueIfConfirmed: number | null,
): ScoreFactor {
  return { key, label, weight, activeWeight, activeWeightIfConfirmed, value, valueIfConfirmed, how: 'How.' };
}

function cite(label: string, excerpt: string, confirmed = true): EvidenceCite {
  return { kind: confirmed ? 'job' : 'project', id: 'src1', label, excerpt, line: excerpt, confirmed };
}

function part(text: string, status: PartStatus, evidence: EvidenceCite[] = []): PartMatch {
  return { text, concept: text, support: status, status, reason: '', weight: null, sharedBy: 1, evidence };
}

const leading: RequirementMatch = {
  id: 'r1',
  skill: 'Leading technical projects or data workflows',
  kind: 'qualification',
  importance: 'required',
  group: 'required',
  category: 'experience',
  quote: 'Experience leading technical projects or data workflows.',
  logic: 'any',
  status: 'direct',
  note: 'One of these is enough. Shown directly.',
  answer: null,
  answeredAt: null,
  canMarkMet: false,
  weight: 1,
  credit: 1,
  creditIfConfirmed: 1,
  strength: 0.8,
  parts: [
    part('leading technical projects', 'direct', [cite('Developer at Liminal', 'Owned systems from requirements discovery')]),
    part('leading data workflows', 'not_shown'),
  ],
};

const automation: RequirementMatch = {
  ...leading,
  id: 'r2',
  skill: 'Pipeline automation',
  kind: 'responsibility',
  group: 'responsibility',
  logic: 'all',
  status: 'partial',
  note: 'Transferable: automation of pipeline workflows. Not shown: task generation.',
  credit: 0.25,
  creditIfConfirmed: 0.25,
  strength: 0.8,
  parts: [
    part('automation of pipeline workflows', 'transferable', [cite('Developer at Liminal', 'automates administrative processes')]),
    part('task generation', 'not_shown'),
  ],
};

const angular: RequirementMatch = {
  ...leading,
  id: 'r3',
  skill: 'Angular',
  importance: 'preferred',
  group: 'preferred',
  logic: 'all',
  status: 'unconfirmed',
  note: 'Shown only in project lines you have not confirmed yet.',
  credit: 0,
  creditIfConfirmed: 1,
  strength: null,
  parts: [part('Angular', 'unconfirmed', [cite('NanoHub', 'Admin app built with Angular', false)])],
};

const authorized: RequirementMatch = {
  ...leading,
  id: 'r4',
  skill: 'Authorized to work in the US',
  logic: 'all',
  status: 'unclear',
  note: "This can't be settled from your saved text.",
  canMarkMet: true,
  weight: null,
  credit: null,
  creditIfConfirmed: null,
  strength: null,
  parts: [part('Authorized to work in the US', 'unclear')],
};

const reading: JobPosting = {
  id: 'jp1',
  title: '',
  company: '',
  pay: '',
  preview: '',
  status: 'reading',
  createdAt: '',
  reader: '',
  matcher: '',
  matchedAt: null,
  evidenceChanged: false,
  error: '',
  body: 'Strategic Project Lead',
  requirements: [],
  score: {
    total: null,
    unrounded: null,
    totalIfConfirmed: null,
    unroundedIfConfirmed: null,
    factors: [],
    formula: 'Nothing can be scored yet.',
    formulaIfConfirmed: 'Nothing can be scored yet.',
    toConfirm: 0,
    toAnswer: 0,
    readiness: { value: 0, checks: [] },
    label: 'Vettyou estimate',
    note: 'It is not an employer score.',
  },
};

const ready: JobPosting = {
  ...reading,
  title: 'Strategic Project Lead',
  company: 'Crossing Hurdles',
  status: 'ready',
  reader: 'openai:gpt-5-mini',
  matcher: 'openai:gpt-5-mini',
  matchedAt: '2026-11-07T17:00:00Z',
  requirements: [leading, automation, angular, authorized],
  score: {
    ...reading.score,
    total: 69,
    unrounded: 68.62,
    totalIfConfirmed: 85,
    unroundedIfConfirmed: 85.11,
    factors: [
      factor('required', 'Required qualifications', 50, 50, 50, 1, 1),
      factor('responsibility', 'Responsibilities', 15, 15, 15, 0.25, 0.25),
      factor('preferred', 'Nice to have', 15, 15, 15, 0, 1),
      factor('strength', 'Evidence strength', 15, 10.0785, 12.891, 0.8, 0.8),
      factor('readability', 'Readability', 15, null, null, null, null),
    ],
    formula:
      '100 x (50 x 1.0000 + 15 x 0.2500 + 15 x 0.0000 + 10.0785 x 0.8000) / 90.0785 = 68.62, rounded to 69. ' +
      'Evidence strength weight: 15 x 0.6719 coverage = 10.0785.',
    toConfirm: 1,
    toAnswer: 1,
    readiness: {
      value: 0.5,
      checks: [
        { label: 'Name', done: true },
        { label: 'Phone', done: false },
      ],
    },
  },
};

describe('JobMatchPage', () => {
  afterEach(() => vi.useRealTimers());

  async function setup() {
    vi.useFakeTimers();
    await TestBed.configureTestingModule({
      imports: [JobMatchPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: 'jp1' })) } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(JobMatchPage);
    return { fixture, page: fixture.componentInstance, http: TestBed.inject(HttpTestingController) };
  }

  it('polls while reading and checking, then shows each part with its label, source, and quote', async () => {
    const { fixture, page, http } = await setup();

    vi.advanceTimersByTime(0);
    http.expectOne('/api/v1/jobs/jp1').flush(reading);
    vi.advanceTimersByTime(2000);
    http.expectOne('/api/v1/jobs/jp1').flush({ ...reading, status: 'matching' });
    expect(page.busy()).toBe(true);
    vi.advanceTimersByTime(2000);
    http.expectOne('/api/v1/jobs/jp1').flush(ready);
    http.expectOne('/api/v1/jobs/jp1/resumes').flush([]);
    vi.advanceTimersByTime(4000);
    http.expectNone('/api/v1/jobs/jp1');

    expect(page.groups().map((group) => group.items.map((item) => item.id))).toEqual([['r1', 'r4'], ['r2'], ['r3']]);
    expect(page.missingParts().map((missing) => missing.text)).toEqual(['task generation']);
    expect(page.toAnswer().map((item) => item.id)).toEqual(['r4']);
    expect(page.toConfirm().map((item) => item.id)).toEqual(['r3']);

    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const text = element.textContent ?? '';
    expect(text).toContain('69');
    expect(text).toContain('Vettyou estimate');
    expect(text).toContain('to see this estimate marked on the PDF');
    expect(text).toContain('85/100 if you confirm 1 flagged project line');
    expect(text).toContain('How it adds up: 100 x (50 x 1.0000');
    expect(text).toContain('weight 15 × 67% coverage = 10.1');
    expect(text).toContain('8.1 of 10.1');
    expect(text).toContain('weight 50');
    expect(text).toContain('left out');
    expect(text).toContain('not part of the score');
    expect(text).toContain('Phone: missing');
    for (const label of [
      'Direct evidence',
      'Transferable or partial evidence',
      'Needs your confirmation',
      'Needs clarification',
    ]) {
      expect(text).toContain(label);
    }
    expect(text).toContain('“Owned systems from requirements discovery”');
    expect(text).toContain('not confirmed yet');
    expect(text).toContain('Download job.md');
    expect(element.querySelector('a[href="/api/v1/jobs/jp1/posting.md"]')).toBeTruthy();
    expect(text).not.toContain('ATS score:');

    const meets = Array.from(element.querySelectorAll('button')).filter((button) =>
      button.textContent?.includes('I meet this'),
    );
    expect(meets.map((button) => button.textContent)).toEqual([expect.stringContaining('Authorized to work in the US')]);
    expect(Array.from(element.querySelectorAll('.share a')).map((link) => link.getAttribute('href'))).toEqual([
      '/jobs/jp1/resume',
      '/jobs/jp1/report',
    ]);
    http.verify();
  });

  it('saves answers and shows why one was refused', async () => {
    const { page, http } = await setup();
    vi.advanceTimersByTime(0);
    http.expectOne('/api/v1/jobs/jp1').flush(ready);
    http.expectOne('/api/v1/jobs/jp1/resumes').flush([]);

    page.answer(authorized, 'meets');
    const save = http.expectOne('/api/v1/jobs/jp1/requirements/r4/answer');
    expect(save.request.method).toBe('PUT');
    expect(save.request.body).toEqual({ answer: 'meets' });
    save.flush({
      ...ready,
      requirements: [leading, automation, angular, { ...authorized, status: 'met', answer: 'meets' }],
    });
    expect(page.toAnswer()).toEqual([]);

    page.answer(leading, 'meets');
    http.expectOne('/api/v1/jobs/jp1/requirements/r1/answer').flush(
      { fieldErrors: [{ field: 'answer', message: "Only items Vettyou can't check from text can be marked as met." }] },
      { status: 422, statusText: 'Unprocessable Content' },
    );
    expect(page.actionError()).toContain("Only items Vettyou can't check");
    expect(page.answering()).toBeNull();
    http.verify();
  });

  it('checks the evidence again and waits for the new match', async () => {
    const { page, http } = await setup();
    vi.advanceTimersByTime(0);
    http.expectOne('/api/v1/jobs/jp1').flush({ ...ready, evidenceChanged: true });
    http.expectOne('/api/v1/jobs/jp1/resumes').flush([]);

    page.rematch();
    const start = http.expectOne('/api/v1/jobs/jp1/match');
    expect(start.request.method).toBe('POST');
    start.flush({ id: 'jp1', title: '', company: '', status: 'matching', createdAt: '' });
    expect(page.posting()?.status).toBe('matching');

    vi.advanceTimersByTime(0);
    http.expectOne('/api/v1/jobs/jp1').flush({ ...ready, status: 'matching' });
    vi.advanceTimersByTime(2000);
    http.expectOne('/api/v1/jobs/jp1').flush(ready);
    http.expectOne('/api/v1/jobs/jp1/resumes').flush([]);
    expect(page.posting()?.evidenceChanged).toBe(false);
    vi.advanceTimersByTime(4000);
    http.expectNone('/api/v1/jobs/jp1');
    http.verify();
  });

  it('marks scored lines on the resume PDF next to the estimate', async () => {
    const { fixture, page, http } = await setup();
    vi.advanceTimersByTime(0);
    http.expectOne('/api/v1/jobs/jp1').flush(ready);
    http.expectOne('/api/v1/jobs/jp1/resumes').flush([
      { id: 'rv1', version: 1, createdAt: '2026-11-07T18:00:00Z', total: 73 },
    ]);
    http.expectOne('/api/v1/jobs/jp1/resumes/rv1').flush({
      id: 'rv1',
      postingId: 'jp1',
      highlights: [
        {
          text: 'Built React screens backed by PostgreSQL',
          why: 'React — React',
          boxes: [{ x: 80, y: 400, width: 400, height: 18 }],
        },
      ],
    });
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector<HTMLImageElement>('.preview img')?.getAttribute('src')).toBe(
      '/api/v1/jobs/jp1/resumes/rv1/preview.png',
    );
    const hit = element.querySelector<HTMLButtonElement>('.hit')!;
    expect(hit.getAttribute('aria-label')).toContain('Built React screens backed by PostgreSQL');
    hit.click();
    fixture.detectChanges();
    expect(element.textContent).toContain('Why this line.');
    expect(element.textContent).toContain('React — React');
    expect(element.textContent).toContain('Show on resume');

    page.showOnResume('React');
    expect(page.picked()).toBe('Built React screens backed by PostgreSQL');
    http.verify();
  });
});
