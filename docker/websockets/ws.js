var AsyncLock = require('async-lock');
var WebSocketServer = require('websocket').server;
var http = require('http');

const readline = require('readline');
var lock = new AsyncLock();

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
  terminal: false
});

var server = http.createServer(function(req, resp) {
    resp.writeHead(404);
    resp.write("Only websockets are supported");
    resp.end();
});

var port = process.argv[2];

if (!port) {
    console.log("Undefined port! Use: 'node ws.js 8080' for example");
    process.exit(1)
}

server.listen(port, function() {
    console.log("Started server on port " + port);
});

wsServer = new WebSocketServer({
    httpServer: server,
    autoAcceptConnections: false
});

var connections = [];

wsServer.on('request', function(request) {
/*    if (request.remoteAddress != '127.0.0.1' && request.remoteAddress != 'localhost' && request.remoteAddress != '::ffff:127.0.0.1' && request.remoteAddress != '::1') {
      console.log((new Date()) + " Rejected request from " + request.remoteAddress);
      request.reject(403, request.remoteAddress + " not in accepted list");
      return;
    }
*/
    var connection = request.accept(null, request.origin);
    console.log((new Date()) + ' Connection accepted.');
    lock.acquire('conns', function(done) {
        connections.push(connection);
        done(null,null);
    }, function(err,ret) {
       console.log("Connection count is now " + connections.length);
    });
    connection.on('message', function(message) {
        if (message.type === 'utf8') {
            console.log('Received Message: ' + message.utf8Data);
            connection.sendUTF("This is a read-only feed");
        }
        else if (message.type === 'binary') {
            console.log('Received Binary Message of ' + message.binaryData.length + ' bytes');
            connection.sendUTF("This is a read-only feed");
        }
    });
    connection.on('close', function(reasonCode, description) {
        console.log((new Date()) + connection.remoteAddress + ' disconnected.');
        lock.acquire('conns', function(done) {
            connections = connections.filter(function(item) { return (item !== connection) });
            done(null,null);
        }, function(err,ret) {
           console.log("Connection count is now " + connections.length);
        });
    });
});

rl.on('line', function(input){
  connections.forEach(function (conn) { conn.sendUTF(input); });
});

rl.on('SIGINT', function() {
  console.log("Ctrl+C pressed, exiting");
  process.exit(0);
});

