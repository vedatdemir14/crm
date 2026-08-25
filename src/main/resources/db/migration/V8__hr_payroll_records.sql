-- FR-HR-04. The table exists so the mock returns the same figures every time it is asked
-- (Veri Modeli §4): payslips that changed between two page loads would be worse than useless for
-- testing the screens that consume them.
CREATE TABLE hr.payroll_records (
    id           UUID         PRIMARY KEY,
    employee_id  UUID         NOT NULL REFERENCES hr.employees (id),
    -- YYYY-MM. Kept as text rather than a date because a payslip belongs to a month, not a day.
    period       VARCHAR(7)   NOT NULL,
    -- Amounts are stored as AES-256-GCM ciphertext (Veri Modeli §6), so the columns are text and
    -- cannot be summed in SQL. Nothing aggregates payroll in the database today, and the
    -- alternative — salaries readable by anyone with a database session — is worse.
    gross_amount VARCHAR(255) NOT NULL,
    net_amount   VARCHAR(255) NOT NULL,
    currency     VARCHAR(3)   NOT NULL,
    generated_at TIMESTAMPTZ  NOT NULL,
    -- Always true while payroll is mocked. It travels into the API response so no screen can
    -- present generated figures as though they came from a real payroll provider.
    is_mock      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uq_payroll_employee_period UNIQUE (employee_id, period),
    CONSTRAINT chk_payroll_period_format CHECK (period ~ '^[0-9]{4}-(0[1-9]|1[0-2])$')
);

CREATE INDEX idx_payroll_employee ON hr.payroll_records (employee_id, period DESC);
