import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { JobPosting, RequirementMatch } from '../../core/api/job-posting-api';
import { TailoredResume, TailoredResumeSummary } from '../../core/api/tailored-resume-api';
import { TailoredResumePage } from './tailored-resume-page';

const posting: JobPosting = {
  id: 'jp1',
  title: 'Full-stack developer',
  company: 'Acme',
  pay: '',
  preview: '',
  status: 'ready',
  createdAt: '2026-11-07T17:00:00Z',
  reader: 'openai:gpt-5-mini',
  matcher: 'openai:gpt-5-mini',
  matchedAt: '2026-11-07T17:00:00Z',
  evidenceChanged: false,
  error: '',
  body: 'We need React and PostgreSQL.',
  requirements: [],
  score: {
    total: 69,
    unrounded: 69.2,
    totalIfConfirmed: 69,
    unroundedIfConfirmed: 69.2,
    factors: [],
    formula: '',
    formulaIfConfirmed: '',
    toConfirm: 0,
    toAnswer: 0,
    readiness: { value: 1, checks: [] },
    label: 'Vettyou estimate',
    note: '',
  },
};

function requirement(id: string, skill: string, status: RequirementMatch['status']): RequirementMatch {
  return {
    id,
    skill,
    kind: 'qualification',
    importance: 'required',
    group: 'required',
    category: 'technology',
    quote: skill,
    logic: 'all',
    status,
    note: '',
    answer: null,
    answeredAt: null,
    canMarkMet: false,
    weight: 1,
    credit: 1,
    creditIfConfirmed: 1,
    strength: 0.8,
    parts: [],
  };
}

const resume: TailoredResume = {
  id: 'rv1',
  postingId: 'jp1',
  version: 1,
  template: 'ats-first',
  createdAt: '2026-11-07T18:00:00Z',
  postingTitle: 'Full-stack developer',
  postingCompany: 'Acme',
  stale: false,
  staleReasons: [],
  score: {
    ...posting.score,
    total: 73,
    factors: [
      {
        key: 'required',
        label: 'Required qualifications',
        weight: 50,
        activeWeight: 50,
        activeWeightIfConfirmed: 50,
        value: 1,
        valueIfConfirmed: 1,
        how: 'Average credit per item.',
      },
      {
        key: 'readability',
        label: 'Readability',
        weight: 15,
        activeWeight: 7.5,
        activeWeightIfConfirmed: 7.5,
        value: 0.875,
        valueIfConfirmed: 0.875,
        how: 'Share of the checks the PDF passes.',
      },
    ],
    formula: '100 x (50 x 1.0000 + 7.5 x 0.8750) / 57.5 = 98.37, rounded to 98.',
    note: 'Our own estimate of how well this resume covers the posting.',
  },
  evidenceTotal: 69,
  evidenceTotalIfConfirmed: 69,
  requirements: [
    requirement('r1', 'React', 'direct'),
    requirement('r2', 'Mentoring', 'hidden'),
    requirement('r3', 'Kubernetes', 'not_shown'),
  ],
  readability: 0.875,
  checks: [
    { key: 'text', label: 'Text can be read', passed: true, detail: '' },
    { key: 'phone', label: 'Phone is on it', passed: false, detail: 'Add a phone number on the profile page.' },
  ],
  lines: [
    {
      kind: 'job',
      sourceId: 'job1',
      label: 'Developer at Acme',
      text: 'Built React screens backed by PostgreSQL',
      gain: 0.6,
      backs: [
        { requirement: 'React', part: 'React', group: 'required', support: 'direct' },
        { requirement: 'React', part: 'React', group: 'required', support: 'direct' },
        { requirement: 'Git', part: 'unit testing', group: 'required', support: 'direct' },
      ],
      reason: '',
    },
    {
      kind: 'job',
      sourceId: 'job1',
      label: 'Developer at Acme',
      text: 'Kept the office printer working',
      gain: 0,
      backs: [],
      reason: '',
    },
  ],
  leftOff: [
    {
      kind: 'project',
      sourceId: 'p1',
      label: 'Team portal',
      text: 'Mentored two junior developers',
      gain: 0.2,
      backs: [{ requirement: 'Mentoring', part: 'Mentoring', group: 'required', support: 'direct' }],
      reason: 'space',
    },
    {
      kind: 'project',
      sourceId: 'p2',
      label: 'Pantry',
      text: 'Lists every shelf with React',
      gain: 0,
      backs: [{ requirement: 'React', part: 'React', group: 'required', support: 'direct' }],
      reason: 'not_needed',
    },
    {
      kind: 'job',
      sourceId: 'job1',
      label: 'Developer at Acme',
      text: 'Ordered the team lunch',
      gain: 0,
      backs: [],
      reason: 'not_needed',
    },
    {
      kind: 'job',
      sourceId: 'job1',
      label: 'Developer at Acme',
      text: 'Older resumes also say Lead Developer',
      gain: 0,
      backs: [],
      reason: 'note',
    },
  ],
  toConfirm: [],
  skills: [
    { name: 'React', category: 'framework', shown: 'React', asked: true, places: 2 },
    { name: 'Postgres', category: 'database', shown: 'PostgreSQL', asked: true, places: 1 },
  ],
  skillsLeftOff: [],
  suggestions: [
    {
      kind: 'space',
      text: 'This line matches the posting but did not fit on one page.',
      line: 'Mentored two junior developers',
      sourceKind: 'project',
      sourceId: 'p1',
    },
  ],
  workOn: [
    {
      requirement: 'Docker and Kubernetes',
      part: 'Kubernetes',
      group: 'required',
      status: 'not_shown',
      reason: 'None of your saved lines show this.',
    },
    {
      requirement: 'Git',
      part: 'Git',
      group: 'required',
      status: 'not_shown',
      reason: 'None of your saved lines show this.',
    },
  ],
  document: { name: 'Ada Lovelace', contact: [], sections: [] },
  plainText: 'Ada Lovelace\nEXPERIENCE',
  pages: 1,
  pdfSha256: 'abc',
  highlights: [
    {
      text: 'Built React screens backed by PostgreSQL',
      why: 'React — React',
      boxes: [{ x: 80, y: 400, width: 400, height: 18 }],
    },
  ],
};

const older: TailoredResumeSummary = { id: 'rv1', version: 1, createdAt: '2026-11-07T18:00:00Z', total: 73 };

const newer: TailoredResumeSummary = { id: 'rv2', version: 2, createdAt: '2026-11-07T19:00:00Z', total: 75 };

describe('TailoredResumePage', () => {
  async function setup() {
    await TestBed.configureTestingModule({
      imports: [TailoredResumePage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: 'jp1' })) } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(TailoredResumePage);
    return { fixture, page: fixture.componentInstance, http: TestBed.inject(HttpTestingController) };
  }

  it('builds the first version and shows its estimate, checks, and why each line is on it', async () => {
    const { fixture, page, http } = await setup();
    http.expectOne('/api/v1/jobs/jp1').flush(posting);
    http.expectOne('/api/v1/jobs/jp1/resumes').flush([]);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('How it is built');
    const build = element.querySelector<HTMLButtonElement>('.toolbar button')!;
    expect(build.textContent?.trim()).toBe('Build the resume');

    build.click();
    http.expectOne({ method: 'POST', url: '/api/v1/jobs/jp1/resumes' }).flush(resume);
    fixture.detectChanges();

    expect(page.versions()).toEqual([older]);
    expect(page.version.value).toBe('rv1');
    const text = element.textContent ?? '';
    expect(text).toContain('73/100');
    expect(text).toContain('Vettyou estimate for this resume');
    expect(text).toContain('All your saved evidence scored 69/100');
    expect(text).toContain('weight 15 × 50% coverage = 7.5');
    expect(text).toContain('1 of 2 passed');
    expect(text).toContain('Phone is on it: failed');
    expect(text).toContain('Add a phone number on the profile page.');
    const backs = Array.from(element.querySelectorAll('.backs li')).map((item) =>
      item.textContent?.replace(/\s+/g, ' ').trim(),
    );
    expect(backs).toEqual(['React (required)', 'Git (required): unit testing']);
    expect(text).toContain("Fills out this job. It doesn't cover anything the posting asks for.");
    expect(text).toContain('Part of Docker and Kubernetes.');
    expect(text).not.toContain('Part of Git.');
    expect(text).toContain('your "Postgres", spelled as the posting does');
    expect(text).toContain("Didn't fit on one page.");
    expect(text).toContain('Already shown by other lines. From');
    expect(text).toContain("Doesn't cover anything this posting asks for. From");
    expect(text).toContain('Reads as a note to yourself, so it is never printed. From');
    expect(text).toContain('Ways to improve it');
    expect(text).toContain('Work on outside the resume');
    expect(text).toContain('In your evidence, not on this resume');
    expect(text).not.toContain('How it is built');

    const download = element.querySelector<HTMLAnchorElement>('.toolbar a[download]')!;
    expect(download.getAttribute('href')).toBe('/api/v1/jobs/jp1/resumes/rv1/resume.pdf');
    const preview = element.querySelector<HTMLImageElement>('.preview img')!;
    expect(preview.getAttribute('src')).toBe('/api/v1/jobs/jp1/resumes/rv1/preview.png');
    expect(preview.alt).toContain('Page 1 of the resume');
    const hit = element.querySelector<HTMLButtonElement>('.hit')!;
    expect(hit.getAttribute('aria-label')).toContain('Built React screens backed by PostgreSQL');
    hit.click();
    fixture.detectChanges();
    expect(element.textContent).toContain('Why this line.');
    expect(element.textContent).toContain('React — React');
    http.verify();
  });

  it('opens the newest version, says when it is out of date, and switches versions', async () => {
    const { fixture, page, http } = await setup();
    http.expectOne('/api/v1/jobs/jp1').flush(posting);
    http.expectOne('/api/v1/jobs/jp1/resumes').flush([newer, older]);
    http.expectOne('/api/v1/jobs/jp1/resumes/rv2').flush({
      ...resume,
      id: 'rv2',
      version: 2,
      stale: true,
      staleReasons: ['Your profile, jobs, projects, education, skills, or answers changed after this version was built.'],
    });
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(page.version.value).toBe('rv2');
    expect(Array.from(element.querySelectorAll('option')).map((option) => option.textContent?.trim())).toEqual([
      expect.stringMatching(/^2 · Nov 7, \d+:00 [AP]M · 75\/100$/),
      expect.stringMatching(/^1 · Nov 7, \d+:00 [AP]M · 73\/100$/),
    ]);
    expect(element.textContent).toContain('This version no longer matches your data.');
    expect(element.textContent).toContain('answers changed after this version was built.');
    expect(element.querySelector('.toolbar button')?.textContent?.trim()).toBe('Build a new version');

    const select = element.querySelector<HTMLSelectElement>('select')!;
    select.value = 'rv1';
    select.dispatchEvent(new Event('change'));
    http.expectOne('/api/v1/jobs/jp1/resumes/rv1').flush(resume);
    fixture.detectChanges();

    expect(page.resume()?.version).toBe(1);
    expect(element.textContent).not.toContain('This version no longer matches your data.');
    http.verify();
  });

  it('waits for the posting to be read and shows why a build failed', async () => {
    const { fixture, http } = await setup();
    http.expectOne('/api/v1/jobs/jp1').flush({ ...posting, status: 'reading' });
    http.expectOne('/api/v1/jobs/jp1/resumes').flush([]);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const build = element.querySelector<HTMLButtonElement>('.toolbar button')!;
    expect(build.disabled).toBe(true);
    expect(element.textContent).toContain('The posting has to finish reading');

    fixture.componentInstance.posting.set(posting);
    fixture.detectChanges();
    build.click();
    http
      .expectOne({ method: 'POST', url: '/api/v1/jobs/jp1/resumes' })
      .flush({ message: 'This posting has not been read yet.' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('This posting has not been read yet.');
    expect(build.disabled).toBe(false);
    http.verify();
  });

  it('says when the posting does not exist', async () => {
    const { fixture, http } = await setup();
    const versions = http.expectOne('/api/v1/jobs/jp1/resumes');
    http.expectOne('/api/v1/jobs/jp1').flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    expect(versions.cancelled).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('That posting was not found.');
  });
});
