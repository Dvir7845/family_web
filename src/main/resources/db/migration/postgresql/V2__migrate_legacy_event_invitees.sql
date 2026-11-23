CREATE TABLE IF NOT EXISTS event_invitees (
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, user_id)
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'events'
          AND column_name = 'invited_user_id'
    ) THEN
        INSERT INTO event_invitees (event_id, user_id)
        SELECT DISTINCT e.id, u.id
        FROM events e
        CROSS JOIN LATERAL UNNEST(e.invited_user_id) AS invited(uid)
        INNER JOIN app_users u ON u.id = invited.uid
        WHERE e.invited_user_id IS NOT NULL
        ON CONFLICT (event_id, user_id) DO NOTHING;

        ALTER TABLE events DROP COLUMN invited_user_id;
    END IF;
END $$;
