# ADR 0005: Tailored resume built by Java, drawn with PDFBox, read back and scored again

- **Status:** Accepted
- **Date:** 2026-11-07

## Context

Piece 3: build a one-page resume for one posting from the person's own saved data, show why each line is on it, and
score it again with readability. The blueprint's rules apply in full:

- Nothing is invented. Every line comes from a saved line or profile field.
- A posting is never evidence about the person.
- Templates change presentation only.
- Saved versions never change.
- The score is a "Vettyou estimate", not an employer ATS score.

## Decision

### Picking lines (no AI)

- **Candidates:** confirmed lines only. Job write-up lines are the person's own words and count as confirmed. Posting
  text is never a candidate. A project line counts as confirmed from its claims' current status, planned claims aside:
  - A line holding a README bullet counts once the bullet is kept, even if a skill named in it is not reviewed yet.
    It is gone once the bullet is marked "Not mine", even if a skill on it was kept.
  - A line that only names skills (a stack line) counts once each skill on it that is not "Not mine" is kept.
  - The match (ADR 0004) reads lines the same way, so keeping a bullet can raise the evidence score.
- **Notes never count:** a line that reads as a note to oneself is never printed and never covers anything. That is
  two or more ` · ` or ` | ` separators ("Own practice · site.com · Lead Developer"), or a line reporting what another
  resume says ("Older resumes also say ...", "An older resume also calls this ...").
- **Order:** greedy coverage over the match from ADR 0004.
  - Each requirement part is a slot worth its share of the score.
  - An "any one" item is one slot. Other items get one slot per part.
  - A line's gain is the share of the posting it covers that the lines above it do not: direct 1, transferable 0.5.
  - The line with the largest gain goes next. Ties go to the better-worded line (ADR 0004 quality), then to the
    order the person wrote them.
  - Job titles and schools count first, because every resume prints them.
- **Never changed:** lines are never shortened or reworded. A line over 400 characters stays off as "too long".
- **Projects:** each project is also ranked on its own lines alone, as if no job showed anything. A project that shows
  what a job already shows can still go on, because a reader looks for projects too.
  - Its share is the part of the posting its own lines show. Ties go to the project with more such lines, then by name.
  - Its lines are ordered the same greedy way, within the project.
- **To confirm:** unconfirmed project lines are listed (up to 12), never printed, when they would add coverage or are
  among the first two lines of one of the three projects that show the most of the posting.

### Skills section

- Only kept skills (approved, or named in a job write-up).
- A skill takes the posting's spelling only when it is the same product: an exact match after ignoring case, spaces,
  dots, and dashes, or a fixed alias list (Postgres/PostgreSQL, k8s/Kubernetes, ...).
  - Angular and AngularJS, .NET and ASP.NET Core, and Agile and Scrum stay apart.
  - A mention inside a longer one ("SQL" in "SQL Server") does not count.
- Skills the posting names come first. A skill the person does not have is never added; it goes on the "work on"
  list instead.

### Fitting one page

Every job and school is printed, newest first: current jobs first, then by end date. Then, each only while the page
still fits:

1. Lines that cover the posting, best first.
2. A first line for each job that has none, so no job prints empty.
3. Up to three projects that show the most of the posting, two lines each, in the order they rank.
4. The other kept skills.
5. A second line for each job.

A job is filled with lines that still show something the posting asks for first, then the best-worded.

Lines that do not fit are listed with a reason: did not fit on one page, not needed for this posting ("Already shown
by other lines" when it does back the posting), too long, or reads as a note.

### Rendering (template `ats-first`)

- **Layout:** PDFBox, already a dependency, so no new library. One column, US Letter.
- **Fonts:** Helvetica (Standard 14, WinAnsi), so the PDF has real text an ATS can read.
- **Content order:** name, a contact line, then SKILLS, EXPERIENCE, PROJECTS (only projects with a printed line, in
  the order they rank), and EDUCATION.
- **Characters:** a character Helvetica cannot draw prints as `?` and fails a check. Non-breaking spaces, special
  dashes, and zero-width characters are normalized first.
- **Preview:** a PNG of page 1, rendered from the same PDF bytes, so the preview cannot differ from the file.
- **Why not HTML/CSS with headless Chrome:** it would add a browser to the server for one page. Its text order in
  the PDF also depends on the CSS.

### Reading back

The PDF's text is extracted again with PDFBox (sorted by position), and 8 checks run on it:

1. The text can be read.
2. The name comes first.
3. The email is on it.
4. The phone is on it.
5. Standard headings, each on its own line, in order.
6. Every line reads back in order.
7. It fits on one page.
8. Every character prints.

Readability is the share of checks passed. Each failed check becomes a suggestion that says how to fix it.

### Scoring the resume

- The same Java scorer as ADR 0004, but only the resume's own lines count: the printed bullets, job titles,
  schools, and each skills row. A skills row can show a technology but is not rated for strength.
- A part backed by saved lines that did not make it onto the resume reads "In your evidence, not on this resume"
  (`hidden`) and earns nothing.
- **Readability weighs 15 × coverage,** like evidence strength, so a clean layout cannot carry a job the person
  does not fit. In the live check, the nursing posting scored 2 with readability at 0.875.
- The response also carries the evidence-page score from the same moment. Readability only counts on a resume, so
  a resume can score a point or two above the evidence page.
- "I meet this" answers still count on the resume, because those items (years, licenses, eligibility) are usually
  asked on the application form, not read from the resume.
- The label stays "Vettyou estimate", with a note that it is not an employer's ATS score.

### Versions

- Each build is a new row in `tailored_resumes`: version number, template, a JSON snapshot of what was shown and
  why, the read-back text, the PDF bytes and their SHA-256, a hash of every source it used, the score, and the
  match time. Rows are never updated (`@Immutable`).
- A version is marked out of date, and stays as it was built, when:
  - the posting was matched again,
  - the posting is being read again or failed,
  - or the profile, jobs, projects, schools, skills, or answers it used changed.
- Building again makes the next version. Two builds at the same moment get `409` instead of a duplicate number.

### Profile email

`profiles.email` is the address employers reply to. It can differ from the login. The resume import fills it, and
the profile page edits it. Application readiness gains an "Email" check (7 checks).

## Consequences

- No model call: a build costs nothing and picks the same lines from the same data.
- The PDF bytes differ between builds of identical data, because PDFBox writes a document id. The SHA is stored per
  version and checked by the download test.
- The read-back uses PDFBox's own text extraction. Real ATS parsers differ; the checks cover the common failure
  modes (columns, missing contact details, odd headings, unprintable characters), not any one vendor.
- Stored AI judgments cite at most three lines per part, so "hidden" is only detected for lines the matcher cited or
  the rules found.
- A project line can repeat what a job above it shows. That is the cost of ranking projects on their own lines; it
  uses at most six lines of the page, and every job keeps at least one line first.
- A project goes on for any match the matcher made, transferable included. In the live check, the nursing posting
  printed one client portal line the matcher judged a transferable match for "Document patient care accurately".
  The fitter does not second-guess the matcher; the line's backing is shown under "What's on it and why".
- **Not built yet:**
  - Project entries print the name only. The repo and live links are not printed.
  - The person cannot pin or drop a line for one resume. Editing the job write-up or project is the only control.
  - A README feature line can be a weak bullet when it is the only line covering a part.
  - Choosing a version as "sent" (and locking it to an application) waits for the application tracker (M4).
