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
  patronymic VARCHAR(255) NOT NULL,
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
  appointmentPeriod BIGINT NOT NULL DEFAULT 2678400000 -- 31 days
);

---- Default schedule
CREATE TYPE day_of_week AS ENUM ('mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun');

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
  hostId INTEGER REFERENCES "HostMeta"
    ON UPDATE RESTRICT
    ON DELETE CASCADE,
  day day_of_week NOT NULL,
  start TIME NOT NULL,
  "end" TIME NOT NULL CHECK ("end" > start),
  place VARCHAR(255) NOT NULL
  -- todo intersection constraint?
);

---- Custom schedule
CREATE TABLE "CustomSchedule" (
  id SERIAL PRIMARY KEY,
  hostId INTEGER REFERENCES "HostMeta"
    ON UPDATE RESTRICT
    ON DELETE CASCADE,
  date DATE NOT NULL,
  start time NOT NULL,
  "end" time NOT NULL CHECK ("end" > start),
  place VARCHAR(255) NOT NULL
  -- todo intersection constraint?
);

---- Appointment
CREATE TYPE appointment_status AS ENUM ('pending', 'finished', 'cancelledByUser', 'cancelledByHost');

CREATE TABLE "Appointment" (
  id BIGSERIAL PRIMARY KEY,
  hostId INT NOT NULL REFERENCES "HostMeta" (id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  visitorId INT NOT NULL REFERENCES "User" (id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  date DATE NOT NULL,
  start TIME NOT NULL,
  "end" TIME NOT NULL,
  status appointment_status NOT NULL DEFAULT 'pending'
  -- todo intersection constraint?
);

CREATE FUNCTION appointment_user_check() RETURNS TRIGGER AS $appointment_user_check$
BEGIN
  IF NOT is_active_user(NEW.hostid) THEN
    RAISE EXCEPTION 'host with id % is blocked', NEW.hostid;
  END IF;

  IF NOT is_active_user(NEW.visitorid) THEN
    RAISE EXCEPTION 'visitor with id % is blocked', NEW.visitorid;
  END IF;

  RETURN NEW;
END;
$appointment_user_check$ LANGUAGE plpgsql;

CREATE TRIGGER appointment_user_check BEFORE INSERT OR UPDATE ON "Appointment"
  FOR EACH ROW EXECUTE PROCEDURE appointment_user_check();

