import { HttpErrorResponse } from '@angular/common/http';

import { apiUnreachable, messageOf } from './session';

describe('messageOf', () => {
  it('names a missing API instead of a generic failure', () => {
    expect(messageOf(new HttpErrorResponse({ status: 0 }))).toBe('Could not reach the Vettyou API.');
    expect(
      messageOf(new HttpErrorResponse({ status: 200, error: { text: '<!doctype html><title>Vettyou</title>' } })),
    ).toBe('Could not reach the Vettyou API.');
    expect(apiUnreachable(new HttpErrorResponse({ status: 401 }))).toBe(false);
  });
});
