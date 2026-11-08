import { Component, input, output } from '@angular/core';

import { ResumeDocument } from '../../core/api/tailored-resume-api';
import { MarkedPart, markedParts } from '../../shared/format/keywords';

export interface SheetSkill {
  label: string;
  text: string;
}

export interface SheetJob {
  title: string;
  org: string;
  subtitle: string;
  dates: string;
  bullets: string[];
}

export interface SheetSchool {
  title: string;
  subtitle: string;
  dates: string;
}

/** The words on a resume. The sheet only draws them. */
export interface SheetModel {
  name: string;
  tagline: string;
  contact: string[];
  skills: SheetSkill[];
  jobs: SheetJob[];
  projects: SheetJob[];
  schools: SheetSchool[];
}

export interface SheetHighlight {
  text: string;
  why: string;
}

export const EMPTY_SHEET: SheetModel = {
  name: '',
  tagline: '',
  contact: [],
  skills: [],
  jobs: [],
  projects: [],
  schools: [],
};

export function sheetFromDocument(document: ResumeDocument): SheetModel {
  const section = (heading: string) => document.sections.find((item) => item.heading === heading);
  const entries = (heading: string, splitOrg: boolean): SheetJob[] =>
    (section(heading)?.entries ?? []).map((entry) => {
      const split = splitOrg ? orgAndPlace(entry.subtitle) : { org: '', subtitle: entry.subtitle };
      return {
        title: entry.title,
        org: split.org,
        subtitle: split.subtitle,
        dates: entry.dates,
        bullets: [...entry.bullets],
      };
    });
  return {
    name: document.name,
    tagline: '',
    contact: [...document.contact],
    skills: (section('Skills')?.rows ?? []).map((row) => ({ label: row.label, text: row.text })),
    jobs: entries('Experience', true),
    projects: entries('Projects', false),
    schools: (section('Education')?.entries ?? []).map((entry) => ({
      title: entry.title,
      subtitle: entry.subtitle,
      dates: entry.dates,
    })),
  };
}

export function sheetText(model: SheetModel): string {
  return [
    model.name,
    model.tagline,
    ...model.contact,
    ...model.skills.flatMap((row) => [row.label, row.text]),
    ...model.jobs.flatMap((job) => [job.title, job.org, job.subtitle, job.dates, ...job.bullets]),
    ...model.projects.flatMap((project) => [project.title, project.org, ...project.bullets]),
    ...model.schools.flatMap((school) => [school.title, school.subtitle, school.dates]),
  ].join('\n');
}

@Component({
  selector: 'app-resume-sheet',
  templateUrl: './resume-sheet.html',
  styleUrl: './resume-sheet.scss',
})
export class ResumeSheet {
  readonly model = input.required<SheetModel>();

  readonly highlights = input<SheetHighlight[]>([]);

  readonly marks = input<string[]>([]);

  readonly picked = input('');

  readonly pick = output<string>();

  protected why(text: string): string {
    return this.highlights().find((item) => item.text === text)?.why ?? '';
  }

  protected choose(text: string): void {
    this.pick.emit(text);
  }

  protected parts(text: string): MarkedPart[] {
    return markedParts(text, this.marks());
  }
}

function orgAndPlace(subtitle: string): { org: string; subtitle: string } {
  const at = subtitle.indexOf(',');
  if (at < 0) {
    return { org: subtitle, subtitle: '' };
  }
  return { org: subtitle.slice(0, at).trim(), subtitle: subtitle.slice(at + 1).trim() };
}
