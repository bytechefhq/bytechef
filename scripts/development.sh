#!/bin/bash

java -jar \
     -Djava.security.egd=file:/dev/./urandom \
     -Dpiper.message-broker.provider=jms \
     -Dpiper.coordinator.enabled=true \
     -Dpiper.worker.enabled=true \
     -Dpiper.worker.subscriptions.tasks=5 \
     -Dpiper.pipeline-repository.git.enabled=true \
     -Dpiper.pipeline-repository.git.url=git@github.com:creactiviti/piper-pipelines.git \
     -Dpiper.pipeline-repository.git.search-paths=demo/,video/ \
     -Dpiper.pipeline-repository.filesystem.enabled=true \
     -Dpiper.pipeline-repository.filesystem.location-pattern=$HOME/piper/**/*.yaml \
     target/piper-0.0.1-SNAPSHOT.jar
