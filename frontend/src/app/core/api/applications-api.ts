import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { HistoryJobPayload } from './history-file';

export type ApplicationStatus =
  | 'draft'
  | 'applied'
  | 'rejected'
  | 'moving_forward'
  | 'interview'
  | 'offer'
  | 'withdrawn'
  | 'no_reply';

export const APPLICATION_STATUSES: { id: ApplicationStatus; label: string }[] = [
  { id: 'draft', label: 'Draft' },
  { id: 'applied', label: 'Applied' },
  { id: 'no_reply', label: 'No reply' },
  { id: 'moving_forward', label: 'Moving forward' },
  { id: 'interview', label: 'Interview' },
  { id: 'offer', label: 'Offer' },
  { id: 'rejected', label: 'Rejected' },
  { id: 'withdrawn', label: 'Withdrawn' },
];

export interface ApplicationEvent {
  id: string;
  status: ApplicationStatus;
  note: string;
  createdAt: string;
}

export interface JobApplication {
  id: string;
  postingId: string | null;
  title: string;
  company: string;
  location: string;
  pay: string;
  status: ApplicationStatus;
  appliedAt: string | null;
  notes: string;
  createdAt: string;
  estimate: number | null;
  events: ApplicationEvent[];
}

export interface ApplicationInput {
  title: string;
  company: string;
  location: string;
  pay: string;
  status: ApplicationStatus;
  postingId: string | null;
  appliedAt: string | null;
  notes: string;
}

export interface HistoryImportResult {
  imported: number;
  skipped: number;
  applications: JobApplication[];
}

@Injectable({ providedIn: 'root' })
export class ApplicationsApi {
  private readonly http = inject(HttpClient);

  list(): Observable<JobApplication[]> {
    return this.http.get<JobApplication[]>('/api/v1/applications');
  }

  create(body: ApplicationInput): Observable<JobApplication> {
    return this.http.post<JobApplication>('/api/v1/applications', body);
  }

  importHistory(jobs: HistoryJobPayload[]): Observable<HistoryImportResult> {
    return this.http.post<HistoryImportResult>('/api/v1/applications/history', { jobs });
  }

  save(id: string, body: ApplicationInput): Observable<JobApplication> {
    return this.http.put<JobApplication>(`/api/v1/applications/${id}`, body);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/applications/${id}`);
  }
}

export function statusLabel(status: ApplicationStatus): string {
  return APPLICATION_STATUSES.find((item) => item.id === status)?.label ?? status;
}
