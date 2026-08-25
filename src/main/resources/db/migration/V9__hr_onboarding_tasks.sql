-- FR-HR-06 and FR-HR-07 share one table, as the data model has it: joining and leaving are the
-- same kind of checklist run in opposite directions, distinguished by task_type.
CREATE TABLE hr.onboarding_task_templates (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    task_type     VARCHAR(20)  NOT NULL,
    display_order INTEGER      NOT NULL,
    -- Days from the hire date (onboarding) or the termination date (offboarding). Negative values
    -- are allowed so an offboarding step such as handover can fall before the last day.
    offset_days   INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    deleted_at    TIMESTAMPTZ,

    CONSTRAINT chk_template_type CHECK (task_type IN ('ONBOARDING', 'OFFBOARDING'))
);

CREATE UNIQUE INDEX uq_task_template_name
    ON hr.onboarding_task_templates (task_type, LOWER(name)) WHERE deleted_at IS NULL;

-- The default checklists from FR-HR-06 ("ekipman, doküman, erişim") and FR-HR-07
-- ("erişim kapatma, devir teslim"), seeded as data so HR can edit them without a release.
INSERT INTO hr.onboarding_task_templates (id, name, task_type, display_order, offset_days, created_at, updated_at)
VALUES (gen_random_uuid(), 'Ekipman teslimi (dizüstü, telefon)', 'ONBOARDING', 1, 0, NOW(), NOW()),
       (gen_random_uuid(), 'Sistem erişimleri açılması',          'ONBOARDING', 2, 0, NOW(), NOW()),
       (gen_random_uuid(), 'Sözleşme ve özlük evrakları',         'ONBOARDING', 3, 3, NOW(), NOW()),
       (gen_random_uuid(), 'Oryantasyon eğitimi',                 'ONBOARDING', 4, 7, NOW(), NOW()),
       (gen_random_uuid(), 'Devir teslim tamamlanması',           'OFFBOARDING', 1, -7, NOW(), NOW()),
       (gen_random_uuid(), 'Sistem erişimlerinin kapatılması',    'OFFBOARDING', 2, 0, NOW(), NOW()),
       (gen_random_uuid(), 'Ekipman iadesi',                      'OFFBOARDING', 3, 0, NOW(), NOW()),
       (gen_random_uuid(), 'Çıkış mülakatı',                      'OFFBOARDING', 4, -1, NOW(), NOW());

CREATE TABLE hr.onboarding_tasks (
    id           UUID         PRIMARY KEY,
    employee_id  UUID         NOT NULL REFERENCES hr.employees (id),
    task_name    VARCHAR(255) NOT NULL,
    task_type    VARCHAR(20)  NOT NULL,
    status       VARCHAR(10)  NOT NULL,
    due_date     DATE,
    assigned_to  UUID         REFERENCES common.users (id),
    completed_at TIMESTAMPTZ,
    completed_by UUID         REFERENCES common.users (id),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    deleted_at   TIMESTAMPTZ,

    CONSTRAINT chk_task_type CHECK (task_type IN ('ONBOARDING', 'OFFBOARDING')),
    -- A completed task must say who finished it and when, or the checklist cannot be audited.
    CONSTRAINT chk_task_completion CHECK (
        status <> 'DONE' OR (completed_at IS NOT NULL AND completed_by IS NOT NULL))
);

CREATE INDEX idx_onboarding_tasks_employee ON hr.onboarding_tasks (employee_id, task_type);
CREATE INDEX idx_onboarding_tasks_assignee ON hr.onboarding_tasks (assigned_to, status);
CREATE INDEX idx_onboarding_tasks_due ON hr.onboarding_tasks (due_date);
