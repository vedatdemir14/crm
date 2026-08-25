-- Pipeline stages are configuration rather than hardcoded values (Veri Modeli §4), so the
-- default set from FR-CRM-04 is seeded here and can be edited by an admin afterwards.
CREATE TABLE crm.pipeline_stages (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(100) NOT NULL UNIQUE,
    display_order INT          NOT NULL,
    is_won_stage  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_lost_stage BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

INSERT INTO crm.pipeline_stages (id, name, display_order, is_won_stage, is_lost_stage, created_at, updated_at)
VALUES (gen_random_uuid(), 'İlk Temas',   1, FALSE, FALSE, NOW(), NOW()),
       (gen_random_uuid(), 'Nitelikli',   2, FALSE, FALSE, NOW(), NOW()),
       (gen_random_uuid(), 'Teklif',      3, FALSE, FALSE, NOW(), NOW()),
       (gen_random_uuid(), 'Müzakere',    4, FALSE, FALSE, NOW(), NOW()),
       (gen_random_uuid(), 'Kazanıldı',   5, TRUE,  FALSE, NOW(), NOW()),
       (gen_random_uuid(), 'Kaybedildi',  6, FALSE, TRUE,  NOW(), NOW());

CREATE TABLE crm.opportunities (
    id                  UUID           PRIMARY KEY,
    name                VARCHAR(255)   NOT NULL,
    contact_id          UUID           REFERENCES crm.contacts (id),
    company_id          UUID           REFERENCES crm.companies (id),
    stage_id            UUID           NOT NULL REFERENCES crm.pipeline_stages (id),
    amount              NUMERIC(18, 2),
    probability         INTEGER,
    expected_close_date DATE,
    status              VARCHAR(20)    NOT NULL,
    lost_reason         VARCHAR(255),
    closed_at           TIMESTAMPTZ,
    owner_user_id       UUID           NOT NULL REFERENCES common.users (id),
    created_at          TIMESTAMPTZ    NOT NULL,
    updated_at          TIMESTAMPTZ    NOT NULL,
    deleted_at          TIMESTAMPTZ,

    -- FR-CRM-09 requires a reason on every lost opportunity. Enforced here as well as in the
    -- service so the rule cannot be bypassed by a future code path.
    CONSTRAINT chk_lost_reason_required CHECK (status <> 'LOST' OR lost_reason IS NOT NULL),
    CONSTRAINT chk_probability_range CHECK (probability IS NULL OR probability BETWEEN 0 AND 100),
    CONSTRAINT chk_amount_not_negative CHECK (amount IS NULL OR amount >= 0)
);

CREATE INDEX idx_opportunities_owner ON crm.opportunities (owner_user_id);
CREATE INDEX idx_opportunities_stage ON crm.opportunities (stage_id);
CREATE INDEX idx_opportunities_status ON crm.opportunities (status);
CREATE INDEX idx_opportunities_contact ON crm.opportunities (contact_id);
CREATE INDEX idx_opportunities_company ON crm.opportunities (company_id);
CREATE INDEX idx_opportunities_closed_at ON crm.opportunities (closed_at);
