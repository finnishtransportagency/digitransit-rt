#!/bin/sh

TMP_FILE="/tmp/mqtt_$$.tmp"
OUT_FILE="/var/www/html/$2"

echo "waiting $1 -> $2 in PID $$"
while [ 1 ]
do
  /Net-MQTT-1.163170/bin/net-mqtt-sub -host realtimepilot.digitransit.eu -username digitransit -password pwd -one $1 | sed 's_^digitransit/[^ ]\+ __' |head -c -1 >/tmp/mqtt_$$.tmp
  if [ $? -eq 0 ]; then
    cp "$TMP_FILE" "$OUT_FILE" || echo "WARNING: Failed to update file"
  else
    echo "MQTT read error on $1 -> $2"
    sleep 5
  fi
done
