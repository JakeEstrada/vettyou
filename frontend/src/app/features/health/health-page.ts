import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, Observable, of, startWith } from 'rxjs';

import { HealthApi, HealthResponse } from '../../core/api/health-api';

type HealthView =
  | { kind: 'loading' }
  | { kind: 'ready'; body: HealthResponse }
  | { kind: 'error' };

@Component({
  selector: 'app-health-page',
  templateUrl: './health-page.html',
  styleUrl: './health-page.scss',
})
export class HealthPage {
  private readonly api = inject(HealthApi);

  protected readonly state = toSignal(
    this.api.get().pipe(
      map((body): HealthView => ({ kind: 'ready', body })),
      catchError((): Observable<HealthView> => of({ kind: 'error' })),
      startWith<HealthView>({ kind: 'loading' }),
    ),
    { requireSync: true },
  );

  protected readonly statusText = computed(() => {
    const view = this.state();
    return view.kind === 'ready' ? view.body.status : '';
  });
}
