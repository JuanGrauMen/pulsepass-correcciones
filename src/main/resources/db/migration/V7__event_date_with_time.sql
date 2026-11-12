-- La API recibe y devuelve fecha y hora del evento (LocalDateTime), pero la
-- columna era DATE y la hora se perdia al guardar. Con ella, las reglas de
-- "fecha futura" se median por dia: un evento de hoy a las 23:00 se rechazaba
-- como no futuro. Se pasa a TIMESTAMP.
--
-- Los eventos existentes conservan su dia y quedan a las 00:00, que es lo que
-- ya devolvia la API para ellos.
ALTER TABLE events
    ALTER COLUMN event_date TYPE TIMESTAMP USING event_date::timestamp;
