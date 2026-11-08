import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ProfilePage } from './profile-page';

describe('ProfilePage', () => {
  async function setup() {
    await TestBed.configureTestingModule({
      imports: [ProfilePage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(ProfilePage);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/v1/profile').flush({
      displayName: '',
      email: '',
      phone: '',
      location: '',
      website: '',
      linkedin: '',
    });
    return { fixture, http };
  }

  it('saves the name and contact details', async () => {
    const { fixture, http } = await setup();

    fixture.componentInstance.form.setValue({
      displayName: 'Jake Estrada',
      email: 'jake@example.com',
      phone: '',
      location: 'Seattle',
      website: '',
      linkedin: '',
    });
    fixture.componentInstance.submit();

    const request = http.expectOne('/api/v1/profile');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({
      displayName: 'Jake Estrada',
      email: 'jake@example.com',
      phone: '',
      location: 'Seattle',
      website: '',
      linkedin: '',
    });
    request.flush({
      displayName: 'Jake Estrada',
      email: 'jake@example.com',
      phone: '',
      location: 'Seattle',
      website: '',
      linkedin: '',
    });
    await fixture.whenStable();

    expect(fixture.componentInstance.saved()).toBe(true);
    http.verify();
  });

  it('does not send an email address that is not one', async () => {
    const { fixture, http } = await setup();

    fixture.componentInstance.form.controls.displayName.setValue('Jake Estrada');
    fixture.componentInstance.form.controls.email.setValue('not an email');
    fixture.componentInstance.submit();
    fixture.detectChanges();

    http.expectNone('/api/v1/profile');
    expect(fixture.nativeElement.textContent).toContain('Enter an email address like name@example.com.');
    http.verify();
  });
});
