FROM node:6
MAINTAINER Reittiopas version: 0.1

ENV DIR_PATH=/opt/navigatorserver
ENV PORT=8080
ENV NODE_OPTS=''
RUN mkdir -p ${DIR_PATH}
WORKDIR ${DIR_PATH}
RUN npm install grunt-cli
ADD package.json ${DIR_PATH}/package.json
RUN npm install
RUN npm install gtfs-realtime-bindings
RUN npm install moment-timezone
ADD . ${DIR_PATH}

CMD node $NODE_OPTS ./node_modules/.bin/grunt server --port ${PORT} --stack
