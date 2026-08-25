-- V5 is taken by the mail/calendar integration branch; this starts at V6 so the two can merge in
-- either order without Flyway seeing a duplicate version.

CREATE TABLE hr.departments (
    id                   UUID         PRIMARY KEY,
    name                 VARCHAR(150) NOT NULL,
    parent_department_id UUID         REFERENCES hr.departments (id),
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    deleted_at           TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_departments_name ON hr.departments (LOWER(name)) WHERE deleted_at IS NULL;
CREATE INDEX idx_departments_parent ON hr.departments (parent_department_id);

CREATE TABLE hr.employees (
    id               UUID         PRIMARY KEY,
    user_id          UUID         REFERENCES common.users (id),
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    email            VARCHAR(255),
    phone            VARCHAR(50),
    -- Stored as AES-256-GCM ciphertext (Kriptografi ve Güvenlik Standartları §5). It is wider than
    -- a national id because the column holds base64 of IV + ciphertext + auth tag.
    national_id      VARCHAR(255),
    birth_date       DATE,
    hire_date        DATE         NOT NULL,
    employment_type  VARCHAR(20)  NOT NULL,
    department_id    UUID         REFERENCES hr.departments (id),
    position_title   VARCHAR(150),
    manager_id       UUID         REFERENCES hr.employees (id),
    status           VARCHAR(20)  NOT NULL,
    termination_date DATE,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    deleted_at       TIMESTAMPTZ,

    -- One user account maps to at most one employee record. users.employee_id was deliberately
    -- dropped in V1, so this column is the single source of truth for the link.
    CONSTRAINT uq_employees_user UNIQUE (user_id),
    CONSTRAINT chk_employee_termination CHECK (status <> 'TERMINATED' OR termination_date IS NOT NULL)
);

CREATE INDEX idx_employees_department ON hr.employees (department_id);
CREATE INDEX idx_employees_manager ON hr.employees (manager_id);
CREATE INDEX idx_employees_status ON hr.employees (status);
CREATE INDEX idx_employees_name ON hr.employees (LOWER(first_name), LOWER(last_name));
