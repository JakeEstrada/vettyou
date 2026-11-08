export interface MarkedPart {
  text: string;
  hit: boolean;
}

const OTHER_RESUME =
  /\b(?:older|one|some|other|earlier|previous|past)\s+(?:older\s+)?resumes?\s+(?:also\s+)?(?:says?|said|describes?|calls?|adds?|lists?|names?|mentions?)\b/i;

/** A note to oneself, not a resume bullet. */
export function noteLike(text: string): boolean {
  return (text.match(/ · | \| /g) ?? []).length >= 2 || OTHER_RESUME.test(text);
}

/** Words the person typed, split on commas or new lines. Two letters or more. */
export function parseKeywords(raw: string): string[] {
  const seen = new Set<string>();
  const words: string[] = [];
  for (const part of raw.split(/[\n,]+/)) {
    const word = part.trim();
    if (word.length < 2) {
      continue;
    }
    const key = word.toLowerCase();
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    words.push(word);
  }
  return words;
}

export function markedParts(text: string, keywords: string[]): MarkedPart[] {
  if (!text) {
    return [];
  }
  if (keywords.length === 0) {
    return [{ text, hit: false }];
  }
  const pattern = keywordPattern(keywords);
  const parts: MarkedPart[] = [];
  let last = 0;
  for (const match of text.matchAll(pattern)) {
    const start = match.index ?? 0;
    if (start > last) {
      parts.push({ text: text.slice(last, start), hit: false });
    }
    parts.push({ text: match[0], hit: true });
    last = start + match[0].length;
  }
  if (last < text.length) {
    parts.push({ text: text.slice(last), hit: false });
  }
  return parts.length > 0 ? parts : [{ text, hit: false }];
}

export function missingKeywords(haystack: string, keywords: string[]): string[] {
  return keywords.filter((word) => !keywordPattern([word]).test(haystack));
}

const SKIP = new Set([
  'assess',
  'plan',
  'implement',
  'educate',
  'administer',
  'clear',
  'written',
  'verbal',
  'communication',
  'experience',
  'families',
  'patients',
]);

/** Short tokens from a posting's requirement lines. Not a score, and not added to the resume. */
export function seedKeywords(skills: string[]): string[] {
  const seen = new Set<string>();
  const words: string[] = [];
  for (const raw of skills) {
    for (const part of raw.split(/\s+and\s+|,\s*/)) {
      const skill = part.trim();
      const bits = skill.split(/\s+/).filter(Boolean);
      if (bits.length === 0 || bits.length > 2) {
        continue;
      }
      if (/\bexperience$/i.test(skill)) {
        continue;
      }
      if (SKIP.has(bits[0].toLowerCase())) {
        continue;
      }
      if (skill.replace(/[^A-Za-z0-9+#.]/g, '').length < 3 && !/[+#.]/.test(skill)) {
        continue;
      }
      const key = skill.toLowerCase();
      if (seen.has(key)) {
        continue;
      }
      seen.add(key);
      words.push(skill);
    }
  }
  return words;
}

function keywordPattern(keywords: string[]): RegExp {
  const body = keywords.map((word) => `(?<![A-Za-z0-9])${escapeRegex(word)}(?![A-Za-z0-9])`).join('|');
  return new RegExp(`(${body})`, 'gi');
}

function escapeRegex(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
