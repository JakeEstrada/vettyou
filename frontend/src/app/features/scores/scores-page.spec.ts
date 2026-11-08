import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { TailoredResume } from '../../core/api/tailored-resume-api';
import { ScoresPage } from './scores-page';

describe('ScoresPage', () => {
  it('lists the latest estimate and deletes the match', async () => {
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

    expect(fixture.nativeElement.textContent).toContain('Vettyou estimate 83/100');

    fixture.componentInstance.remove('jp1');
    const deleted = http.expectOne((request) => request.method === 'DELETE' && request.url === '/api/v1/jobs/jp1');
    deleted.flush(null);
    await fixture.whenStable();
    expect(fixture.componentInstance.rows()).toEqual([]);
    http.verify();
  });

  it('marks the line that explains the estimate', async () => {
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
    const detail = http.expectOne('/api/v1/jobs/jp1/resumes/r1');
    detail.flush({
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

    expect(fixture.nativeElement.textContent).toContain('Keep React on the project page.');
    const marked = fixture.nativeElement.querySelector('.hit button') as HTMLButtonElement;
    expect(marked.textContent).toContain('Built React screens');
    marked.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('React — React');
    http.verify();
  });
});
