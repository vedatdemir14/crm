CREATE TABLE hr.leave_types (
    id                   UUID         PRIMARY KEY,
    name                 VARCHAR(100) NOT NULL,
    is_paid              BOOLEAN      NOT NULL,
    default_annual_days  INTEGER      NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    deleted_at           TIMESTAMPTZ,

    CONSTRAINT chk_leave_type_days CHECK (default_annual_days >= 0)
);

CREATE UNIQUE INDEX uq_leave_types_name ON hr.leave_types (LOWER(name)) WHERE deleted_at IS NULL;

INSERT INTO hr.leave_types (id, name, is_paid, default_annual_days, created_at, updated_at)
VALUES (gen_random_uuid(), 'Yıllık İzin',   TRUE,  14, NOW(), NOW()),
       (gen_random_uuid(), 'Hastalık İzni', TRUE,  10, NOW(), NOW()),
       (gen_random_uuid(), 'Ücretsiz İzin', FALSE,  0, NOW(), NOW()),
       (gen_random_uuid(), 'Mazeret İzni',  TRUE,   3, NOW(), NOW());

-- FR-HR-03: the public holiday calendar. Working-day counts subtract these, so a request spanning
-- a national holiday does not consume the employee's entitlement for it.
CREATE TABLE hr.public_holidays (
    id           UUID         PRIMARY KEY,
    holiday_date DATE         NOT NULL UNIQUE,
    name         VARCHAR(150) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_public_holidays_date ON hr.public_holidays (holiday_date);

CREATE TABLE hr.leave_requests (
    id            UUID         PRIMARY KEY,
    employee_id   UUID         NOT NULL REFERENCES hr.employees (id),
    leave_type_id UUID         NOT NULL REFERENCES hr.leave_types (id),
    start_date    DATE         NOT NULL,
    end_date      DATE         NOT NULL,
    days_count    INTEGER      NOT NULL,
    reason        TEXT,
    status        VARCHAR(20)  NOT NULL,
    approver_id   UUID         REFERENCES common.users (id),
    decision_note TEXT,
    requested_at  TIMESTAMPTZ  NOT NULL,
    decided_at    TIMESTAMPTZ,
    updated_at    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT chk_leave_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_leave_days CHECK (days_count > 0),
    -- A decided request must record who decided it and when; a decision with no decider cannot be
    -- audited afterwards.
    CONSTRAINT chk_leave_decision CHECK (
        status IN ('PENDING', 'CANCELLED') OR (approver_id IS NOT NULL AND decided_at IS NOT NULL))
);

CREATE INDEX idx_leave_requests_employee ON hr.leave_requests (employee_id, start_date);
CREATE INDEX idx_leave_requests_status ON hr.leave_requests (status);

CREATE TABLE hr.leave_balances (
    id            UUID        PRIMARY KEY,
    employee_id   UUID        NOT NULL REFERENCES hr.employees (id),
    leave_type_id UUID        NOT NULL REFERENCES hr.leave_types (id),
    year          INTEGER     NOT NULL,
    total_days    INTEGER     NOT NULL,
    used_days     INTEGER     NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,

    -- One balance row per employee, type and year. Without this a concurrent approval could create
    -- a second row and quietly double the entitlement.
    CONSTRAINT uq_leave_balance UNIQUE (employee_id, leave_type_id, year),
    CONSTRAINT chk_balance_used CHECK (used_days >= 0),
    CONSTRAINT chk_balance_total CHECK (total_days >= 0)
);
