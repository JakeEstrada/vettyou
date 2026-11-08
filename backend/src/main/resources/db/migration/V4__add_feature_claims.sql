ALTER TABLE evidence_claims DROP CONSTRAINT evidence_claims_kind_check;
ALTER TABLE evidence_claims ADD CONSTRAINT evidence_claims_kind_check CHECK (kind IN ('skill', 'feature'));

ALTER TABLE evidence_claims DROP CONSTRAINT evidence_claims_project_id_name_key;
ALTER TABLE evidence_claims ADD CONSTRAINT evidence_claims_project_kind_name_key UNIQUE (project_id, kind, name);
