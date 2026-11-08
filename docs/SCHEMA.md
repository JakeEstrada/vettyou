# Schema

Living reference for the data model. Flyway migrations are the actual source; keep this in sync.

## Conventions

- UUID primary keys
- `user_id` on every user-owned table
- `created_at` / `updated_at` as `timestamptz` (UTC)
- Foreign keys with explicit `ON DELETE` behavior
- Sensitive secrets stored encrypted or by reference, never in plain columns

## Invariants the schema must protect

- Claims link to a specific `source_version` and span or user field.
- `source_versions` are immutable once written.
- `tailored_resumes` rows are immutable once saved. Building again adds the next version.
- Status changes go through `application_status_events` (audited).

## Tables by milestone

| Milestone | Tables |
| --- | --- |
| M0 | `users` |
| M1 so far | `profiles`, `work_experiences`, `source_versions` (V2); `educations`, `projects`, `project_source_versions`, `evidence_claims`, `claim_decisions` (V3); `resume_uploads` (V5, the text that was read, immutable) |
| M1 still to come | `source_spans`, `skills`, `skill_aliases`, `claim_source_links`, `audit_events` |
| M2 | `approved_phrases`, `resume_templates`, `base_resume_versions`, `rendered_artifacts` |
| M3 so far | `job_postings` (pasted text, `status` reading/ready/failed, `reader`, `error`) and `job_requirements` (`posting_id` cascade, `position`, `skill`, `importance` required/preferred, `category`, `quote` copied from the posting) (V6). V7: `job_postings` gains status `matching`, `matcher`, `evidence_hash` (fingerprint of the evidence lines matched), `matched_at`. `job_requirements` gains `kind` qualification/responsibility, `logic` all/any, `answer` meets/gap, `answered_at`. New `job_requirement_parts` (`user_id`, `requirement_id` cascade, `position`, `text`, `concept`, `support` direct/transferable/none/unclear, `reason`) and `job_part_evidence` (`user_id`, `part_id` cascade, `position`, `source_kind` job/project/school, `source_id`, `line`, `excerpt` word for word from `line`). The score is computed on read, not stored. V8: `profiles` gains `email` (the address employers reply to, default empty). New `tailored_resumes` (`user_id` cascade, `posting_id` cascade, `version` unique per posting, `template` ats-first, `snapshot` JSON of what was shown and why, `plain_text` read back from the PDF, `pdf` bytes, `pdf_sha256`, `sources_hash` of every source used, `total`, `matched_at`, `created_at`); rows are never updated (ADR 0005). |
| M3 still to come | `outside_reviews`, `match_results`, `work_items` |
| M4 so far | `applications` (`user_id` cascade, optional `posting_id` set null if the posting is deleted, `title`, `company`, `location`, `pay` amount named in the posting or empty, `status` draft/applied/rejected/moving_forward/interview/offer/withdrawn/no_reply, `applied_at`, `notes`) and `application_status_events` (`user_id` cascade, `application_id` cascade, `status`, `note`) (V9; `pay` in V10). Status can be set from a confirmed mail message. |
| M5 so far | `mail_connections` (`user_id` unique cascade, `email`, `host`, `port`, `secret` AES-GCM of the IMAP password, `verified_at`) (V11). `mail_sync_cursors` (`user_id`+`folder`, `uidvalidity`, `last_uid`), `mail_messages` (folder/uid, sender, subject, snippet, quote, `category`, `review_state` pending/accepted/ignored, optional proposed application), `mail_application_links` (V12). Folder names Applications Recieved / Rejections / WINS classify the message; Job Listings folders are not synced. Status is written only after confirm. |
| M5 still to come | Richer matching, sent-mail coverage beyond the last 200, classifier versions beyond `rules-1` |
| M6 | `diagnostic_snapshots` |

Full descriptions in `PRODUCT_BLUEPRINT.md` section 11.

## ERD

_Add a Mermaid ERD here once M1 tables exist._
