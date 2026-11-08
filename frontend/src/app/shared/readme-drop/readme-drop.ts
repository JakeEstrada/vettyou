import { Component, input, output, signal } from '@angular/core';

export interface ReadmeFile {
  fileName: string;
  body: string;
}

const MAX_README_CHARS = 200_000;

@Component({
  selector: 'app-readme-drop',
  templateUrl: './readme-drop.html',
  styleUrl: './readme-drop.scss',
  host: {
    '(document:dragover)': '$event.preventDefault()',
    '(document:drop)': '$event.preventDefault()',
  },
})
export class ReadmeDrop {
  readonly label = input('Drop a README.md here');

  readonly busy = input(false);

  readonly picked = output<ReadmeFile>();

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
    if (!/\.(md|markdown|txt)$/i.test(file.name)) {
      this.error.set('That is not a markdown file. Use a .md, .markdown or .txt file.');
      return;
    }
    const body = await file.text();
    if (body.trim().length === 0) {
      this.error.set('That file is empty.');
      return;
    }
    if (body.length > MAX_README_CHARS) {
      this.error.set('That file is too long. Keep it under 200,000 characters.');
      return;
    }
    this.picked.emit({ fileName: file.name, body });
  }
}
