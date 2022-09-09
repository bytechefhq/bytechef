---
id: quick-start
title: Quick Start
---

TODO

## Hello World

Start a local Postgres database:

```
./scripts/database.sh
```

Start a local RabbitMQ instance:

```
./scripts/rabbit.sh
``` 

Build ByteChef Atlas:

```
./scripts/build.sh
```

Start ByteChef Atlas:

```
./scripts/development.sh
```

Go to the browser at <a href="http://localhost:8080/jobs" target="_blank">http://localhost:8080/jobs</a>

Which should give you something like:

```
{
  number: 0,
  totalItems: 0,
  size: 0,
  totalPages: 0,
  items: [ ]
}
```

The `/jobs` endpoint lists all jobs that are either running or were previously run on ByteChef Atlas.

Start a demo job:

```
curl -s \
     -X POST \
     -H Content-Type:application/json \
     -d '{"workflowId":"samples/hello.json","inputs":{"yourName":"Joe Jones"}}' \
     http://localhost:8080/jobs
```

Which should give you something like this as a response:

```
{
  "createTime": "2017-07-05T16:56:27.402+0000",
  "webhooks": [],
  "inputs": {
    "yourName": "Joe Jones"
  },
  "id": "8221553af238431ab006cc178eb59129",
  "label": "Hello Demo",
  "priority": 0,
  "workflowId": "samples/hello.json",
  "status": "CREATED",
  "tags": []
}
```


If you'll refresh your browser page now you should see the executing job.

In case you are wondering, the `samples/hello.json` workflow is located at <a href="https://github.com/ByteChef/atlas/blob/master/server/modules/apps/atlas-app/src/main/resources/workflow/samples/hello.yaml" target="_blank">here</a>


## Writing your first workflow

Create the directory `~/atlas/workflows` and create a file in there called `myworkflow.yaml`.

Edit the file and the following text:

```
label: My Workflow

inputs:
  - name: name
    type: string
    required: true

tasks:      
  - label: Print a greeting
    type: io/print
    text: Hello ${name}
       
  - label: Print a farewell
    type: io/print
    text: Goodbye ${name}
    
```  

Execute your workflow

```
curl -s -X POST -H Content-Type:application/json -d '{"workflowId":"myworkflow","inputs":{"name":"Arik"}}' http://localhost:8080/jobs
```

You can make changes to your workflow and execute the `./scripts/clear.sh` to clear the cache to reload the workflow.

## Scaling ByteChef Atlas

Depending on your workload you will probably exhaust the ability to run ByteChef Atlas on a single node fairly quickly. Good, because that's where the fun begins.

Start RabbitMQ:

```
./scripts/rabbit.sh
```

Start the Coordinator:

```
./scripts/coordinator.sh 
```

From another terminal window, start a Worker:

```
./scripts/worker.sh 
```

Execute the sample workflow:

```
curl -s \
     -X POST \
     -H Content-Type:application/json \
     -d '{"workflowId":"samples/hello.json","inputs":{"yourName":"Joe Jones"}}' \
     http://localhost:8080/jobs
```
