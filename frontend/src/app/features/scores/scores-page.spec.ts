import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { JobPosting } from '../../core/api/job-posting-api';
import { TailoredResume } from '../../core/api/tailored-resume-api';
import { ScoresPage } from './scores-page';

function posting(overrides: Partial<JobPosting> = {}): JobPosting {
  return {
    id: 'jp1',
    title: 'Engineer',
    company: 'Acme',
    pay: '',
    preview: 'Build React',
    status: 'ready',
    createdAt: '2026-11-01T00:00:00Z',
    reader: 'rules',
    matcher: 'rules',
    matchedAt: '2026-11-01T00:00:00Z',
    evidenceChanged: false,
    error: '',
    body: 'Build screens in React.\nRust required.',
    requirements: [
      {
        id: 'a',
        skill: 'React',
        kind: 'qualification',
        importance: 'required',
        group: 'required',
        category: 'framework',
        quote: 'React',
        logic: 'all',
        status: 'direct',
        note: '',
        answer: null,
        answeredAt: null,
        canMarkMet: false,
        weight: 1,
        credit: 1,
        creditIfConfirmed: 1,
        strength: 1,
        parts: [],
      },
      {
        id: 'b',
        skill: 'TypeScript',
        kind: 'qualification',
        importance: 'preferred',
        group: 'preferred',
        category: 'language',
        quote: 'TypeScript',
        logic: 'all',
        status: 'partial',
        note: '',
        answer: null,
        answeredAt: null,
        canMarkMet: false,
        weight: 1,
        credit: 0.5,
        creditIfConfirmed: 0.5,
        strength: 0.5,
        parts: [],
      },
      {
        id: 'c',
        skill: 'Rust',
        kind: 'qualification',
        importance: 'required',
        group: 'required',
        category: 'language',
        quote: 'Rust',
        logic: 'all',
        status: 'not_shown',
        note: '',
        answer: null,
        answeredAt: null,
        canMarkMet: false,
        weight: 1,
        credit: 0,
        creditIfConfirmed: 0,
        strength: 0,
        parts: [],
      },
    ],
    score: {
      total: 83,
      unrounded: 83,
      totalIfConfirmed: 83,
      unroundedIfConfirmed: 83,
      factors: [],
      formula: '',
      formulaIfConfirmed: '',
      toConfirm: 0,
      toAnswer: 0,
      readiness: { value: 1, checks: [] },
      label: 'Vettyou estimate',
      note: 'Not an employer score.',
    },
    ...overrides,
  };
}

describe('ScoresPage', () => {
  it('lists the latest estimate on ATS global and deletes the match', async () => {
    await TestBed.configureTestingModule({
      imports: [ScoresPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(ScoresPage);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/jobs').flush([
      { id: 'jp1', title: 'Engineer', company: 'Acme', status: 'ready', createdAt: '2026-11-01T00:00:00Z' },
    ]);
    http.expectOne('/api/v1/jobs/jp1/resumes').flush([
      { id: 'r1', version: 5, createdAt: '2026-11-07T00:00:00Z', total: 83 },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('ATS global');
    expect(text).toContain('Engineer');
    expect(text).toContain('75 or above');
    expect(text).not.toContain('ATS score:');
    expect(fixture.nativeElement.querySelector('circle.over')).toBeTruthy();

    fixture.componentInstance.remove('jp1');
    const deleted = http.expectOne((request) => request.method === 'DELETE' && request.url === '/api/v1/jobs/jp1');
    deleted.flush(null);
    await fixture.whenStable();
    expect(fixture.componentInstance.rows()).toEqual([]);
    http.verify();
  });

  it('opens matched, partial, and missing plus the marked resume', async () => {
    await TestBed.configureTestingModule({
      imports: [ScoresPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(ScoresPage);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/jobs').flush([
      { id: 'jp1', title: 'Engineer', company: 'Acme', status: 'ready', createdAt: '2026-11-01T00:00:00Z' },
    ]);
    http.expectOne('/api/v1/jobs/jp1/resumes').flush([
      { id: 'r1', version: 5, createdAt: '2026-11-07T00:00:00Z', total: 83 },
    ]);
    await fixture.whenStable();

    fixture.componentInstance.open(fixture.componentInstance.rows()[0]);
    http.expectOne('/api/v1/jobs/jp1').flush(posting());
    http.expectOne('/api/v1/jobs/jp1/resumes/r1').flush({
      id: 'r1',
      postingId: 'jp1',
      version: 5,
      template: 'ats-first',
      createdAt: '2026-11-07T00:00:00Z',
      postingTitle: 'Engineer',
      postingCompany: 'Acme',
      stale: false,
      staleReasons: [],
      score: { total: 83, note: 'Not an employer score.' },
      suggestions: [{ kind: 'skills', text: 'Keep React on the project page.', line: '', sourceKind: '', sourceId: null }],
      workOn: [],
      checks: [],
      lines: [
        {
          kind: 'job',
          sourceId: 'j1',
          label: 'Engineer',
          text: 'Built React screens',
          gain: 0.4,
          backs: [{ requirement: 'React', part: 'React', group: 'required', support: 'direct' }],
          reason: '',
        },
      ],
      highlights: [
        {
          text: 'Built React screens',
          why: 'React — React',
          boxes: [{ x: 80, y: 400, width: 400, height: 18 }],
        },
      ],
      document: {
        name: 'Ada',
        contact: [],
        sections: [
          { heading: 'Skills', rows: [], entries: [] },
          {
            heading: 'Experience',
            rows: [],
            entries: [{ title: 'Engineer', dates: '2024', subtitle: 'Acme', bullets: ['Built React screens'] }],
          },
        ],
      },
    } as unknown as TailoredResume);
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Matched');
    expect(text).toContain('React');
    expect(text).toContain('TypeScript');
    expect(text).toContain('Rust');
    expect(text).toContain('Download job.md');
    expect(text).toContain('Build screens in React');
    expect(fixture.nativeElement.querySelector('a[href="/api/v1/jobs/jp1/posting.md"]')).toBeTruthy();
    const marked = fixture.nativeElement.querySelector('.hit') as HTMLButtonElement;
    expect(marked.getAttribute('aria-label')).toContain('Built React screens');
    marked.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('React — React');
    http.verify();
  });
});
