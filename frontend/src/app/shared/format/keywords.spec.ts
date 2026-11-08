import { markedParts, missingKeywords, seedKeywords } from './keywords';

describe('keywords', () => {
  it('does not mark SQL inside SQLite', () => {
    const parts = markedParts('MongoDB, PostgreSQL, SQLite', ['SQL']);
    expect(parts.filter((part) => part.hit).map((part) => part.text)).toEqual([]);
  });

  it('marks a whole word and lists only missing whole words', () => {
    const haystack = 'Languages. Python, TypeScript. Frameworks. React. Databases. PostgreSQL, SQLite';
    expect(missingKeywords(haystack, ['React', 'Kubernetes', 'SQL'])).toEqual(['Kubernetes', 'SQL']);
    expect(markedParts(haystack, ['React']).some((part) => part.hit && part.text === 'React')).toBe(true);
  });

  it('seeds short skills and drops verbs and CI/CD fragments', () => {
    expect(
      seedKeywords([
        'React and TypeScript',
        'Docker and Kubernetes',
        'CI/CD',
        'TypeScript experience',
        'Assess, plan, implement',
        'Administer medications',
        'REST APIs',
      ]),
    ).toEqual(['React', 'TypeScript', 'Docker', 'Kubernetes', 'CI/CD', 'REST APIs']);
  });
});
