import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./shared/layout/shell').then((m) => m.Shell),
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () => import('./features/home/home-page').then((m) => m.HomePage),
      },
      {
        path: 'health',
        loadComponent: () => import('./features/health/health-page').then((m) => m.HealthPage),
      },
      {
        path: 'register',
        loadComponent: () => import('./features/auth/register-page').then((m) => m.RegisterPage),
      },
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
      },
      {
        path: 'resume',
        loadComponent: () => import('./features/resume/resume-page').then((m) => m.ResumePage),
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/profile/profile-page').then((m) => m.ProfilePage),
      },
      {
        path: 'experience',
        loadComponent: () => import('./features/experience/experience-page').then((m) => m.ExperiencePage),
      },
      {
        path: 'experience/:id',
        loadComponent: () => import('./features/experience/job-page').then((m) => m.JobPage),
      },
      {
        path: 'projects',
        loadComponent: () => import('./features/projects/projects-page').then((m) => m.ProjectsPage),
      },
      {
        path: 'projects/:id',
        loadComponent: () => import('./features/projects/project-page').then((m) => m.ProjectPage),
      },
      {
        path: 'education',
        loadComponent: () => import('./features/education/education-page').then((m) => m.EducationPage),
      },
      {
        path: 'education/:id',
        loadComponent: () => import('./features/education/school-page').then((m) => m.SchoolPage),
      },
      {
        path: 'skills',
        loadComponent: () => import('./features/skills/skills-page').then((m) => m.SkillsPage),
      },
      {
        path: 'applications',
        loadComponent: () => import('./features/applications/applications-page').then((m) => m.ApplicationsPage),
      },
      {
        path: 'mailbox',
        loadComponent: () => import('./features/mail/mailbox-page').then((m) => m.MailboxPage),
      },
      {
        path: 'scores',
        loadComponent: () => import('./features/scores/scores-page').then((m) => m.ScoresPage),
      },
      {
        path: 'jobs',
        loadComponent: () => import('./features/jobs/jobs-page').then((m) => m.JobsPage),
      },
      {
        path: 'jobs/:id',
        loadComponent: () => import('./features/jobs/job-match-page').then((m) => m.JobMatchPage),
      },
      {
        path: 'jobs/:id/report',
        loadComponent: () => import('./features/jobs/job-report-page').then((m) => m.JobReportPage),
      },
      {
        path: 'jobs/:id/resume',
        loadComponent: () => import('./features/jobs/tailored-resume-page').then((m) => m.TailoredResumePage),
      },
      {
        path: 'account',
        loadComponent: () => import('./features/auth/account-page').then((m) => m.AccountPage),
      },
    ],
  },
];
