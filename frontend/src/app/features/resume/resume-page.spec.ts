import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ResumePage } from './resume-page';

const PROFILE = {
  displayName: 'Ada Lovelace',
  email: 'ada@example.com',
  phone: '',
  location: '',
  website: '',
  linkedin: '',
};

function flushLibrary(http: HttpTestingController): void {
  http.expectOne('/api/v1/experiences').flush([]);
  http.expectOne('/api/v1/educations').flush([]);
  http.expectOne('/api/v1/projects').flush([]);
  http.expectOne('/api/v1/skills').flush([]);
  http.expectOne('/api/v1/jobs').flush([]);
}

describe('ResumePage', () => {
  async function setup() {
    await TestBed.configureTestingModule({
      imports: [ResumePage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(ResumePage);
    const http = TestBed.inject(HttpTestingController);
    return { fixture, http };
  }

  it('sends a pasted resume and shows what was saved', async () => {
    const { fixture, http } = await setup();
    http.expectOne('/api/v1/profile').flush(PROFILE);
    flushLibrary(http);

    fixture.componentInstance.paste.setValue('Jane Estrada\n\nEXPERIENCE\n\nEngineer\nAcme\n');
    fixture.componentInstance.pasteResume();

    const request = http.expectOne('/api/v1/resumes/import');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      fileName: 'pasted-resume.txt',
      body: 'Jane Estrada\n\nEXPERIENCE\n\nEngineer\nAcme\n',
      encoding: 'text',
    });
    request.flush({
      profile: { displayName: 'Jane Estrada', email: '', phone: '', location: '', website: '', linkedin: '' },
      jobs: [{ id: 'j1', label: 'Engineer at Acme' }],
      projects: [],
      schools: [],
      skipped: [],
    });
    http.expectOne('/api/v1/profile').flush(PROFILE);
    flushLibrary(http);
    await fixture.whenStable();

    expect(fixture.componentInstance.result()?.jobs.map((job) => job.label)).toEqual(['Engineer at Acme']);
    http.verify();
  });

  it('shows a job line on the page as it is typed, and saves that write-up', async () => {
    const { fixture, http } = await setup();
    http.expectOne('/api/v1/profile').flush(PROFILE);
    http.expectOne('/api/v1/experiences').flush([
      { id: 'j1', employer: 'Acme', title: 'Engineer', startDate: '2020-01-01', endDate: null, current: true },
    ]);
    http.expectOne('/api/v1/educations').flush([]);
    http.expectOne('/api/v1/projects').flush([]);
    http.expectOne('/api/v1/skills').flush([]);
    http.expectOne('/api/v1/jobs').flush([]);
    http.expectOne('/api/v1/experiences/j1').flush({
      id: 'j1',
      employer: 'Acme',
      title: 'Engineer',
      location: 'Remote',
      startDate: '2020-01-01',
      endDate: null,
      current: true,
      body: 'Shipped the billing screen',
      versions: [],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    const sheet = () => fixture.nativeElement.querySelector('.sheet') as HTMLElement;
    expect(sheet().textContent).toContain('Shipped the billing screen');

    fixture.componentInstance.bodyControl('j1').setValue('Shipped the billing screen\nAdded audit logs');
    fixture.detectChanges();
    expect(sheet().textContent).toContain('Added audit logs');
    expect(fixture.componentInstance.dirty()).toBe(true);

    fixture.componentInstance.save();
    const put = http.expectOne('/api/v1/experiences/j1');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body.body).toContain('Added audit logs');
    put.flush({
      id: 'j1',
      employer: 'Acme',
      title: 'Engineer',
      location: 'Remote',
      startDate: '2020-01-01',
      endDate: null,
      current: true,
      body: 'Shipped the billing screen\nAdded audit logs',
      versions: [],
    });
    await fixture.whenStable();
    expect(fixture.componentInstance.dirty()).toBe(false);
    http.verify();
  });
});
