DELETE FROM pairing_code WHERE used_at IS NULL;
ALTER TABLE pairing_code DROP CONSTRAINT ck_pairing_code_format;
ALTER TABLE pairing_code ALTER COLUMN code TYPE varchar(8);
ALTER TABLE pairing_code ADD CONSTRAINT ck_pairing_code_format CHECK (code ~ '^[0-9]{6}([0-9]{2})?$');

ALTER TABLE device ADD COLUMN email_verified_at timestamptz;
ALTER TABLE device ADD COLUMN email_token_hash varchar(64);
CREATE UNIQUE INDEX uq_device_email_token ON device (email_token_hash) WHERE email_token_hash IS NOT NULL;
-- Familiares cadastrados antes da confirmação por e-mail continuam recebendo avisos.
UPDATE device SET email_verified_at = now() WHERE role = 'GUARDIAN';
