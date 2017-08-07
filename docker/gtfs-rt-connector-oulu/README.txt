GtfsRtConnector usage:
1. build assembled jar with sbt (this will also contain message-processor)
$ sbt assembly

2. copy the resulting jar file to docker/gtfs-rt-connector-oulu
$ cp target/scala-2.11/root-assembly-0.1.0-SNAPSHOT.jar docker/gtfs-rt-connector-oulu/

3. build Docker image
$ docker build -t gtfs-rt-connector-oulu:v1

4. run the image
$ docker run gtfs-rt-connector-oulu:v1

Parameters for connector are specified in Dockerfile. Edit those for your own needs 
java -cp root-assembly-0.1.0-SNAPSHOT.jar fi.liikennevirasto.realtime.GtfsRtConnector --host livirtp-dev-acsagents.westeurope.cloudapp.azure.com --port 1883 --sendto transit --source http://92.62.36.215/RTIX/trip-updates/text

--host and --port = mqtt address. Free tip for dev purposes: command docker ps shows where Mosquitto is running
--sendto = mqtt topic
--source = in the case points to GTFS-RT json feed from city of Oulu 
