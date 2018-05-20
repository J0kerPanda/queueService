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