import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { MailboxConnect } from './mailbox-connect';

describe('MailboxConnect', () => {
  it('sends the password to connect and never shows it after a successful login', async () => {
    await TestBed.configureTestingModule({
      imports: [MailboxConnect],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    const fixture = TestBed.createComponent(MailboxConnect);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/mail/connection').flush({
      connected: false,
      email: '',
      host: 'mail.privateemail.com',
      port: 993,
      verifiedAt: null,
    });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Connect email');
    expect(text).toContain('mail.privateemail.com');
    expect(fixture.nativeElement.querySelector('#mailbox-password')).not.toBeNull();

    fixture.componentInstance.form.setValue({
      email: 'contact@jakeestrada.org',
      password: 'app-pass-1',
      host: 'mail.privateemail.com',
      port: 993,
    });
    fixture.componentInstance.connect();
    const posted = http.expectOne('/api/v1/mail/connections');
    expect(posted.request.body).toEqual({
      email: 'contact@jakeestrada.org',
      password: 'app-pass-1',
      host: 'mail.privateemail.com',
      port: 993,
    });
    posted.flush({
      connected: true,
      email: 'contact@jakeestrada.org',
      host: 'mail.privateemail.com',
      port: 993,
      verifiedAt: '2026-11-08T00:00:00Z',
    });
    http.expectOne('/api/v1/mail/messages').flush([]);
    fixture.detectChanges();

    const connected = fixture.nativeElement.textContent as string;
    expect(connected).toContain('contact@jakeestrada.org');
    expect(connected).toContain('Read mail');
    expect(connected).not.toContain('Applications received');
    expect(connected).not.toContain('Rejections');
    expect(connected).not.toContain('WINS');
    expect(connected).not.toContain('app-pass-1');
    expect(fixture.nativeElement.querySelector('#mailbox-password')).toBeNull();
    expect(fixture.componentInstance.form.controls.password.value).toBe('');
    http.verify();
  });

  it('shows the job on a compact line, opens the email on click, and saves a rejection after confirm', async () => {
    await TestBed.configureTestingModule({
      imports: [MailboxConnect],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    const fixture = TestBed.createComponent(MailboxConnect);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/mail/connection').flush({
      connected: true,
      email: 'contact@jakeestrada.org',
      host: 'mail.privateemail.com',
      port: 993,
      verifiedAt: '2026-11-08T00:00:00Z',
    });
    http.expectOne('/api/v1/mail/messages').flush([
      {
        id: 'm1',
        sentAt: '2026-01-03T00:00:00Z',
        fromName: 'Ensign Talent',
        fromAddress: 'noreply@ensignservices.com',
        subject: 'Your application update',
        snippet: 'Unfortunately we chose another candidate.',
        quote: 'Unfortunately we chose another candidate.',
        folder: 'INBOX.Rejections',
        section: 'rejections',
        category: 'rejected',
        reviewState: 'pending',
        proposedApplicationId: null,
        proposedTitle: 'Registered Nurse - Medical/Surgical Unit',
        proposedCompany: 'Ensign Services, Inc.',
      },
    ]);
    fixture.detectChanges();

    const closed = fixture.nativeElement.textContent as string;
    expect(closed).toContain('Rejected');
    expect(closed).toContain('Registered Nurse - Medical/Surgical Unit');
    expect(closed).toContain('Ensign Services, Inc.');
    expect(closed).not.toContain('Unfortunately we chose another candidate.');
    expect(closed).not.toContain('Applications received');

    fixture.nativeElement.querySelector('.row').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Unfortunately we chose another candidate.');

    fixture.componentInstance.accept(fixture.componentInstance.messages()[0]);
    const saved = http.expectOne('/api/v1/mail/messages/m1/accept');
    expect(saved.request.body).toEqual({ applicationId: null });
    saved.flush({
      id: 'a1',
      status: 'rejected',
      company: 'Ensign Services, Inc.',
      title: 'Registered Nurse - Medical/Surgical Unit',
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Registered Nurse - Medical/Surgical Unit');
    http.verify();
  });
});
