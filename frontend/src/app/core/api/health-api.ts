import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface HealthResponse {
  status: string;
}

@Injectable({ providedIn: 'root' })
export class HealthApi {
  private readonly http = inject(HttpClient);

  get(): Observable<HealthResponse> {
    return this.http.get<HealthResponse>('/api/v1/health');
  }
}
