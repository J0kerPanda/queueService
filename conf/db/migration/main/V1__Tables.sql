-- Connection must be done as superuser
CREATE EXTENSION IF NOT EXISTS pgcrypto;

---- User
CREATE TABLE "User" (
  id SERIAL PRIMARY KEY,
  firstName VARCHAR(255) NOT NULL,
  surname VARCHAR(255) NOT NULL,
  patronymic VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  isHost BOOLEAN NOT NULL DEFAULT FALSE,
  isBlocked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE OR REPLACE FUNCTION is_host_user(int) RETURNS BOOLEAN AS $$
SELECT EXISTS(
    SELECT id FROM "User" WHERE id = $1 AND isHost = TRUE
)
$$ LANGUAGE sql;

---- Host Meta
CREATE TABLE "HostMeta" (
  id INT PRIMARY KEY REFERENCES "User"
    ON UPDATE RESTRICT
    ON DELETE CASCADE
    CHECK (is_host_user(id)),
  appointmentPeriod INTERVAL NOT NULL DEFAULT '31 day'
);

-- todo intersection constraints?
-- TimeRange
CREATE OR REPLACE FUNCTION time_subtype_diff(x time, y time) RETURNS float8 AS $$
  SELECT EXTRACT(EPOCH FROM (x - y))
$$ LANGUAGE sql STRICT IMMUTABLE;

CREATE TYPE timerange AS RANGE (
  subtype = time,
  subtype_diff = time_subtype_diff
);

---- Repeat schedule
CREATE TABLE "RepeatedSchedule" (
  id SERIAL PRIMARY KEY,
  hostId INT REFERENCES "HostMeta"
    ON UPDATE RESTRICT
    ON DELETE CASCADE,
  repeatDate DATE NOT NULL,
  repeatPeriod INTERVAL NOT NULL DEFAULT interval '7 day',
  appointmentIntervals timerange[] NOT NULL,
  appointmentDuration INTERVAL NOT NULL DEFAULT interval '30 minutes',
  place VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX host_day_unique_idx ON "RepeatedSchedule"(hostId, EXTRACT(isodow from repeatdate));

---- Schedule
CREATE TABLE "Schedule" (
  id SERIAL PRIMARY KEY,
  hostId INT REFERENCES "HostMeta"
    ON UPDATE RESTRICT
    ON DELETE CASCADE,
  repeatId INT REFERENCES "RepeatedSchedule"
    ON UPDATE CASCADE
    ON DELETE SET NULL
    NULL,
  date DATE NOT NULL,
  appointmentIntervals timerange[] NOT NULL,
  appointmentDuration INTERVAL NOT NULL DEFAULT interval '30 minutes',
  place VARCHAR(255) NOT NULL,
  isBlocked BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT host_date_unique UNIQUE (hostId, date)
);

---- Appointment
CREATE TABLE "Appointment" (
  id BIGSERIAL PRIMARY KEY,
  scheduleId INT NOT NULL REFERENCES "Schedule" (id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  visitorId INT NOT NULL REFERENCES "User" (id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  start TIME NOT NULL,
  "end" TIME NOT NULL CHECK ("end" > start)
);
