import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type PostingStatus = 'reading' | 'matching' | 'ready' | 'failed';

/**
 * How well a requirement is shown, after confirmation and answers. `hidden` only appears on a tailored resume: saved
 * lines show it, but none of them is on the resume.
 */
export type MatchStatus = 'direct' | 'partial' | 'unconfirmed' | 'unclear' | 'not_shown' | 'gap' | 'met' | 'hidden';

export type PartStatus = 'direct' | 'transferable' | 'unconfirmed' | 'unclear' | 'changed' | 'not_shown' | 'hidden';

export type MatchGroup = 'required' | 'responsibility' | 'preferred';

export type Answer = 'meets' | 'gap';

export interface JobPostingSummary {
  id: string;
  title: string;
  company: string;
  pay: string;
  preview: string;
  status: PostingStatus;
  createdAt: string;
}

/** A word-for-word excerpt from one of the person's own lines, or from a tailored resume's skills section. */
export interface EvidenceCite {
  kind: 'project' | 'job' | 'school' | 'skills';
  id: string;
  label: string;
  excerpt: string;
  line: string;
  confirmed: boolean;
}

export interface PartMatch {
  text: string;
  concept: string;
  support: string;
  status: PartStatus;
  reason: string;
  weight: number | null;
  sharedBy: number;
  evidence: EvidenceCite[];
}

export interface RequirementMatch {
  id: string;
  skill: string;
  kind: 'qualification' | 'responsibility';
  importance: 'required' | 'preferred';
  group: MatchGroup;
  category: string;
  quote: string;
  logic: 'all' | 'any';
  status: MatchStatus;
  note: string;
  answer: Answer | null;
  answeredAt: string | null;
  canMarkMet: boolean;
  weight: number | null;
  credit: number | null;
  creditIfConfirmed: number | null;
  strength: number | null;
  parts: PartMatch[];
}

export interface ScoreFactor {
  key: string;
  label: string;
  weight: number;
  /** The weight the total uses: `weight` for coverage groups, `weight` x coverage for evidence strength. */
  activeWeight: number | null;
  activeWeightIfConfirmed: number | null;
  value: number | null;
  valueIfConfirmed: number | null;
  how: string;
}

export interface Readiness {
  value: number;
  checks: { label: string; done: boolean }[];
}

export interface MatchScore {
  total: number | null;
  unrounded: number | null;
  totalIfConfirmed: number | null;
  unroundedIfConfirmed: number | null;
  factors: ScoreFactor[];
  formula: string;
  formulaIfConfirmed: string;
  toConfirm: number;
  toAnswer: number;
  readiness: Readiness;
  label: string;
  note: string;
}

export interface JobPosting extends JobPostingSummary {
  reader: string;
  matcher: string;
  matchedAt: string | null;
  evidenceChanged: boolean;
  error: string;
  body: string;
  requirements: RequirementMatch[];
  score: MatchScore;
}

export function reportUrl(id: string, format: 'txt' | 'pdf'): string {
  return `/api/v1/jobs/${id}/report.${format}`;
}

@Injectable({ providedIn: 'root' })
export class JobPostingApi {
  private readonly http = inject(HttpClient);

  list(): Observable<JobPostingSummary[]> {
    return this.http.get<JobPostingSummary[]>('/api/v1/jobs');
  }

  create(body: string): Observable<JobPostingSummary> {
    return this.http.post<JobPostingSummary>('/api/v1/jobs', { body });
  }

  get(id: string): Observable<JobPosting> {
    return this.http.get<JobPosting>(`/api/v1/jobs/${id}`);
  }

  reportText(id: string): Observable<string> {
    return this.http.get(reportUrl(id, 'txt'), { responseType: 'text' });
  }

  /** Reads the posting from scratch. Requirements are replaced, so answers are cleared. */
  reread(id: string): Observable<JobPostingSummary> {
    return this.http.post<JobPostingSummary>(`/api/v1/jobs/${id}/read`, null);
  }

  /** Checks the saved requirements against the person's current evidence. Answers are kept. */
  rematch(id: string): Observable<JobPostingSummary> {
    return this.http.post<JobPostingSummary>(`/api/v1/jobs/${id}/match`, null);
  }

  answer(id: string, requirementId: string, answer: Answer | null): Observable<JobPosting> {
    return this.http.put<JobPosting>(`/api/v1/jobs/${id}/requirements/${requirementId}/answer`, { answer });
  }

  removeRequirement(id: string, requirementId: string): Observable<JobPosting> {
    return this.http.delete<JobPosting>(`/api/v1/jobs/${id}/requirements/${requirementId}`);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/jobs/${id}`);
  }
}
