# Vettyou — project brief for resume writing

This file is the source of truth for **what Vettyou actually is**, so another model can add a **project section** and **skills** to Jake Estrada’s resume without inventing anything.

**How to use this file (for an AI):**

1. Read this whole file before writing.
2. Use only facts stated here. Do not add metrics, user counts, revenue, scale claims, or libraries that are not listed.
3. Do not list Kafka, Redis, Kubernetes, GraphQL, NgRx, microservices, or event sourcing. They were deliberately not used.
4. Do not call the score an employer ATS score. It is a **Vettyou estimate**.
5. Do not claim the Java API is hosted in production. The Angular UI is on Vercel; the API runs locally.
6. Prefer the sections **Resume-ready bullets** and **Skills this project demonstrates**. Merge skills into existing groups; do not invent proficiency levels (expert / advanced) unless the resume already uses them.
7. Keep Jake’s facts. Vettyou never invents skills, titles, dates, metrics, credentials, or clearances. Neither should you.

**Author:** Jake Estrada  
**Role on this project:** sole builder (product, backend, frontend, schema, tests, local ops, frontend deploy)  
**Repo:** `https://github.com/JakeEstrada/vettyou`  
**Live UI:** `https://www.vettyou.com` (Angular only)  
**Local app:** `http://127.0.0.1:4200` + API on port 8080  

---

## One sentence

Vettyou is an evidence-grounded resume and job-application product: it stores the person’s own words and project READMEs, tailors a one-page PDF to a pasted job without inventing a line, explains a Vettyou estimate of coverage, tracks applications, and can connect a private mailbox so status changes are confirmed by the user.

## Problem it solves

Most resume tools stuff a posting’s keywords onto the page. Vettyou does the opposite. Job postings and outside reviews are **not** evidence about the person. Every printed line comes from a saved job write-up, project claim, school record, or profile field. Extracted claims stay **candidates** until the person approves them. Rejected stays rejected. A sent tailored resume does not change later.

---

## Stack (as built)

Use these names and versions. Do not upgrade them in resume copy.

| Layer | What is actually in the repo |
| --- | --- |
| Language | Java 21 (Temurin), TypeScript 5.9 |
| Backend | Spring Boot **4.1.2** modular monolith, Spring Web MVC, Spring Security, Spring Data JPA, Bean Validation, Actuator |
| Persistence | PostgreSQL **16**, Flyway (12 versioned migrations), UUID primary keys, UTC timestamps, `user_id` on every user-owned table |
| API | REST/JSON under `/api/v1`, field-level 422 errors, 202 + work id for slow reads |
| Frontend | Angular **21** standalone components, Reactive Forms, Signals, RxJS, lazy feature routes, SCSS tokens |
| Auth | Backend-owned session cookie (not JWT in localStorage), CSRF, ownership checks |
| PDF | Apache PDFBox **3.0.5** — import selectable text from uploaded PDFs, and **draw** the ATS-first one-page resume (not Playwright, not a separate HTML print path) |
| AI | OpenAI Chat Completions, model `gpt-5-mini`, **no vendor SDK**. Spring `RestClient` + strict `json_schema`. Key only on the server |
| Mail | Jakarta Mail / Eclipse Angus Mail, IMAPS to Namecheap Private Email (`mail.privateemail.com:993`). Stored IMAP password encrypted with AES-GCM (`MAIL_ENCRYPTION_KEY`) |
| Tests | JUnit 5, Mockito, Spring Boot Test, Testcontainers PostgreSQL; Angular tests on Vitest/jsdom |
| CI | GitHub Actions: `./mvnw test` and `npm test` + `npm run build` (Node 22) |
| Local ops | Docker Compose PostgreSQL, Maven wrapper, `ng serve` proxy `/api` → `:8080` |
| Hosting | Vercel for the Angular SPA (`www.vettyou.com`, Node 22, SPA rewrites). Java API + Postgres not hosted yet (Render was paused) |

**Explicitly not in the stack:** Go, Kafka, Redis, Kubernetes, GraphQL, microservices, event sourcing, NgRx.

---

## Architecture

Modular monolith, not a service forest.

```
Angular SPA
  Home | Resume | Jobs | Applications | Email | Scores | Profile | Projects | Education
           |
      HTTPS /api/v1  (session cookie)
           |
Spring Boot packages: auth, profile, projects, evidence, templates, resumes,
  jobs, matching, applications, mail, ai, common
           |
PostgreSQL (Flyway)
           |
OpenAI (server only)     optional IMAP/TLS
```

Layering on the API: **controller → service → repository**. DTOs at the boundary. JPA entities never leave the service layer.

Slow work (read a posting, match evidence) runs off the request thread on virtual threads, returns **202**, and the page polls. Work left `reading` or `matching` is re-queued on API startup.

### How AI is allowed to run

```
Untrusted posting or README
  → Java stores the raw text
  → OpenAI proposes structured JSON (title, requirements, or evidence judgments)
  → Java validates: quote must appear in the source, enums, caps, no invented tools
  → User approves claims; Java persists status
```

The model never writes to the database and never changes a status. If OpenAI is down after a posting was already read, matching falls back to the deterministic tech-list matcher so a resume can still be built.

---

## What exists in the product today

These are working vertical slices, not empty interfaces.

**Accounts and profile.** Register / log in. Contact, jobs, education, projects. Import a master resume as `.txt`, `.md`, or `.pdf` (PDFBox copies the text layer; no model). Project README upload; extracted feature/skill claims are candidates until kept or rejected.

**Resume studio.** Master resume from confirmed lines. ATS-first one-page PDF (petrol/copper Signature tokens exist in the design system). Tailored versions are immutable once saved.

**Job loop.** Paste a posting → background OpenAI read (or rules if no key) → Java drops any requirement whose quote is not in the posting → evidence match (OpenAI, then Java citation check) → explainable **Vettyou estimate** with factors, not an employer ATS score. Copper marks on the PDF preview show which printed bullets counted.

**Applications.** Tracker with title, company, pay, posting, status. Import from a local job-history JSON. Opening a title shows the posting markdown; the score links to the full match.

**Email.** Connect IMAP, encrypt the mailbox password on the server, sync recent messages, classify with phrase rules plus folder names. **Applied / rejected is written only after the user confirms a message.** Job listings in the mailbox are skipped.

**Home.** Signed-out landing (product animation). Signed-in dashboard of the master resume and recent jobs.

**Quality bar that is actually in the tests.** Cross-account 404s on user-owned endpoints. Quote-must-appear validation for AI output. PDF read-back after generate. Frontend form and feature-service tests.

---

## What is not done (do not claim on a resume)

- Production Java API and Postgres (Render paused)
- Insights / skill-gap analytics
- Object storage (S3) for original files
- DOCX import
- Durable `work_items` table / separate worker process (in-process virtual threads today)
- Session store that survives API restart (in-memory sessions)
- Billing
- Reading mailbox mail older than the latest 200

---

## Design decisions worth naming

| Decision | Why it matters on a resume |
| --- | --- |
| Angular + Spring Boot on purpose | Portfolio step into that stack while keeping a product, not a demo of extra infrastructure |
| AI proposes, Java validates | Safety pattern for LLM output: schema, quote checks, user approval |
| Flyway-only schema | One new migration per change; never edit an applied file |
| PDFBox for both import and generate | Same library family; generated PDF text is read back and scored |
| Score labeled “Vettyou estimate” | Honest product copy; no fake ATS |
| ADRs in `docs/ADR/` | Stack, PDF text, OpenAI reader, evidence matching, tailored resume, IMAP |
| Untrusted input | READMEs, postings, and emails are treated as data; embedded instructions are ignored |

---

## Skills this project demonstrates

Copy these into a skills section. Group names can match the rest of the resume. Do not add tools that are not here.

**Languages:** Java 21, TypeScript, SQL, HTML, SCSS

**Backend:** Spring Boot, Spring Security, Spring Web MVC, Spring Data JPA, Bean Validation, REST APIs, Flyway, PostgreSQL, UUID data model, session cookies, CSRF

**Frontend:** Angular (standalone components), Reactive Forms, Signals, RxJS, lazy-loaded routes, accessible forms (labels, keyboard, visible focus, reduced motion)

**Documents / PDF:** Apache PDFBox (text extraction and PDF generation), layout-aware highlight boxes on a preview PNG

**AI integration:** OpenAI Chat Completions, structured JSON schema, server-side `RestClient`, prompt/untrusted-input isolation, Java-side validation of model output, fallback when the model is unreachable

**Email:** IMAPS, Angus Mail, AES-GCM encryption of stored mailbox credentials, human-in-the-loop status writes

**Testing / quality:** JUnit 5, Mockito, Testcontainers, ownership tests, Angular unit tests, GitHub Actions CI

**Delivery:** Docker Compose, Maven Wrapper, Vercel static Angular deploy, Git, architecture decision records

**Product / domain:** evidence provenance, claim approval workflow, explainable matching, immutable sent artifacts

---

## Resume-ready bullets

Use or lightly edit. Do not inflate.

- Built Vettyou, an evidence-grounded resume and application tracker, as a Java 21 / Spring Boot modular monolith with an Angular 21 SPA and PostgreSQL (Flyway).
- Constrained OpenAI (`gpt-5-mini`) to structured JSON; Java keeps a requirement or citation only when its quote appears in the source, so the model cannot invent skills, titles, dates, or metrics.
- Generated one-page ATS-first PDFs with PDFBox, read the PDF text back to check parseability, and overlaid copper marks on the bullets that contribute to a labeled Vettyou estimate (not an employer ATS score).
- Modeled every user-owned row with UUID keys and `user_id` scoping, and added cross-account access tests so one account cannot read another’s jobs, mail, or resumes.
- Connected Namecheap Private Email over IMAPS, stored the mailbox password encrypted, and required explicit confirmation before writing Applied or Rejected on an application.
- Deployed the Angular UI to Vercel at www.vettyou.com and ran the API locally against Dockerized PostgreSQL, with GitHub Actions running Maven and Angular tests on each push.

Shorter one-liner if space is tight:

- Vettyou — Angular 21 + Spring Boot 4 resume product that tailors a PDF from sourced evidence, scores coverage as a Vettyou estimate, and tracks applications with optional IMAP confirm.

---

## Suggested resume placement

**Project title:** Vettyou  
**Subtitle:** Evidence-grounded resume tailoring and application tracking  
**Stack line:** Java 21, Spring Boot, Angular, TypeScript, PostgreSQL, Flyway, PDFBox, OpenAI (server-side), IMAP  

If the resume already lists Java, TypeScript, Angular, PostgreSQL, React, or Node from other work, **add** from this project: Spring Boot, Spring Security, Flyway, PDFBox, IMAP, structured LLM validation, Testcontainers, Vercel. Do not delete prior skills.

---

## Pointers (not required to copy onto a resume)

| Doc | What it is |
| --- | --- |
| `docs/PRODUCT_BLUEPRINT.md` | Full product spec |
| `docs/PROGRESS.md` | Session log of what shipped |
| `docs/ADR/` | Why Angular/Spring, PDFBox, OpenAI, IMAP |
| `README.md` | How to run the repo |

Built by Jake Estrada.
