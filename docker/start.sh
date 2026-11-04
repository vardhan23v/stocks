#!/bin/sh
# Railway (and similar hosts) tell the container which port to listen on in
# $PORT. Tomcat's HTTP connector is moved to that port before starting.
set -e
PORT="${PORT:-8080}"
sed -i "s/port=\"8080\"/port=\"${PORT}\"/" /usr/local/tomcat/conf/server.xml
exec catalina.sh run
