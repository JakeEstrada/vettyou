# Vettyou — Project Blueprint for Claude and Cursor (Angular + Spring Boot)

**Purpose:** Build a professional, evidence-grounded resume and job-application platform that creates and maintains a base resume, tailors new resume versions to job descriptions, produces reliable PDF designs, tracks applications and relevant email, and explains recurring job-market skill gaps.

**Implementation direction:** Java 21 + Spring Boot backend; Angular + TypeScript frontend; PostgreSQL; Spring Security; JPA/Hibernate; Flyway; object storage; HTML/CSS to PDF; external LLM via a server-side provider abstraction; optional IMAP mailbox connector. Docker + automated tests + GitHub Actions. Deploy backend on Render and frontend on Vercel initially. Prefer a modular monolith rather than unnecessary microservices.

**Usage:** Place this file at `docs/PRODUCT_BLUEPRINT.md` in a new Git repository. Use `CLAUDE.md` for Claude Code and `.cursor/rules/vettyou.mdc` for Cursor. Both agents should follow this document, but they should only implement the milestone you request.

**Document authority:** The original Vettyou concept governs product behavior. This document adds a concrete proposed implementation. Where a technical choice is not specified by the original concept, it is a recommendation, not an already-established fact. Do not assume any feature described here exists yet.

---

## 1. Product vision

A person enters professional experience in their own words and uploads project write-ups (usually README files). Vettyou builds an editable master resume, displays that same content through selectable resume templates, and exports a PDF. The person pastes a job description. Vettyou extracts the employer's requirements, compares them with the user's sourced experience, produces a separate tailored resume for that application, and explains its job-match score and recommendations. Each application becomes a tracked record, which can later be updated from the person's manually authorized private-email account. Reporting shows application outcomes and recurring skill gaps.

**Product promise:** The system can select, emphasize, reorganize, and faithfully rewrite supported claims. It must never invent experience, skills, credentials, responsibilities, clearances, dates, or metrics to please a job posting.

**Product boundaries:** This is not an actual employer ATS system, does not have privileged access to employers' internal rankings, cannot guarantee interviews, and should never present a score as an employer-generated outcome. It calculates its own explainable estimate of requirement coverage and parseability.

**Primary user:** A job seeker with existing professional history and project documentation. Start as a single-user experience with a real multi-user data model for later expansion.

## 2. Architecture and module ownership

```
Angular + TypeScript (Vercel)
  Profile | Projects | Resume Studio | Job Analyzer | Applications | Inbox | Insights
                         |
                      HTTPS API
                         |
Java 21 + Spring Boot (Render, modular monolith)
  auth/            Profile and account security
  evidence/        Extract, normalize, cite, and approve source facts
  projects/        Uploads, README parsing, skill evidence
  templates/       Layout definitions, rendering, print-preview
  resumes/         Base/tailored versions, editing, approval, PDF export
  jobs/            Job post parsing, normalized requirements, reviews
  matching/        Evidence alignment, explainable scoring, suggestions
  applications/    Application state, sent artifact snapshots, activity log
  mail/            IMAP connector, matching, classification, review queue
  insights/        Counts, timelines, repeated gaps, guarded recommendations
  ai/              Model/provider adapters, prompts, validation, provenance
  worker/          Durable asynchronous task execution
                         |
          PostgreSQL + S3-compatible object storage
                         |
             Optional IMAP over TLS account
                         |
          External model API (backend only)
```

Keep authorization, validation, user data, and external API credentials on the backend. Do not expose LLM API keys, IMAP credentials, or private documents to the browser or other users. All database entities should be scoped to the current user. Use UUID identifiers and UTC timestamps with timezone-aware display in the UI.

### Recommended packages
- Java: Spring Web, Validation, Security, Data JPA, Actuator; Flyway migrations; Jackson JSON; PostgreSQL driver; JUnit 5, Mockito, Spring Boot Test, Testcontainers.
- File text: Markdown/text directly, PDFBox for PDFs; POI for DOCX when implemented. File parsing is distinct from generation.
- Email: Jakarta Mail/Angus Mail with IMAP/TLS and robust MIME parsing, only after checking the user's actual provider's supported login and authentication options.
- PDF: HTML/CSS templates and headless Chromium through Playwright for Java. Use the **same HTML and CSS** for preview and PDF. If browser runtime is too costly for the first deployment, isolate the renderer behind an interface for easy substitution.
- Frontend: Angular (supported stable release), TypeScript, Angular CLI, Angular Router with lazy-loaded feature routes, standalone components, Reactive Forms, HttpClient, route guards, interceptors, and Angular CDK where useful. Use signals for local UI state and RxJS for async HTTP, streaming, and form workflows. Do not introduce NgRx without demonstrated shared-state complexity.
- Background work: use a database-backed `work_items` table and a Spring worker for retries. Begin with in-process execution if hosting permits; move to a separate worker on Render when reliable recurring jobs are required. Do not rely on long-running jobs inside a web request.

## 2A. Explicit product decisions for this build

**Decided for the initial build:** Angular rather than React; Java rather than Go for the main backend; Spring Boot modular monolith; PostgreSQL; Flyway; REST/JSON; Markdown/text README ingestion before PDF/DOCX; an explainable Vettyou match score rather than a purported employer ATS score; manual application tracking before mailbox integration; **private email via a provider-neutral connector** (do not assume Gmail).

**Not yet decided:** the user's private email hosting provider, its authentication mode, an LLM vendor, commercial billing/pricing, production custom domain, maximum document storage, and retention policy. Hide those behind interfaces and configuration rather than making guesses. Never hardcode the user's account credentials.

**Initial release boundary:** users can create accounts and professional evidence, approve claims, generate a base resume, preview ATS-first and Signature templates, paste/analyze a job, see supported and unsupported requirements, tailor/export a version, and track it manually. Mail auto-sync and cross-application analytics are *later milestones*, not release blockers.

**A note about purpose:** Vettyou is intended to be a full product, not a demonstration of every popular library. Choose technologies because they serve its domain. Java + Spring Boot and Angular are intentional additions to the developer's portfolio; the user already has React/Node experience. Do not add Go, Kubernetes, Kafka, Redis, GraphQL, microservices, or event sourcing just to make the stack look more impressive.

## 2B. Angular architecture and user experience

Build an Angular SPA with **standalone components**, typed **Reactive Forms**, **Angular Router**, and a consistent API layer. Use modern supported Angular and TypeScript versions selected from their compatibility matrix at implementation time; do not assume an unverified version. Prioritize accessibility, keyboard operation, readable validation, and desktop-first responsive layouts that still work on mobile.

Suggested Angular structure:

```text
frontend/
  src/app/
    core/                 # auth/session, interceptors, guards, API error handling
    shared/               # buttons, dialogs, tables, field components, layout primitives
    features/
      onboarding/
      profile/
      projects/
      evidence/
      resume-studio/
      jobs/
      applications/
      mailbox/
      insights/
      settings/
    app.routes.ts
    app.config.ts
  src/styles/             # SCSS, typography, tokens, reset, print-safe tokens
```

Use **signals for simple local UI state** and RxJS for HTTP, cancellation/debouncing, forms, and event streams. Use feature services for data orchestration and keep backend rules authoritative. State stores such as NgRx can be introduced only if cross-feature state complexity justifies them. Avoid duplicate representations of authoritative data across components.

**Screen behavior:**

- **Profile:** typed forms with date validation, drafts, and visible source wording. Do not silently rewrite a user's history.
- **Projects:** upload README; extraction state; source quote alongside each proposed claim; approve/edit/reject actions.
- **Resume Studio:** content editor on one side, template picker and real preview on the other, responsive to screen size. Indicate unsaved changes; do not report `Exported` as `Applied`.
- **Job Analyzer:** original posting and parsed required/preferred qualifications side by side, corrections allowed, evidence status per requirement.
- **Tailor:** show exact before/after text, selected projects and source links, score explanation, two separate recommendations (`Improve this resume` and `Skills to build`). User must approve before an export becomes the selected version.
- **Tracker:** filterable application list, status timeline, notes, which exact PDF was sent, and the score at send time.
- **Mailbox (later):** sync/connection status, review-before-applying uncertain changes, unmatched inbox, correction/undo.
- **Insights (later):** show sample size with outcome counts; no unsupported causal statements.

**Style direction:** polished but practical product UI. Build an application design system (CSS variables, spacing scale, typography, focus states, empty/error/loading states) distinct from printable resume templates. Template rendering must stay deterministic and printable. For the Signature resume preserve original tokens `#183E45` (petrol), `#B67850` (copper), and `#35636B` (steel), plus optional photo, frosted skills band, and projects on page two. Provide a plain single-column no-photo ATS-first alternative. The preview and downloaded PDF should originate from the same sanitized HTML/CSS rendering output, not separate duplicate implementations.

**API and deployment:** Angular frontend calls an authenticated Spring Boot REST API. Use environment-specific configuration for API base URLs, never put backend or AI secrets into Angular build artifacts. Prefer same-site custom subdomains for deployed frontend/API if possible; if split across origins, explicitly configure credentials, allowlisted CORS, cookie behavior, and CSRF mitigations. Set up an Angular SPA route fallback on Vercel. Auth session storage is a backend-owned security decision, not a frontend localStorage shortcut.

## 2C. Proposed repository layout and engineering workflow

```text
vettyou/
  README.md
  CLAUDE.md
  docs/
    PRODUCT_BLUEPRINT.md
    ADR/                       # architecture decision records
    API_CONTRACTS.md
    SCHEMA.md
    PROGRESS.md
  .cursor/rules/vettyou.mdc
  backend/
    pom.xml                   # Maven, or build.gradle if one is deliberately chosen
    src/main/java/.../
      auth/
      profile/
      projects/
      evidence/
      resumes/
      jobs/
      matching/
      applications/
      mail/
      insights/
      ai/
      common/
    src/main/resources/db/migration/
    src/test/
  frontend/
    angular.json
    package.json
    src/app/{core,shared,features}/
    src/styles/
  infra/
    docker-compose.yml        # local PostgreSQL
  .github/workflows/
    ci.yml
  .env.example                # placeholders only
```

Prefer feature-based Java packages with a `controller -> service/domain -> repository` separation, DTOs at API boundaries, and entities hidden from API responses. Keep business rules in services/domain logic rather than controllers or Angular components. Use Flyway migrations rather than auto-mutating production schemas.

Each milestone must produce a **working vertical slice**, not a forest of empty service interfaces. Any agent should: inspect existing files first, write a short plan, implement the narrow request, run tests, summarize changed files and decisions, and update `docs/PROGRESS.md`. Do not jump to future milestones. Protect existing manual changes.

## 2D. API contract and asynchronous job conventions

Use `/api/v1/` as an initial prefix, version DTOs and AI extraction schemas, and document endpoints in OpenAPI. Suggested response conventions: typed data payloads, field-level validation errors, consistent `401/403/404/409/422/429` behavior, pagination with stable sorting, correlation IDs in logs, and `202 Accepted` with a job ID for slow extraction/render/tailoring tasks.

An extraction/tailoring request should have an **idempotency key**, a durable work-item row, status (`queued`, `running`, `needs_review`, `completed`, `failed`), retriable errors, and an explicit timeout. The UI polls job status in v1; use SSE/WebSockets only if justified later. No AI API request should block a browser response indefinitely. Never run unattended recurring sync on an ephemeral web service without verifying its scheduler/worker guarantees.

**Authoritative flow:** uploads and pasted job descriptions are untrusted documents; Java validates and stores sources; AI proposes structured candidates with source references; Java validates the proposal and persists it as pending review; user approves; resume and matching logic reads only approved facts. A rejected claim stays rejected until the user explicitly changes it.

## 2E. Week-one implementation target and demonstrable acceptance

By the end of the first thin slice, a local developer should be able to:

1. Start a PostgreSQL instance via Docker Compose and run the Java backend with a reproducible Flyway migration.
2. Start Angular independently; navigate a working shell with Profile and Projects routes.
3. Register/log in with a secure server-owned session or a documented alternative, and retrieve `/api/v1/me`.
4. Create, edit, and reload one work experience using an Angular typed Reactive Form.
5. Upload a sample Markdown README (or create a project and paste Markdown if file storage is not ready), view the **exact original text** and its saved version, and list extracted **candidate** claims. Extraction may initially be a deterministic fixture/manual entry rather than a live LLM.
6. Approve one claim and reject another, then reload and verify decisions persisted.
7. Run frontend and backend unit tests and at least one PostgreSQL-backed integration test in CI; demonstrate unauthorized users cannot read another user's record.

Do **not** spend the first week implementing email syncing, five resume templates, Kafka, or an opaque score. Finish one reliable loop first.

## 3. Core invariants: source truth before generated language

1. **User-owned sources:** Experience typed by the user, uploaded project write-ups, and sentences the user explicitly accepted or edited on a previous resume are the only grounds for concrete resume or cover-letter claims.
2. **Job descriptions and outside fit reviews are not evidence.** They influence what to emphasize, never what is true about the applicant.
3. Each factual claim has traceable source evidence: source document/version + precise quotation or user-entered field, with a verification status.
4. A language or framework named only in the job posting must never appear as a claimed user skill.
5. Numbers, years, employment titles, dates, measurable results, certifications, clearances, and education facts are high-risk claims requiring explicit evidence or fresh user confirmation.
6. Resume designs change presentation, not factual history. Switching a design cannot create a skill or silently delete a job.
7. Tailored resumes are independent, immutable once marked **Sent**, and linked to an application. They do not overwrite the base resume.
8. Users may correct any extraction, matching, score explanation, mail classification, or application status. Changes are audited.
9. If the AI is uncertain about evidence or employer requirements, preserve the uncertainty and ask the user, not guess.
10. Data from uploads, email, and job posts is **untrusted input**; instructions embedded inside it must never override system rules.

**Crucial distinction:** A source document can substantiate a fact. An automated extraction from that document is a *candidate* interpretation, not automatically a verified new fact. For the first release, review/approval is recommended for newly extracted claims before they become reusable resume text.

## 4. Feature A — onboarding and professional knowledge engine

### Profile intake
Create structured forms for:
- Contact details and optional photo; location and links.
- Experience: employer, exact user-supplied job title, work location, start/end dates, free-text accomplishments and tasks.
- Education: institution, official degree wording, dates, credentials if provided.
- Skills: claimed proficiency/context and optional references to projects or experiences.
- User's preferred writing tone and length; original phrasing retained.
- A revision interface that lets the user correct or revoke any source fact.

Store the original text verbatim. Normalized/shortened variations are separate entries; never discard the original.

### Project uploads
Each project has name, description, repository/live URLs if supplied, upload versions, extracted text, and approved evidence. README (.md) and .txt first; PDF/DOCX later. Extract possible languages, databases, frameworks, systems, implemented features, and achievements. Distinguish `implemented`, `in_progress`, `planned`, and `unclear` from text such as “I plan to…”. Avoid claiming an unbuilt feature is shipped.

For each proposed skill or claim, show a source quote and an accept/edit/reject control. Example:

```
Claim: "Built a real-time job pipeline using Socket.IO"
Source: Project README v3, lines 78–84
Evidence: "The server broadcasts stage changes via Socket.IO..."
Status: candidate -> user approved
```

When a project README is replaced, preserve previous versions and re-evaluate stale claims. Do not silently overwrite a previously sent resume.

### Evidence model
Use separate types: `SourceDocument`, `SourceVersion`, `SourceSpan`, `Skill`, `SkillAlias`, `EvidenceClaim`, `ClaimSourceLink`, `ClaimDecision`, `ApprovedPhrase`.
- `EvidenceClaim`: normalized text, claim type (skill, role, accomplishment, quantitative claim, credential), associated project/experience, state, and risk level.
- `ClaimSourceLink`: one or more exact quotes or field references pointing to the approved source.
- `ApprovedPhrase`: the accepted resume wording and underlying claim IDs. If the user writes an entirely new fact, request confirmation that it is added to profile/experience; do not smuggle it in as a mere stylistic edit.
- `ClaimDecision`: approved, edited, rejected, retired, timestamp, user. Explicitly rejected claims should not be regenerated; a line omitted just to fit a page need not be globally blacklisted.

For v1, full-text searching over PostgreSQL plus structured claim links is sufficient. Vector embeddings and RAG are optional if the source library grows. Retrieval is useful for choosing relevant claims; it does not replace factual verification.

## 5. Feature B — base resume and resume design engine

### Master resume
Generate a generic resume from approved profile and project evidence **before any job is pasted**. Let user preview, edit, and approve. Save the base resume as a version; corrections to facts update underlying profile/claims only after explicit user confirmation. Tailored resumes start from this source-backed knowledge base, not from another application's unsupervised text.

### Content and presentation are different objects
- `ResumeContent`: sections, stable item IDs, approved phrase/claim IDs, ordering, visibility choices.
- `ResumeTemplate`: template ID/version, CSS/HTML layout, tokens for colors/typography/margins, photo rules, section placement rules, allowed page count.
- `RenderedArtifact`: content hash, template version, generated HTML/PDF, creation timestamp, PDF validation results.

**How Vettyou “knows how to design” a resume:** You implement a **template library**. The AI may choose which approved bullets to prioritize or suggest a template, but the visual design is deterministic HTML/CSS, not improvised by each LLM call. User selection is authoritative.

### Required initial templates
**Template 1 — ATS-first**: single-column, black-and-white, no photo, clear section headings, conventional reading order, selectable text, standard fonts, contact information as text, no critical information encoded as icons/shapes. Make this the default for US job applications.

**Template 2 — Signature/photo**: the already specified design: petrol header `#183E45`, copper markers `#B67850`, steel links `#35636B`, frosted skills band, square photo top left, projects on page two, two pages. Maintain its intended design faithfully, while allowing warnings that some designs are less parser-friendly.

Further templates: data-driven library. Templates may control optional photo and visual style; the underlying content is unchanged except for explicit user-edited visibility/length selections. Changing design must never add a skill or drop a user-provided job.

### Rendering and export
1. Backend validates resume content references, renders deterministic HTML/CSS.
2. Preview displays identical HTML in a protected, appropriately sandboxed viewer.
3. PDF engine prints from that HTML to letter-size PDF with configurable margins and pagination.
4. Use PDFBox to extract PDF text and check expected headings/contact information/order survived rendering. Flag overflow or content clipping; do not silently omit claims.
5. Store PDF with template version and content hash. The user can download/re-export.
6. One-page cover-letter export can use the same header design where appropriate, but is a separate artifact.

**Important:** A visually attractive PDF is not automatically ATS-friendly. Offer a clear readability warning and a plain-text extraction preview. A photo is optional, not necessary for the ATS-first template.

## 6. Feature C — job description intelligence engine

### Input and storage
Accept pasted job-description text. Store the exact posting separately from AI interpretations. Optional pasted outside review is stored as a separate document, never merged into source posting and never treated as evidence about the applicant. Create an application even if company is missing; use the job title/placeholder and ask for the employer later.

### Structured extraction
Use an LLM to propose structured JSON in an explicit schema. Validate with JSON Schema / Java DTO validation. Use deterministic regex/rule extraction as cross-checks for hard fields such as salaries, job location, years, credentials, and modality, but avoid pretending regex alone understands the job.

```json
{
  "title": "Junior Software Engineer",
  "company": null,
  "location": {"text": "Seal Beach, CA", "remotePolicy": "onsite"},
  "seniority": "junior",
  "employmentType": "full_time",
  "salary": {"min": null, "max": null, "currency": "USD"},
  "responsibilities": [
    {"text": "Maintain backend services", "sourceQuote": "..."}
  ],
  "requirements": [
    {
      "id": "req_001",
      "rawText": "Experience with Java and Spring Boot",
      "normalizedSkills": ["java", "spring-boot"],
      "importance": "required",
      "category": "technology",
      "yearsRequired": null,
      "sourceQuote": "Experience with Java and Spring Boot",
      "confidence": 0.94
    }
  ],
  "ambiguities": []
}
```

Real data must be extracted from the supplied posting; the above JSON is only an illustrative schema. Never fill `company`, `salary`, requirements or benefits without source support. Retain text quotes so the reviewer can check a classification.

### Understanding a job description
Separate five layers:
1. **Explicit required qualifications**: “must”, “required”, degree, license, clearance, experience years.
2. **Preferred qualifications**: “nice to have”, “preferred”, “bonus”.
3. **Core work**: responsibilities, systems to maintain, expected deliverables.
4. **Technical vocabulary**: languages, tools, libraries, protocols, cloud platforms, databases.
5. **Context**: domain, seniority, location, employment arrangement, pay, travel, eligibility constraints.

Define normalized skill IDs and aliases, with caution: `Java != JavaScript`; `Spring Boot != Spring Framework` as automatic proof; `React != React Native`; `C# != C++`; `SQL != SQL Server` as the same proficiency claim. Equivalence rules can suggest related knowledge but cannot assert a tool was used. Recognize alternate wording like `ASP.NET Core` and `.NET Core` with explicit context, avoiding broad automatic substitutions.

Require user review of uncertain interpretations. Jobs often contain vague wording; when the JD says “cloud experience”, do not infer AWS specifically.

### Job analysis UI
Present: title/company/seniority, top required and preferred qualifications, responsibilities, technologies, eligibility constraints, and ambiguity warnings. Every requirement links to the original job quote. Let users correct priorities or remove a false extraction, and save a parser version for repeatability.

## 7. Feature D — match, tailor, score, and explanations

### Evidence matching
For each normalized requirement, search (1) approved profile experience, (2) approved project facts, and (3) previously accepted supported phrases. Produce statuses:
- `supported_on_resume`: evidence exists and the active resume says it clearly.
- `supported_but_hidden`: evidence exists, but the active resume does not surface it.
- `partially_supported`: related evidence exists but not the exact requested qualification.
- `not_supported`: no source supports the qualification.
- `uncertain`: model cannot confidently resolve evidence or meaning.

Record source IDs and exact quotes for every supported match. Use semantic/LLM reasoning only to propose matches; deterministic checks enforce that nothing is used without traceable evidence. User corrections take precedence.

### Tailoring workflow
1. Save job posting and parser result.
2. Produce a requirement-to-evidence matrix.
3. Rank existing approved bullets by job relevance and whether they demonstrate the user's actual experience. Rank; do not invent.
4. Generate a draft with job-specific summary/tagline, selected true bullets, and project ordering. Keep typed job titles and dates unchanged. Preserve historical jobs; adapt bullets/sections within user-approved length constraints.
5. Validate each generated sentence: attached claim/source IDs, skill names, numbers, dates, job titles, citations and unsupported statements. Reject invalid output and request regeneration with reasons; do not rely solely on an LLM saying it is grounded.
6. Render preview and explain changes. User edits/approves.
7. Snapshot generated content, score, source versions and PDF; on mark-as-applied freeze the sent version and create an application status event.

### Two suggestions lists
- **Change this resume**: qualifications are verified but buried, relevant project omitted, terminology can be made clearer without overstating, different approved bullet order, parser-friendlier design.
- **Work on outside the resume**: qualification genuinely lacks evidence (e.g., Spring Boot, Kafka, healthcare interoperability). Repeated missing skills do not become claims until the user adds real experience/project evidence.

### ATS-style compatibility scoring (product's own estimator)
Do **not** claim Vettyou can run or predict a specific proprietary employer ATS. Brand it as `Job Match & ATS Readability`, optionally show a 0–100 **Vettyou estimate** with factor breakdown and the reason behind each factor. Some employers configure parsing/ranking differently; no external universal ATS score exists.

Proposed initial transparent formula:

```
Total = 50 * requiredCoverage
      + 15 * preferredCoverage
      + 15 * evidenceStrength
      + 15 * readability
      +  5 * completeness
```

All terms normalized to 0–1. Weights are product defaults to revise against usability tests, not empirically validated employer ATS weights. For each requirement, award 1 if it is clearly represented on the resume with evidence; 0.5 for genuinely partial, 0 for missing; weight strongly worded requirements more heavily inside their group. A preferred qualification cannot compensate entirely for missing required criteria. `evidenceStrength` checks whether claims demonstrate relevant work rather than keyword stuffing; `readability` includes text extractability, column/order problems, contact fields, headings, missing/clipped content; `completeness` checks sections/dates/contact. For requirements that are unmeasurable (e.g., secret clearance), only user-verified eligibility is supported, and the UI must distinguish an actual credential from a willingness/ability to obtain one.

Example: required coverage .75, preferred .50, evidence .80, readability .93, completeness 1.0 => **76/100**, with explanations. Include the unrounded factor values in internal records. Never optimize by inserting missing keywords without real source evidence.

**Separate views:** The unpaired base resume can receive a *completeness/readability check*, not a job-match score. The tailored resume receives a job-match score that reflects the specific posting and saved resume version.

### Why both semantic and deterministic methods?
An LLM can interpret “built internal web tools” as potentially relevant to backend web development, but rules must stop it from rewriting that as “5 years of Spring Boot experience.” A requirement matrix, explicit user-verified facts, and sentence-level source references supply safeguards that a single prompting step cannot.

## 8. Feature E — application tracking

Each pasted job produces an application. Default statuses: `Draft`, `Applied`, `Rejected`, `Moving forward`, `Interview`, `Offer`, `Withdrawn`, `No reply`. Store: company, title, location, pay, original posting, outside review, parsed requirements, chosen resume version and template, frozen sent PDF, score at send time, dates, notes, linked email threads, and status history.

The application list supports search, filters, sorting, kanban/list views optional, and detail page with original posting + exact sent resume + score + timeline + linked mail. User can manually change any status. A saved manual override should not immediately be undone by older/equivalent automated evidence; newer and more-specific messages may propose a change under a documented state precedence policy. Allow undo and maintain an audit log.

A `No reply` label should be user-configurable or manually applied after a defined waiting period; elapsed time is not proof of rejection.

## 9. Feature F — private-email integration and classification

The user currently uses a private email account at `contact@jakeestrada.org` for job applications, **not Gmail**. Do not assume the Gmail API. First verify the actual hosting provider and supported authentication method. Build a mailbox-provider abstraction, starting with **IMAP over TLS** if supported; a provider-specific API can later implement the same interface.

### Integration steps
1. Opt-in only; user supplies provider/server settings or completes supported OAuth authorization.
2. Keep secrets encrypted at rest and never log email bodies or credentials. Prefer provider-supported app passwords or OAuth where available. Use least privilege and TLS certificate verification. Provide disconnect and sync controls.
3. Perform incremental sync using IMAP UID and UIDVALIDITY, Message-ID, mailbox folder, and message timestamps. De-duplicate reruns and handle moved messages, pagination, and partial sync failures.
4. Parse MIME safely: text/plain preferred, sanitize HTML; handle sender, subject, date, recipients, `In-Reply-To`, `References`, attachments metadata. Do not execute content or fetch remote images automatically.
5. Match to existing applications using company identifiers/domains, role title, thread identifiers, and context. Never create a new application automatically from unrecognized marketing email. Unmatched mail goes to a review queue.
6. Classification labels: Rejected, Moving forward, Interview, Offer, or Unrelated/Unknown. Extract a supporting exact quote and confidence. Include a review-needed path when ambiguous.
7. Apply only high-confidence permissible status updates, or ask confirmation. Save status transition/event, source message ID, timestamp, classifier version. Let users override and correct labels.
8. Disconnect stops sync, removes/revokes saved credentials as appropriate, and preserves application statuses already saved unless user requests deletion.

### Mail matching and classification rules
- “We chose another candidate” -> likely Rejected (high confidence).
- “We'd like to schedule a phone screen” -> Moving forward.
- “Your interview is confirmed for…” -> Interview.
- “We'd like to offer you the role” -> Offer, but route sensitive/ambiguous cases for confirmation.
- “Thanks for applying” -> acknowledgement, **not** Moving forward by itself.
- A reply from a recruiter with no clear status -> Unknown, leave existing state unchanged.

The status policy must be monotonic only when justified by evidence; false automatic transitions harm the user's records. `MailMessage` keeps accessible message details on the application and in global views for rejections/moving-forward items.

## 10. Feature G — diagnostics and feedback into tailoring

Compute factual counts and durations:
- Applications by status and role type.
- Time from applying to first response, rejection, interview, or offer; unresolved applications displayed separately.
- Response ratios grouped by role, resume version, template, and skills emphasized.
- Requirements recurring in the **outside the resume** list, with frequencies and job source links.
- Stored score at send time alongside later status outcome.

**Important statistical guardrails:** Do not claim a template causes interviews based on a handful of applications. Display sample size, date window, and descriptive language (“Among 8 applications, 2 received a response”). Avoid automatic, opaque model retraining from tiny, confounded samples. Suggestions can rank user-approved projects and themes by observed outcomes as a soft recommendation, while the user retains control.

Treat user edits to email labels or requirement parsing as evaluation data. Store classifier versions and correction history so model regressions can be tested before changing prompts.

## 11. Proposed database schema (conceptual)

Use migrations to implement these tables with UUID PKs, `user_id` scoping, created/updated timestamps, and clear foreign keys. Detailed SQL can follow the model design:

| Table | Main purpose |
| --- | --- |
| `users` | Accounts, auth identities, settings |
| `profiles` | Contact/profile fields and preferences |
| `work_experiences` | Exact titles/dates and original text |
| `educations` | Schools/degrees/credentials |
| `projects` | Project metadata, links, lifecycle |
| `source_documents` | Uploaded document metadata and storage key |
| `source_versions` | Immutable text extraction version + digest |
| `source_spans` | Exact quote and location in a source version |
| `skills`, `skill_aliases` | Controlled vocabulary and equivalence restrictions |
| `evidence_claims` | Normalized candidate/approved factual claims |
| `claim_source_links` | Claim-to-source provenance |
| `claim_decisions` | User acceptance/rejection/correction history |
| `approved_phrases` | Allowed resume wording linked to claims |
| `resume_templates` | Template/version/config/style tokens |
| `base_resume_versions` | Master resume content snapshots |
| `job_postings` | Exact pasted posting text, extracted metadata |
| `job_requirements` | Individual requirements, quote, importance |
| `outside_reviews` | Outside analyses, stored separately |
| `applications` | Job/application metadata and current status |
| `application_status_events` | Audited manual/mail changes |
| `tailored_resume_versions` | Immutable content and evidence references per application |
| `match_results` | Score breakdown and per-requirement evidence |
| `rendered_artifacts` | PDF/HTML assets, hashes, renderer version |
| `mail_connections` | IMAP/provider metadata and protected secret references |
| `mail_sync_cursors` | Incremental UID tracking and sync state |
| `mail_messages` | Message metadata/content, classified category |
| `mail_application_links` | Match confidence, provenance, review state |
| `diagnostic_snapshots` | Aggregates with sample/window metadata |
| `work_items` | Durable asynchronous tasks and retry state |
| `audit_events` | Important source, document, status, AI decisions |

Do not store raw credentials in ordinary tables or echo them in API responses. For v1, a simplified schema is acceptable provided the core invariant of traceable claims and immutable sent versions is preserved.

## 12. Proposed API endpoints (illustrative)

```
POST   /api/auth/register
POST   /api/auth/login
GET    /api/me
GET    /api/profile
PUT    /api/profile
POST   /api/experiences
PATCH  /api/experiences/{id}
POST   /api/projects
POST   /api/projects/{id}/documents
GET    /api/projects/{id}/evidence
POST   /api/evidence/{id}/decision
POST   /api/resumes/base/generate
GET    /api/resumes/base/versions
PATCH  /api/resumes/base/versions/{id}
GET    /api/resume-templates
POST   /api/resumes/{id}/render
GET    /api/rendered-artifacts/{id}/pdf
POST   /api/jobs/analyze
GET    /api/jobs/{id}/analysis
PATCH  /api/jobs/{id}/requirements/{requirementId}
POST   /api/applications
POST   /api/applications/{id}/tailor
GET    /api/applications/{id}/match
POST   /api/applications/{id}/mark-applied
POST   /api/applications/{id}/status
GET    /api/applications
GET    /api/applications/{id}
POST   /api/mail/connections
POST   /api/mail/sync
GET    /api/mail/unmatched
POST   /api/mail/messages/{id}/attach
POST   /api/mail/messages/{id}/classification-override
DELETE /api/mail/connections/{id}
GET    /api/insights/overview
GET    /api/insights/skill-gaps
GET    /api/work-items/{id}
```

Define DTO validation, authentication, scoping, rate limits, error codes, pagination, idempotency, and asynchronous job handles before implementing these. Never serve another user's file even if a UUID is guessed.

## 13. User interface / screens

1. **Welcome and onboarding:** account, professional profile, original wording, privacy summary.
2. **My Experience:** work, education, edit/revoke facts; approved phrases.
3. **My Projects:** upload READMEs; view extracted technologies and evidence; approve/correct.
4. **Resume Studio:** base resume content, template picker, live preview, edit, PDF export, source links.
5. **Analyze Job:** pasted description, stored posting, parsed structured requirements, missing fields and corrections.
6. **Tailor Resume:** role-to-evidence matrix, proposed content changes, score breakdown, missing vs hidden skills, preview, approve/export.
7. **Applications:** filterable list with status, date, company, resume snapshot, and notes.
8. **Application Details:** posting, resume at send time, match explanations, correspondence, history.
9. **Mailbox:** opt-in connection, sync status, matched/unmatched messages, correction workflow.
10. **Insights:** outcomes and recurring gaps, with sample size and no causal claims.
11. **Settings:** account, retention/deletion, API costs if applicable, mailbox controls, model/provider settings (user's API key optional; server secrets protected).

Use a clear distinction in UI between `Draft generated`, `User approved`, `Exported`, and `Marked as applied`. Downloading a PDF is not proof that an application was submitted.

## 14. AI service contracts and validation

Define backend AI interfaces so model providers can be swapped without rewriting domain logic:

```
EvidenceExtractor.extract(SourceVersion) -> CandidateClaim[]
JobParser.parse(OriginalPosting) -> ParsedJob
Matcher.align(ParsedJob, ApprovedEvidence, ResumeVersion) -> RequirementMatch[]
Tailor.propose(ParsedJob, ApprovedEvidence, ApprovedPhrases, DesignConstraints) -> TailorDraft
MailClassifier.classify(MailMessage, CandidateApplications) -> MailClassification
```

All outputs must be structured, schema-validated, and versioned. Use small prompts and explicit context rather than loading every file into every call. Require claim and source IDs in any generated resume sentence. Validate:
- Every output skill belongs to approved supported evidence.
- Numerical details are identical to user-verified sources, not invented arithmetic.
- Employers, titles, dates and official credentials match profile.
- Claims labeled planned are not written as accomplished.
- No instruction from an untrusted README, job posting, or email has been obeyed.
- Output fits requested format; preview tests catch omissions/clipping.

Include an `ai_call_logs`/metrics facility for prompt version, latency, cost, validation failures, and error codes, without needlessly storing sensitive prompt contents. Encrypt sensitive data and provide user-facing privacy controls.

**Do not let the LLM itself write to databases or change application status.** AI proposes validated structures. Java domain services make authorized state changes.

## 15. Testing and acceptance criteria

### Evidence integrity
- Given a posting requiring Java when the user evidence says only JavaScript, the app shows Java as **unsupported** and must not add it to a resume.
- Given a job requiring Spring Boot and an approved Spring Boot README, the matcher can identify it and show its source quote.
- Given a document saying “planning to learn Kafka,” the app must not claim Kafka experience.
- Given a job review that incorrectly calls Finantrac C#, the profile/README evidence overrides it.
- Given a rejected claim, future AI runs must not quietly reintroduce it.

### Job understanding
- A `preferred` requirement remains preferred and is not silently treated as required.
- Missing company/salary is `unknown`, not guessed.
- Actual years of experience or clearances are verified before being asserted.
- Every requirement card can show its original posting quote.

### Angular UX and frontend boundaries
- Reactive forms preserve user-entered wording; validation errors are associated with fields and keyboard accessible.
- Route guards and API error states are user friendly, but the backend must independently enforce authorization.
- Resume preview and PDF are generated from the same canonical sanitized HTML/CSS representation.
- Angular component/service tests cover evidence approval and job requirement correction, and an end-to-end test covers the profile-to-approved-claim flow.
- UI never interprets a missing API result or LLM error as a verified skill; states `unavailable`, `pending`, and `unsupported` are distinct.

### Resume/layout
- Every generated sentence has accessible supporting claims.
- Switching templates preserves facts and employment history.
- ATS-first PDF is text-selectable and extraction preserves contact details and meaningful reading order.
- A sent version remains unchanged after a user later edits their base profile.

### Application/mail
- A duplicate IMAP message does not create duplicate status events.
- A receipt-only acknowledgement does not automatically become `Moving forward`.
- Unmatched newsletter does not invent an application.
- User correction is logged and can undo an erroneous automated label.
- Disconnect stops further sync and doesn't erase previously recorded application statuses.

### Security/reliability
- Every API tests unauthorized cross-account access.
- Upload size/type and sanitization constraints enforced.
- AI provider downtime cannot corrupt profile data.
- Background job retries use idempotent writes.
- CI runs unit and integration tests, including Testcontainers/PostgreSQL.

## 16. Security and privacy baseline

Because profiles and mail contain personally identifiable information:
- Passwords hashed with a modern one-way password hash; avoid storing raw passwords.
- Sessions/cookies or tokens handled securely (HttpOnly, Secure, SameSite as appropriate); CSRF protections when relevant.
- File ownership checks on read and download; user-specific S3 object namespaces plus signed/short-lived access when applicable.
- IMAP TLS, provider-approved auth, encrypted connection secrets, strict permissions, never log mailbox bodies in plaintext diagnostics.
- Rate limit AI requests, upload endpoints and auth; budget ceilings and timeouts.
- Detect malicious uploads and sanitize parsed HTML. Prevent prompt injection through source documents.
- Provide user export/deletion and retention settings, and a clear policy for stored AI data and sensitive documents.
- Never use applicant photo, name, or demographic indicators as suitability or matching factors.

## 17. Development plan — build in vertical slices

### Milestone 0: Repository and engineering skeleton
- `backend/` Java 21 Spring Boot Gradle/Maven, layered modules, PostgreSQL local Docker Compose, Flyway, smoke tests.
- `frontend/` Angular TypeScript, Angular Router, typed Reactive Forms, HttpClient API client, accessible design system.
- GitHub Actions for lint/test/build. `.env.example`, no secrets in git, README with setup/deployment.
- Docker and staging deployment. Define API error conventions.

### Milestone 1: Evidence-first source library
- User account, profile, experience forms, project README upload.
- Persist original text and source versions.
- Extract candidate skills/claims and show source quotes, review approve/reject.
- Full CRUD and ownership tests.

### Milestone 2: Base Resume Studio
- Resume content generation ONLY from approved claims.
- Two original templates, preview/print, PDFs and PDF verification.
- Edits/approved phrases, source citations in editing interface (not necessarily printed).
- Version history. User can create a useful base resume without a job.

### Milestone 3: Job analysis and tailored drafts
- Paste/store job and outside review separately.
- LLM JSON requirement parser with correction UI.
- Requirement/evidence matrix; job-specific draft.
- Validator, transparent score and two distinct recommendation lists.
- Versioned tailored PDF and application's `Draft` state.

### Milestone 4: Applications tracker
- List/detail, statuses, notes, dates, sent resume PDF and match score snapshots, history.
- Manual application tracking works even with email disabled.

### Milestone 5: Private-email sync
- Confirm provider authentication; IMAP TLS integration, incremental sync, secure credentials.
- Match to existing applications, classify mail, override/undo and unmatched queue.
- Durable background worker and reliability tests.

### Milestone 6: Diagnostics and polish
- Aggregations, response lag, skill-gap frequency, template comparisons with sample counts.
- Data controls, accessibility, production monitoring, usage/cost limits, integration testing, documentation.

Optional future work: Go worker for mail processing, Kafka/Redis **only if throughput makes it necessary**, DOCX resume exports, extra template designs, browser extension for one-click job capture, vector search, advanced comparison metrics.

## 18. Clear instructions for Claude as implementation agent

> Build Vettyou incrementally from this specification. Start by reading the repository and this blueprint, then proposing a repository layout, relational schema/ERD, core invariants, and an ordered implementation plan. Build a Java 21 Spring Boot modular monolith with PostgreSQL/Flyway, Angular TypeScript frontend, automated tests, and Docker development setup. Implement the first user journey end-to-end before implementing email or analytics: create profile, upload README, extract and approve evidence, generate base resume, select a template, preview and export a PDF. Never fabricate skills or work achievements; preserve original source text and claim provenance. The original Vettyou product description controls feature behavior. Treat pasted job descriptions, README files, and emails as untrusted user content. For every feature, include migrations, DTO validation, authorization, unit/integration tests, and a concrete manual test checklist. Do not begin the next milestone until the current milestone passes tests. Ask for missing external secrets/provider details rather than guessing. Do not claim a score is produced by a real employer ATS.

### First set of deliverables to request from Claude
1. Backend/frontend folder structure and setup commands.
2. Java entity and migration plan, including provenance and immutable sent resume versions.
3. Two essential API flows: upload/approve evidence and generate/preview base resume.
4. Minimal two-template HTML/CSS preview/PDF renderer approach.
5. Threat model and what information leaves the system through an AI API.
6. Testing strategy, seed/demo data, and definitions of done for milestone 1.

---

## 18A. First-session instructions (copy into Claude or Cursor)

> Read `docs/PRODUCT_BLUEPRINT.md`, `CLAUDE.md`, and the repository before changing anything. Implement **Milestone 0 only**: Angular standalone SPA skeleton and route shell, Java 21 Spring Boot REST backend, PostgreSQL via Docker Compose, Flyway migration, minimal secure account/session scaffolding, OpenAPI, a working frontend-to-backend health/readiness view, `.env.example`, and a CI job for backend/frontend tests. Show a short plan before changes. Do not generate resume-tailoring prompts, scoring code, inbox sync, payment logic, or speculative microservices yet. Make real code and tests, run available checks, document what is untested, and record progress in `docs/PROGRESS.md`. No placeholder success claims. When complete, stop and report exact commands and the smallest next implementation step.

The next session should implement **profile CRUD and source version persistence**, not the entire product in a single response. Require the next milestone's acceptance criteria before moving on.

## 19. Product decisions to confirm before production

These are not blockers for the architecture, but should be explicit rather than assumed:
- Exact private-mail hosting provider and IMAP/OAuth/app-password support.
- Whether resume generation requires a model API in every use or supports a deterministic, no-AI first draft.
- Pricing/business model, user quotas, and per-user AI budget.
- Whether uploaded PDF/DOCX project write-ups are in first release or later.
- Exact template print dimensions and font licensing/availability.
- Email retention and deletion behavior; whether raw email body remains stored or only selected metadata/quotes.
- What “accepted sentence updates the profile” means for edits to meaning versus edits to tone.
- Scoring formula adjustments after comparing with real job descriptions and human review.

**Guiding rule:** Vettyou's competitive advantage is explainability and trust. A visibly supported 72/100 with actionable evidence is more valuable than an opaque 96/100 achieved by fabricating keywords.
