#!/bin/sh
# sed is used to filter out the topic that is written before every message received.
/Net-MQTT-1.163170/bin/net-mqtt-sub -host realtimepilot.digitransit.eu -username digitransit -password pwd $1 | sed 's_^digitransit/[^ ]\+ __' | head -c -1
