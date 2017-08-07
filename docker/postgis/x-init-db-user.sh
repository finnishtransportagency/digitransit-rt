#!/usr/bin/env bash

if psql -lqt | cut -d \| -f 1 | grep -qw livi_realtime_test; then
  echo "Database exists, not creating from scratch"
  CHECK_DB=1
else
  echo "Initializing database"
  psql -v ON_ERROR_STOP=1 --username postgres <<-EOSQL
        CREATE USER livi_realtime_test PASSWORD 'changeme';
        CREATE DATABASE livi_realtime_test TEMPLATE template_postgis;
        ALTER DATABASE livi_realtime_test SET timezone TO 'Europe/Helsinki';
        CREATE TABLESPACE data_pg OWNER livi_realtime_test LOCATION '/ratapurkki';
        CREATE ROLE osboxes;
        GRANT osboxes to livi_realtime_test;
        GRANT ALL PRIVILEGES ON DATABASE livi_realtime_test TO livi_realtime_test;
EOSQL
  CHECK_DB=0
fi

cd /tmp/
mkdir gtfs/
set -e
find -type f -iname "*.zip" -print0 | while IFS= read -r -d $'\0' zipPath; do

    zipFile="${zipPath##*/}"
    folderName="${zipFile%.zip}"
    mkdir -p "${folderName}/"
    cp ${zipPath} "/tmp/${folderName}/"
    cd ${folderName/}
    unzip ${zipFile}
    [ -f agency.txt ] && dos2unix agency.txt
    [ -f calendar.txt ] && dos2unix calendar.txt
    [ -f calendar_dates.txt ] && dos2unix calendar_dates.txt
    [ -f routes.txt ] && dos2unix routes.txt
    [ -f shapes.txt ] && dos2unix shapes.txt
    [ -f stops.txt ] && dos2unix stops.txt
    [ -f stop_times.txt ] && dos2unix stop_times.txt
    [ -f transfers.txt ] && dos2unix transfers.txt
    [ -f trips.txt ] && dos2unix trips.txt
    cd ..

done

python data_merger.py oulu/ lahti/ jyvaskyla/ trains/
cd gtfs/
zip gtfs *
MYPWD=$(pwd)
PGPASSWORD=changeme psql -U livi_realtime_test -d livi_realtime_test -c "CREATE TABLE IF NOT EXISTS public.last_import (hashsum text);" > /dev/null
PGPASSWORD=changeme psql -U livi_realtime_test -d livi_realtime_test -c "SELECT hashsum from last_import;" -A -t 1> gtfs.zip.md5 2> /dev/null || echo "No previous installation found"

echo "md5 sum contents:"
cat gtfs.zip.md5
echo "-----------------"

if [ -s gtfs.zip.md5 ]
then
  echo "md5 sum found"
  HASHSUM=$(md5sum --status -c gtfs.zip.md5)
else
  echo "md5 sum not found"
  HASHSUM=1
fi

echo "Exit level is $HASHSUM"

if [ $HASHSUM -eq 0 ]
then
  echo "Fixture has not changed, not reloading"
else
  echo "Reloading static data from file"
  PGPASSWORD=changeme psql -U livi_realtime_test -n -v ON_ERROR_STOP=1 -f /fixture.sql
  PGPASSWORD=changeme psql -U livi_realtime_test -n -v ON_ERROR_STOP=1 -f /convert.sql
  PGPASSWORD=changeme psql -U livi_realtime_test -n -v ON_ERROR_STOP=1 -f /statistic.sql
  PGPASSWORD=changeme psql -U livi_realtime_test -c "TRUNCATE last_import"
  PGPASSWORD=changeme psql -U livi_realtime_test -c "INSERT INTO last_import VALUES ('`md5sum gtfs.zip`')"
  pg_restore -U livi_realtime_test -d livi_realtime_test /ratapurkki.dmp
  echo "Ratapurkki data loaded successfully"
fi

cd $MYPWD
