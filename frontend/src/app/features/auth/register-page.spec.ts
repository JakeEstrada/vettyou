import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { RegisterPage } from './register-page';

describe('RegisterPage', () => {
  it('sends the email and password to create an account', async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    const fixture = TestBed.createComponent(RegisterPage);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    fixture.componentInstance.form.setValue({ email: 'a@example.com', password: 'password1' });
    fixture.componentInstance.submit();

    const request = http.expectOne('/api/v1/auth/register');
    expect(request.request.body).toEqual({ email: 'a@example.com', password: 'password1' });
    request.flush({ id: '11111111-1111-1111-1111-111111111111', email: 'a@example.com' });
    http.expectOne('/api/v1/me').flush({ id: '11111111-1111-1111-1111-111111111111', email: 'a@example.com' });
    await fixture.whenStable();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/account');
    http.verify();
  });
});
