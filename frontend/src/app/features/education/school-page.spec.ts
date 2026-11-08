import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { Education } from '../../core/api/education-api';
import { SchoolPage } from './school-page';

const schools: Education[] = [
  {
    id: 'e1',
    institution: 'California State University, Fullerton',
    credential: 'B.S.',
    fieldOfStudy: 'Computer Science',
    startDate: null,
    endDate: '2026-05-01',
  },
  {
    id: 'e2',
    institution: 'Saddleback College',
    credential: 'A.A.',
    fieldOfStudy: 'Mathematics',
    startDate: null,
    endDate: '2023-12-01',
  },
];

describe('SchoolPage', () => {
  async function open(id: string) {
    await TestBed.configureTestingModule({
      imports: [SchoolPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id })) } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(SchoolPage);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/educations').flush(schools);
    await fixture.whenStable();
    return { fixture, http, page: fixture.componentInstance };
  }

  it('shows the school picked in the sidebar', async () => {
    const { fixture, http, page } = await open('e2');

    expect(page.degree()).toBe('A.A., Mathematics');
    expect(page.when()).toBe('Dec 2023');
    expect((fixture.nativeElement as HTMLElement).querySelector('h1')?.textContent).toContain('Saddleback College');
    http.verify();
  });

  it('says so when the school is not in the list', async () => {
    const { http, page } = await open('missing');

    expect(page.loadError()).toBe('That school was not found.');
    http.verify();
  });

  it('deletes after confirming and refreshes the sidebar', async () => {
    const { http, page } = await open('e1');
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    page.remove();
    const remove = http.expectOne('/api/v1/educations/e1');
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
    http.expectOne('/api/v1/educations').flush([schools[1]]);

    expect(navigate).toHaveBeenCalledWith(['/education']);
    http.verify();
  });
});
