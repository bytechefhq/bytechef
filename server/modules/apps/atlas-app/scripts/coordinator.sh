#!/bin/bash

java -jar \
     -Djava.security.egd=file:/dev/./urandom \
     -Dpiper.message-broker.provider=amqp \
     -Dpiper.coordinator.enabled=true \
     -Dserver.port=8080 \
     -Dpiper.worker.enabled=false \
     -Dpiper.workflow-repository.git.enabled=false \
     -Dpiper.workflow-repository.filesystem.enabled=true \
     -Dpiper.workflow-repository.filesystem.location-pattern=$HOME/piper/**/*.yaml \
     lib/atlas-app.jar
