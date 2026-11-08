import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { Claim, Project } from '../../core/api/project-api';
import { ProjectPage } from './project-page';

const claim: Claim = {
  id: 'c1',
  kind: 'skill',
  name: 'Angular',
  category: 'framework',
  quote: 'Built with Angular.',
  lineNumber: 2,
  sourceVersionId: 's1',
  planned: false,
  status: 'candidate',
};

const project: Project = {
  id: 'p1',
  name: 'Pantry',
  summary: '',
  repoUrl: '',
  liveUrl: '',
  sources: [],
  claims: [],
};

describe('ProjectPage', () => {
  it('sends the pasted README and records a decision', async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: 'p1' })) } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ProjectPage);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/projects/p1').flush(project);

    const page = fixture.componentInstance;
    page.readme.setValue({ fileName: 'README.md', body: '# Pantry\nBuilt with Angular.' });
    page.uploadReadme();

    const upload = http.expectOne('/api/v1/projects/p1/sources');
    expect(upload.request.method).toBe('POST');
    expect(upload.request.body).toEqual({ fileName: 'README.md', body: '# Pantry\nBuilt with Angular.' });
    upload.flush({
      ...project,
      sources: [{ id: 's1', versionNumber: 1, fileName: 'README.md', body: '# Pantry\nBuilt with Angular.', createdAt: '' }],
      claims: [
        claim,
        { ...claim, id: 'f2', kind: 'feature', name: 'Expiry alerts', category: 'Features', lineNumber: 6 },
        { ...claim, id: 'f1', kind: 'feature', name: 'Barcode scanning', category: 'Features', lineNumber: 5 },
        { ...claim, id: 'f3', kind: 'feature', name: 'npm install', category: 'Setup', lineNumber: 9 },
      ],
    });
    http.expectOne('/api/v1/projects').flush([]);
    await fixture.whenStable();

    expect(page.bulletSections()).toEqual([
      { section: 'Features', items: [expect.objectContaining({ id: 'f1' }), expect.objectContaining({ id: 'f2' })] },
      { section: 'Setup', items: [expect.objectContaining({ id: 'f3' })] },
    ]);
    expect(page.stack().review.map((item) => item.name)).toEqual(['Angular']);
    expect(page.versionOf(claim)).toBe(1);

    page.decide(claim, 'rejected');
    const decision = http.expectOne('/api/v1/evidence/c1/decision');
    expect(decision.request.body).toEqual({ decision: 'rejected' });
    decision.flush({ ...claim, status: 'rejected' });
    await fixture.whenStable();

    expect(page.stack().review).toEqual([]);
    expect(page.notMine().map((item) => item.name)).toEqual(['Angular']);
    expect(page.stack().notMine.length).toBe(1);
    http.verify();
  });

  it('deletes the project and goes back to the list', async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: 'p1' })) } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ProjectPage);
    const http = TestBed.inject(HttpTestingController);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    http.expectOne('/api/v1/projects/p1').flush(project);

    fixture.componentInstance.remove();
    const remove = http.expectOne('/api/v1/projects/p1');
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
    http.expectOne('/api/v1/projects').flush([]);

    expect(navigate).toHaveBeenCalledWith(['/projects']);
    http.verify();
  });
});
