import { inject, Injectable, signal } from '@angular/core';

import { ExperienceSummary, ProfileApi } from './profile-api';

@Injectable({ providedIn: 'root' })
export class ExperienceList {
  private readonly api = inject(ProfileApi);

  readonly items = signal<ExperienceSummary[]>([]);

  refresh(): void {
    this.api.experiences().subscribe({
      next: (jobs) => this.items.set(jobs),
      error: () => this.items.set([]),
    });
  }

  clear(): void {
    this.items.set([]);
  }
}
