DROP TABLE IF EXISTS public.vehicle_positions;
DROP TABLE IF EXISTS public.trip_updates;
DROP TABLE IF EXISTS public.stop_time_update;

CREATE TABLE vehicle_positions (
  id BIGSERIAL NOT NULL PRIMARY KEY,
  timestamp TEXT NOT NULL,
  feed_origin VARCHAR(40) NOT NULL,
  trip_id TEXT NOT NULL,
  route_id TEXT NULL,
  vehicle_id TEXT NOT NULL,
  latitude DECIMAL NOT NULL,
  longitude DECIMAL NOT NULL
);

CREATE TABLE trip_updates (
  id BIGSERIAL NOT NULL PRIMARY KEY,
  trip_update_id TEXT NOT NULL,
  timestamp TEXT NOT NULL,
  trip_id TEXT NOT NULL,
  route_id TEXT NOT NULL,
  direction_id NUMERIC,
  start_date TEXT,
  start_time TEXT,
  vehicle_id TEXT
);

CREATE TABLE stop_time_update (
  id BIGSERIAL NOT NULL PRIMARY KEY,
  trip_update_id BIGSERIAL NOT NULL,
  stop_id TEXT,
  update_sequence NUMERIC,
  stop_sequence NUMERIC,
  arrival_time NUMERIC,
  arrival_delay NUMERIC,
  departure_time NUMERIC,
  departure_delay NUMERIC
);
