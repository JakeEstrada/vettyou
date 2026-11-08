import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { HomePage } from './home-page';

describe('HomePage', () => {
  it('shows the name, job title, a kept skill, and marks a typed keyword', async () => {
    await TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(HomePage);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/profile').flush({
      displayName: 'Jake Estrada',
      email: '',
      phone: '',
      location: '',
      website: '',
      linkedin: '',
    });
    http.expectOne('/api/v1/experiences').flush([
      { id: 'j1', employer: 'Acme', title: 'Full Stack Engineer', startDate: null, endDate: null, current: true },
    ]);
    http.expectOne('/api/v1/educations').flush([]);
    http.expectOne('/api/v1/projects').flush([]);
    http.expectOne('/api/v1/skills').flush([
      {
        name: 'React',
        category: 'framework',
        status: 'kept',
        places: 2,
        uses: [{ kind: 'job', id: 'j1', label: 'Acme', quote: 'Built React screens', status: 'kept' }],
      },
    ]);
    http.expectOne('/api/v1/jobs').flush([]);
    http.expectOne('/api/v1/applications').flush([]);
    http.expectOne('/api/v1/experiences/j1').flush({
      id: 'j1',
      employer: 'Acme',
      title: 'Full Stack Engineer',
      location: '',
      startDate: null,
      endDate: null,
      current: true,
      body: 'Built React screens',
      versions: [],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Jake Estrada');
    expect(text).toContain('Full Stack Engineer');
    expect(text).toContain('React');
    expect(text).toContain('Acme');

    fixture.componentInstance.keywords.setValue('React, Kubernetes');
    fixture.detectChanges();
    expect(fixture.componentInstance.missingWords()).toEqual(['Kubernetes']);
    expect(fixture.nativeElement.querySelector('mark')?.textContent).toBe('React');
    http.verify();
  });
});
