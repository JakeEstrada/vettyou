import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { MAIL_CATEGORY_LABEL, MailApi, MailConnection, MailMessage } from '../../core/api/mail-api';
import { fieldErrorsOf, messageOf } from '../../core/auth/session';
import { day } from '../../shared/format/dates';

@Component({
  selector: 'app-mailbox-connect',
  imports: [ReactiveFormsModule],
  templateUrl: './mailbox-connect.html',
  styleUrl: './mailbox-connect.scss',
})
export class MailboxConnect {
  private readonly api = inject(MailApi);

  readonly recorded = output<void>();

  readonly loading = signal(true);

  readonly saving = signal(false);

  readonly disconnecting = signal(false);

  readonly syncing = signal(false);

  readonly error = signal('');

  readonly connection = signal<MailConnection | null>(null);

  readonly messages = signal<MailMessage[]>([]);

  readonly working = signal('');

  readonly opened = signal('');

  readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    host: new FormControl('mail.privateemail.com', { nonNullable: true, validators: [Validators.required] }),
    port: new FormControl(993, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.max(65535)],
    }),
  });

  constructor() {
    this.refresh();
  }

  label(category: MailMessage['category']): string {
    return MAIL_CATEGORY_LABEL[category] ?? category;
  }

  job(item: MailMessage): string {
    return item.proposedTitle || item.subject || '(no subject)';
  }

  company(item: MailMessage): string {
    return item.proposedCompany || item.fromName || item.fromAddress;
  }

  when(iso: string): string {
    return day(iso);
  }

  toggle(id: string): void {
    this.opened.set(this.opened() === id ? '' : id);
  }

  connect(): void {
    this.error.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.api.connect(this.form.getRawValue()).subscribe({
      next: (connection) => {
        this.connection.set(connection);
        this.form.controls.password.setValue('');
        this.saving.set(false);
        this.loadMessages();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.form.controls.password.setValue('');
        this.applyError(error);
      },
    });
  }

  disconnect(): void {
    this.error.set('');
    this.disconnecting.set(true);
    this.api.disconnect().subscribe({
      next: () => {
        this.connection.set({ connected: false, email: '', host: 'mail.privateemail.com', port: 993, verifiedAt: null });
        this.messages.set([]);
        this.opened.set('');
        this.disconnecting.set(false);
      },
      error: (error: unknown) => {
        this.disconnecting.set(false);
        this.error.set(messageOf(error));
      },
    });
  }

  sync(): void {
    this.error.set('');
    this.syncing.set(true);
    this.api.sync().subscribe({
      next: (result) => {
        this.messages.set(result.messages);
        this.syncing.set(false);
      },
      error: (error: unknown) => {
        this.syncing.set(false);
        this.error.set(messageOf(error));
      },
    });
  }

  accept(item: MailMessage): void {
    this.error.set('');
    this.working.set(item.id);
    this.api.accept(item.id, item.proposedApplicationId).subscribe({
      next: () => {
        this.drop(item.id);
        this.recorded.emit();
      },
      error: (error: unknown) => {
        this.working.set('');
        this.error.set(messageOf(error));
      },
    });
  }

  ignore(item: MailMessage): void {
    this.error.set('');
    this.working.set(item.id);
    this.api.ignore(item.id).subscribe({
      next: () => this.drop(item.id),
      error: (error: unknown) => {
        this.working.set('');
        this.error.set(messageOf(error));
      },
    });
  }

  private drop(id: string): void {
    this.messages.set(this.messages().filter((row) => row.id !== id));
    if (this.opened() === id) {
      this.opened.set('');
    }
    this.working.set('');
  }

  private refresh(): void {
    this.api.get().subscribe({
      next: (connection) => {
        this.connection.set(connection);
        if (connection.connected) {
          this.form.patchValue({ email: connection.email, host: connection.host, port: connection.port });
          this.loadMessages();
        } else {
          this.loading.set(false);
        }
      },
      error: (error: unknown) => {
        this.loading.set(false);
        if (!(error instanceof HttpErrorResponse && error.status === 401)) {
          this.error.set(messageOf(error));
        }
      },
    });
  }

  private loadMessages(): void {
    this.api.messages().subscribe({
      next: (messages) => {
        this.messages.set(messages);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.error.set(messageOf(error));
      },
    });
  }

  private applyError(error: unknown): void {
    this.error.set('');
    for (const fieldError of fieldErrorsOf(error)) {
      this.form.get(fieldError.field)?.setErrors({ server: fieldError.message });
      this.error.set(fieldError.message);
    }
    if (fieldErrorsOf(error).length === 0) {
      this.error.set(messageOf(error));
    }
  }
}
