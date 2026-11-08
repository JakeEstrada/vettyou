CREATE TABLE mail_sync_cursors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    folder TEXT NOT NULL,
    uidvalidity BIGINT NOT NULL,
    last_uid BIGINT NOT NULL,
    synced_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, folder)
);

CREATE TABLE mail_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    folder TEXT NOT NULL,
    uid BIGINT NOT NULL,
    uidvalidity BIGINT NOT NULL,
    internet_message_id TEXT NOT NULL DEFAULT '',
    from_address TEXT NOT NULL,
    from_name TEXT NOT NULL,
    subject TEXT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL,
    snippet TEXT NOT NULL,
    quote TEXT NOT NULL DEFAULT '',
    category TEXT NOT NULL,
    classifier TEXT NOT NULL,
    review_state TEXT NOT NULL,
    proposed_application_id UUID REFERENCES applications (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, folder, uidvalidity, uid)
);

CREATE INDEX mail_messages_user_review_idx ON mail_messages (user_id, review_state, sent_at DESC);

CREATE TABLE mail_application_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    message_id UUID NOT NULL REFERENCES mail_messages (id) ON DELETE CASCADE,
    application_id UUID NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    match TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (message_id, application_id)
);
