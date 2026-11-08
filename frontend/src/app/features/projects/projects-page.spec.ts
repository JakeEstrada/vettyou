import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { ProjectsPage } from './projects-page';

describe('ProjectsPage', () => {
  it('creates a project from a dropped README and opens it', async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(ProjectsPage);
    const http = TestBed.inject(HttpTestingController);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    http.expectOne('/api/v1/projects').flush([]);

    fixture.componentInstance.importReadme({ fileName: 'README.md', body: '# Pantry\n- Barcode scanning' });

    const request = http.expectOne('/api/v1/projects/import');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ fileName: 'README.md', body: '# Pantry\n- Barcode scanning' });
    request.flush({ id: 'p1', name: 'Pantry', summary: '', repoUrl: '', liveUrl: '', sources: [], claims: [] });
    http.expectOne('/api/v1/projects').flush([{ id: 'p1', name: 'Pantry', summary: '' }]);

    expect(navigate).toHaveBeenCalledWith(['/projects', 'p1']);
    http.verify();
  });
});
