CREATE TABLE mail_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    host TEXT NOT NULL,
    port INTEGER NOT NULL,
    secret TEXT NOT NULL,
    verified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX mail_connections_user_id_idx ON mail_connections (user_id);
