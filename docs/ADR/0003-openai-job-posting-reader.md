# ADR 0003: Read job postings with OpenAI, checked by Java

- **Status:** Accepted
- **Date:** 2026-11-07

## Context

A pasted job posting is a wall of text. The person wants every skill, tool, degree, and experience requirement pulled out, marked required or nice to have, and compared with their evidence. A fixed list of tech names catches tools but misses most requirements ("3+ years building REST APIs", "Bachelor's in CS or related field", "on-call rotation"). The user chose OpenAI.

## Decision

- Provider: OpenAI Chat Completions with a strict JSON schema (`response_format: json_schema`). Model comes from `AI_MODEL` (currently `gpt-5-mini`); key from `OPENAI_API_KEY` (or `AI_API_KEY`). Both live only in the backend `.env`. Nothing reaches the Angular build.
- No SDK. Spring's `RestClient` sends one HTTPS request. No new dependency.
- The model only proposes `{title, company, requirements[{skill, importance, category, quote}]}`. Java then:
  - drops any requirement whose `quote` is not found in the posting (after collapsing whitespace and quote/dash styles),
  - drops a requirement whose skill shares no word with its quote, unless the fixed tech list finds that skill in the quote,
  - keeps `title` and `company` only if they appear in the posting,
  - checks enums, trims lengths, merges duplicates (required beats preferred), caps the list at 80,
  - runs the fixed tech list over the posting and adds any tech the model missed (cross-check).
- The posting is untrusted. The system prompt says so, the posting is sent inside `<posting>` tags, and the validation above means an obeyed instruction still cannot add text that is not in the posting.
- Reading is slow work: `POST /api/v1/jobs` saves the posting and returns `202` with the posting id and `status: reading`. A background thread calls the model, validates, and saves. The page polls `GET /api/v1/jobs/{id}`. Postings left `reading` by a restart are re-queued on startup.
- With no key configured, the same endpoint reads with the fixed tech list only and says so (`reader: rules`).
- Matching and the score are deterministic Java over the person's own data. No profile, project, or job write-up text is sent to OpenAI in this step.
- Tests never call OpenAI: the test profile blanks the key, and integration tests mock the client.

## Consequences

- Each posting costs one model call. Re-reading is explicit (`POST /api/v1/jobs/{id}/read`).
- If OpenAI is down or returns invalid output, the posting is marked `failed` with a plain message and can be retried. The rules reader is not silently substituted when a key is set.
- No `ai_call_logs` table yet. Latency and validation drops are logged (counts only, no posting text).
