import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { Session } from '../../core/auth/session';

@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  protected readonly session = inject(Session);

  private readonly router = inject(Router);

  signOut(): void {
    this.session.signOut().subscribe(() => {
      void this.router.navigateByUrl('/');
    });
  }
}
