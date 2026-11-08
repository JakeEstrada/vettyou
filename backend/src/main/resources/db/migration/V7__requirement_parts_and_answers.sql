ALTER TABLE job_postings DROP CONSTRAINT job_postings_status_check;
ALTER TABLE job_postings ADD CONSTRAINT job_postings_status_check
    CHECK (status IN ('reading', 'matching', 'ready', 'failed'));
ALTER TABLE job_postings ADD COLUMN matcher TEXT NOT NULL DEFAULT '';
ALTER TABLE job_postings ADD COLUMN evidence_hash TEXT NOT NULL DEFAULT '';
ALTER TABLE job_postings ADD COLUMN matched_at TIMESTAMPTZ;

ALTER TABLE job_requirements ADD COLUMN kind TEXT NOT NULL DEFAULT 'qualification'
    CHECK (kind IN ('qualification', 'responsibility'));
ALTER TABLE job_requirements ADD COLUMN logic TEXT NOT NULL DEFAULT 'all' CHECK (logic IN ('all', 'any'));
ALTER TABLE job_requirements ADD COLUMN answer TEXT CHECK (answer IN ('meets', 'gap'));
ALTER TABLE job_requirements ADD COLUMN answered_at TIMESTAMPTZ;

CREATE TABLE job_requirement_parts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    requirement_id UUID NOT NULL REFERENCES job_requirements (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    text TEXT NOT NULL,
    concept TEXT NOT NULL,
    support TEXT CHECK (support IN ('direct', 'transferable', 'none', 'unclear')),
    reason TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX job_requirement_parts_requirement_id_idx ON job_requirement_parts (requirement_id);
CREATE INDEX job_requirement_parts_user_id_idx ON job_requirement_parts (user_id);

CREATE TABLE job_part_evidence (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    part_id UUID NOT NULL REFERENCES job_requirement_parts (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    source_kind TEXT NOT NULL CHECK (source_kind IN ('job', 'project', 'school')),
    source_id UUID NOT NULL,
    line TEXT NOT NULL,
    excerpt TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX job_part_evidence_part_id_idx ON job_part_evidence (part_id);
CREATE INDEX job_part_evidence_user_id_idx ON job_part_evidence (user_id);
