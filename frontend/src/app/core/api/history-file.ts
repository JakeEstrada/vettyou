export interface HistoryJobPayload {
  company: string;
  jobTitle: string;
  location: string;
  workArrangement: string;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string;
  salaryUnit: string;
  employmentType: string;
  jobDescription: string;
  technicalRequirements: string;
  preferredQualifications: string;
  source: string;
  descriptionCompleteness: string;
}

/** Reads a saved history file. Empty salaries stay empty. Unknown titles are still sent; the API skips them. */
export function jobsFromHistory(raw: unknown): HistoryJobPayload[] {
  if (raw === null || typeof raw !== 'object' || !('jobs' in raw)) {
    throw new Error('That file is not a job history JSON.');
  }
  const jobs = (raw as { jobs: unknown }).jobs;
  if (!Array.isArray(jobs)) {
    throw new Error('That file is not a job history JSON.');
  }
  return jobs.map(jobFrom);
}

function jobFrom(raw: unknown): HistoryJobPayload {
  const job = raw !== null && typeof raw === 'object' ? (raw as Record<string, unknown>) : {};
  return {
    company: text(job['company']),
    jobTitle: text(first(job, 'jobTitle', 'job_title')),
    location: text(job['location']),
    workArrangement: text(first(job, 'workArrangement', 'work_arrangement')),
    salaryMin: money(first(job, 'salaryMin', 'salary_min')),
    salaryMax: money(first(job, 'salaryMax', 'salary_max')),
    salaryCurrency: text(first(job, 'salaryCurrency', 'salary_currency')),
    salaryUnit: text(first(job, 'salaryUnit', 'salary_unit')),
    employmentType: text(first(job, 'employmentType', 'employment_type')),
    jobDescription: text(first(job, 'jobDescription', 'job_description')),
    technicalRequirements: text(first(job, 'technicalRequirements', 'technical_requirements')),
    preferredQualifications: text(first(job, 'preferredQualifications', 'preferred_qualifications')),
    source: text(job['source']),
    descriptionCompleteness: text(first(job, 'descriptionCompleteness', 'description_completeness')),
  };
}

function first(job: Record<string, unknown>, camel: string, snake: string): unknown {
  return job[camel] ?? job[snake];
}

function text(value: unknown): string {
  return typeof value === 'string' ? value.replace(/\s+/g, ' ').trim() : '';
}

function money(value: unknown): number | null {
  if (value === null || value === undefined || value === '') {
    return null;
  }
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === 'string') {
    const parsed = Number(value.trim());
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}
