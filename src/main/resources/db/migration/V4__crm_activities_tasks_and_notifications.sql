CREATE TABLE crm.activities (
    id             UUID         PRIMARY KEY,
    type           VARCHAR(20)  NOT NULL,
    subject        VARCHAR(255) NOT NULL,
    description    TEXT,
    contact_id     UUID         REFERENCES crm.contacts (id),
    opportunity_id UUID         REFERENCES crm.opportunities (id),
    occurred_at    TIMESTAMPTZ  NOT NULL,
    created_by     UUID         NOT NULL REFERENCES common.users (id),
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    deleted_at     TIMESTAMPTZ,

    -- An activity that hangs off nothing cannot appear on any timeline (FR-CRM-05), so it must
    -- belong to at least one of the two.
    CONSTRAINT chk_activity_has_parent CHECK (contact_id IS NOT NULL OR opportunity_id IS NOT NULL)
);

CREATE INDEX idx_activities_contact ON crm.activities (contact_id, occurred_at DESC);
CREATE INDEX idx_activities_opportunity ON crm.activities (opportunity_id, occurred_at DESC);
CREATE INDEX idx_activities_created_by ON crm.activities (created_by);

CREATE TABLE crm.tasks (
    id                     UUID         PRIMARY KEY,
    title                  VARCHAR(255) NOT NULL,
    description            TEXT,
    due_date               DATE         NOT NULL,
    status                 VARCHAR(10)  NOT NULL,
    assigned_to            UUID         NOT NULL REFERENCES common.users (id),
    related_contact_id     UUID         REFERENCES crm.contacts (id),
    related_opportunity_id UUID         REFERENCES crm.opportunities (id),
    completed_at           TIMESTAMPTZ,
    created_by             UUID         NOT NULL REFERENCES common.users (id),
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL,
    deleted_at             TIMESTAMPTZ,

    CONSTRAINT chk_task_completed_at CHECK (status <> 'DONE' OR completed_at IS NOT NULL)
);

CREATE INDEX idx_tasks_assigned_to ON crm.tasks (assigned_to, status);
CREATE INDEX idx_tasks_due_date ON crm.tasks (due_date);

CREATE TABLE common.notifications (
    id                  UUID         PRIMARY KEY,
    user_id             UUID         NOT NULL REFERENCES common.users (id),
    type                VARCHAR(40)  NOT NULL,
    title               VARCHAR(255) NOT NULL,
    message             TEXT,
    related_entity_type VARCHAR(40),
    related_entity_id   UUID,
    read_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_notifications_user ON common.notifications (user_id, read_at, created_at DESC);

-- The reminder job runs repeatedly; this is what stops it from producing a duplicate notification
-- on every pass for the same task and the same kind of reminder (FR-CRM-07).
CREATE UNIQUE INDEX uq_notifications_dedup
    ON common.notifications (user_id, type, related_entity_id)
    WHERE related_entity_id IS NOT NULL;
