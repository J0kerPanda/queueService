-- Connection must be done as superuser
CREATE EXTENSION IF NOT EXISTS pgcrypto;

---- Category
CREATE TABLE "Category" (
  id SERIAL PRIMARY KEY,
  parentId INTEGER NULL REFERENCES "Category" (id)
    ON UPDATE RESTRICT
    ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL UNIQUE,
  isFinal BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE FUNCTION is_final_category(int) RETURNS BOOLEAN AS $$
SELECT EXISTS(
    SELECT id FROM "Category" WHERE id = $1 AND isFinal = TRUE
);
$$ LANGUAGE sql;

---- User
CREATE TABLE "User" (
  id SERIAL PRIMARY KEY,
  firstName VARCHAR(255) NOT NULL,
  surname VARCHAR(255) NOT NULL,
  lastName VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  googleId VARCHAR(255) NOT NULL UNIQUE,
  isHost BOOLEAN NOT NULL DEFAULT FALSE,
  isBlocked BOOLEAN NOT NULL DEFAULT FALSE,
  categoryId INTEGER NOT NULL REFERENCES "Category" (id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
    CHECK (is_final_category(categoryId))
);

CREATE FUNCTION is_host_user(int) RETURNS BOOLEAN AS $$
SELECT EXISTS(
    SELECT id FROM "User" WHERE id = $1 AND isHost = TRUE
)
$$ LANGUAGE sql;

CREATE FUNCTION is_active_host_user(int) RETURNS BOOLEAN AS $$
SELECT EXISTS(
    SELECT id FROM "User" WHERE id = $1 AND isHost = TRUE AND isBlocked = FALSE
)
$$ LANGUAGE sql;

CREATE FUNCTION is_active_user(int) RETURNS BOOLEAN AS $$
SELECT EXISTS(
    SELECT id FROM "User" WHERE id = $1 AND isBlocked = FALSE
)
$$ LANGUAGE sql;

---- Host Meta
CREATE TABLE "HostMeta" (
  id INTEGER PRIMARY KEY REFERENCES "User"
    ON UPDATE RESTRICT
    ON DELETE CASCADE,
  appointmentInterval INTERVAL DAY NOT NULL DEFAULT '31 days'
);

---- Appointment
CREATE TYPE visit_status AS ENUM ('pending', 'finished', 'cancelledByUser', 'cancelledByHost');

CREATE TABLE "Appointment" (
  id BIGSERIAL PRIMARY KEY,
  hostId INT NOT NULL REFERENCES "User" (id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
    CHECK (is_active_host_user(hostId)),
  visitorId INT NOT NULL REFERENCES "User" (id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
    CHECK (is_active_user(visitorId)),
  date TIMESTAMP NOT NULL,
  status visit_status NOT NULL DEFAULT 'pending',
  CONSTRAINT TimeTable_unique UNIQUE (hostId, date)
)

