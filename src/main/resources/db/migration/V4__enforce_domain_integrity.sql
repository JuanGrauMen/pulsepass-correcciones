-- Reinforce domain catalogs and business constraints at the database layer.
ALTER TABLE events
    ADD CONSTRAINT chk_events_category
        CHECK (category IN ('MUSIC', 'SPORTS', 'TECHNOLOGY', 'EDUCATION', 'CULTURE', 'ENTERTAINMENT')),
    ADD CONSTRAINT chk_events_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'SOLD_OUT', 'CANCELLED', 'FINISHED')),
    ADD CONSTRAINT chk_events_minimum_age
        CHECK (minimum_age >= 0);

ALTER TABLE tickets
    ADD CONSTRAINT chk_tickets_type
        CHECK (type IN ('GENERAL', 'VIP', 'BACKSTAGE', 'STUDENT')),
    ADD CONSTRAINT chk_tickets_status
        CHECK (status IN ('RESERVED', 'PAID', 'CANCELLED', 'USED'));

-- The service treats emails case-insensitively; enforce the same rule under concurrent writes.
CREATE UNIQUE INDEX ux_users_email_lower ON users (LOWER(email));
