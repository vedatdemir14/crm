-- FR-CRM-12: activities that were imported from an external mail/calendar source rather than
-- typed in by a user. external_source records where a row came from, so an imported activity is
-- never mistaken for one a salesperson logged by hand.
ALTER TABLE crm.activities
    ADD COLUMN external_id     VARCHAR(255),
    ADD COLUMN external_source VARCHAR(60);

-- A single message can legitimately produce one activity per matched contact (a meeting with two
-- people belongs on both timelines), so the identity of an import is the pair, not the id alone.
-- This is what makes re-running a sync safe.
CREATE UNIQUE INDEX uq_activities_external
    ON crm.activities (external_id, contact_id)
    WHERE external_id IS NOT NULL;

CREATE TABLE common.integration_sync_state (
    provider       VARCHAR(60)  PRIMARY KEY,
    last_synced_at TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL
);
