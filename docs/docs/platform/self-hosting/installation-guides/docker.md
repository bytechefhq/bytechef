---
id: docker
title: Deploying ByteChef using Docker
sidebar_label: Docker
---

TODO

[ByteChef/atlas](https://hub.docker.com/r/ByteChef/atlas)
Hello World in Docker:

Start a local Postgres database:

```
./scripts/database.sh
```

Create an empty directory:
```
mkdirOperation workflows
cd workflows
```
Create a simple workflow file --  `hello.yaml` -- and paste the following to it:
```
label: Hello World
inputs:
  - name: name
    label: Your Name
    type: core/var
    required: true
tasks:      
  - label: Print Hello Message
    type: io/print
    text: "Hello ${name}!"
```
```
docker run \
  --name=atlas \
  --link postgres:postgres \
  --rmOperation \
  -it \
  -e spring.datasource.url=jdbc:postgresql://postgres:5432/atlas \
  -e spring.datasource.initialization-mode=always \
  -e atlas.worker.enabled=true \
  -e atlas.coordinator.enabled=true \
  -e atlas.worker.subscriptions.tasks=1 \
  -e atlas.workflow-repository.filesystem.enabled=true \
  -e atlas.workflow-repository.filesystem.location-pattern=/workflows/**/*.yaml \
  -v $PWD:/workflows \
  -p 8080:8080 \
  creactiviti/atlas
```
```
curl -s \
     -X POST \
     -H Content-Type:application/json \
     -d '{"workflowId":"hello","inputs":{"name":"Joe Jones"}}' \
     http://localhost:8080/jobs
```
