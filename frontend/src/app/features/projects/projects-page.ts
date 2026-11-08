import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { ProjectApi, ProjectSummary } from '../../core/api/project-api';
import { ProjectList } from '../../core/api/project-list';
import { fieldErrorsOf, messageOf } from '../../core/auth/session';
import { ReadmeDrop, ReadmeFile } from '../../shared/readme-drop/readme-drop';

@Component({
  selector: 'app-projects-page',
  imports: [RouterLink, ReadmeDrop],
  templateUrl: './projects-page.html',
})
export class ProjectsPage {
  private readonly api = inject(ProjectApi);

  private readonly router = inject(Router);

  private readonly projectList = inject(ProjectList);

  readonly projects = signal<ProjectSummary[]>([]);

  readonly loading = signal(true);

  readonly missing = signal(false);

  readonly loadError = signal('');

  readonly importing = signal(false);

  readonly importError = signal('');

  constructor() {
    this.api.list().subscribe({
      next: (projects) => {
        this.projects.set(projects);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.missing.set(true);
        } else {
          this.loadError.set(messageOf(error));
        }
      },
    });
  }

  importReadme(file: ReadmeFile): void {
    this.importError.set('');
    this.importing.set(true);
    this.api.importReadme(file.fileName, file.body).subscribe({
      next: (project) => {
        this.projectList.refresh();
        void this.router.navigate(['/projects', project.id]);
      },
      error: (error: unknown) => {
        this.importing.set(false);
        const fieldErrors = fieldErrorsOf(error);
        this.importError.set(fieldErrors.length > 0 ? fieldErrors[0].message : messageOf(error));
      },
    });
  }
}
