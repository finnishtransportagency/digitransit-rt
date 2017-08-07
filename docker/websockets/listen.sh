#!/bin/sh

while [ 1 ]
do
  echo "starting mqtt-listen $1 -> $2"
  /mqtt-listen.sh $1 | nodejs /ws.js $2
  echo "mqtt-listen.sh $1 -> $2 died, restarting after 1 second"
#  systemctl restart nginx
  sleep 1
done
