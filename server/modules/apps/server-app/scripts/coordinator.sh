#!/bin/bash

java -jar \
     -Djava.security.egd=file:/dev/./urandom \
     -Datlas.message-broker.provider=amqp \
     -Datlas.coordinator.enabled=true \
     -Dserver.port=8080 \
     -Datlas.worker.enabled=false \
     -Datlas.workflow-repository.git.enabled=false \
     -Datlas.workflow-repository.filesystem.enabled=true \
     -Datlas.workflow-repository.filesystem.location-pattern=$HOME/atlas/**/*.yaml \
     lib/server-app.jar
