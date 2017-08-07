FROM mdillon/postgis
MAINTAINER CGI

# Add some metadata to the image
LABEL Description="Postgis database"

COPY fixture.sql /
COPY convert.sql /
COPY statistic.sql /
COPY ratapurkki.dmp /
COPY cities /tmp/
COPY data_merger.py /tmp/

RUN mkdir /ratapurkki

RUN apt-get update
RUN apt-get -y install zip unzip dos2unix && \
    chmod -R 777 /tmp/ && \
    chown postgres:postgres /ratapurkki

COPY postgresql.conf /
# Add init script. The image will execute the script automatically
ADD x-init-db-user.sh /docker-entrypoint-initdb.d/

