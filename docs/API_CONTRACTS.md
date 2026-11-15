# API Contracts

Conventions for the Vettyou REST API. Endpoint details get added here as each milestone ships. OpenAPI (springdoc) is the generated reference.

## Basics

- Base path: `/api/v1`
- JSON only, `camelCase` fields
- UUID identifiers, ISO 8601 UTC timestamps
- Every resource is scoped to the authenticated user

## Errors

```json
{
  "status": 422,
  "code": "VALIDATION_FAILED",
  "message": "Request has invalid fields",
  "fieldErrors": [{ "field": "startDate", "message": "must be before endDate" }],
  "correlationId": "c1b2..."
}
```

| Status | Meaning |
| --- | --- |
| 400 | Malformed request |
| 401 | Not authenticated |
| 403 | Authenticated but not allowed |
| 404 | Not found or not owned by caller (do not leak existence) |
| 409 | Conflict (duplicate, immutable resource, stale version) |
| 422 | Validation failed |
| 429 | Rate limited |

## Pagination

`?page=0&size=20&sort=createdAt,desc` with stable sorting. Response wraps `items`, `page`, `size`, `totalItems`.

## Async work

Slow tasks (extraction, tailoring, rendering, mail sync):

1. Client sends request with `Idempotency-Key` header.
2. Server returns `202 Accepted` with `{ "workItemId": "..." }`.
3. Client polls `GET /api/v1/work-items/{id}`.
4. Status: `queued`, `running`, `needs_review`, `completed`, `failed`.

## Endpoints

### Health

`GET /api/v1/health` — public. `200 {"status":"ok"}`.

### Accounts

Session cookie. The browser also gets an `XSRF-TOKEN` cookie and sends it back as `X-XSRF-TOKEN`.

| Method | Path | Auth | Result |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/register` | public | `201` `{ "id", "email" }` and a session. `409 EMAIL_TAKEN` if the email exists. `422` for a bad email or a password outside 8–72 characters. |
| POST | `/api/v1/auth/login` | public | `200` `{ "id", "email" }` and a session. `401 UNAUTHENTICATED` with message “Email or password is incorrect”. |
| POST | `/api/v1/auth/logout` | public | `204`. Clears the session. |
| GET | `/api/v1/me` | session | `200` `{ "id", "email" }` for the signed-in account only. `401 UNAUTHENTICATED` when there is no session. |

Emails are stored lowercase. The response never includes the password hash.

### Resume import

Signed-in only. The file is untrusted text. Lines are copied under their headings. Instructions inside the file are ignored. A second upload does not duplicate a job, project, or school the account already has.

| Method | Path | Result |
| --- | --- | --- |
| POST | `/api/v1/resumes/import` | Body `{fileName, body, encoding}`. `encoding` is `text` or `base64`. `201` `{profile, jobs, projects, schools, skipped}`. Each added row is `{id, label}`. Profile fields that the file states are saved; blank fields on the file leave the current profile value. Jobs, projects, and schools are created from Experience, Projects, and Education headings. `422` for a file that is not `.txt`, `.md`, `.markdown`, or `.pdf`, a PDF that is not base64, a password-protected or empty PDF, or text over 200,000 characters. |

An entry starts at a line that ends in a date or date range (`Title, Company    May 2025 – Present`). Lines under it that are not bullets, such as `City · site.com · note`, stay with that entry; for a job they are kept in its write-up. Wrapped sentences from a PDF are joined back into one line. In Education, each line that ends in a date is its own school, and `A.A.`, `B.S.`, `Associate`, `Bachelor`, etc. become the degree with the rest as the field of study.

A month is stored as the 1st of that month. A year with no month is not turned into a date; the original line stays on the job, or on the school's field of study when the school has no other field.

### Profile and experience

Signed-in only. Another account's job id returns `404 NOT_FOUND`.

| Method | Path | Result |
| --- | --- | --- |
| GET | `/api/v1/profile` | `200` name, email, phone, location, website, linkedin. Empty strings when nothing is saved yet. `email` is the address employers reply to; it can differ from the login. |
| PUT | `/api/v1/profile` | `200` the saved profile. `422` when the name is missing or the email is not an address. |
| GET | `/api/v1/experiences` | `200` list of the caller's jobs. |
| POST | `/api/v1/experiences` | `201` the job, including version 1 of the words they typed. |
| GET | `/api/v1/experiences/{id}` | `200` that job and every saved version of the words. |
| PUT | `/api/v1/experiences/{id}` | `200`. A changed write-up adds a new version. Version 1 is never overwritten. `422` when the end date is before the start date. |
| DELETE | `/api/v1/experiences/{id}` | `204`. Removes the job and every saved version of the words. |

### Education

Signed-in only. Another account's id returns `404 NOT_FOUND`.

| Method | Path | Result |
| --- | --- | --- |
| GET | `/api/v1/educations` | `200` list of the caller's schools. |
| POST | `/api/v1/educations` | `201` the saved entry. `422` when the school is missing or the end date is before the start date. |
| PUT | `/api/v1/educations/{id}` | `200` the saved entry. Same `422` rules. |
| DELETE | `/api/v1/educations/{id}` | `204`. |

### Projects and README stack

Signed-in only. Another account's project or claim id returns `404 NOT_FOUND`. The README is untrusted text. It is only matched against a fixed list of tech names, never followed.

| Method | Path | Result |
| --- | --- | --- |
| GET | `/api/v1/projects` | `200` list of `{id, name, summary}`. |
| POST | `/api/v1/projects/import` | Body `{fileName, body}`. `201` a new project built from the README: name from the first `#` heading (else the file name), summary from the first paragraph, version 1 of the README, and candidate claims for stack and built features. Same `422` rules as `/sources`. |
| GET | `/api/v1/projects/{id}` | `200` project, every README version, and every claim. |
| PUT | `/api/v1/projects/{id}` | Body `{summary, repoUrl, liveUrl}`. `200`. The name is not editable here; it comes from the README. |
| DELETE | `/api/v1/projects/{id}` | `204`. Removes the project, every README version, and every claim and decision on it. |
| POST | `/api/v1/projects/{id}/reread` | No body. `200` the project. Runs the current parser over every saved README version and adds any claims not already on the project. Existing decisions are untouched; no new version is created. |
| POST | `/api/v1/projects/{id}/sources` | Body `{fileName, body}`. Saves the text as the next README version, renames the project if the README has a `#` title, and adds a `candidate` claim for each tech name or bullet not already seen on this project. Claims the user already approved or rejected keep that status. `422` for a file name not ending in `.md`, `.markdown` or `.txt`, a body over 200,000 characters, or a body containing NUL. |
| POST | `/api/v1/evidence/{claimId}/decision` | Body `{decision: "approved" \| "rejected"}`. `200` the claim. Every decision is also recorded in `claim_decisions`. |

A claim is `{id, kind, name, category, quote, lineNumber, sourceVersionId, planned, status}`. `kind` is `skill` (a tech name; `category` is language, framework, etc.) or `feature` (a bullet point describing what was built; `category` is the README section it sat under). `quote` is the exact README line (up to 1,000 characters). `planned` is true when the line or any heading above it reads like a roadmap or todo, or the bullet is an unchecked `[ ]` item. Every bullet outside code blocks becomes a feature, in README order, under its deepest heading (`README` when there is none). Table-of-contents links and duplicate bullets are skipped. Tailoring, not extraction, picks which bullets go on a resume.

### Skills

Signed-in only. Built on the fly from the caller's own data; nothing new is stored.

| Method | Path | Result |
| --- | --- | --- |
| GET | `/api/v1/skills` | `200` list of `{name, category, status, places, uses}`. `status` is `kept` (an approved project claim, or the name appears in one of the caller's job write-ups), `review` (only candidate claims), `planned` (only roadmap lines), or `rejected`. `places` counts distinct projects, jobs, and schools. Each use is `{kind: project \| job \| school, id, label, quote, status}`; `quote` is the exact line. Sorted kept, review, planned, rejected, then by places. |
| POST | `/api/v1/skills/decision` | Body `{name, decision: "approved" \| "rejected"}`. `200` the updated skill list. Approves every candidate claim with that skill name, or rejects every claim with it that is not already rejected. Each change is recorded in `claim_decisions`. `404` when the caller has no claim with that name. `422` for a blank name or another decision value. |

### Job match

Signed-in only. Another account's posting or requirement id returns `404 NOT_FOUND`. The posting is untrusted text; it is never evidence about the user, and instructions inside it are ignored (ADR 0003). Matching sends the user's job, project, and school lines (not contact details) to OpenAI; every quote it returns is checked word for word (ADR 0004).

| Method | Path | Result |
| --- | --- | --- |
| POST | `/api/v1/jobs` | Body `{body}` (the pasted posting, up to 60,000 characters). `202` `{id, status: "reading", ...}`. Reading and then evidence matching run in the background; poll `GET /jobs/{id}`. `422` for an empty body. `429 RATE_LIMITED` when 3 of the caller's postings are already reading or matching. |
| GET | `/api/v1/jobs` | `200` list of `{id, title, company, pay, preview, status, createdAt}`, newest first. `title` is the role named in the posting (not the company, never a placeholder). `pay` is copied from a salary/compensation/pay line when the posting names an amount; otherwise empty. `preview` is the start of the posting text. |
| GET | `/api/v1/jobs/{id}` | `200` `{id, title, company, pay, preview, status, reader, matcher, matchedAt, evidenceChanged, error, body, createdAt, requirements, score}`. `status` is `reading`, `matching`, `ready`, or `failed` (with a plain `error`). `reader` is `openai:<model>` or `rules` when no key is set. `matcher` is `openai:<model>`, `rules`, or empty for postings read before evidence matching. `evidenceChanged` is true when the user's saved lines changed after `matchedAt`. Requirements and score are empty until `ready`. Stored judgments are re-checked against the current lines on every GET, and the score is recomputed. |
| POST | `/api/v1/jobs/{id}/read` | `202`. Reads the saved text again, replaces the requirements (answers are cleared), and matches again. `409 CONFLICT` while it is reading or matching. `429` as above. |
| POST | `/api/v1/jobs/{id}/match` | `202`. Matches the saved requirements against the user's current evidence again. Answers are kept. `409` while reading or matching. `429` as above. |
| PUT | `/api/v1/jobs/{id}/requirements/{requirementId}/answer` | Body `{answer: "meets" \| "gap" \| null}`. `200` the updated posting. `gap` ("I don't have this") is allowed on any item. `meets` is allowed only when `canMarkMet` is true (items text cannot settle, such as years or a license); otherwise `422` on `answer`. `null` clears the answer. |
| GET | `/api/v1/jobs/{id}/report.txt` | `200` `text/plain` report for sharing: score, each factor with its weight and value, both formulas, the crediting rules, every requirement with its status, weight, credit, strength, each part with its label, reason, and cited lines (and whether each line is confirmed), and the posting quote, then the posting text. `409` until the posting is `ready`. |
| GET | `/api/v1/jobs/{id}/report.pdf` | `200` `application/pdf` (inline, `vettyou-job-match-report.pdf`) with the same lines. Characters Helvetica cannot draw print as `?`. `409` until `ready`. |
| GET | `/api/v1/jobs/{id}/posting.md` | `200` `text/markdown` attachment `job.md` with the saved posting text. Available as soon as the paste is stored. `404` for another account. |
| DELETE | `/api/v1/jobs/{id}/requirements/{requirementId}` | `200` the updated posting without that requirement ("not a real requirement"). |
| DELETE | `/api/v1/jobs/{id}` | `204`. Removes the posting, its requirements, and its tailored resumes. |

A requirement is `{id, skill, kind, importance, group, category, quote, logic, status, note, answer, answeredAt, canMarkMet, weight, credit, creditIfConfirmed, strength, parts}`.

- `kind` is `qualification` or `responsibility`. `importance` is `required` or `preferred`. `group` is `required`, `responsibility`, or `preferred`.
- `quote` is copied from the posting; the reader drops any item whose quote is not found in it.
- `logic` is `any` (one part is enough, "A or B") or `all`.
- `status`:
  - `direct`: direct evidence.
  - `partial`: transferable or partial evidence.
  - `unconfirmed`: the support is only in project lines not confirmed yet.
  - `unclear`: needs clarification, such as years, a license, or eligibility.
  - `not_shown`: not evidenced in the available material.
  - `gap`: the user said "I don't have this" (an explicitly confirmed gap).
  - `met`: the user said "I meet this".
  - `hidden`: only on a tailored resume. Saved lines show it, but none of them is on the resume.
- `weight` and `credit` are what the group uses. Both are null while the item waits for an answer.
- `strength` rates the lines behind the credit, or null without credit.

A part is `{text, concept, support, status, reason, weight, sharedBy, evidence}`.

- `support` is the stored judgment: `direct`, `transferable`, `none`, or `unclear`.
- `status` is what counts now: `direct`, `transferable`, `unconfirmed`, `not_shown`, `unclear`, `changed` (a cited line was edited away), or, on a tailored resume, `hidden` (backed by a saved line the resume left off).
- `reason` says why, and for transferable parts what is not shown.
- `weight` is the part's share of its group after repeated ideas share one weight; `sharedBy` counts the items asking for the same concept.
- Each cite in `evidence` is `{kind: job \| project \| school \| skills, id, label, excerpt, line, confirmed}`. `excerpt` is word for word from `line`. `skills` cites a row of a tailored resume's skills section.

`score` is `{total, unrounded, totalIfConfirmed, unroundedIfConfirmed, factors, formula, formulaIfConfirmed, toConfirm, toAnswer, readiness, label: "Vettyou estimate", note}`.

- Each factor is `{key, label, weight, activeWeight, activeWeightIfConfirmed, value, valueIfConfirmed, how}`. Values run from 0 to 1 and carry 4 decimals.
- A factor with nothing to score has a null value and drops out of the divisor:
  - Nice to have, when the posting marks nothing preferred.
  - Readability, on the match page. It is only scored on a tailored resume.
- Weights:

  | Factor | Weight |
  | --- | --- |
  | Required qualifications | 50 |
  | Responsibilities | 15 |
  | Nice to have | 15 |
  | Evidence strength | 15 x coverage (see below) |
  | Readability | 15 x coverage, tailored resumes only |

- `total = 100 × Σ(activeWeight × value) / Σ activeWeight`, rounded half up.
  - Coverage is the coverage groups combined, to 4 decimals.
  - The formula string shows every term, the divisor, the unrounded total, and the scaled weights, so it can be added up by hand.
- `totalIfConfirmed` counts unconfirmed project lines as confirmed. `toConfirm` counts those parts, and `toAnswer` counts items waiting for an answer.
- `readiness` is `{value, checks}` for profile completeness (name, email, phone, location, a job, job dates, education). It is shown next to the score and is not part of it.
- The score is never presented as an employer ATS score.

### Tailored resumes

Signed-in only. Another account's posting or resume id returns `404 NOT_FOUND`, including a resume id under the caller's own posting. Built by Java from the caller's confirmed lines; no model call (ADR 0005). Versions are never changed after they are saved.

| Method | Path | Result |
| --- | --- | --- |
| POST | `/api/v1/jobs/{postingId}/resumes` | No body. `201` the new version (below). `409` while the posting is reading or matching, after it failed, or when another build saved the same version number at the same moment. |
| GET | `/api/v1/jobs/{postingId}/resumes` | `200` list of `{id, version, createdAt, total}`, newest first. |
| GET | `/api/v1/jobs/{postingId}/resumes/{id}` | `200` the version exactly as built, with `stale` and `staleReasons` worked out now. |
| GET | `/api/v1/jobs/{postingId}/resumes/{id}/resume.pdf` | `200` `application/pdf`, inline, `<Name>-resume.pdf` (`resume.pdf` without a name), `Cache-Control: private, max-age=3600`. The bytes match `pdfSha256`. |
| GET | `/api/v1/jobs/{postingId}/resumes/{id}/preview.png` | `200` `image/png` of page 1 at 100 DPI, rendered from the same PDF. Same cache header. |

A version is `{id, postingId, version, template: "ats-first", createdAt, postingTitle, postingCompany, stale, staleReasons, score, evidenceTotal, evidenceTotalIfConfirmed, requirements, readability, checks, lines, leftOff, toConfirm, skills, skillsLeftOff, suggestions, workOn, document, plainText, pages, pdfSha256, highlights}`.

- `score` and `requirements` use the job match shapes above, counting only what the resume shows. `evidenceTotal` is the match page's score when the version was built.
- `checks` are the 8 read-back checks `{key, label, passed, detail}`: `text`, `name`, `email`, `phone`, `headings`, `order`, `pages`, `characters`. `readability` is the share passed.
- `lines` are the printed bullets in the order they were picked: `{kind: job \| project, sourceId, label, text, gain, backs, reason}`. Lines that cover the posting come first, then a first line for each job, then up to three projects with two lines each, then a second line for each job. `gain` is the share of the posting the line adds (0 for a filler line or a project line a job already shows). Each of `backs` is `{requirement, part, group, support}`.
- `leftOff` uses the same shape with `reason` `space` (did not fit on one page), `not_needed`, `too_long`, or `note` (reads as a note to oneself, such as "Own practice · site.com · Lead Developer" or "Older resumes also say ...", so it is never printed). A `not_needed` line with `backs` is one that other printed lines already show. `toConfirm` lists unconfirmed project lines that would add coverage or would put one of the projects that show the most of the posting on the resume, with `reason` `confirm`.
- `skills` and `skillsLeftOff` are `{name, category, shown, asked, places}`. `shown` is the posting's spelling when it is the same product, otherwise the saved name. `asked` is true when the posting names it.
- `suggestions` are `{kind: space \| too_long \| confirm \| skills \| readability, text, line, sourceKind, sourceId}`, changes to the user's own data that would let the next version show more.
- `workOn` are `{requirement, part, group, status: not_shown \| changed \| gap \| unclear, reason}`, what the posting asks for that the resume does not claim.
- `document` is what was drawn: `{name, contact, sections: [{heading, rows: [{label, text}], entries: [{title, dates, subtitle, bullets}]}]}`. `plainText` is the text read back out of the PDF.
- `highlights` are the printed bullets that count toward the estimate, with boxes on the first-page PNG (`preview.png`, 850×1100 at 100 DPI, origin top-left): `{text, why, boxes: [{x, y, width, height}]}`. `why` names the posting parts that line shows. Empty when no scored line is on page 1.
- `staleReasons` (plain sentences) are given when the posting was matched again, is being read again, failed, or when the profile, jobs, projects, schools, skills, or answers the version used changed.

### Applications

Signed-in only. Another account's application id returns `404 NOT_FOUND`. Status will later come from the mailbox; the UI does not take a manual status. Mail does not write these rows yet.

| Method | Path | Result |
| --- | --- | --- |
| GET | `/api/v1/applications` | `200` the caller's applications, newest created first. |
| POST | `/api/v1/applications/history` | Body `{jobs:[{company, jobTitle, location, workArrangement, salaryMin, salaryMax, salaryCurrency, salaryUnit, employmentType, jobDescription, technicalRequirements, preferredQualifications, source, descriptionCompleteness}]}`. `200` `{imported, skipped, applications}`. Copies only stated titles, companies, salaries, and posting text into a ready posting plus a `draft` application. Skips missing titles, `reference only` rows, and duplicates. Does not run a read/match and does not change the profile. Another account cannot see the created rows. `422` when `jobs` is empty or longer than 100. |
| POST | `/api/v1/applications` | `201` the saved row and its first status event. `422` when the title is missing or `status` is not one of the values below. A `postingId` that is not the caller's posting returns `404`. |
| PUT | `/api/v1/applications/{id}` | `200`. A changed status adds an event. Same `422` / `404` rules. |
| DELETE | `/api/v1/applications/{id}` | `204`. |

An application is `{id, postingId, title, company, location, pay, status, appliedAt, notes, createdAt, estimate, events}`. `title` and `company` are copied from the posting. `pay` is the amount named in the posting, or empty — never invented. `estimate` is the latest Vettyou estimate for the linked posting, or null. It is not an employer ATS score. `status` is `draft`, `applied`, `rejected`, `moving_forward`, `interview`, `offer`, `withdrawn`, or `no_reply`. Each event is `{id, status, note, createdAt}`.

### Mailbox

Signed-in only. One mailbox per account. The password is tested with IMAP login, then stored encrypted. It is never returned. Reading mail does not change application status until the person confirms a message.

| Method | Path | Result |
| --- | --- | --- |
| GET | `/api/v1/mail/connection` | `200` `{connected, email, host, port, verifiedAt}`. When none is saved, `connected` is false and `email` is empty. |
| POST | `/api/v1/mail/connections` | Body `{email, password, host?, port?}`. Default host `mail.privateemail.com`, port `993`. `201` the connection without `password`. `422` when login is rejected (`password`) or the host cannot be reached (`host`). `503 MAIL_NOT_READY` when `MAIL_ENCRYPTION_KEY` is missing. A second connect replaces the saved mailbox after a successful login. |
| DELETE | `/api/v1/mail/connection` | `204`. Idempotent when none is saved. Another account's mailbox is not visible or removable. Stored messages for that account are removed. Saved application statuses stay. |
| POST | `/api/v1/mail/sync` | Lists IMAP folders, then reads Applications received, Rejections, WINS, INBOX, and Sent (up to 200 newest). Skips Job listings (Indeed, Glassdoor, Monster) — those are ads, not applications. Folder name wins over phrase rules. `200` `{fetched, pending, messages}`. Does not write application status. `404` when no mailbox is connected. |
| GET | `/api/v1/mail/messages` | `200` pending messages for the caller, newest first. Another account sees none. |
| POST | `/api/v1/mail/messages/{id}/accept` | Body `{applicationId}` optional. Confirms the message. If `applicationId` is missing, uses the proposed match or creates an application from the subject and sender. Sets status from the classification. `200` the application. `404` when the message is not the caller's. |
| POST | `/api/v1/mail/messages/{id}/ignore` | `204`. Leaves application status unchanged. |

A pending message is `{id, sentAt, fromName, fromAddress, subject, snippet, quote, folder, section, category, reviewState, proposedApplicationId, proposedTitle, proposedCompany}`. `section` is `received`, `rejections`, `wins`, `listings`, or `other`. `quote` is copied from the message. Titles and companies are taken from a matched application or from the sender/subject, never invented.
