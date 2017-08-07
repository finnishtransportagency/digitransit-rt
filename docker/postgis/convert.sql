CREATE TABLE IF NOT EXISTS public.last_import (
    hashsum text
);

-- Convert to internal format

DROP TABLE IF EXISTS public.stop_timetables;
DROP TABLE IF EXISTS public.operating_days;
DROP TABLE IF EXISTS public.trips;
DROP TABLE IF EXISTS public.stops;
DROP TABLE IF EXISTS public.routes;
DROP TABLE IF EXISTS public.agency;

CREATE TABLE agency(
  agency_id text not null PRIMARY KEY,
  agency_name text not null,
  agency_url text,
  agency_timezone text,
  agency_lang text,
  agency_phone text,
  agency_fare_url text,
  connector_name text
);

CREATE TABLE operating_days (
  service_id text not null PRIMARY KEY,
  monday boolean not null,
  tuesday boolean not null,
  wednesday boolean not null,
  thursday boolean not null,
  friday boolean not null,
  saturday boolean not null,
  sunday boolean not null,
  start_date date not null,
  end_date date
);

INSERT INTO agency (SELECT *, null as connector_name FROM import.agency);

INSERT INTO operating_days
  (SELECT service_id,
     monday=1 as monday,
     tuesday=1 as tuesday,
     wednesday=1 as wednesday,
     thursday=1 as thursday,
     friday=1 as friday,
     saturday=1 as saturday,
     sunday=1 as sunday,
     TO_DATE(start_date, 'YYYYMMDD') as start_date,
     TO_DATE(end_date, 'YYYYMMDD') as end_date
   FROM import.calendar);

-- Calendar dates
CREATE TABLE calendar_dates AS (SELECT * FROM import.calendar_dates);

-- Routes
CREATE TABLE routes AS (SELECT route_id, agency_id, route_short_name, route_long_name, route_type FROM import.routes);
ALTER TABLE routes ADD PRIMARY KEY (route_id);
SELECT AddGeometryColumn ('public','routes','route_shape',4326,'LINESTRING',2);

-- Route shapes
UPDATE routes SET route_shape =
  (SELECT ST_MakeLine(ST_GeomFromEWKT('SRID=4326;POINT(' || shape_pt_lat || ' ' || shape_pt_lon || ')')) as locations
     FROM import.shapes WHERE shape_id = route_id);
CREATE INDEX routes_gix ON routes USING GIST (route_shape);
ALTER TABLE routes ADD CONSTRAINT agencyfk FOREIGN KEY (agency_id) REFERENCES agency (agency_id) MATCH FULL;

-- Stops
DROP SEQUENCE IF EXISTS stops_seq;
CREATE SEQUENCE stops_seq CACHE 1000;
CREATE TABLE stops AS (SELECT nextval('stops_seq') as id, stop_id, stop_code, stop_name,
                         st_geomfromtext('POINT(' || stop_lat || ' ' || stop_lon || ')') as location,
                         stop_lat, stop_lon, zone_id, stop_url, location_type FROM import.stops);
ALTER TABLE stops ADD PRIMARY KEY (id);
ALTER TABLE stops ALTER COLUMN id SET DEFAULT nextval('stops_seq');
ALTER TABLE stops ALTER COLUMN location TYPE geometry(POINT,4326) USING ST_SetSRID(location,4326);
ALTER TABLE stops ALTER COLUMN stop_id SET NOT NULL;
CREATE UNIQUE INDEX stops_uniq_idx ON stops(stop_id);
CREATE INDEX stops_gix ON stops USING GIST (location);

-- Trips
DROP SEQUENCE IF EXISTS trips_seq;
CREATE SEQUENCE trips_seq CACHE 1000;
CREATE TABLE trips AS (SELECT nextval('trips_seq') as id, route_id, service_id, trip_id, trip_headsign, direction_id,block_id
                       FROM import.trips);
ALTER TABLE trips ADD PRIMARY KEY (id);
ALTER TABLE trips ALTER COLUMN id SET DEFAULT nextval('trips_seq');
CREATE UNIQUE INDEX trips_uniq_idx ON trips(trip_id);
ALTER TABLE trips ADD CONSTRAINT routefk FOREIGN KEY (route_id) REFERENCES routes (route_id) MATCH FULL;

-- Stop timetables
DROP SEQUENCE IF EXISTS stop_timetables_seq;
CREATE SEQUENCE stop_timetables_seq CACHE 1000;
CREATE TABLE stop_timetables AS (SELECT nextval('stop_timetables_seq') as id, trips.trip_id AS trip_id,
                                        stops.stop_id AS stop_id, stop_sequence, arrival_time::interval,
                                   departure_time::interval, shape_dist_traveled FROM import.stop_times st
  LEFT JOIN stops ON (stops.stop_id = st.stop_id) LEFT JOIN trips ON (trips.trip_id = st.trip_id));
ALTER TABLE stop_timetables ADD PRIMARY KEY (id);
ALTER TABLE stop_timetables ALTER COLUMN id SET DEFAULT nextval('stop_timetables_seq');
ALTER TABLE stop_timetables ADD CONSTRAINT tripfk FOREIGN KEY (trip_id) REFERENCES trips (trip_id) MATCH FULL;
ALTER TABLE stop_timetables ADD CONSTRAINT stopfk FOREIGN KEY (stop_id) REFERENCES stops (stop_id) MATCH FULL;

-- Connector mappings:
-- Add here new connector names for message processing mappings (use connector configuration to add a name such as 'oulu' to the message)
UPDATE agency SET connector_name = 'oulu' WHERE agency_name in ('Oulun joukkoliikenne', 'R-kioskit');
UPDATE agency SET connector_name = 'lahti' WHERE agency_name in ('Koiviston Auto Oy', 'Lehtimäen Liikenne Oy','Lahden Seudun Liikenne');
UPDATE agency SET connector_name = 'trains' WHERE agency_name = 'VR Osakeyhtiö';

-- Indexes
CREATE INDEX agency_connector_idx ON agency(connector_name) WHERE connector_name IS NOT NULL;
CREATE INDEX operating_days_validity ON operating_days(start_date, end_date);
CREATE INDEX routes_agency_idx ON routes(agency_id);
CREATE UNIQUE INDEX stop_timetables_trip_idx ON stop_timetables(trip_id, stop_sequence);
CREATE INDEX stop_timetables_stop_idx ON stop_timetables(stop_id);
CREATE INDEX trips_route_idx ON trips(route_id);
CREATE INDEX trips_seid_idx ON trips(trip_id);
CREATE INDEX routes_short_name_idx ON routes(route_short_name);
