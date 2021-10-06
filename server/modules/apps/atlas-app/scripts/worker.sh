#!/bin/bash

java -jar \
     -Djava.security.egd=file:/dev/./urandom \
     -Dpiper.message-broker.provider=amqp \
     -Dpiper.coordinator.enabled=false \
     -Dserver.port=8181 \
     -Dpiper.worker.enabled=true \
     -Dpiper.worker.subscriptions.tasks=10 \
     lib/atlas-app.jar
