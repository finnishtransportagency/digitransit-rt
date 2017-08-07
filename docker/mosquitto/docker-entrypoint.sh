#!/bin/sh
mosquitto_passwd -b /mosquitto/config/pw digitransit pwd
mosquitto_passwd -b /mosquitto/config/pw www www
set -e
exec "$@"
