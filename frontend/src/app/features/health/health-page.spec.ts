import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { HealthPage } from './health-page';

describe('HealthPage', () => {
  it('shows the status returned by the server', async () => {
    await TestBed.configureTestingModule({
      imports: [HealthPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    const fixture = TestBed.createComponent(HealthPage);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/health').flush({ status: 'ok' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Status: ok');
    http.verify();
  });
});
