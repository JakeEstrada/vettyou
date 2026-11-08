import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface EducationInput {
  institution: string;
  credential: string;
  fieldOfStudy: string;
  startDate: string | null;
  endDate: string | null;
}

export interface Education extends EducationInput {
  id: string;
}

@Injectable({ providedIn: 'root' })
export class EducationApi {
  private readonly http = inject(HttpClient);

  list(): Observable<Education[]> {
    return this.http.get<Education[]>('/api/v1/educations');
  }

  create(body: EducationInput): Observable<Education> {
    return this.http.post<Education>('/api/v1/educations', body);
  }

  save(id: string, body: EducationInput): Observable<Education> {
    return this.http.put<Education>(`/api/v1/educations/${id}`, body);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/educations/${id}`);
  }
}
