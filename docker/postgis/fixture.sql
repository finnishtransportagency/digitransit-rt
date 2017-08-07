CREATE SCHEMA IF NOT EXISTS import;

drop table if exists import.shapes;
drop table if exists import.stop_times;
drop table if exists import.trips;
drop table if exists import.calendar_dates;
drop table if exists import.calendar;
drop table if exists import.stops;
drop table if exists import.routes;
drop table if exists import.agency;

-- operators
create table import.agency(
  agency_id text not null,
  agency_name text not null,
  agency_url text,
  agency_timezone text,
  agency_lang text,
  agency_phone text,
  agency_fare_url text
);

-- routes
create table import.routes(
  route_id text not null,
  agency_id text not null,
  route_short_name text,
  route_long_name text,
  route_desc text,
  route_type int,
  route_url text,
  route_color text,
  route_text_color text
);

-- stops in municipality
create table import.stops(
  stop_id text not null,
  stop_code text,
  stop_name text,
  stop_desc text,
  stop_lat numeric,
  stop_lon numeric,
  zone_id text,
  stop_url text,
  location_type text,
  parent_station text,
  stop_timezone text,
  vehicle_type NUMERIC,
  platform_code TEXT
--  stop_desc text
);

-- days a service is operated
create table import.calendar(
  service_id text not null,
  monday int not null,
  tuesday int not null,
  wednesday int not null,
  thursday int not null,
  friday int not null,
  saturday int not null,
  sunday int not null,
  start_date varchar(8) not null,
  end_date varchar(8) not null  
);

-- exceptions by date
create table import.calendar_dates(
  service_id text not null,
  date varchar(8) not null,
  exception_type int not null
);

-- trip-service mapping?
create table import.trips (
  route_id text not null,
  service_id text not null,
  trip_id text not null,
  trip_headsign text,
  direction_id NUMERIC,
  block_id text,
  shape_id TEXT,
  wheelchair_accessible numeric,
  bikes_allowed NUMERIC
);

create table import.shapes (
  shape_id text not null,
  shape_pt_sequence int not null,
  shape_pt_lat numeric not null,
  shape_pt_lon numeric not null
);

-- static stopping times
create table import.stop_times (
  trip_id text not null,
  arrival_time text not null, --importing as text because of non-standard time format, read later as interval
  departure_time text not null, --importing as text because of non-standard time format, read later as interval
  stop_id text not null,
  stop_sequence int not null,
  stop_headsign text,
  pickup_type text,
  drop_off_type text,
  shape_dist_traveled text,
  timepoint text
);

\copy import.agency from agency.txt csv header
\copy import.calendar from calendar.txt csv header
\copy import.calendar_dates from calendar_dates.txt csv header
\copy import.routes from routes.txt csv header
\copy import.stops from stops.txt csv header
\copy import.trips from trips.txt csv header
\copy import.stop_times from stop_times.txt csv header
-- \copy import.shapes from shapes.txt csv header

ALTER TABLE import.agency ADD PRIMARY KEY (agency_id);
ALTER TABLE import.routes ADD PRIMARY KEY (route_id);
ALTER TABLE import.stops ADD PRIMARY KEY (stop_id);
ALTER TABLE import.calendar ADD PRIMARY KEY (service_id);
ALTER TABLE import.trips ADD PRIMARY KEY (trip_id);
ALTER TABLE import.shapes ADD PRIMARY KEY (shape_id, shape_pt_sequence);

ALTER TABLE import.routes ADD CONSTRAINT agencyfk FOREIGN KEY (agency_id) REFERENCES import.agency (agency_id) MATCH FULL;
--ALTER TABLE import.calendar_dates ADD CONSTRAINT servicefk FOREIGN KEY (service_id) REFERENCES import.calendar (service_id) MATCH FULL;
ALTER TABLE import.trips ADD CONSTRAINT routefk FOREIGN KEY (route_id) REFERENCES import.routes(route_id) MATCH FULL;
--ALTER TABLE import.trips ADD CONSTRAINT servicefk FOREIGN KEY (service_id) REFERENCES import.calendar (service_id) MATCH FULL;
ALTER TABLE import.trips ADD CONSTRAINT tripfk FOREIGN KEY (trip_id) REFERENCES import.trips(trip_id) MATCH FULL;
ALTER TABLE import.stop_times ADD CONSTRAINT tripfk FOREIGN KEY (trip_id) REFERENCES import.trips(trip_id) MATCH FULL;
ALTER TABLE import.stop_times ADD CONSTRAINT agencyfk FOREIGN KEY (stop_id) REFERENCES import.stops(stop_id) MATCH FULL;

analyze;

