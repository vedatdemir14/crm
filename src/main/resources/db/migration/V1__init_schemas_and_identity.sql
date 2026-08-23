-- Schema separation per Mimari Tasarım Dokümanı §5: common / crm / hr.
CREATE SCHEMA IF NOT EXISTS common;
CREATE SCHEMA IF NOT EXISTS crm;
CREATE SCHEMA IF NOT EXISTS hr;

CREATE TABLE common.users (
    id               UUID         PRIMARY KEY,
    username         VARCHAR(100) NOT NULL UNIQUE,
    email            VARCHAR(255) NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    last_login_at    TIMESTAMPTZ,
    failed_attempts  INT          NOT NULL DEFAULT 0,
    locked_until     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'))
);

CREATE TABLE common.roles (
    id          UUID        PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE common.user_roles (
    user_id UUID NOT NULL REFERENCES common.users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES common.roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_role ON common.user_roles (role_id);

-- Refresh tokens are stored hashed; the raw value only ever exists in the client.
-- Rotation: each refresh issues a new row and sets replaced_by on the old one.
CREATE TABLE common.refresh_tokens (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES common.users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    replaced_by UUID        REFERENCES common.refresh_tokens (id),
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_refresh_tokens_user ON common.refresh_tokens (user_id);

CREATE TABLE common.audit_logs (
    id          UUID         PRIMARY KEY,
    entity_name VARCHAR(100) NOT NULL,
    entity_id   VARCHAR(100) NOT NULL,
    action      VARCHAR(20)  NOT NULL,
    changed_by  UUID         REFERENCES common.users (id),
    changed_at  TIMESTAMPTZ  NOT NULL,
    old_value   JSONB,
    new_value   JSONB,
    CONSTRAINT audit_logs_action_check CHECK (action IN ('CREATE', 'UPDATE', 'DELETE'))
);

CREATE INDEX idx_audit_logs_entity ON common.audit_logs (entity_name, entity_id);

INSERT INTO common.roles (id, name, description) VALUES
    (gen_random_uuid(), 'ROLE_ADMIN',          'Sistem yöneticisi — tüm modüllere tam erişim'),
    (gen_random_uuid(), 'ROLE_SALES_REP',      'Satış temsilcisi — kendi CRM kayıtlarını yönetir'),
    (gen_random_uuid(), 'ROLE_SALES_MANAGER',  'Satış müdürü — ekip pipeline''ını ve raporları görür'),
    (gen_random_uuid(), 'ROLE_HR_ADMIN',       'İK yöneticisi — personel, izin ve bordro görünümü'),
    (gen_random_uuid(), 'ROLE_EMPLOYEE',       'Çalışan — ESS portalı erişimi');
