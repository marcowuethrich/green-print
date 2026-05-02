CREATE TABLE spaces (
    id      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    name    VARCHAR(255) NOT NULL
);

CREATE TABLE rooms (
    id       UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name     VARCHAR(255) NOT NULL,
    space_id UUID NOT NULL,
    CONSTRAINT fk_rooms_spaces FOREIGN KEY (space_id) REFERENCES spaces (id) ON DELETE CASCADE,
    CONSTRAINT uq_room_name_per_space UNIQUE (space_id, name)
);

CREATE INDEX idx_spaces_user_id ON spaces(user_id);
