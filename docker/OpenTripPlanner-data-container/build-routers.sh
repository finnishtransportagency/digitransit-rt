#!/bin/bash
set -e
set -x
set -o pipefail

# Default value is for tmalloc threshold is 1073741824
# This is too small, and causes GTFS shape mapfit to
# log info, which then breaks the build
# Therefore, we increase this value
export TCMALLOC_LARGE_ALLOC_REPORT_THRESHOLD=2147483648

# Base locations
ROOT=/opt/opentripplanner-data-container
ROUTER_FINLAND=$ROOT/router-finland

# Tools
FIT_GTFS=$ROOT/gtfs_shape_mapfit/fit_gtfs.bash
OBA_GTFS=$ROOT/one-busaway-gtfs-transformer/onebusaway-gtfs-transformer-cli.jar


function retrieveJyvaskyla() {
  echo "Retrieving Jyväskylä data..."
  cd $ROUTER_FINLAND
  curl -sS "http://gtfs.trapeze.fi/jyvaskyla/gtfs-static/gtfs-data.zip" -o jyvaskyla.zip
  add_feed_id jyvaskyla.zip JYVASKYLA

  #cp jyvaskyla.zip $ROUTER_WALTTI
}

function retrieveOulu() {
  echo "Retrieving Oulu data..."
  cd $ROUTER_FINLAND
  curl -sS "http://www.transitdata.fi/oulu/google_transit.zip" -o oulu.zip
  add_feed_id oulu.zip OULU

  #cp oulu.zip $ROUTER_WALTTI
}

function retrieveLahti() {
  echo "Retrieving Lahti data..."
  cd $ROUTER_FINLAND
  curl -sS "http://dev.hsl.fi/gtfs.lahti/lahti.zip" -o lahti.zip

  add_feed_id lahti.zip LAHTI
}

function retrieveVrTrains() {
    echo "Retrieving rail data..."
    cd $ROUTER_FINLAND
    #curl -sS "http://reaaliaikapilotti:salasana@api.reittiopas.fi/data/google_transit.zip" -o trains.zip
    cp ../trains.zip trains.zip
    add_feed_id trains.zip VR
}

#add (or modify) feed_info.txt that contains the feed_id
function add_feed_id() {
  set +o pipefail
  filename=$1
  id=$2

  contains_fileinfo=`unzip -l $filename|grep feed_info.txt|wc -l`

  if [ "$contains_fileinfo" -ne "1" ]; then
    echo "creating new feed-info"
    #no feed info available in zip, write whole file
    cat <<EOT > feed_info.txt
feed_publisher_name,feed_publisher_url,feed_lang,feed_id
$id-fake-name,$id-fake-url,$id-fake-lang,$id
EOT
  else
    unzip -o $filename feed_info.txt
    #see if if feed_id is already there
    count=`grep feed_id feed_info.txt|wc -l`
    if [ "$count" -ne "1" ]; then
      echo "adding feed_id column"
      #no feed_id in feed_info.txt, append
      awk -vc="feed_id" -vd="$id" 'NR==1{$0=c","$0}NR!=1{$0=d","$0}1' feed_info.txt > feed_info_new.txt
    else
      echo "changing existing id"
      #existing feed_id, replace it
      original=`awk -F ',' 'NR==1 {for (i=1; i<=NF; i++) {ix[$i] = iNR>1 {printf "%s\n", $ix["feed_id"]}' c1=feed_id feed_info.txt`
      echo "chaning existing id $original to $id"
      cat feed_info.txt | sed s/$original/$id/g > feed_info_new.txt
    fi
    mv feed_info_new.txt feed_info.txt
  fi

  # add feed_info to zip
  zip $filename feed_info.txt
  set -o pipefail
}

retrieveJyvaskyla
retrieveOulu
retrieveLahti
retrieveVrTrains

# build zip packages
mkdir ${WEBROOT}
cd $ROOT
zip -D ${WEBROOT}/router-finland.zip router-finland/*
cp ${WORK}/routers.txt ${WEBROOT}
