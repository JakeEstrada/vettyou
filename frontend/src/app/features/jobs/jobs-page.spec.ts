import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { JobsPage } from './jobs-page';

describe('JobsPage', () => {
  it('sends the pasted posting and opens its match page', async () => {
    await TestBed.configureTestingModule({
      imports: [JobsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(JobsPage);
    const http = TestBed.inject(HttpTestingController);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    http.expectOne('/api/v1/jobs').flush([]);
    await fixture.whenStable();

    const page = fixture.componentInstance;
    page.submit();
    expect(page.form.controls.body.touched).toBe(true);
    http.expectNone((request) => request.method === 'POST');

    page.form.controls.body.setValue('Frontend Engineer\nRequirements: React');
    page.submit();
    const create = http.expectOne((request) => request.method === 'POST' && request.url === '/api/v1/jobs');
    expect(create.request.body).toEqual({ body: 'Frontend Engineer\nRequirements: React' });
    create.flush({ id: 'jp1', title: '', company: '', status: 'reading', createdAt: '' });

    expect(navigate).toHaveBeenCalledWith(['/jobs', 'jp1']);
    http.verify();
  });

  it('deletes an earlier posting', async () => {
    await TestBed.configureTestingModule({
      imports: [JobsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(JobsPage);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/jobs').flush([
      { id: 'jp1', title: 'Engineer', company: 'Acme', status: 'ready', createdAt: '' },
    ]);
    await fixture.whenStable();

    fixture.componentInstance.remove('jp1');
    const deleted = http.expectOne((request) => request.method === 'DELETE' && request.url === '/api/v1/jobs/jp1');
    deleted.flush(null);
    await fixture.whenStable();
    expect(fixture.componentInstance.postings()).toEqual([]);
    http.verify();
  });
});
