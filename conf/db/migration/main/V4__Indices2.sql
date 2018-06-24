DROP INDEX appointment_generic_select_idx;
CREATE INDEX appointment_generic_select_idx ON "Appointment" (visitorid, scheduleid)
