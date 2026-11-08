import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { MailboxPage } from './mailbox-page';

describe('MailboxPage', () => {
  it('is the email and tracking section without a folder rail', async () => {
    await TestBed.configureTestingModule({
      imports: [MailboxPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(MailboxPage);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/applications').flush([]);
    http.expectOne('/api/v1/jobs').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();
    http.expectOne('/api/v1/mail/connection').flush({
      connected: true,
      email: 'contact@jakeestrada.org',
      host: 'mail.privateemail.com',
      port: 993,
      verifiedAt: '2026-11-08T00:00:00Z',
    });
    http.expectOne('/api/v1/mail/messages').flush([]);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Email');
    expect(text).toContain('Read mail');
    expect(text).toContain('contact@jakeestrada.org');
    expect(text).not.toContain('Applications received');
    expect(text).not.toContain('Rejections');
    expect(text).not.toContain('WINS');
    expect(text).not.toContain('Honeywell');
    http.verify();
  });
});
