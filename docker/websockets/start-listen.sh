#!/bin/sh

/listen.sh "digitransit/oulu/tripupdates" 9002 &
/listen.sh "digitransit/lahti/tripupdates" 9003 &
/listen.sh "digitransit/lahti/servicealert" 9004 &
/listen.sh "digitransit/jyvaskyla/tripupdates" 9005 &
/listen.sh "digitransit/vr/tripupdates" 9006 &
/listen.sh "digitransit/oulu/servicealert" 9007 &
/save.sh "digitransit/oulu/tripupdates" oulu-tripupdates &
/save.sh "digitransit/lahti/tripupdates" lahti-tripupdates &
/save.sh "digitransit/jyvaskyla/tripupdates" jyvaskyla-tripupdates &
/save.sh "digitransit/lahti/servicealert" lahti-servicealert &
/save.sh "digitransit/vr/tripupdates" vr-tripupdates &
/save.sh "digitransit/oulu/servicealert" oulu-servicealert &
echo "All services started"
while [ 1 ]
do
  sleep 1000
done
