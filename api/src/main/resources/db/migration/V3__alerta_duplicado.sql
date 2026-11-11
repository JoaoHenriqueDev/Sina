ALTER TABLE alert ADD COLUMN text_hash varchar(64);

CREATE INDEX ix_alert_protected_hash ON alert (protected_id, text_hash, created_at DESC);
