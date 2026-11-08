CREATE TABLE profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    display_name TEXT NOT NULL,
    phone TEXT NOT NULL DEFAULT '',
    location TEXT NOT NULL DEFAULT '',
    website TEXT NOT NULL DEFAULT '',
    linkedin TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE work_experiences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    employer TEXT NOT NULL,
    title TEXT NOT NULL,
    location TEXT NOT NULL DEFAULT '',
    start_date DATE,
    end_date DATE,
    still_here BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX work_experiences_user_id_idx ON work_experiences (user_id);

CREATE TABLE source_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    experience_id UUID NOT NULL REFERENCES work_experiences (id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (experience_id, version_number)
);

CREATE INDEX source_versions_user_id_idx ON source_versions (user_id);
