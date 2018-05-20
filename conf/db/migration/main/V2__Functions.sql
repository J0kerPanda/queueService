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
