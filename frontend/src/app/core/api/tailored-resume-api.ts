import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { MatchGroup, MatchScore, RequirementMatch } from './job-posting-api';

/** A requirement part one of the person's lines backs. */
export interface LineBacking {
  requirement: string;
  part: string;
  group: MatchGroup;
  support: 'direct' | 'transferable';
}

/** Empty for lines on the resume. */
export type LineReason = '' | 'space' | 'not_needed' | 'too_long' | 'note' | 'confirm';

/** One of the person's own lines and what it does for this posting. */
export interface ResumeLine {
  kind: 'job' | 'project';
  sourceId: string;
  label: string;
  text: string;
  /** Share of the posting this line adds that better lines do not, 0 to 1. */
  gain: number;
  backs: LineBacking[];
  reason: LineReason;
}

/** A kept skill. `shown` is the posting's spelling when it names the same product. */
export interface ResumeSkill {
  name: string;
  category: string;
  shown: string;
  asked: boolean;
  places: number;
}

export interface ReadCheck {
  key: string;
  label: string;
  passed: boolean;
  /** What failed and how to fix it; empty when passed. */
  detail: string;
}

export interface ResumeSuggestion {
  kind: 'space' | 'too_long' | 'confirm' | 'skills' | 'readability';
  text: string;
  line: string;
  sourceKind: string;
  sourceId: string | null;
}

export interface WorkOnItem {
  requirement: string;
  /** Empty when the whole requirement is meant. */
  part: string;
  group: MatchGroup;
  status: 'not_shown' | 'changed' | 'gap' | 'unclear';
  reason: string;
}

export interface ResumeDocument {
  name: string;
  contact: string[];
  sections: {
    heading: string;
    rows: { label: string; text: string }[];
    entries: { title: string; dates: string; subtitle: string; bullets: string[] }[];
  }[];
}

/** Pixel box on the first-page PNG preview, origin at the top left. */
export interface HighlightBox {
  x: number;
  y: number;
  width: number;
  height: number;
}

/** A printed bullet that counts toward the estimate, and where it sits on the PDF page. */
export interface ResumeHighlight {
  text: string;
  why: string;
  boxes: HighlightBox[];
}

/** Native size of `preview.png` (letter at 100 DPI). */
export const PREVIEW_WIDTH = 850;

export const PREVIEW_HEIGHT = 1100;

export interface TailoredResumeSummary {
  id: string;
  version: number;
  createdAt: string;
  total: number | null;
}

/** One saved version, exactly as built. It never changes; building again makes a new version. */
export interface TailoredResume {
  id: string;
  postingId: string;
  version: number;
  template: string;
  createdAt: string;
  postingTitle: string;
  postingCompany: string;
  stale: boolean;
  staleReasons: string[];
  /** The Vettyou estimate for what this resume shows. */
  score: MatchScore;
  /** The Vettyou estimate for all saved evidence when this version was built. */
  evidenceTotal: number | null;
  evidenceTotalIfConfirmed: number | null;
  requirements: RequirementMatch[];
  readability: number;
  checks: ReadCheck[];
  lines: ResumeLine[];
  leftOff: ResumeLine[];
  toConfirm: ResumeLine[];
  skills: ResumeSkill[];
  skillsLeftOff: ResumeSkill[];
  suggestions: ResumeSuggestion[];
  workOn: WorkOnItem[];
  document: ResumeDocument;
  /** The text an applicant tracking system would read out of the PDF. */
  plainText: string;
  pages: number;
  pdfSha256: string;
  /** Printed bullets that count toward the estimate, with boxes on `preview.png`. */
  highlights: ResumeHighlight[];
}

export function resumeFileUrl(postingId: string, id: string, file: 'resume.pdf' | 'preview.png'): string {
  return `/api/v1/jobs/${postingId}/resumes/${id}/${file}`;
}

@Injectable({ providedIn: 'root' })
export class TailoredResumeApi {
  private readonly http = inject(HttpClient);

  list(postingId: string): Observable<TailoredResumeSummary[]> {
    return this.http.get<TailoredResumeSummary[]>(`/api/v1/jobs/${postingId}/resumes`);
  }

  get(postingId: string, id: string): Observable<TailoredResume> {
    return this.http.get<TailoredResume>(`/api/v1/jobs/${postingId}/resumes/${id}`);
  }

  /** Builds and saves a new version from the person's current data. */
  build(postingId: string): Observable<TailoredResume> {
    return this.http.post<TailoredResume>(`/api/v1/jobs/${postingId}/resumes`, null);
  }
}
