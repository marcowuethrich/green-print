CREATE TABLE plants (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    room_id UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    species VARCHAR(128),
    pot_size VARCHAR(64),
    soil_type VARCHAR(128),
    watering_interval_days INT NOT NULL,
    last_watered_date TIMESTAMP,
    photo_url VARCHAR(255),
    archive_date TIMESTAMP,
    archive_reason VARCHAR(255),
    x REAL NOT NULL DEFAULT 0.0,
    y REAL NOT NULL DEFAULT 0.0,
    CONSTRAINT fk_plants_rooms FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE,
    CONSTRAINT uq_plant_name_per_room UNIQUE (room_id, name)
);

CREATE INDEX idx_plants_room_id ON plants(room_id);
