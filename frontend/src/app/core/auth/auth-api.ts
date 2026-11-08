import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface Account {
  id: string;
  email: string;
}

export interface Credentials {
  email: string;
  password: string;
}

export interface ApiErrorBody {
  status: number;
  code: string;
  message: string;
  fieldErrors: { field: string; message: string }[];
}

@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);

  register(body: Credentials): Observable<Account> {
    return this.http.post<Account>('/api/v1/auth/register', body);
  }

  login(body: Credentials): Observable<Account> {
    return this.http.post<Account>('/api/v1/auth/login', body);
  }

  me(): Observable<Account> {
    return this.http.get<Account>('/api/v1/me');
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/v1/auth/logout', {});
  }
}
