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
    ON DELETE CASCADE
    CHECK (is_host_user(id)),
  appointmentInterval INTERVAL DAY NOT NULL DEFAULT INTERVAL '31 days'
);

---- Default schedule
CREATE TYPE day_of_week AS ENUM ('mon', 'tue', 'wen', 'thu', 'fri', 'sat', 'sun');

CREATE FUNCTION day_to_int(day_of_week) RETURNS INTEGER AS $$
SELECT (enumsortorder - 1)::integer FROM
  (
    SELECT enumlabel, enumsortorder FROM pg_catalog.pg_enum
    WHERE enumtypid = 'day_of_week'::regtype ORDER BY enumsortorder
  ) AS temp
WHERE enumlabel = $1::text;
$$ LANGUAGE sql;

CREATE TABLE "DefaultSchedule" (
  id SERIAL PRIMARY KEY,
  hostId INTEGER REFERENCES "User"
    ON UPDATE RESTRICT
    ON DELETE CASCADE
    CHECK (is_host_user(id)),
  day day_of_week NOT NULL,
  start time NOT NULL,
  stop time NOT NULL
  -- todo intersection constraint?
);

---- Default schedule
CREATE TABLE "CustomSchedule" (
  id SERIAL PRIMARY KEY,
  hostId INTEGER REFERENCES "User"
    ON UPDATE RESTRICT
    ON DELETE CASCADE
    CHECK (is_host_user(id)),
  date date NOT NULL,
  start time NOT NULL,
  stop time NOT NULL
  -- todo intersection constraint?
);

---- Appointment
CREATE TYPE appointment_status AS ENUM ('pending', 'finished', 'cancelledByUser', 'cancelledByHost');

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
  status appointment_status NOT NULL DEFAULT 'pending',
  CONSTRAINT TimeTable_unique UNIQUE (hostId, date)
)

