import { FormControl, FormGroup, Validators } from '@angular/forms';

import { fieldErrorsOf, messageOf } from '../../core/auth/session';

export function credentialsForm(): FormGroup<{
  email: FormControl<string>;
  password: FormControl<string>;
}> {
  return new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8)],
    }),
  });
}

export function applyServerErrors(
  form: ReturnType<typeof credentialsForm>,
  error: unknown,
  formError: { set: (value: string) => void },
): void {
  formError.set('');
  for (const fieldError of fieldErrorsOf(error)) {
    form.get(fieldError.field)?.setErrors({ server: fieldError.message });
  }
  if (fieldErrorsOf(error).length === 0) {
    formError.set(messageOf(error));
  }
}
