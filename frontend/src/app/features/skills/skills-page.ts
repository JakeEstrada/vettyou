import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { concat, last } from 'rxjs';

import { SkillItem, SkillsApi, useLink } from '../../core/api/skills-api';
import { messageOf } from '../../core/auth/session';

export interface SkillGroup {
  category: string;
  label: string;
  items: SkillItem[];
}

const CATEGORY_LABELS: Record<string, string> = {
  language: 'Languages',
  framework: 'Frameworks and libraries',
  database: 'Databases',
  platform: 'Platforms and services',
  tool: 'Tools',
  practice: 'Practices',
};

@Component({
  selector: 'app-skills-page',
  imports: [RouterLink],
  templateUrl: './skills-page.html',
  styleUrl: './skills-page.scss',
})
export class SkillsPage {
  private readonly api = inject(SkillsApi);

  readonly skills = signal<SkillItem[]>([]);

  readonly loading = signal(true);

  readonly missing = signal(false);

  readonly error = signal('');

  readonly busy = signal(false);

  readonly link = useLink;

  readonly toCheck = computed(() => this.skills().filter((skill) => skill.status === 'review'));

  readonly planned = computed(() => this.skills().filter((skill) => skill.status === 'planned'));

  readonly notMine = computed(() => this.skills().filter((skill) => skill.status === 'rejected'));

  readonly kept = computed(() => this.skills().filter((skill) => skill.status === 'kept'));

  readonly groups = computed<SkillGroup[]>(() => {
    const groups = new Map<string, SkillItem[]>();
    for (const skill of this.kept()) {
      groups.set(skill.category, [...(groups.get(skill.category) ?? []), skill]);
    }
    return Object.keys(CATEGORY_LABELS)
      .filter((category) => groups.has(category))
      .concat([...groups.keys()].filter((category) => !(category in CATEGORY_LABELS)))
      .map((category) => ({ category, label: CATEGORY_LABELS[category] ?? category, items: groups.get(category)! }));
  });

  constructor() {
    this.api.list().subscribe({
      next: (skills) => {
        this.skills.set(skills);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.missing.set(true);
        } else {
          this.error.set(messageOf(error));
        }
      },
    });
  }

  decide(skill: SkillItem, decision: 'approved' | 'rejected'): void {
    this.run([skill], decision);
  }

  keepAll(): void {
    this.run(this.toCheck(), 'approved');
  }

  projectCount(skill: SkillItem): number {
    return new Set(skill.uses.filter((use) => use.kind === 'project').map((use) => use.id)).size;
  }

  private run(skills: SkillItem[], decision: 'approved' | 'rejected'): void {
    if (skills.length === 0) {
      return;
    }
    this.busy.set(true);
    this.error.set('');
    concat(...skills.map((skill) => this.api.decide(skill.name, decision)))
      .pipe(last())
      .subscribe({
        next: (updated) => this.skills.set(updated),
        error: (error: unknown) => {
          this.busy.set(false);
          this.error.set(messageOf(error));
        },
        complete: () => this.busy.set(false),
      });
  }
}
