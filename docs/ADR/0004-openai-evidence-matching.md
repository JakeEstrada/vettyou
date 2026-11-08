# ADR 0004: Match requirements to evidence with OpenAI, scored by Java

- **Status:** Accepted
- **Date:** 2026-11-07
- **Supersedes:** the line in ADR 0003 that says no profile, project, or job write-up text is sent to OpenAI.

## Context

An audit of a 37/100 result (Crossing Hurdles, "Strategic Project Lead, Coding/SWE") found the keyword matcher could not
do the job:

- Requirements read by OpenAI are whole sentences ("Strong understanding of software engineering and coding
  fundamentals"). The matcher needed every word of the sentence, in order, in one line of evidence, so sentences
  almost never matched, even when job write-ups plainly showed the work.
- "A or B" was split with a regex that left junk alternatives, so either side could fail to count.
- A soft-skill fallback matched on a single last word ("teams", "workflows", "consistency") and produced coincidences.
- Responsibilities were scored as hard requirements, one bullet at a time, so one missing domain (coding-data
  pipelines) was penalized in several bullets.
- Evidence strength rose with repetition (the same project in three sections) and re-penalized missing items that
  coverage had already counted.
- Profile completeness (phone, location) sat inside the fit score.
- Project lines the person had not confirmed were ignored entirely, with no way to see what confirming them would do.

The person chose to let OpenAI read their saved evidence for matching, with Java checking every citation.

## Decision

### Reading the posting (extends ADR 0003)

Each item keeps its posting sentence and adds:

- `kind`: `qualification` (what the candidate must have) or `responsibility` (what the job does).
- `parts`: the separate things the item asks for (1 to 6), each with a `concept` key so the same capability in two
  items can be recognized.
- `logic`: `any` when one part is enough ("technical projects or data workflows"), otherwise `all`.

Java keeps a part only when it shares a word with the item's quote or names a listed technology in it. Title and
company are checked against the posting. A title equal to the company is dropped, and a `Position:`/`Role:` line is
used instead.

Java also cleans the parts:

- A part made only of filler words ("strong relevant experience", "proven track record") is dropped, and so is an
  item whose name is only filler.
- A verb alone takes the object of a sibling part. "Assess", "plan", "evaluate nursing care" become "Assess nursing
  care", "plan nursing care", "evaluate nursing care".
- Concept keys are spaced and lower-cased ("task_generation" and "taskGeneration" become "task generation").

### Matching (new)

- **Sent to OpenAI:** the posting's job title and employer, the requirements and parts, plus numbered evidence lines:
  - job titles and job write-up lines, labeled with employer and dates
  - project lines that are not rejected or planned, including ones not yet confirmed
  - schools (credential, field, institution)
- **Not sent:** name, email, phone, location, links, uploaded files.
- **Returned by the model:**
  - `work`: one sentence on what this job's work is and who it is for, written before any judgment so that domain
    words ("coding data pipelines") are read in context. It is logged and not stored.
  - Per part, in this order:
    - up to 3 citations, each `{line, excerpt}`
    - `differs`: what the part asks for that the cited lines do not show, or "none"
    - `support`: `direct`, `transferable`, `none`, or `unclear`
    - a one-sentence `reason`
- **What Java checks before anything is saved:**
  - The part and line ids exist. Part ids are read with or without their letter ("R1.2", "1.2", "Part (R1.2)").
  - Each excerpt appears word for word in the cited line (case, spacing, and quote styles ignored) and is at least
    2 words long.
  - `direct` with a stated difference becomes `transferable`, and the difference is added to the reason as
    "Not shown: ...".
  - `direct` or `transferable` with no valid citation becomes `none`.
  - A part the model skipped becomes `unclear`.
  - **Metrics rule:** a part that asks for metrics, KPIs, measurement, or data-driven work keeps only the cited
    lines that state a measured result or say the person used metrics. With none left it becomes `none`.
    Building a metrics page or dashboard shows the feature, not its use. This rule also runs on stored judgments.
  - A part that is just a listed technology is cross-checked with the fixed list. Lines that name it are added as
    direct evidence.
- **Stored:** judgments go in `job_requirement_parts` and `job_part_evidence`, together with a fingerprint of the
  evidence lines. The same data gives the same score until the person asks for a re-check.
- **Checked again on every read:**
  - Each stored excerpt must still be in the current line. Edited or removed lines drop out.
  - Whether a project line counts as confirmed comes from the claim's current status.
  - When the evidence fingerprint changes, the page offers a re-check (`POST /api/v1/jobs/{id}/match`).
- **Untrusted input:** job posts and evidence are both untrusted. They are sent inside tags with an
  ignore-instructions rule. The model cannot change a status, write to the database, or add text that is not already
  in the person's data.
- **No key:** the deterministic matcher judges parts when no key is set, and for postings read before this change. It
  is the same matcher as before, without the single-word soft-skill fallback.

### Scoring (Java only)

- **Part credit:**
  - direct = 1, transferable = 0.5, not shown = 0.
  - Support that rests only on unconfirmed project lines counts 0 now and its full value in a second "if you
    confirm" score. It is never half credit.
  - `unclear` parts (years, clearances, eligibility) are left out until the person answers.
- **Requirement credit:** the mean of its parts for `all`, the best part for `any`.
  - "I meet this" counts 1. It is only allowed on items that need clarification.
  - "I don't have this" counts 0 and is labeled a confirmed gap.
- **Groups:**

  | Group | Weight |
  | --- | --- |
  | Required qualifications | 50 |
  | Responsibilities | 15 |
  | Nice to have | 15 |
  | Evidence strength | 15 |
  | Readability | 15, left out until a tailored resume exists |

  - A group with nothing assessable is left out, and its weight drops from the divisor.
- **Overlap:** inside a group, parts with the same concept share one part's weight, so a repeated idea counts once.
  Either-or items count as one unit.
- **Evidence strength:** averaged over items that have credit, so missing items are not penalized twice.
  - Each item takes its best cited line: 0.4 base, + 0.2 if specific (names a tool, has a number, or is detailed),
    + 0.2 if it shows the person's own action, + 0.2 if it shows a measured result.
  - Each extra independent source adds 0.1 x (1 - 0.5^(k-1)) in total, capped at 1.
  - Lines about the same project count as one source, whether they sit in a job write-up or the project.
  - School records are not rated.
  - **Its weight is 15 x coverage**, where coverage is the coverage groups combined (the score without strength).
    Strength rates the lines behind the credit, so it weighs in proportion to the credit. With full weight it gave
    a nursing posting 17/100 to a software profile that met none of the qualifications; 12.6 of those points came
    from well-written lines about transferable duties. The weight shrinks in the divisor too, so missing items
    still do not count against strength. Strong fits barely move: the live Crossing Hurdles (86) and Brightline
    (81) scores did not change, and the nursing score went from 17 to 5.
- **Exact arithmetic:** each factor value is rounded to 4 decimals once, and the total is computed from exactly
  those numbers. Adding up the printed formula by hand gives the printed total.
- **Profile completeness** moves out of the fit score into a separate "application readiness" check. This is a
  deliberate change from the blueprint formula: contact details are not evidence of fit for a job.
- The response includes every factor's weight, unrounded value, and points, the divisor, and the formula with the
  numbers filled in, for both scores. The label stays "Vettyou estimate" and never says ATS score.

## Consequences

- A first read makes two model calls; a re-check makes one. Re-reading replaces the requirements and clears answers.
- The person's job write-ups and project lines leave the machine for OpenAI on every read or re-check.
- Model judgments can differ between runs. Storing them keeps a result stable and auditable until the person asks
  for a re-check.
- Tests never call OpenAI. The test profile blanks the key, and integration tests mock both calls.
- Logs carry counts (parts judged, answers returned, citations dropped, support downgraded or narrowed, parts left
  unanswered), the model's one-sentence reading of the job, and the first unknown part id if any. Evidence text is
  never logged.
- `gpt-5-mini` still varies between runs on whether domain-specific duties are direct or transferable. In five live
  runs on the same posting it sometimes called building an admin metrics page "tracking pipeline health". The Java
  rules above catch the cases they can name; for the rest, the reason and the cited lines are shown, and the person
  can answer "I don't have this".
