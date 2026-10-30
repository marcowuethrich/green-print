-- Plants carry the time of their last change, and every watering is logged (inventory.md §5).
ALTER TABLE plants ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT now();

CREATE TABLE watering_events (
    id         UUID PRIMARY KEY,
    plant_id   UUID NOT NULL,
    watered_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_watering_events_plants FOREIGN KEY (plant_id) REFERENCES plants (id) ON DELETE CASCADE
);

CREATE INDEX idx_watering_events_plant_id ON watering_events(plant_id);
