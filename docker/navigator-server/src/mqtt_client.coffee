gtfs = require 'gtfs-realtime-bindings'
mqtt = require 'mqtt'
moment = require 'moment-timezone'
mqtt_publisher = require './mqtt_publisher.js'

# MQTTClient connects to Mosquitto server (Realtime API of vehicle locations) and
# converts the received real-time data to the format used by city-navigator clients.
# to the clients.
class MqttClient
  constructor: (@callback, @args) ->

  connect: =>
    @client = mqtt.connect('mqtt://realtimepilot.digitransit.eu:1883', { username:'digitransit', password: 'pwd'})
    @client.on 'connect', =>
      console.log 'MQTT Navigator Client connected'
      @client.subscribe('/hfp/journey/#')
    @client.on 'message', (topic, message) =>
      @handle_message(topic, message)


  handle_message: (topic, message) =>
    [_, _, _, mode, vehi, route, dir, headsign, start_time, next_stop] = topic.split '/'

    info = JSON.parse(message).VP

    out_info =
      vehicle:
        id: info.id
      trip:
        route: info.route
        operator: info.operator
        direction: info.direction
        start_time: info.start_time
        start_date: info.oday
      position:
        latitude: info.lat
        longitude: info.long
        bearing: info.hdg
        odometer: info.odometer
        next_stop: next_stop
        speed: info.speed
        delay: info.delay
        next_stop_index: info.stop_index
      timestamp: info.tsi
      source: info.source

    # Create path/channel that is used for publishing the out_info for the
    # interested navigator-proto clients via the @callback function
    route = out_info.trip.route.replace " ", "_"
    vehicle_id = out_info.vehicle.id.replace " ", "_"
    path = "/location/#{out_info.source}/#{route}/#{vehicle_id}"
    @callback path, out_info, @args

module.exports.MqttClient = MqttClient # make MQTTClient visible in server.coffee
