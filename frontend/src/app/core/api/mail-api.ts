import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface MailConnection {
  connected: boolean;
  email: string;
  host: string;
  port: number;
  verifiedAt: string | null;
}

export interface MailConnectInput {
  email: string;
  password: string;
  host: string;
  port: number;
}

export type MailCategory = 'applied' | 'rejected' | 'moving_forward' | 'interview' | 'offer' | 'unknown';

export type MailSection = 'received' | 'rejections' | 'wins' | 'listings' | 'other';

export interface MailMessage {
  id: string;
  sentAt: string;
  fromName: string;
  fromAddress: string;
  subject: string;
  snippet: string;
  quote: string;
  folder: string;
  section: MailSection;
  category: MailCategory;
  reviewState: string;
  proposedApplicationId: string | null;
  proposedTitle: string;
  proposedCompany: string;
}

export interface MailSync {
  fetched: number;
  pending: number;
  messages: MailMessage[];
}

export const MAIL_CATEGORY_LABEL: Record<MailCategory, string> = {
  applied: 'Applied',
  rejected: 'Rejected',
  moving_forward: 'Moving forward',
  interview: 'Interview',
  offer: 'Offer',
  unknown: 'Needs a look',
};

export const MAIL_SECTIONS: { id: MailSection; label: string }[] = [
  { id: 'received', label: 'Applications received' },
  { id: 'rejections', label: 'Rejections' },
  { id: 'wins', label: 'WINS' },
  { id: 'other', label: 'Needs a look' },
];

@Injectable({ providedIn: 'root' })
export class MailApi {
  private readonly http = inject(HttpClient);

  get(): Observable<MailConnection> {
    return this.http.get<MailConnection>('/api/v1/mail/connection');
  }

  connect(body: MailConnectInput): Observable<MailConnection> {
    return this.http.post<MailConnection>('/api/v1/mail/connections', body);
  }

  disconnect(): Observable<void> {
    return this.http.delete<void>('/api/v1/mail/connection');
  }

  messages(): Observable<MailMessage[]> {
    return this.http.get<MailMessage[]>('/api/v1/mail/messages');
  }

  sync(): Observable<MailSync> {
    return this.http.post<MailSync>('/api/v1/mail/sync', {});
  }

  accept(id: string, applicationId: string | null): Observable<unknown> {
    return this.http.post(`/api/v1/mail/messages/${id}/accept`, { applicationId });
  }

  ignore(id: string): Observable<void> {
    return this.http.post<void>(`/api/v1/mail/messages/${id}/ignore`, {});
  }
}
