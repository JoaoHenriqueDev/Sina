CREATE TABLE device (
                        id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                        role          varchar(16)  NOT NULL,
                        display_name  varchar(80),
                        fcm_token     varchar(4096),
                        email         varchar(254),
                        token_hash    varchar(64)  NOT NULL,
                        last_seen_at  timestamptz,
                        created_at    timestamptz  NOT NULL DEFAULT now(),
                        CONSTRAINT uq_device_token_hash UNIQUE (token_hash),
                        CONSTRAINT ck_device_role CHECK (role IN ('PROTECTED', 'GUARDIAN')),
                        CONSTRAINT ck_device_email_role CHECK (email IS NULL OR role = 'GUARDIAN')
);

CREATE TABLE pairing_code (
                              id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                              code          varchar(6)   NOT NULL,
                              protected_id  uuid         NOT NULL REFERENCES device (id) ON DELETE CASCADE,
                              expires_at    timestamptz  NOT NULL,
                              used_at       timestamptz,
                              created_at    timestamptz  NOT NULL DEFAULT now(),
                              CONSTRAINT ck_pairing_code_format CHECK (code ~ '^[0-9]{6}$')
    );

CREATE UNIQUE INDEX uq_pairing_code_active ON pairing_code (code) WHERE used_at IS NULL;
CREATE INDEX ix_pairing_code_protected ON pairing_code (protected_id);

CREATE TABLE pairing (
                         id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                         protected_id  uuid         NOT NULL REFERENCES device (id) ON DELETE CASCADE,
                         guardian_id   uuid         NOT NULL REFERENCES device (id) ON DELETE CASCADE,
                         created_at    timestamptz  NOT NULL DEFAULT now(),
                         CONSTRAINT uq_pairing UNIQUE (protected_id, guardian_id),
                         CONSTRAINT ck_pairing_distinct CHECK (protected_id <> guardian_id)
);

CREATE INDEX ix_pairing_guardian ON pairing (guardian_id);

CREATE TABLE alert (
                       id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                       protected_id    uuid              NOT NULL REFERENCES device (id) ON DELETE CASCADE,
                       source          varchar(16)       NOT NULL,
                       verdict         varchar(16)       NOT NULL,
                       scam_type       varchar(40),
                       confidence      double precision  NOT NULL,
                       family_summary  varchar(500)      NOT NULL,
                       created_at      timestamptz       NOT NULL DEFAULT now(),
                       CONSTRAINT ck_alert_source CHECK (source IN ('SMS', 'WHATSAPP')),
                       CONSTRAINT ck_alert_verdict CHECK (verdict IN ('SCAM', 'SUSPICIOUS')),
                       CONSTRAINT ck_alert_confidence CHECK (confidence >= 0 AND confidence <= 1)
);

CREATE INDEX ix_alert_protected_created ON alert (protected_id, created_at DESC, id DESC);

CREATE TABLE alert_delivery (
                                id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                alert_id     uuid         NOT NULL REFERENCES alert (id) ON DELETE CASCADE,
                                guardian_id  uuid         NOT NULL REFERENCES device (id) ON DELETE CASCADE,
                                channel      varchar(16)  NOT NULL,
                                status       varchar(16)  NOT NULL DEFAULT 'PENDING',
                                created_at   timestamptz  NOT NULL DEFAULT now(),
                                CONSTRAINT ck_alert_delivery_channel CHECK (channel IN ('PUSH', 'EMAIL')),
                                CONSTRAINT ck_alert_delivery_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX ix_alert_delivery_alert ON alert_delivery (alert_id);
CREATE INDEX ix_alert_delivery_guardian ON alert_delivery (guardian_id);

CREATE TABLE analysis_cache (
                                text_hash      varchar(64) PRIMARY KEY,
                                verdict        varchar(16)  NOT NULL,
                                response_json  jsonb        NOT NULL,
                                created_at     timestamptz  NOT NULL DEFAULT now(),
                                CONSTRAINT ck_analysis_cache_verdict CHECK (verdict IN ('SCAM', 'SUSPICIOUS', 'SAFE'))
);

CREATE INDEX ix_analysis_cache_created ON analysis_cache (created_at);