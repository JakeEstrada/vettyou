CREATE TABLE job_postings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title TEXT NOT NULL DEFAULT '',
    company TEXT NOT NULL DEFAULT '',
    body TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('reading', 'ready', 'failed')),
    reader TEXT NOT NULL DEFAULT '',
    error TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX job_postings_user_id_idx ON job_postings (user_id);
CREATE INDEX job_postings_status_idx ON job_postings (status);

CREATE TABLE job_requirements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    posting_id UUID NOT NULL REFERENCES job_postings (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    skill TEXT NOT NULL,
    importance TEXT NOT NULL CHECK (importance IN ('required', 'preferred')),
    category TEXT NOT NULL,
    quote TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX job_requirements_posting_id_idx ON job_requirements (posting_id);
CREATE INDEX job_requirements_user_id_idx ON job_requirements (user_id);
