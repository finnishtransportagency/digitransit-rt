import os,sys,csv

cityFolders = sys.argv

agencyList = []
calendarList = []
datesList = []
routesList = []
shapesList = []
stopsList = []
stopTimesList = []
transfersList = []
tripsList = []

for folder in cityFolders:
  for directory,subs,files in os.walk(folder):
    for filename in files:
      if (filename == 'agency.txt'):
        agencyList.append(directory + filename)
      if (filename == 'calendar.txt'):
        calendarList.append(directory + filename)
      if (filename == 'calendar_dates.txt'):
        datesList.append(directory + filename)
      if (filename == 'routes.txt'):
        routesList.append(directory + filename)
      if (filename == 'shapes.txt'):
        shapesList.append(directory + filename)
      if (filename == 'stops.txt'):
        stopsList.append(directory + filename)
      if (filename == 'stop_times.txt'):
        stopTimesList.append(directory + filename)
      if (filename == 'transfers.txt'):
        transfersList.append(directory + filename)
      if (filename == 'trips.txt'):
      	tripsList.append(directory + filename)


agency_headers = ['agency_id','agency_name','agency_url','agency_timezone','agency_lang','agency_phone','agency_fare_url']
calendar_headers = ['service_id','monday','tuesday','wednesday','thursday','friday','saturday','sunday','start_date','end_date']
dates_headers = ['service_id','date','exception_type']
routes_headers = ['route_id','agency_id','route_short_name','route_long_name','route_desc','route_type','route_url','route_color','route_text_color']
shapes_headers = ['shape_id','shape_pt_lat','shape_pt_lon','shape_pt_sequence','shape_dist_traveled']
stops_headers = ['stop_id','stop_code','stop_name','stop_desc','stop_lat','stop_lon','zone_id','stop_url','location_type','parent_station','stop_timezone','vehicle_type','platform_code' ]
stopTimes_headers = ['trip_id','arrival_time','departure_time','stop_id','stop_sequence','stop_headsign','pickup_type','drop_off_type','shape_dist_traveled','timepoint']
transfers_headers = ['from_stop_id','to_stop_id','transfer_type','min_transfer_time']
trips_headers = ['route_id','service_id','trip_id','trip_headsign','direction_id','block_id','shape_id','wheelchair_accessible','bikes_allowed']


def mergeFiles (inputs, final_headers, out_file):
  print("Creating merged " + out_file)
  for filename in inputs:
    with open(filename, "rb") as f_in:
      reader = csv.reader(f_in)
      headers = reader.next()
      for h in headers:
        if h not in final_headers:
          final_headers.append(h)

  with open(out_file, "wb") as f_out:
    writer = csv.DictWriter(f_out,fieldnames=final_headers)
    writer.writeheader()
    for filename in inputs:
      with open(filename, "rb") as f_in:
        dir = os.path.dirname(filename)
        reader = csv.DictReader(f_in, skipinitialspace=True)
        for line in reader:
          for column in line:
            if column == "route_id":
              line[column] = dir.upper() + "_" + line[column]
          writer.writerow(line)
  return;


mergeFiles(agencyList, agency_headers, 'gtfs/agency.txt')
mergeFiles(calendarList, calendar_headers, 'gtfs/calendar.txt')
mergeFiles(datesList, dates_headers, 'gtfs/calendar_dates.txt')
mergeFiles(routesList, routes_headers, 'gtfs/routes.txt')
mergeFiles(shapesList, shapes_headers, 'gtfs/shapes.txt')
mergeFiles(stopsList, stops_headers, 'gtfs/stops.txt')
mergeFiles(stopTimesList, stopTimes_headers, 'gtfs/stop_times.txt')
mergeFiles(transfersList, transfers_headers, 'gtfs/transfers.txt')
mergeFiles(tripsList, trips_headers, 'gtfs/trips.txt')