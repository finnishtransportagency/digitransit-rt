# OpenTripPlanner-data-container
[![Build Status](https://snap-ci.com/HSLdevcom/OpenTripPlanner-data-container/branch/master/build_image)](https://snap-ci.com/HSLdevcom/OpenTripPlanner-data-container/branch/master)

## What does this image provide?

It:

1. Fetches HSL, Liikennevirasto, and 3rd party city GTFS data
2. Alters, and transforms that data
3. Forms OpenTripPlanner router zip files from the data
4. Starts http server and listens to port 8080

From http server, you can get:
- http://localhost:8080/routers.txt

That file contains all available router zip bundles that currently are:
- http://localhost:8080/router-finland.zip
- http://localhost:8080/router-hsl.zip

each of the bundles contain also OpenStreetMap graph file, so you should be able to load them into OpenTripPlanner
