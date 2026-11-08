import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface Profile {
  displayName: string;
  /** The address the person applies with, printed on resumes. It can differ from the login. */
  email: string;
  phone: string;
  location: string;
  website: string;
  linkedin: string;
}

export interface ExperienceSummary {
  id: string;
  employer: string;
  title: string;
  startDate: string | null;
  endDate: string | null;
  current: boolean;
}

export interface ExperienceVersion {
  number: number;
  body: string;
  createdAt: string;
}

export interface Experience extends ExperienceSummary {
  location: string;
  body: string;
  versions: ExperienceVersion[];
}

export interface ExperienceInput {
  employer: string;
  title: string;
  location: string;
  startDate: string | null;
  endDate: string | null;
  current: boolean;
  body: string;
}

@Injectable({ providedIn: 'root' })
export class ProfileApi {
  private readonly http = inject(HttpClient);

  get(): Observable<Profile> {
    return this.http.get<Profile>('/api/v1/profile');
  }

  save(body: Profile): Observable<Profile> {
    return this.http.put<Profile>('/api/v1/profile', body);
  }

  experiences(): Observable<ExperienceSummary[]> {
    return this.http.get<ExperienceSummary[]>('/api/v1/experiences');
  }

  experience(id: string): Observable<Experience> {
    return this.http.get<Experience>(`/api/v1/experiences/${id}`);
  }

  createExperience(body: ExperienceInput): Observable<Experience> {
    return this.http.post<Experience>('/api/v1/experiences', body);
  }

  saveExperience(id: string, body: ExperienceInput): Observable<Experience> {
    return this.http.put<Experience>(`/api/v1/experiences/${id}`, body);
  }

  deleteExperience(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/experiences/${id}`);
  }
}
