#!/bin/bash

REALENV="dev"
RELOADPG=false

while getopts ':dsr' opt; do
    case $opt in
        "d") REALENV="dev";;
        "s") REALENV="staging";;
        "r") RELOADPG=true;;
        *) ;;
    esac
done

echo "Building for environment $REALENV"

if [ "$RELOADPG" = true ]
then
    echo "Preparing Postgis"
    cd postgis
    rm -R data/
    rm cities/*
    #if [ -e gtfs.zip ]
    #then
    #    echo "gtfs.zip exists, assuming its latest..."
    #else
        echo "Fetching gtfs.zip file"
        curl -sS "http://dev.hsl.fi/gtfs.lahti/lahti.zip" -o cities/lahti.zip
        curl -sS "http://www.transitdata.fi/oulu/google_transit.zip" -o cities/oulu.zip
        curl -sS "http://gtfs.trapeze.fi/jyvaskyla/gtfs-static/gtfs-data.zip" -o cities/jyvaskyla.zip
        #curl -sS "http://reaaliaikapilotti:salasana@api.reittiopas.fi/data/google_transit.zip" -o cities/trains.zip
        cp ../OpenTripPlanner-data-container/trains.zip cities/trains.zip
    #fi
docker-compose build
cd ..
fi

cd ..
./sbt -Drealtime.env=$REALENV assembly || exit 1

echo "Build ready, copying to targets"
cd docker
cp ../target/scala-2.11/root-assembly-0.1.0-SNAPSHOT.jar message-processor/root-assembly-0.1.0-SNAPSHOT.jar
cp ../target/scala-2.11/root-assembly-0.1.0-SNAPSHOT.jar gtfs-rt-connector-oulu/root-assembly-0.1.0-SNAPSHOT.jar
cp ../target/scala-2.11/root-assembly-0.1.0-SNAPSHOT.jar gtfs-binary-connector-oulu/tripupdate/root-assembly-0.1.0-SNAPSHOT.jar
cp ../target/scala-2.11/root-assembly-0.1.0-SNAPSHOT.jar gtfs-binary-connector-oulu/vehicleposition/root-assembly-0.1.0-SNAPSHOT.jar
cp ../target/scala-2.11/root-assembly-0.1.0-SNAPSHOT.jar gtfs-binary-connector-oulu/servicealert/root-assembly-0.1.0-SNAPSHOT.jar
cp ../target/scala-2.11/root-assembly-0.1.0-SNAPSHOT.jar gtfs-binary-connector-lahti/tripupdate/root-assembly-0.1.0-SNAPSHOT.jar
cp ../target/scala-2.11/root-assembly-0.1.0-SNAPSHOT.jar gtfs-binary-connector-lahti/vehicleposition/root-assembly-0.1.0-SNAPSHOT.jar
cp ../target/scala-2.11/root-assembly-0.1.0-SNAPSHOT.jar gtfs-binary-connector-lahti/servicealert/root-assembly-0.1.0-SNAPSHOT.jar
cp ../target/scala-2.11/root-assembly-0.1.0-SNAPSHOT.jar gtfs-binary-connector-jyvaskyla/tripupdate/root-assembly-0.1.0-SNAPSHOT.jar
cp ../target/scala-2.11/root-assembly-0.1.0-SNAPSHOT.jar gtfs-binary-connector-jyvaskyla/vehicleposition/root-assembly-0.1.0-SNAPSHOT.jar
cp ../target/scala-2.11/root-assembly-0.1.0-SNAPSHOT.jar json-connector-vr/tripupdate/root-assembly-0.1.0-SNAPSHOT.jar
cp ../target/scala-2.11/root-assembly-0.1.0-SNAPSHOT.jar json-connector-vr/vehicleposition/root-assembly-0.1.0-SNAPSHOT.jar
