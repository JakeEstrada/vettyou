import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Answer, MatchStatus, PartStatus, RequirementMatch } from '../../core/api/job-posting-api';
import { useLink } from '../../core/api/skills-api';

/** Same wording as the report. */
export const STATUS_LABELS: Record<MatchStatus, string> = {
  direct: 'Direct evidence',
  partial: 'Transferable or partial evidence',
  unconfirmed: 'Needs your confirmation',
  unclear: 'Needs clarification',
  not_shown: 'Not evidenced in the available material',
  gap: 'Confirmed gap',
  met: 'You confirmed you meet this',
  hidden: 'In your evidence, not on this resume',
};

export const PART_LABELS: Record<PartStatus, string> = {
  direct: 'Direct',
  transferable: 'Transferable',
  unconfirmed: 'Not confirmed yet',
  unclear: 'Needs clarification',
  changed: 'Line changed',
  not_shown: 'Not shown',
  hidden: 'Not on this resume',
};

/** One posting requirement: its parts, the quotes behind each, its numbers, and the answer buttons. */
@Component({
  selector: 'app-requirement-item',
  imports: [RouterLink],
  templateUrl: './requirement-item.html',
  styleUrl: './requirement-item.scss',
})
export class RequirementItem {
  readonly item = input.required<RequirementMatch>();

  readonly saving = input(false);

  readonly answered = output<Answer | null>();

  readonly removed = output<void>();

  /** When true, a control marks this item's lines on the resume PDF. */
  readonly canShow = input(false);

  readonly show = output<void>();

  readonly statusLabels = STATUS_LABELS;

  readonly partLabels = PART_LABELS;

  readonly link = useLink;

  number(value: number | null): string {
    return value === null ? 'n/a' : value.toFixed(2);
  }
}
