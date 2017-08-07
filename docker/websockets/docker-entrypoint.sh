#!/bin/sh
cd /
npm install websocket async-lock 
cd /Net-MQTT-1.163170/
perl Makefile.PL
make
make install
/etc/init.d/nginx start
set -e
exec "$@"
