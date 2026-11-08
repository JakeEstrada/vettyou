import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Session } from '../../core/auth/session';

@Component({
  selector: 'app-account-page',
  imports: [RouterLink],
  templateUrl: './account-page.html',
})
export class AccountPage {
  protected readonly session = inject(Session);
}
