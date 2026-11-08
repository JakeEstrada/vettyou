export type PostingBlock =
  | { kind: 'heading'; text: string }
  | { kind: 'paragraph'; text: string }
  | { kind: 'list'; items: string[] };

/** Turns pasted posting text into readable blocks. Headings, lists, and paragraphs only. */
export function postingBlocks(body: string): PostingBlock[] {
  const blocks: PostingBlock[] = [];
  let paragraph: string[] = [];
  let list: string[] = [];

  const flushParagraph = (): void => {
    const text = paragraph.join(' ').replace(/\s+/g, ' ').trim();
    if (text) {
      blocks.push({ kind: 'paragraph', text });
    }
    paragraph = [];
  };

  const flushList = (): void => {
    if (list.length > 0) {
      blocks.push({ kind: 'list', items: list });
      list = [];
    }
  };

  for (const raw of body.replace(/\r\n/g, '\n').split('\n')) {
    const line = raw.trim();
    if (line === '') {
      flushParagraph();
      flushList();
      continue;
    }
    const heading = line.match(/^#{1,6}\s+(.+)$/);
    if (heading) {
      flushParagraph();
      flushList();
      blocks.push({ kind: 'heading', text: heading[1].trim() });
      continue;
    }
    const item = line.match(/^(?:[-*•]|\d+[.)])\s+(.+)$/);
    if (item) {
      flushParagraph();
      list.push(item[1].trim());
      continue;
    }
    if (/^[^:]{2,80}:$/.test(line)) {
      flushParagraph();
      flushList();
      blocks.push({ kind: 'heading', text: line.slice(0, -1).trim() });
      continue;
    }
    flushList();
    paragraph.push(line);
  }
  flushParagraph();
  flushList();
  return blocks;
}
