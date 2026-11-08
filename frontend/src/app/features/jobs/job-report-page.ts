import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { switchMap, tap } from 'rxjs';

import { JobPostingApi, reportUrl } from '../../core/api/job-posting-api';
import { messageOf } from '../../core/auth/session';

@Component({
  selector: 'app-job-report-page',
  imports: [RouterLink],
  templateUrl: './job-report-page.html',
  styleUrl: './job-report-page.scss',
})
export class JobReportPage {
  private readonly api = inject(JobPostingApi);

  private readonly textBox = viewChild<ElementRef<HTMLPreElement>>('report');

  readonly id = signal('');

  readonly text = signal<string | null>(null);

  readonly error = signal('');

  readonly copied = signal('');

  readonly pdfUrl = (id: string) => reportUrl(id, 'pdf');

  readonly txtUrl = (id: string) => reportUrl(id, 'txt');

  constructor() {
    inject(ActivatedRoute)
      .paramMap.pipe(
        tap((params) => {
          this.id.set(params.get('id') ?? '');
          this.text.set(null);
          this.error.set('');
          this.copied.set('');
        }),
        switchMap(() => this.api.reportText(this.id())),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: (text) => this.text.set(text),
        error: (error: unknown) =>
          this.error.set(
            error instanceof HttpErrorResponse && error.status === 404
              ? 'That posting was not found.'
              : error instanceof HttpErrorResponse && error.status === 409
                ? 'This posting has not been read yet. Open it and wait for the score first.'
                : messageOf(error),
          ),
      });
  }

  copy(): void {
    const text = this.text();
    if (text === null) {
      return;
    }
    this.selectAll();
    if (document.execCommand?.('copy')) {
      this.copied.set('Copied. Paste it into ChatGPT.');
      return;
    }
    if (!navigator.clipboard) {
      this.copied.set('Your browser blocked copying. The report is selected; press Ctrl+C.');
      return;
    }
    navigator.clipboard.writeText(text).then(
      () => this.copied.set('Copied. Paste it into ChatGPT.'),
      () => {
        this.selectAll();
        this.copied.set('Your browser blocked copying. The report is selected; press Ctrl+C.');
      },
    );
  }

  selectAll(): void {
    const box = this.textBox()?.nativeElement;
    const selection = window.getSelection();
    if (!box || !selection) {
      return;
    }
    const range = document.createRange();
    range.selectNodeContents(box);
    selection.removeAllRanges();
    selection.addRange(range);
  }
}
