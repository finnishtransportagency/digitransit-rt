0 Run ./prepare-docker.sh to build jar files for connectors
1 Change the IP of the hostmachine in the docker-compose.yml file to point to the logstash port
2 run: docker-compose build (this will build all the images)
3 run: docker-compose up (this will start all containers)
3 open the http://<hostmachine>:5601 to access the Kibana interface

If you are building for Azure deployment, use
./prepare-docker.sh staging
to build with configuration suitable for Azure environment.
