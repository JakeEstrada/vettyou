const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/** Calendar day in UTC, for ledger rows. */
export function day(iso: string | null | undefined): string {
  if (!iso) {
    return '—';
  }
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '—';
  }
  return `${MONTHS[date.getUTCMonth()]} ${date.getUTCDate()}, ${date.getUTCFullYear()}`;
}

export function monthYear(date: string | null): string {
  if (!date) {
    return '';
  }
  const [year, month] = date.split('-');
  return `${MONTHS[Number(month) - 1] ?? ''} ${year}`.trim();
}

export function dateRange(start: string | null, end: string | null, current = false): string {
  const from = monthYear(start);
  const to = current ? 'Present' : monthYear(end);
  if (from && to) {
    return `${from} – ${to}`;
  }
  return from || to;
}

/** Splits a write-up into the lines the person typed, without list marks. */
export function lines(body: string): string[] {
  return body
    .split('\n')
    .map((line) => line.replace(/^\s*(?:[-*+•]|\d+[.)])\s+/, '').trim())
    .filter((line) => line.length > 0);
}
