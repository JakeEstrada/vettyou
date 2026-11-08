import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface SkillUse {
  kind: 'project' | 'job' | 'school';
  id: string;
  label: string;
  quote: string;
  status: string;
}

export type SkillStatus = 'kept' | 'review' | 'planned' | 'rejected';

export interface SkillItem {
  name: string;
  category: string;
  status: SkillStatus;
  places: number;
  uses: SkillUse[];
}

@Injectable({ providedIn: 'root' })
export class SkillsApi {
  private readonly http = inject(HttpClient);

  list(): Observable<SkillItem[]> {
    return this.http.get<SkillItem[]>('/api/v1/skills');
  }

  decide(name: string, decision: 'approved' | 'rejected'): Observable<SkillItem[]> {
    return this.http.post<SkillItem[]>('/api/v1/skills/decision', { name, decision });
  }
}

/** Where a piece of evidence lives in the app. A resume's skills section comes from the skills page. */
export function useLink(use: { kind: string; id: string }): string[] {
  switch (use.kind) {
    case 'project':
      return ['/projects', use.id];
    case 'job':
      return ['/experience', use.id];
    case 'school':
      return ['/education', use.id];
    default:
      return ['/skills'];
  }
}
