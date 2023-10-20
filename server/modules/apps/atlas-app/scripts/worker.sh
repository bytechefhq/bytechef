#!/bin/bash

java -jar \
     -Djava.security.egd=file:/dev/./urandom \
     -Datlas.message-broker.provider=amqp \
     -Datlas.coordinator.enabled=false \
     -Dserver.port=8181 \
     -Datlas.worker.enabled=true \
     -Datlas.worker.subscriptions.tasks=10 \
     lib/atlas-app.jar
