import { inject, Injectable, signal } from '@angular/core';

import { Education, EducationApi } from './education-api';

@Injectable({ providedIn: 'root' })
export class EducationList {
  private readonly api = inject(EducationApi);

  readonly items = signal<Education[]>([]);

  refresh(): void {
    this.api.list().subscribe({
      next: (schools) => this.items.set(schools),
      error: () => this.items.set([]),
    });
  }

  clear(): void {
    this.items.set([]);
  }
}
