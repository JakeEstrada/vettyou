import { Component, computed, input, output } from '@angular/core';

import { PREVIEW_HEIGHT, PREVIEW_WIDTH, ResumeHighlight, resumeFileUrl } from '../../core/api/tailored-resume-api';

/** The first page of the saved PDF, with the lines the estimate is reading marked on top. */
@Component({
  selector: 'app-resume-preview',
  templateUrl: './resume-preview.html',
  styleUrl: './resume-preview.scss',
})
export class ResumePreview {
  readonly postingId = input.required<string>();

  readonly resumeId = input.required<string>();

  readonly highlights = input<ResumeHighlight[]>([]);

  readonly picked = input('');

  readonly pick = output<string>();

  readonly previewUrl = computed(() => resumeFileUrl(this.postingId(), this.resumeId(), 'preview.png'));

  readonly pdfUrl = computed(() => resumeFileUrl(this.postingId(), this.resumeId(), 'resume.pdf'));

  readonly why = computed(() => this.highlights().find((item) => item.text === this.picked())?.why ?? '');

  protected readonly pageWidth = PREVIEW_WIDTH;

  protected readonly pageHeight = PREVIEW_HEIGHT;

  protected choose(text: string): void {
    this.pick.emit(text);
  }

  protected left(x: number): string {
    return `${(x / PREVIEW_WIDTH) * 100}%`;
  }

  protected top(y: number): string {
    return `${(y / PREVIEW_HEIGHT) * 100}%`;
  }

  protected width(width: number): string {
    return `${(width / PREVIEW_WIDTH) * 100}%`;
  }

  protected height(height: number): string {
    return `${(height / PREVIEW_HEIGHT) * 100}%`;
  }
}
