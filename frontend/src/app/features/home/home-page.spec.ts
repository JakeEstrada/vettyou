import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { HomePage } from './home-page';

describe('HomePage', () => {
  it('shows a product homepage when nobody is signed in', async () => {
    await TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(HomePage);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/profile').flush({ message: 'Sign in required.' }, { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('A resume from what you can prove.');
    expect(text).toContain('Vettyou estimate');
    expect(text).toContain('Create an account');
    expect(text).toContain('Log in');
    expect(text).toContain('never invents');
    http.verify();
  });

  it('shows ATS global when signed in', async () => {
    await TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(HomePage);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/profile').flush({
      displayName: 'Jake Estrada',
      email: '',
      phone: '',
      location: '',
      website: '',
      linkedin: '',
    });
    await fixture.whenStable();
    fixture.detectChanges();
    http.expectOne('/api/v1/jobs').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('ATS global');
    expect(text).toContain('No matches yet');
    expect(text).toContain('Vettyou estimate');
    expect(text).not.toContain('ATS score:');
    http.verify();
  });
});
