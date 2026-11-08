import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { SkillItem } from '../../core/api/skills-api';
import { SkillsPage } from './skills-page';

const react: SkillItem = {
  name: 'React',
  category: 'framework',
  status: 'kept',
  places: 2,
  uses: [
    { kind: 'project', id: 'p1', label: 'PunchTrack', quote: 'React, Node.js, PostgreSQL', status: 'approved' },
    { kind: 'job', id: 'j1', label: 'Freelance Web Developer at Liminal', quote: 'Built React sites', status: 'written' },
  ],
};

const docker: SkillItem = {
  name: 'Docker',
  category: 'platform',
  status: 'review',
  places: 0,
  uses: [{ kind: 'project', id: 'p1', label: 'PunchTrack', quote: 'Runs on Docker', status: 'candidate' }],
};

describe('SkillsPage', () => {
  it('groups kept skills and keeps one that needs checking', async () => {
    await TestBed.configureTestingModule({
      imports: [SkillsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(SkillsPage);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/skills').flush([react, docker]);
    await fixture.whenStable();

    const page = fixture.componentInstance;
    expect(page.groups()).toEqual([{ category: 'framework', label: 'Frameworks and libraries', items: [react] }]);
    expect(page.toCheck().map((skill) => skill.name)).toEqual(['Docker']);
    expect(page.projectCount(docker)).toBe(1);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Built React sites');

    page.decide(docker, 'approved');
    const decision = http.expectOne('/api/v1/skills/decision');
    expect(decision.request.body).toEqual({ name: 'Docker', decision: 'approved' });
    decision.flush([react, { ...docker, status: 'kept', places: 1 }]);

    expect(page.toCheck()).toEqual([]);
    expect(page.kept().map((skill) => skill.name)).toEqual(['React', 'Docker']);
    expect(page.busy()).toBe(false);
    http.verify();
  });
});
