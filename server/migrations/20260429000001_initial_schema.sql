CREATE TABLE spaces (
    id      BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    name    VARCHAR(255) NOT NULL
);

CREATE TABLE rooms (
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(255) NOT NULL,
    space_id BIGINT NOT NULL,
    CONSTRAINT fk_rooms_spaces FOREIGN KEY (space_id) REFERENCES spaces (id) ON DELETE CASCADE
);

CREATE INDEX idx_spaces_user_id ON spaces(user_id);
