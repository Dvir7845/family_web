-- Handle any legacy null or oversized values before tightening constraints
UPDATE tasks
SET name = ''
WHERE name IS NULL;

UPDATE tasks
SET name = SUBSTRING(name, 1, 100)
WHERE LENGTH(name) > 100;

UPDATE shopping_items
SET product_name = SUBSTRING(product_name, 1, 100)
WHERE LENGTH(product_name) > 100;

-- Enforce nullability and align column lengths with Bean Validation (@Size)
ALTER TABLE tasks ALTER COLUMN name SET DATA TYPE VARCHAR(100);
ALTER TABLE tasks ALTER COLUMN name SET NOT NULL;
ALTER TABLE tasks ALTER COLUMN description SET DATA TYPE VARCHAR(500);
ALTER TABLE shopping_items ALTER COLUMN product_name SET DATA TYPE VARCHAR(100);
ALTER TABLE events ALTER COLUMN description SET DATA TYPE VARCHAR(500);

-- Indexes for foreign keys and frequent query filters
CREATE INDEX idx_tasks_owner_id ON tasks(owner_id);
CREATE INDEX idx_tasks_completed_at ON tasks(completed_at);
CREATE INDEX idx_vehicle_bookings_booking_date ON vehicle_bookings(booking_date);
CREATE INDEX idx_vehicle_bookings_user_id ON vehicle_bookings(user_id);
CREATE INDEX idx_event_invitees_user_id ON event_invitees(user_id);
CREATE INDEX idx_events_event_date ON events(event_date);
