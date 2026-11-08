ALTER TABLE profiles ADD COLUMN email TEXT NOT NULL DEFAULT '';

CREATE TABLE tailored_resumes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    posting_id UUID NOT NULL REFERENCES job_postings (id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    template TEXT NOT NULL CHECK (template IN ('ats-first')),
    snapshot TEXT NOT NULL,
    plain_text TEXT NOT NULL,
    pdf BYTEA NOT NULL,
    pdf_sha256 TEXT NOT NULL,
    sources_hash TEXT NOT NULL,
    total INTEGER,
    matched_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (posting_id, version)
);

CREATE INDEX tailored_resumes_user_id_idx ON tailored_resumes (user_id);
