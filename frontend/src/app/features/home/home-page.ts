import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ProfileApi } from '../../core/api/profile-api';
import { apiUnreachable, messageOf } from '../../core/auth/session';
import { ScoresPage } from '../scores/scores-page';

@Component({
  selector: 'app-home-page',
  imports: [RouterLink, ScoresPage],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss',
})
export class HomePage {
  private readonly profiles = inject(ProfileApi);

  readonly loading = signal(true);

  readonly missing = signal(false);

  readonly error = signal('');

  constructor() {
    this.profiles.get().subscribe({
      next: () => this.loading.set(false),
      error: (error: unknown) => {
        this.loading.set(false);
        if ((error instanceof HttpErrorResponse && error.status === 401) || apiUnreachable(error)) {
          this.missing.set(true);
        } else {
          this.error.set(messageOf(error));
        }
      },
    });
  }
}
