CREATE TABLE crm.companies (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    industry      VARCHAR(120),
    website       VARCHAR(255),
    address       TEXT,
    owner_user_id UUID         NOT NULL REFERENCES common.users (id),
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    deleted_at    TIMESTAMPTZ
);

CREATE INDEX idx_companies_owner ON crm.companies (owner_user_id);
CREATE INDEX idx_companies_name ON crm.companies (LOWER(name));

CREATE TABLE crm.contacts (
    id            UUID         PRIMARY KEY,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(255),
    phone         VARCHAR(50),
    title         VARCHAR(120),
    company_id    UUID         REFERENCES crm.companies (id),
    source        VARCHAR(50),
    owner_user_id UUID         NOT NULL REFERENCES common.users (id),
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    deleted_at    TIMESTAMPTZ
);

-- FR-CRM-02 asks for duplicate *detection with a warning*, not prevention, so
-- e-posta/telefon get plain lookup indexes rather than unique constraints.
CREATE INDEX idx_contacts_email ON crm.contacts (LOWER(email));
CREATE INDEX idx_contacts_phone ON crm.contacts (phone);
CREATE INDEX idx_contacts_owner ON crm.contacts (owner_user_id);
CREATE INDEX idx_contacts_company ON crm.contacts (company_id);
