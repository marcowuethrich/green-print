CREATE TABLE plants (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(255) NOT NULL,
    species VARCHAR(255),
    notes   TEXT
);
