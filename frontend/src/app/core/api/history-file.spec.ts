import { jobsFromHistory } from './history-file';

describe('jobsFromHistory', () => {
  it('maps snake_case history rows and leaves empty salaries empty', () => {
    const jobs = jobsFromHistory({
      metadata: { record_count: 2 },
      jobs: [
        {
          company: 'L3Harris Technologies',
          job_title: 'Specialist Software Engineer',
          location: 'Anaheim, CA',
          salary_min: 97000,
          salary_max: 180000,
          salary_currency: 'USD',
          salary_unit: 'year',
          job_description: 'Preferred additional skills: Python or C#.',
          description_completeness: 'partial verbatim excerpt',
        },
        {
          company: 'Relativity Space',
          job_title: 'Full Stack Software Engineer I, Factory Platform',
          salary_min: '',
          salary_max: null,
        },
      ],
    });

    expect(jobs[0].jobTitle).toBe('Specialist Software Engineer');
    expect(jobs[0].salaryMin).toBe(97000);
    expect(jobs[0].salaryMax).toBe(180000);
    expect(jobs[1].salaryMin).toBeNull();
    expect(jobs[1].salaryMax).toBeNull();
  });

  it('rejects a file without a jobs list', () => {
    expect(() => jobsFromHistory({ metadata: {} })).toThrow('That file is not a job history JSON.');
  });
});
