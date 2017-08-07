
http = require 'http'
faye = require 'faye'
url = require 'url'
mqtt_publisher = require './mqtt_publisher.js'


# global state: a mapping from vehicle id to its latest data
state = {}

# faye is used for handling lower level messaging between this navigator-server and
# navigator-clients.
bayeux = new faye.NodeAdapter
    mount: '/faye', timeout: 45

# Handle non-Bayeux requests
server = http.createServer (request, response) ->
  console.log "#{request.method} #{request.url}"
  pathname = url.parse(request.url).pathname
  now = new Date()

  if pathname.match /^\/hfp\//
    response.writeHead 200, {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'}
    messages = {}
    pattern = decodeURIComponent(pathname).replace /\/$/, "/#"
    for id, data of state
      topic = mqtt_publisher.to_mqtt_topic(data)
      if now.getTime() < data.timestamp*1000 + 5*60*1000 and mqtt_publisher.mqtt_match(pattern, topic)
        messages[topic] = mqtt_publisher.to_mqtt_payload(data)
    response.write JSON.stringify messages
    response.end()
  else
    response.writeHead 404, {'Content-Type': 'text/plain'}
    response.write "Not found: #{request.url}"
    response.end()
    
  
bayeux.attach server
server.bayeux = bayeux
module.exports = server # Make server visible in the Gruntfile.coffee

bayeux.bind 'handshake', (client_id) ->
    console.log "client " + client_id + " connected"

client = bayeux.getClient()

handle_event = (path, msg) ->
    state[msg.vehicle.id] = msg
    client.publish path, msg

mqtt = require './mqtt_client.js'

mqtt_client = new mqtt.MqttClient handle_event
mqtt_client.connect()
