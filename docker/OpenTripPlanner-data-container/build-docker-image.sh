#!/bin/bash
NAME=opentripplanner-data-container
DOCKER_TAG=$1

ORG=$2
DOCKER_IMAGE=$NAME

#echo "Enter dockerhub credentials"
#echo "---------------------------"

echo -n Username:
read USERNAME

echo -n Password:
read -s PASSWORD

# Set these environment variables
#${DOCKER_TAG:="latest"}
DOCKER_USER=$USERNAME
DOCKER_AUTH=$PASSWORD

# Build image
docker build --tag="$ORG/$DOCKER_IMAGE:builder" -f Dockerfile.builder .
mkdir target
docker run --rm --entrypoint tar $ORG/opentripplanner-data-container:builder -c /opt/opentripplanner-data-container/webroot|tar x -C target 
docker build --tag="$ORG/$DOCKER_IMAGE:$DOCKER_TAG" -f Dockerfile .
docker login -u $DOCKER_USER -p $DOCKER_AUTH
docker push $ORG/$DOCKER_IMAGE:$DOCKER_TAG
docker tag $ORG/$DOCKER_IMAGE:$DOCKER_TAG $ORG/$DOCKER_IMAGE:latest
docker push $ORG/$DOCKER_IMAGE:latest

rm -rf target
