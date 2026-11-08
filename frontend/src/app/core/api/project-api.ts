import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface ProjectSummary {
  id: string;
  name: string;
  summary: string;
}

export interface ProjectDetails {
  summary: string;
  repoUrl: string;
  liveUrl: string;
}

export interface ProjectSource {
  id: string;
  versionNumber: number;
  fileName: string;
  body: string;
  createdAt: string;
}

export type ClaimStatus = 'candidate' | 'approved' | 'rejected';

export interface Claim {
  id: string;
  kind: 'skill' | 'feature';
  name: string;
  category: string;
  quote: string;
  lineNumber: number;
  sourceVersionId: string;
  planned: boolean;
  status: ClaimStatus;
}

export interface Project extends ProjectDetails {
  id: string;
  name: string;
  sources: ProjectSource[];
  claims: Claim[];
}

@Injectable({ providedIn: 'root' })
export class ProjectApi {
  private readonly http = inject(HttpClient);

  list(): Observable<ProjectSummary[]> {
    return this.http.get<ProjectSummary[]>('/api/v1/projects');
  }

  get(id: string): Observable<Project> {
    return this.http.get<Project>(`/api/v1/projects/${id}`);
  }

  save(id: string, body: ProjectDetails): Observable<Project> {
    return this.http.put<Project>(`/api/v1/projects/${id}`, body);
  }

  importReadme(fileName: string, body: string): Observable<Project> {
    return this.http.post<Project>('/api/v1/projects/import', { fileName, body });
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/projects/${id}`);
  }

  reread(id: string): Observable<Project> {
    return this.http.post<Project>(`/api/v1/projects/${id}/reread`, {});
  }

  addReadme(id: string, fileName: string, body: string): Observable<Project> {
    return this.http.post<Project>(`/api/v1/projects/${id}/sources`, { fileName, body });
  }

  decide(claimId: string, decision: 'approved' | 'rejected'): Observable<Claim> {
    return this.http.post<Claim>(`/api/v1/evidence/${claimId}/decision`, { decision });
  }
}
