#!/bin/bash

java -jar \
     -Djava.security.egd=file:/dev/./urandom \
     -Dpiper.message-broker.provider=amqp \
     -Dpiper.coordinator.enabled=true \
     -Dpiper.worker.enabled=true \
     -Dpiper.worker.subscriptions.tasks=5 \
     -Dpiper.workflow-repository.git.enabled=true \
     -Dpiper.workflow-repository.git.url=https://github.com/creactiviti/piper-workflows.git \
     -Dpiper.workflow-repository.git.search-paths=demo/,video/ \
     -Dpiper.workflow-repository.filesystem.enabled=true \
     -Dpiper.workflow-repository.filesystem.location-pattern=$HOME/piper/**/*.yaml \
     -Dspring.datasource.initialization-mode=always \
     lib/atlas-app.jar
