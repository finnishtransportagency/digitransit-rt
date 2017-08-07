FROM debian:jessie

LABEL Description="Websockets forwarding agent"

COPY docker-entrypoint.sh /
COPY Net-MQTT-1.163170.tar.gz /
COPY mqtt-listen.sh /
COPY start-listen.sh /
COPY listen.sh /
COPY ws.js /
COPY save.sh /

#telnet, curl, nano, tcpdump for debugging purposes
RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y perl make nginx \
    && apt-get install -y telnet curl nano tcpdump \
    && tar xzf /Net-MQTT-1.163170.tar.gz \
    && chmod +x /Net-MQTT-1.163170/bin/net-mqtt-sub /mqtt-listen.sh /docker-entrypoint.sh /listen.sh /start-listen.sh /save.sh

RUN curl -sL https://deb.nodesource.com/setup_6.x | bash - \
    && apt-get install -y nodejs

COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY nginx/default /etc/nginx/sites-available/default

ENTRYPOINT ["/docker-entrypoint.sh"]

CMD /start-listen.sh
