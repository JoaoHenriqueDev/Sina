ALTER TABLE device ADD COLUMN inactive_notified_at timestamptz;

CREATE INDEX ix_device_protected_last_seen ON device (last_seen_at) WHERE role = 'PROTECTED' AND inactive_notified_at IS NULL;
