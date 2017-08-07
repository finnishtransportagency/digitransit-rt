FROM debian:jessie

LABEL Description="Mosquitto MQTT Broker"

COPY mosquitto.conf /
COPY mosquitto.acl /
COPY docker-entrypoint.sh /

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y wget \
    && wget -q -O - http://repo.mosquitto.org/debian/mosquitto-repo.gpg.key | apt-key add - \
    && wget -q -O /etc/apt/sources.list.d/mosquitto-jessie.list http://repo.mosquitto.org/debian/mosquitto-jessie.list \
    && apt-get update \
    && apt-get install -y mosquitto \
    && mkdir -p /mosquitto/config /mosquitto/log \
    && cp /mosquitto.conf /mosquitto/config \
	&& cp /mosquitto.acl /mosquitto/config \
	&& cat > /mosquitto/config/pw \
	&& cat > /mosquitto/log/mosquitto.log \
	&& chown -R mosquitto:mosquitto /mosquitto \
	&& chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]

CMD ["/usr/sbin/mosquitto", "-c", "/mosquitto/config/mosquitto.conf"]