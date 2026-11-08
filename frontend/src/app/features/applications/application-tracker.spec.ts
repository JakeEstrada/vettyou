import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ApplicationTracker } from './application-tracker';

describe('ApplicationTracker', () => {
  it('names the job from the posting, stores company and pay, and does not take a status from a dropdown', async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationTracker],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(ApplicationTracker);
    fixture.componentRef.setInput('items', [
      {
        id: 'a1',
        postingId: 'p0',
        title: 'Untitled posting',
        company: 'Ensign Services, Inc.',
        location: '',
        pay: '',
        status: 'draft',
        appliedAt: '2026-01-01T00:00:00Z',
        notes: '',
        createdAt: '2026-01-01T00:00:00Z',
        estimate: null,
        events: [],
      },
    ]);
    fixture.componentRef.setInput('openings', [
      {
        postingId: 'p1',
        title: 'Full Stack Software Engineer (Junior)',
        company: 'Brightline Logistics',
        pay: '$120,000',
        preview: 'Build React screens for logistics operations.',
        createdAt: '2026-11-07T00:00:00Z',
        total: 83,
      },
      {
        postingId: 'p0',
        title: 'Registered Nurse - Medical/Surgical Unit',
        company: 'Ensign Services, Inc.',
        pay: '$38.00 - $45.00 per hour',
        preview: 'We are looking for a nurse to join a medical-surgical unit.',
        createdAt: '2026-01-01T00:00:00Z',
        total: 2,
      },
    ]);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Registered Nurse - Medical/Surgical Unit');
    expect(text).not.toContain('Untitled posting');
    expect(text).toContain('Ensign Services, Inc.');
    expect(text).toContain('$38.00 - $45.00 per hour');
    expect(text).toContain('Brightline Logistics');
    expect(text).toContain('$120,000');
    expect(text).toContain('Score');
    expect(text).toContain('83/100');
    expect(text).toContain('Vettyou estimate');
    expect(text).toContain('Waiting on email');
    expect(text).toContain('not an employer ATS score');
    expect(fixture.nativeElement.querySelector('select')).toBeNull();
    expect(fixture.nativeElement.querySelector('form')).toBeNull();

    const http = TestBed.inject(HttpTestingController);
    fixture.nativeElement.querySelector('button.open:not(.score)').click();
    const posting = http.expectOne('/api/v1/jobs/p1');
    posting.flush({
      id: 'p1',
      title: 'Full Stack Software Engineer (Junior)',
      company: 'Brightline Logistics',
      pay: '$120,000',
      preview: 'Build React screens for logistics operations.',
      status: 'ready',
      createdAt: '2026-11-07T00:00:00Z',
      reader: 'rules',
      matcher: 'rules',
      matchedAt: null,
      evidenceChanged: false,
      error: '',
      body: '# Full Stack Software Engineer (Junior)\n\nBuild React screens for logistics operations.\n\nRequirements:\n- React',
      requirements: [],
      score: {
        total: 83,
        unrounded: 83,
        totalIfConfirmed: 83,
        unroundedIfConfirmed: 83,
        factors: [
          {
            key: 'required',
            label: 'Required qualifications',
            weight: 50,
            activeWeight: 50,
            activeWeightIfConfirmed: 50,
            value: 1,
            valueIfConfirmed: 1,
            how: 'All required items are shown.',
          },
        ],
        formula: 'rounded to 83.',
        formulaIfConfirmed: 'rounded to 83.',
        toConfirm: 0,
        toAnswer: 0,
        readiness: { value: 1, checks: [] },
        label: 'Vettyou estimate',
        note: 'Not an employer ATS score.',
      },
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Build React screens for logistics operations.');
    expect(fixture.nativeElement.querySelector('.posting h3')?.textContent).toContain(
      'Full Stack Software Engineer (Junior)',
    );

    const score = fixture.nativeElement.querySelector('a.score') as HTMLAnchorElement;
    expect(score.getAttribute('href')).toBe('/jobs/p1');
    expect(score.textContent).toContain('83/100');

    fixture.componentInstance.track({
      postingId: 'p1',
      title: 'Full Stack Software Engineer (Junior)',
      company: 'Brightline Logistics',
      pay: '$120,000',
      preview: 'Build React screens for logistics operations.',
      createdAt: '2026-11-07T00:00:00Z',
      total: 83,
    });
    const created = http.expectOne('/api/v1/applications');
    expect(created.request.body.title).toBe('Full Stack Software Engineer (Junior)');
    expect(created.request.body.company).toBe('Brightline Logistics');
    expect(created.request.body.pay).toBe('$120,000');
    expect(created.request.body.postingId).toBe('p1');
    created.flush({
      id: 'a2',
      postingId: 'p1',
      title: 'Full Stack Software Engineer (Junior)',
      company: 'Brightline Logistics',
      location: '',
      pay: '$120,000',
      status: 'draft',
      appliedAt: null,
      notes: '',
      createdAt: '2026-11-08T00:00:00Z',
      estimate: 83,
      events: [],
    });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.componentInstance.rows()[0].title).toBe('Full Stack Software Engineer (Junior)');
    expect(fixture.componentInstance.rows()[0].pay).toBe('$120,000');
    http.verify();
  });

  it('imports a history file as tracked jobs with pay and posting', async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationTracker],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(ApplicationTracker);
    fixture.componentRef.setInput('items', []);
    fixture.componentRef.setInput('openings', []);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Import job history');
    expect(fixture.nativeElement.textContent).toContain('does not change your profile');

    const file = new File(
      [
        JSON.stringify({
          jobs: [
            {
              company: 'L3Harris Technologies',
              job_title: 'Specialist Software Engineer',
              salary_min: 97000,
              salary_max: 180000,
              salary_unit: 'year',
            },
          ],
        }),
      ],
      'job_postings_history.json',
      { type: 'application/json' },
    );
    await fixture.componentInstance.importFile({
      target: { files: [file], value: '' },
    } as unknown as Event);
    await fixture.whenStable();

    const http = TestBed.inject(HttpTestingController);
    const sent = http.expectOne('/api/v1/applications/history');
    expect(sent.request.body.jobs[0].jobTitle).toBe('Specialist Software Engineer');
    expect(sent.request.body.jobs[0].salaryMin).toBe(97000);
    sent.flush({
      imported: 1,
      skipped: 0,
      applications: [
        {
          id: 'h1',
          postingId: 'hp1',
          title: 'Specialist Software Engineer',
          company: 'L3Harris Technologies',
          location: 'Anaheim, CA',
          pay: '$97,000 – $180,000',
          status: 'draft',
          appliedAt: null,
          notes: 'Saved from history.',
          createdAt: '2026-11-08T00:00:00Z',
          estimate: null,
          events: [],
        },
      ],
    });
    await fixture.whenStable();
    fixture.detectChanges();
    http.expectOne('/api/v1/jobs/hp1').flush({
      id: 'hp1',
      title: 'Specialist Software Engineer',
      company: 'L3Harris Technologies',
      pay: '$97,000 – $180,000',
      preview: 'Preferred additional skills.',
      status: 'ready',
      createdAt: '2026-11-08T00:00:00Z',
      reader: 'history',
      matcher: '',
      matchedAt: null,
      evidenceChanged: false,
      error: '',
      body: '# Specialist Software Engineer\n\nPreferred additional skills.',
      requirements: [],
      score: {
        total: null,
        unrounded: null,
        totalIfConfirmed: null,
        unroundedIfConfirmed: null,
        factors: [],
        formula: '',
        formulaIfConfirmed: '',
        toConfirm: 0,
        toAnswer: 0,
        readiness: { value: 0, checks: [] },
        label: 'Vettyou estimate',
        note: 'Not an employer ATS score.',
      },
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Specialist Software Engineer');
    expect(fixture.nativeElement.textContent).toContain('L3Harris Technologies');
    expect(fixture.nativeElement.textContent).toContain('$97,000 – $180,000');
    expect(fixture.nativeElement.textContent).toContain('Imported 1 job.');
    expect(fixture.nativeElement.textContent).toContain('Waiting on email');
    http.verify();
  });
});
