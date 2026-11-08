import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Profile } from './profile-api';

export interface ResumeAdded {
  id: string;
  label: string;
}

export interface ResumeImport {
  profile: Profile;
  jobs: ResumeAdded[];
  projects: ResumeAdded[];
  schools: ResumeAdded[];
  skipped: string[];
}

@Injectable({ providedIn: 'root' })
export class ResumeApi {
  private readonly http = inject(HttpClient);

  importResume(fileName: string, body: string, encoding: 'text' | 'base64'): Observable<ResumeImport> {
    return this.http.post<ResumeImport>('/api/v1/resumes/import', { fileName, body, encoding });
  }
}
