# Vettyou

**Evidence-grounded resume builder and job application tracker.**

Vettyou turns your real experience and project write-ups into a source-backed master resume, tailors it to each job posting without inventing anything, exports clean PDFs, and tracks every application from draft to offer.

For a resume-oriented brief of the stack, architecture, and skills this repo demonstrates, see [`Vettyou.md`](Vettyou.md).

> A visibly supported 72/100 with real evidence beats an opaque 96/100 built on fabricated keywords.

---

## Table of contents

- [What it does](#what-it-does)
- [Core principles](#core-principles)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Repository structure](#repository-structure)
- [Getting started](#getting-started)
- [Environment variables](#environment-variables)
- [Development workflow](#development-workflow)
- [Roadmap](#roadmap)
- [Documentation](#documentation)
- [Status](#status)

---

## What it does

| Feature | Description |
| --- | --- |
| **Evidence library** | Enter experience in your own words, upload project READMEs, approve or reject extracted claims with source quotes |
| **Resume Studio** | Generate a base resume from approved evidence only, switch templates, preview and export PDF |
| **Job Analyzer** | Paste a job posting, extract required vs preferred qualifications with quotes from the original text |
| **Tailoring** | Build a job-specific resume from true bullets, with an explainable match score and two recommendation lists |
| **Application tracker** | Track status, notes, the exact PDF sent, and the score at send time |
| **Mailbox connect** | IMAP (Namecheap Private Email). Applied/rejected is saved only after you confirm a message |
| **Insights** *(later)* | Outcome counts and recurring skill gaps, always with sample sizes |

## Core principles

- **Never fabricate.** Skills, titles, dates, metrics and credentials must trace to user-owned sources.
- **Job postings are not evidence.** They shape emphasis, never facts about the applicant.
- **Every claim has provenance.** Source document, version and exact quote.
- **AI proposes, Java decides.** The LLM never writes to the database or changes status directly.
- **Design changes presentation only.** Switching templates never adds or drops facts.
- **Sent resumes are immutable.** Tailored versions are frozen once marked as applied.
- **Untrusted input stays untrusted.** Instructions inside READMEs, postings or emails are ignored.
- **Honest scoring.** The Vettyou score is our own estimate, never presented as a real employer ATS result.

## Tech stack

| Layer | Choice |
| --- | --- |
| Frontend | Angular (standalone components), TypeScript, Reactive Forms, Signals, RxJS, SCSS |
| Backend | Java 21, Spring Boot (modular monolith), Spring Security, Spring Data JPA |
| Database | PostgreSQL, Flyway migrations |
| PDF | Apache PDFBox: import resume text and generate the ATS-first PDF (read back for scoring) |
| AI | OpenAI Chat Completions (`gpt-5-mini`) via Spring `RestClient`; Java validates every quote |
| Mail | Jakarta/Angus Mail over IMAP/TLS; status writes wait for user confirm |
| Testing | JUnit 5, Mockito, Testcontainers PostgreSQL, Angular (Vitest) |
| CI/CD | GitHub Actions, Docker Compose for local Postgres |
| Hosting | Vercel (Angular at www.vettyou.com). Java API is local until Render is on |

## Architecture

```
Angular SPA (Vercel: www.vettyou.com)
  Home | Resume | Jobs | Applications | Email | Scores
                              |
                          HTTPS / REST  (/api/v1)
                              |
Spring Boot modular monolith (local today)
  auth  profile  projects  evidence  templates  resumes  jobs
  matching  applications  mail  ai  common
                              |
                     PostgreSQL (Flyway)
                              |
          OpenAI (server side)   Optional IMAP/TLS
```

Request flow for anything AI-driven:

```
Untrusted source -> Java validates + stores -> AI proposes candidates
  -> Java validates proposal -> pending review -> user approves -> usable fact
```

## Repository structure

```
vettyou/
├── README.md
├── CLAUDE.md                        # Rules for Claude Code
├── .cursor/rules/vettyou.mdc        # Rules for Cursor
├── .env.example                     # Placeholders only, never real secrets
├── .gitignore
├── docs/
│   ├── PRODUCT_BLUEPRINT.md         # Full product spec (source of truth)
│   ├── PROGRESS.md                  # Milestone log, updated every session
│   ├── API_CONTRACTS.md             # Endpoint and error conventions
│   ├── SCHEMA.md                    # Data model and invariants
│   └── ADR/                         # Architecture decision records
├── backend/
│   └── src/
│       ├── main/java/com/vettyou/
│       │   ├── auth/                # Accounts, sessions, security config
│       │   ├── profile/             # Contact info, experience, education
│       │   ├── projects/            # Projects and README uploads
│       │   ├── evidence/            # Claims, sources, spans, decisions
│       │   ├── templates/           # Resume template registry
│       │   ├── resumes/             # Base and tailored versions, rendering, PDF
│       │   ├── jobs/                # Job postings and parsed requirements
│       │   ├── matching/            # Evidence alignment and scoring
│       │   ├── applications/        # Application state and history
│       │   ├── mail/                # IMAP connector and classification
│       │   ├── insights/            # Aggregates and skill gaps
│       │   ├── ai/                  # Provider adapters, prompts, validation
│       │   ├── worker/              # Durable background work items
│       │   └── common/              # Errors, IDs, auditing, shared utils
│       ├── main/resources/
│       │   ├── db/migration/        # Flyway SQL migrations
│       │   └── templates/resume/    # ats-first/, signature/
│       └── test/java/com/vettyou/   # Mirrors main packages
├── frontend/
│   └── src/
│       ├── app/
│       │   ├── core/                # auth, interceptors, guards, api
│       │   ├── shared/              # ui primitives, layout
│       │   └── features/            # onboarding, profile, projects, evidence,
│       │                            # resume-studio, jobs, applications,
│       │                            # mailbox, insights, settings
│       └── styles/                  # Tokens, typography, reset
├── infra/
│   └── docker-compose.yml           # Local PostgreSQL
├── samples/
│   ├── readmes/                     # Sample project READMEs for testing
│   └── job-postings/                # Sample job descriptions for testing
└── .github/workflows/               # CI (added in Milestone 0)
```

Each backend module follows `controller -> service -> repository`, with DTOs at the API boundary and entities never returned directly.

## Getting started

### Prerequisites

- Java 21 (this machine uses Temurin in `~/.local/jdks/temurin-21`)
- Node.js 22
- Docker
- The Maven wrapper in `backend/` (`./mvnw`). A separate Maven install is not required.

### 1. Clone and configure

```bash
git clone <your-repo-url> vettyou
cd vettyou
cp .env.example .env
```

### 2. Start PostgreSQL

```bash
docker compose --env-file .env -f infra/docker-compose.yml up -d
```

### 3. Start the API

```bash
cd backend
./mvnw spring-boot:run
```

### 4. Start the site

```bash
cd frontend
npm install
npm start
```

The dev server proxies `/api` to `http://localhost:8080`, so the browser stays on port 4200.

### 5. Verify

- Site: `http://localhost:4200`
- API health: `http://localhost:8080/api/v1/health` returns `{"status":"ok"}`

### Tests

```bash
cd backend && ./mvnw test
cd frontend && npm test
```

GitHub Actions runs those same checks, plus `npm run build`, on every push and pull request (`.github/workflows/ci.yml`). The backend tests start their own Postgres with Testcontainers, so they do not use the compose database.

## Environment variables

See `.env.example`. Never commit real values.

| Variable | Purpose |
| --- | --- |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Local database |
| `SPRING_DATASOURCE_URL` | JDBC URL for the backend |
| `APP_FRONTEND_ORIGIN` | Allowed CORS origin |
| `AI_PROVIDER` / `AI_API_KEY` | LLM provider (backend only) |
| `STORAGE_*` | S3-compatible object storage |
| `MAIL_ENCRYPTION_KEY` | Encrypts stored IMAP passwords. Not the mailbox password |

## Development workflow

This repo is built to be driven by an AI coding agent (Cursor or Claude Code), one milestone at a time.

1. Agent reads `docs/PRODUCT_BLUEPRINT.md`, the rules file, and `docs/PROGRESS.md`.
2. Agent proposes a short plan before touching code.
3. Agent implements only the requested milestone as a working vertical slice.
4. Tests run. No placeholder success claims.
5. Agent updates `docs/PROGRESS.md` with what changed and the next smallest step.

**Branching:** `main` stays deployable. Work on `feat/<milestone>-<topic>` branches.
**Commits:** Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `chore:`).
**Migrations:** New Flyway file per change, never edit an applied migration.

## Roadmap

- [x] **M0** Repo skeleton, Spring Boot + Angular shells, Postgres, Flyway, auth scaffold, CI
- [x] **M1** Evidence library: profile, experience, README upload, claim review *(in use)*
- [x] **M2** Resume Studio: master resume, ATS-first PDF, import *(in use)*
- [x] **M3** Job analysis, tailoring, explainable Vettyou estimate *(in use)*
- [x] **M4** Application tracker *(in use)*
- [x] **M5** Private email sync over IMAP/TLS with confirm-before-status *(in use locally)*
- [ ] **M6** Insights, polish, hosted API, data controls

Details and acceptance criteria live in `docs/PRODUCT_BLUEPRINT.md` section 17.

## Documentation

| Doc | Purpose |
| --- | --- |
| [`Vettyou.md`](Vettyou.md) | Resume brief: stack, skills, suggested bullets |
| [`docs/PRODUCT_BLUEPRINT.md`](docs/PRODUCT_BLUEPRINT.md) | Full spec and source of truth |
| [`docs/PROGRESS.md`](docs/PROGRESS.md) | Running milestone log |
| [`docs/API_CONTRACTS.md`](docs/API_CONTRACTS.md) | API conventions |
| [`docs/SCHEMA.md`](docs/SCHEMA.md) | Data model |
| [`docs/ADR/`](docs/ADR) | Architecture decisions |

## Status

Local product is past M0: evidence library, resume PDF, job match, tailored resume, application tracker, and confirmed IMAP mail. Frontend is on Vercel; the API is still local. Details that are safe to put on a resume are in [`Vettyou.md`](Vettyou.md).

---

Built by Jake Estrada.
