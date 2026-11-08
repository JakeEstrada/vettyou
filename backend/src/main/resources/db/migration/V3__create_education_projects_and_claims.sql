CREATE TABLE educations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    institution TEXT NOT NULL,
    credential TEXT NOT NULL DEFAULT '',
    field_of_study TEXT NOT NULL DEFAULT '',
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX educations_user_id_idx ON educations (user_id);

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    summary TEXT NOT NULL DEFAULT '',
    repo_url TEXT NOT NULL DEFAULT '',
    live_url TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX projects_user_id_idx ON projects (user_id);

CREATE TABLE project_source_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    file_name TEXT NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (project_id, version_number)
);

CREATE INDEX project_source_versions_user_id_idx ON project_source_versions (user_id);

CREATE TABLE evidence_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    source_version_id UUID NOT NULL REFERENCES project_source_versions (id) ON DELETE CASCADE,
    kind TEXT NOT NULL CHECK (kind IN ('skill')),
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    quote TEXT NOT NULL,
    line_number INTEGER NOT NULL,
    planned BOOLEAN NOT NULL DEFAULT false,
    status TEXT NOT NULL CHECK (status IN ('candidate', 'approved', 'rejected')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (project_id, name)
);

CREATE INDEX evidence_claims_user_id_idx ON evidence_claims (user_id);

CREATE TABLE claim_decisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    claim_id UUID NOT NULL REFERENCES evidence_claims (id) ON DELETE CASCADE,
    decision TEXT NOT NULL CHECK (decision IN ('approved', 'rejected')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX claim_decisions_claim_id_idx ON claim_decisions (claim_id);
