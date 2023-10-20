#!/bin/sh

echo "The application will start in ${SLEEP}s..." && sleep ${SLEEP}
exec java ${JAVA_OPTS} -noverify -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/./urandom -cp /app/resources/:/app/classes/:/app/libs/* "com.integri.atlas.AtlasApp"  "$@"
