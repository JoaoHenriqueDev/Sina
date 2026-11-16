ALTER TABLE device DROP CONSTRAINT ck_device_email_role;
ALTER TABLE device ADD COLUMN phone varchar(13);
