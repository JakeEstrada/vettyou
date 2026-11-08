import { inject, Injectable, signal } from '@angular/core';

import { ProjectApi, ProjectSummary } from './project-api';

@Injectable({ providedIn: 'root' })
export class ProjectList {
  private readonly api = inject(ProjectApi);

  readonly items = signal<ProjectSummary[]>([]);

  refresh(): void {
    this.api.list().subscribe({
      next: (projects) => this.items.set(projects),
      error: () => this.items.set([]),
    });
  }

  clear(): void {
    this.items.set([]);
  }
}
