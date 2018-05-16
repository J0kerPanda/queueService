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

CREATE OR REPLACE FUNCTION is_final_category(int) RETURNS BOOLEAN AS $$
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
  isHost BOOLEAN NOT NULL DEFAULT FALSE,
  isBlocked BOOLEAN NOT NULL DEFAULT FALSE,
  categoryId INTEGER NOT NULL REFERENCES "Category" (id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
    CHECK (is_final_category(categoryId))
);

CREATE OR REPLACE FUNCTION is_host_user(int) RETURNS BOOLEAN AS $$
SELECT EXISTS(
    SELECT id FROM "User" WHERE id = $1 AND isHost = TRUE
)
$$ LANGUAGE sql;

CREATE OR REPLACE FUNCTION is_active_user(int) RETURNS BOOLEAN AS $$
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
  appointmentPeriod INTERVAL NOT NULL DEFAULT '31 day'
);

  ---- Default schedule

CREATE TABLE "DefaultSchedule" (
  id SERIAL PRIMARY KEY,
  hostId INTEGER REFERENCES "HostMeta"
    ON UPDATE RESTRICT
    ON DELETE CASCADE,
  firstDate DATE NOT NULL,
  repeatPeriod INTERVAL NOT NULL DEFAULT interval '7 day',
  start TIME NOT NULL,
  "end" TIME NOT NULL CHECK ("end" > start),
  appointmentDuration INTERVAL NOT NULL DEFAULT interval '30 minutes',
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
  appointmentDuration INTERVAL NOT NULL DEFAULT interval '30 minutes',
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
  "end" TIME NOT NULL CHECK ("end" > start),
  status appointment_status NOT NULL DEFAULT 'pending'
  -- todo intersection constraint?
);

CREATE OR REPLACE FUNCTION appointment_user_check() RETURNS TRIGGER AS $appointment_user_check$
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

-- functions

CREATE OR REPLACE FUNCTION generate_schedule(p_hostId INTEGER, p_from DATE, p_to Date)
  RETURNS TABLE (
    g_id INTEGER,
    g_date DATE,
    g_start time,
    g_end time,
    g_appointmentDuration INTERVAL,
    g_place VARCHAR(255)
  )
AS $$
DECLARE
  ds "DefaultSchedule";
  currentDate DATE;
BEGIN
  FOR ds IN SELECT * FROM "DefaultSchedule" WHERE firstdate <= p_to AND hostId = p_hostId LOOP
    currentDate := ds.firstdate;

    WHILE currentDate < p_to LOOP
      IF currentDate >= p_from THEN
        g_id := ds.id;
        g_date := currentDate;
        g_start = ds.start;
        g_end = ds.end;
        g_appointmentDuration = ds.appointmentduration;
        g_place = ds.place;
        RETURN NEXT;
      END IF;
      currentDate := currentDate + ds.repeatperiod;
    END LOOP;
  END LOOP;
END
$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION get_schedule(p_hostid INTEGER, p_from DATE, p_to Date)
  RETURNS TABLE (
    c_id INTEGER,
    c_date DATE,
    c_start time,
    c_end time,
    c_appointmentDuration INTERVAL,
    c_place VARCHAR(255),
    c_isCustom BOOLEAN
  )
AS $$
BEGIN
  RETURN QUERY
  SELECT id, date, start, "end", appointmentduration, place, TRUE FROM "CustomSchedule"
  WHERE date >= p_from AND date < p_to AND hostid = p_hostid
  UNION
  SELECT g_id, g_date, g_start, g_end, g_appointmentDuration, g_place, FALSE FROM generate_schedule(p_hostid, p_from, p_to) as GEN
  WHERE GEN.g_date NOT IN (SELECT date FROM "CustomSchedule" WHERE date >= p_from and Date < p_to AND hostid = p_hostid);
END;
$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION generate_appointments(p_from time, p_to time, p_interval interval)
  RETURNS TABLE (
    g_start time,
    g_end time
  )
AS $$
DECLARE
  curr_time time;
BEGIN
  curr_time := p_from;

  WHILE curr_time < p_to LOOP
    g_start = curr_time;
    g_end = curr_time + p_interval;
    RETURN NEXT;
    curr_time := curr_time + p_interval;
  END LOOP;
END
$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION generate_appointments(p_scheduleIds INT[], p_isCustom BOOLEAN)
  RETURNS TABLE (
    g_start time,
    g_end time
  )
AS $$
DECLARE
  cs "CustomSchedule";
  ds "DefaultSchedule";
BEGIN
  IF p_isCustom THEN
    FOR cs IN SELECT * FROM "CustomSchedule" AS C WHERE C.id = ANY(p_scheduleIds) LOOP
      FOR g_start, g_end IN SELECT * FROM generate_appointments(cs.start, cs.end, cs.appointmentduration) LOOP
        RETURN NEXT;
      END LOOP;
    END LOOP;
  ELSE
    FOR ds IN SELECT * FROM "DefaultSchedule" AS D WHERE D.id = ANY(p_scheduleIds) LOOP
      FOR g_start, g_end IN SELECT * FROM generate_appointments(ds.start, ds.end, ds.appointmentduration) LOOP
        RETURN NEXT;
      END LOOP;
    END LOOP;
  END IF;
END
$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION get_appointments(p_hostId INT, p_date DATE, p_scheduleIds INT[], p_isCustom BOOLEAN)
  RETURNS TABLE (
    g_visitorId INTEGER,
    g_visitorFullName VARCHAR(255),
    g_start time,
    g_end time,
    g_status appointment_status
  )
AS $$
BEGIN
  RETURN QUERY
  SELECT V.id, format('%s %s %s', V.surname, V.firstname, V.patronymic)::VARCHAR(255), a.start, a.end, A.status FROM "Appointment" AS A
    JOIN "User" AS V ON V.id = A.visitorid
  WHERE hostid = p_hostId AND date = p_date
  UNION
  SELECT NULL, NULL, G.g_start, G.g_end, NULL FROM generate_appointments(p_scheduleIds, p_isCustom) AS G
  WHERE G.g_start NOT IN (SELECT start FROM "Appointment" WHERE date = p_date);

END
$$ LANGUAGE 'plpgsql';

