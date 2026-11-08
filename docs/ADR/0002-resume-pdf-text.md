# ADR 0002: Read uploaded resumes as text, including PDF

- **Status:** Accepted
- **Date:** 2026-11-07

## Context

People keep resumes as plain text, Markdown, and PDF. The app needs the words on the page so it can copy contact details, jobs, projects, and education into the evidence library. PDF is a binary file; the JDK cannot read it.

## Decision

- Accept `.txt`, `.md`, `.markdown`, and `.pdf` on `POST /api/v1/resumes/import`.
- Use Apache PDFBox only to copy selectable text out of a PDF. The parser then runs on that text.
- Do not run PDF JavaScript, follow links, or store the original bytes. Store the text that was read.
- Word files are out of this step. The page tells the person to export PDF or paste the text.
- No model call. Splitting is deterministic and copies lines; it does not invent titles, employers, or dates.

## Consequences

- A scanned PDF with no text layer imports nothing useful. The API returns a field error and the page asks for a paste.
- PDFBox is the one new library. A future format (DOCX) needs its own decision.
