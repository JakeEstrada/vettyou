import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { JobReportPage } from './job-report-page';

describe('JobReportPage', () => {
  async function setup() {
    await TestBed.configureTestingModule({
      imports: [JobReportPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: 'jp1' })) } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(JobReportPage);
    return { fixture, http: TestBed.inject(HttpTestingController) };
  }

  it('shows the report text with PDF and text links, and copies it', async () => {
    const { fixture, http } = await setup();
    http.expectOne('/api/v1/jobs/jp1/report.txt').flush('VETTYOU JOB MATCH REPORT\nFormula: 100 x (50 x 1.000) / 50');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('pre')?.textContent).toContain('Formula: 100 x');
    const links = Array.from(element.querySelectorAll('.actions a')).map((link) => link.getAttribute('href'));
    expect(links).toEqual(['/api/v1/jobs/jp1/report.pdf', '/api/v1/jobs/jp1/report.pdf', '/api/v1/jobs/jp1/report.txt']);

    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
    fixture.componentInstance.copy();
    await Promise.resolve();
    expect(writeText).toHaveBeenCalledWith('VETTYOU JOB MATCH REPORT\nFormula: 100 x (50 x 1.000) / 50');
    expect(fixture.componentInstance.copied()).toContain('Copied');
    http.verify();
  });

  it('explains when the posting is not read yet', async () => {
    const { fixture, http } = await setup();
    http.expectOne('/api/v1/jobs/jp1/report.txt').flush('', { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toContain('not been read yet');
  });
});
