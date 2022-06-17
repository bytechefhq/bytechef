#!/bin/bash

java -jar \
     -Djava.security.egd=file:/dev/./urandom \
     -Datlas.message-broker.provider=amqp \
     -Datlas.coordinator.enabled=true \
     -Datlas.worker.enabled=true \
     -Datlas.worker.subscriptions.tasks=5 \
     -Datlas.workflow-repository.git.enabled=true \
     -Datlas.workflow-repository.git.url=https://github.com/creactiviti/atlas-workflows.git \
     -Datlas.workflow-repository.git.search-paths=samples/,video/ \
     -Datlas.workflow-repository.filesystem.enabled=true \
     -Datlas.workflow-repository.filesystem.location-pattern=$HOME/atlas/**/*.yaml \
     -Dspring.datasource.initialization-mode=always \
     lib/server-app.jar
