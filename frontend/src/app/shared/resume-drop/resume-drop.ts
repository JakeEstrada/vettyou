import { Component, input, output, signal } from '@angular/core';

export interface ResumeFile {
  fileName: string;
  body: string;
  encoding: 'text' | 'base64';
}

const MAX_TEXT = 200_000;
const MAX_PDF_BYTES = 5_000_000;

@Component({
  selector: 'app-resume-drop',
  templateUrl: './resume-drop.html',
  styleUrl: './resume-drop.scss',
  host: {
    '(document:dragover)': '$event.preventDefault()',
    '(document:drop)': '$event.preventDefault()',
  },
})
export class ResumeDrop {
  readonly label = input('Drop a resume here');

  readonly busy = input(false);

  readonly picked = output<ResumeFile>();

  readonly over = signal(false);

  readonly error = signal('');

  dragOver(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'copy';
    }
    this.over.set(true);
  }

  dragLeave(event: DragEvent): void {
    const target = event.currentTarget as HTMLElement;
    if (!(event.relatedTarget instanceof Node) || !target.contains(event.relatedTarget)) {
      this.over.set(false);
    }
  }

  async drop(event: DragEvent): Promise<void> {
    event.preventDefault();
    this.over.set(false);
    await this.read(event.dataTransfer?.files?.[0]);
  }

  async choose(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    await this.read(input.files?.[0]);
    input.value = '';
  }

  private async read(file: File | undefined): Promise<void> {
    this.error.set('');
    if (!file || this.busy()) {
      return;
    }
    const pdf = /\.pdf$/i.test(file.name);
    const text = /\.(txt|md|markdown)$/i.test(file.name);
    if (!pdf && !text) {
      this.error.set('Use a .txt, .md, or .pdf file. For Word, export a PDF or paste the text.');
      return;
    }
    if (pdf) {
      if (file.size > MAX_PDF_BYTES) {
        this.error.set('That PDF is over 5 MB. Paste the text instead.');
        return;
      }
      this.picked.emit({ fileName: file.name, body: await toBase64(file), encoding: 'base64' });
      return;
    }
    const body = await file.text();
    if (body.trim().length === 0) {
      this.error.set('That file is empty.');
      return;
    }
    if (body.length > MAX_TEXT) {
      this.error.set('That file is too long. Keep it under 200,000 characters.');
      return;
    }
    this.picked.emit({ fileName: file.name, body, encoding: 'text' });
  }
}

async function toBase64(file: File): Promise<string> {
  const bytes = new Uint8Array(await file.arrayBuffer());
  let binary = '';
  const size = 0x8000;
  for (let index = 0; index < bytes.length; index += size) {
    binary += String.fromCharCode(...bytes.subarray(index, index + size));
  }
  return btoa(binary);
}
