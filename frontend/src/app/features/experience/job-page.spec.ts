import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { Experience } from '../../core/api/profile-api';
import { JobPage } from './job-page';

const job: Experience = {
  id: 'j1',
  employer: 'UPS',
  title: 'Operations Supervisor',
  location: 'Irvine, CA',
  startDate: '2014-06-01',
  endDate: '2022-06-01',
  current: false,
  body: '- Led a team of 30\n- Cut missed scans by a third\n',
  versions: [{ number: 1, body: '- Led a team of 30', createdAt: '' }],
};

describe('JobPage', () => {
  async function open() {
    await TestBed.configureTestingModule({
      imports: [JobPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: 'j1' })) } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(JobPage);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/experiences/j1').flush(job);
    await fixture.whenStable();
    return { fixture, http, page: fixture.componentInstance };
  }

  it('shows every line as a bullet with the date range', async () => {
    const { fixture, http, page } = await open();

    expect(page.bullets()).toEqual(['Led a team of 30', 'Cut missed scans by a third']);
    expect(page.when()).toBe('Jun 2014 – Jun 2022');
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Operations Supervisor');
    expect(text).toContain('Cut missed scans by a third');
    http.verify();
  });

  it('saves edits and refreshes the sidebar', async () => {
    const { http, page } = await open();

    page.form.controls.current.setValue(true);
    page.submit();
    const save = http.expectOne('/api/v1/experiences/j1');
    expect(save.request.method).toBe('PUT');
    expect(save.request.body).toEqual(expect.objectContaining({ current: true, endDate: null }));
    save.flush({ ...job, current: true, endDate: null });
    http.expectOne('/api/v1/experiences').flush([]);

    expect(page.saved()).toBe(true);
    expect(page.when()).toBe('Jun 2014 – Present');
    http.verify();
  });

  it('deletes only after confirming, then goes back to the list', async () => {
    const { fixture, http, page } = await open();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    page.confirming.set(true);
    await fixture.whenStable();
    page.remove();
    const remove = http.expectOne('/api/v1/experiences/j1');
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
    http.expectOne('/api/v1/experiences').flush([]);

    expect(navigate).toHaveBeenCalledWith(['/experience']);
    http.verify();
  });
});
