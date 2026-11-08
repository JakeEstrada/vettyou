CREATE TABLE applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    posting_id UUID REFERENCES job_postings (id) ON DELETE SET NULL,
    title TEXT NOT NULL,
    company TEXT NOT NULL DEFAULT '',
    location TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL,
    applied_at TIMESTAMPTZ,
    notes TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT applications_status_check CHECK (status IN (
        'draft', 'applied', 'rejected', 'moving_forward', 'interview', 'offer', 'withdrawn', 'no_reply'
    ))
);

CREATE INDEX applications_user_id_idx ON applications (user_id);
CREATE INDEX applications_posting_id_idx ON applications (posting_id);

CREATE TABLE application_status_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    application_id UUID NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    status TEXT NOT NULL,
    note TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT application_status_events_status_check CHECK (status IN (
        'draft', 'applied', 'rejected', 'moving_forward', 'interview', 'offer', 'withdrawn', 'no_reply'
    ))
);

CREATE INDEX application_status_events_application_id_idx ON application_status_events (application_id);
