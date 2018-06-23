DROP INDEX host_day_unique_idx;

CREATE INDEX user_is_host_idx ON "User" (ishost);

CREATE INDEX schedule_repeat_id_fk_idx ON "Schedule" (repeatid);

CREATE INDEX repeated_schedule_host_id_fk_idx ON "RepeatedSchedule" (hostid);

CREATE INDEX appointment_interval_idx ON "Appointment" (start, "end");
CREATE INDEX appointment_generic_select_idx ON "Appointment" (scheduleid, visitorid)
