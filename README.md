# Introduction

Piper is an open-source, distributed workflow engine built on Spring Boot, designed to be dead simple. 

Piper can run on one or a thousand machines depending on your scaling needs. 

In Piper, work to be done is defined as a set of tasks called a Workflow. Workflows can be sourced from many locations but typically they live on a Git repository where they can be versioned and tracked.

Piper was originally built to support the need to transcode massive amounts of video in parallel. Since transcoding video is a CPU and time instensive process I had to scale horizontally. Moreover, I needed a way to monitor these long running jobs, auto-retry them and otherwise control their execution. 

# Tasks

Tasks are the basic building blocks of a workflow. Each task has a `type` property which maps to a `TaskHandler` implementation, responsible for carrying out the task.

For example here's the `RandomInt` `TaskHandler` implementation:

```
  public class RandomInt implements TaskHandler<Object> {

    @Override
    public Object handle(Task aTask) throws Exception {
      int startInclusive = aTask.getInteger("startInclusive", 0);
      int endInclusive = aTask.getInteger("endInclusive", 100);
      return RandomUtils.nextInt(startInclusive, endInclusive);
    }
    
  }
```

While it doesn't do much beyond generating a random integer, it does  demonstrate how a `TaskHandler` works. a `Task` instance is passed as  an argument to 
the `TaskHandler` which contains all the Key-Value pairs of that task.

The `TaskHandler` is then responsible for executing the task using this input and optionally returning an output which can be used by other workflow tasks downstream.


# Workflows

Piper workflows are authored in YAML, a JSON superset. 

Here is an example of a basic workflow definition.

```
name: Hello Demo

inputs:                --+
  - name: yourName       |
    label: Your Name     | - This defines the inputs
    type: string         |   expected by the workflow
    required: true     --+
    
outputs:                 --+
  - name: myMagicNumber    | - You can output any of the job's
    value: ${randomNumber} |   variable as the job's output.
                         --+   
tasks: 
  - name: randomNumber               --+
    label: Generate a random number    |
    type: random/int                   | - This is a task
    startInclusive: 0                  |
    endInclusive: 10000              --+
                            
  - label: Print a greeting 
    type: io/print             
    text: Hello ${yourName} 
                           
  - label: Sleep a little
    type: time/sleep        --+
    millis: ${randomNumber}   | - tasks may refer to the result of a previous task
                            --+
  - label: Print a farewell
    type: io/print
    text: Goodbye ${yourName}
```


So tasks are nothing but a collection of key-value pairs. At a minimum each task contains a `type` property which maps to an appropriate `TaskHandler` that needs to execute it.

Tasks may also specify a `name` property which can be used to name the output of the task so it can be used later in the workflow.

The `label` property is used to give a human-readble description for the task.

The `node` property can be used to route tasks to work queues other than the default `tasks` queue. This allows one to design a cluster of worker nodes of different types, of different capacity, different 3rd party software dependencies and so on.

The `retry` property can be used to specify the number of times that a task is allowed to automatically retry in case of a failure.

The `timeout` property can be used to specify the number of seconds/minutes/hours that a task may execute before it is cancelled.

The `output` property can be used to modify the output of the task in some fashion. e.g. convert it to an integer.

All other key-value pairs are task-specific and may or may not be required depending on the specific task.


# Architecture

Piper is composed of the following components: 

**Coordinator**: The Coordinator is the like the central nervous system of Piper. It keeps tracks of jobs, dishes out work to be done by Worker machines, keeps track of failures, retries and other job-level details. Unlike Worker nodes, it does not execute actual work but delegate all task activities to Worker instances. 

**Worker**: Workers are the work horses of Piper. These are the Piper nodes that actually execute tasks requested to be done by the Coordinator machine. Unlike the Coordinator, the workers are stateless, which by that is meant that they do not interact with a database or keep any state in memory about the job or anything else. This makes it very easy to scale up and down the number of workers in the system without fear of losing application state. 

**Message Broker**:  All communication between the Coordinator and the Worker nodes is done through a messaging broker. This has many advantages: 
  1. if all workers are busy the message broker will simply queue the message until they can handle it. 
  2. when workers boot up they subscribe to the appropriate queues for the type of work they are intended to handle 
  3. if a worker crashes the task will automatically get re-queued to be handle by another worker.
  4. Last but not least, workers and `TaskHandler` implementations can be written in any language since they decoupled completely through message passing.  

**Database**: This piece holds all the jobs state in the system, what tasks completed, failed etc. It is used by the Coordinator as its "mind". 

**Workflow Repository**: The component where workflows (workflows) are created, edited etc. by workflow engineers.

# Control Flow

Piper support the following constructs to control the flow of execution:

## Each

Applies the function `iteratee` to each item in `list`, in parallel. Note, that since this function applies iteratee to each item in parallel, there is no guarantee that the `iteratee` functions will complete in order.


```
- type: each
  list: [1000,2000,3000]
  iteratee:
    type: time/sleep         
    millis: ${item} 
```

This will generate three parallel tasks, one for each items in the list, which will `sleep` for 1, 2 and 3 seconds respectively.

## Parallel

Run the `tasks` collection of functions in parallel, without waiting until the previous function has completed.

```
- type: parallel
  tasks: 
    - type: io/print
      text: hello
        
    - type: io/print
      text: goodbye
```

## Fork/Join

Executes each branch in the `branches` as a seperate and isolated sub-flow. Branches are executed internally in sequence.

```
- type: fork
  branches: 
     - - name: randomNumber                 <-- branch 1 start here
         label: Generate a random number
         type: random/int
         startInclusive: 0
         endInclusive: 5000
           
       - type: time/sleep
         millis: ${randomNumber}
           
     - - name: randomNumber                 <-- branch 2 start here
         label: Generate a random number
         type: random/int
         startInclusive: 0
         endInclusive: 5000
           
       - type: time/sleep
         millis: ${randomNumber}      
```

## Switch

Executes one and only one branch of execution based on the `expression` value.

```
- type: switch
  expression: ${selector} <-- determines which case will be executed
  cases: 
     - key: hello                 <-- case 1 start here
       tasks: 
         - type: io/print
           text: hello world
     - key: bye                   <-- case 2 start here
       tasks: 
         - type: io/print
           text: goodbye world
  default:
    - tasks:
        -type: io/print
         text: something else
```

## Map

Produces a new collection of values by mapping each value in `list` through the `iteratee` function. The `iteratee` is called with an item from `list` in parallel. When the `iteratee` is finished executing on all items the `map` task will return a list of execution results in an order which corresponds to the order of the source `list`.

```
- name: fileSizes 
  type: map
  list: ["/path/to/file1.txt","/path/to/file2.txt","/path/to/file3.txt"]
  iteratee:
    type: io/filesize         
    file: ${item}
```

## Subflow

Starts a new job as a sub-flow of the current job. Output of the sub-flow job is the output of the task. 

```    
- type: subflow
  workflowId: copy_files
  inputs: 
    - source: /path/to/source/dir
    - destination: /path/to/destination/dir
```

## Pre/Post/Finalize

Each task can define a set of tasks that will be executed prior to its execution (`pre`), 
after its succesful execution (`post`) and at the end of the task's lifecycle regardless of the outcome of the task's 
execution (`finalize`). 

`pre/post/finalize` tasks always execute on the same node which will execute the task itself and are considered to be an atomic part of the task. That is, failure in any of the `pre/post/finalize` tasks is considered a failure of the entire task.


```
  - label: 240p
    type: media/ffmpeg
    options: [
      "-y",
      "-i",
      "/some/input/video.mov",
      "-vf","scale=w=-2:h=240",
      "${workDir}/240p.mp4"
    ]
    pre:
      - name: workDir
        type: core/var
        value: "${temptDir()}/${uuid()}"
      - type: io/mkdir
        path: "${workDir}"
    post: 
      - type: s3/putObject
        uri: s3://my-bucket/240p.mp4
    finalize:
      - type: io/rm
        path: ${workDir}
```   


## Webhooks

Piper provide the ability to register HTTP webhooks to receieve notifications for certain events. 

Registering webhooks is done when creating the job. E.g.:

```
{
  "workflowId": "demo/hello",
  "inputs": {
    ...
  },
  "webhooks": [{
    "type": "job.status", 
    "url": "http://example.com",
    "retry": {   # optional configuration for retry attempts in case of webhook failure 
      "initialInterval":"3s" # default 2s
      "maxInterval":"10s" # default 30s
      "maxAttempts": 4 # default 5
      "multiplier": 2.5 # default 2.0
    }
  }]
}
```

`type` is the type of event you would like to be notified on and `url` is the URL that Piper would be calling when the event occurs. 

Supported types are `job.status` and `task.started`.

# Task Handlers


[core/var](src/main/java/com/creactiviti/piper/taskhandler/core/Var.java)

```
  name: pi
  type: core/var
  value: 3.14159
```

[io/createTempDir](src/main/java/com/creactiviti/piper/taskhandler/io/CreateTempDir.java)

```
  name: tempDir
  type: io/create-temp-dir
```

[io/filepath](src/main/java/com/creactiviti/piper/taskhandler/io/FilePath.java)

```
  name: myFilePath
  type: io/filepath
  filename: /path/to/my/file.txt
```

[io/ls](src/main/java/com/creactiviti/piper/taskhandler/io/Ls.java)

```
  name: listOfFiles
  type: io/ls
  recursive: true # default: false
  path: /path/to/directory
```

[io/mkdir](src/main/java/com/creactiviti/piper/taskhandler/io/Mkdir.java)

```
  type: io/mkdir
  path: /path/to/directory
```

[io/print](src/main/java/com/creactiviti/piper/taskhandler/io/Print.java)

```
  type: io/print
  text: hello world
```

[io/rm](src/main/java/com/creactiviti/piper/taskhandler/io/Rm.java)

```
  type: io/rm
  path: /some/directory
```

[media/dar](src/main/java/com/creactiviti/piper/taskhandler/media/Dar.java)

```
  name: myDar
  type: media/dar
  input: /path/to/my/video/mp4
```

[media/ffmpeg](src/main/java/com/creactiviti/piper/taskhandler/media/Ffmpeg.java)

```
  type: media/ffmpeg
  options: [
    -y,
    -i, "${input}",
    "-pix_fmt","yuv420p",
    "-codec:v","libx264",
    "-preset","fast",
    "-b:v","500k",
    "-maxrate","500k",
    "-bufsize","1000k",
    "-vf","scale=-2:${targetHeight}",
    "-b:a","128k",
    "${output}"
  ]
```

[media/ffprobe](src/main/java/com/creactiviti/piper/taskhandler/media/Ffprobe.java)

```
  name: ffprobeResults
  type: media/ffprobe
  input: /path/to/my/media/file.mov
```

[media/framerate](src/main/java/com/creactiviti/piper/taskhandler/media/Framerate.java)

```
  name: framerate
  type: media/framerate
  input: /path/to/my/video/file.mov
```

[media/mediainfo](src/main/java/com/creactiviti/piper/taskhandler/media/Mediainfo.java)

```
  name: mediainfoResult
  type: media/mediainfo
  input: /path/to/my/media/file.mov
```

[media/vduration](src/main/java/com/creactiviti/piper/taskhandler/media/Vduration.java)

```
  name: duration
  type: media/vduration
  input: /path/to/my/video/file.mov
```

[media/vsplit](src/main/java/com/creactiviti/piper/taskhandler/media/Vsplit.java)

```
  name: chunks
  type: media/vsplit
  input: /path/to/my/video.mp4
  chunkSize: 30s
```

[media/vstitch](src/main/java/com/creactiviti/piper/taskhandler/media/Vstitch.java)

```
  type: media/vstitch
  chunks:
    - /path/to/chunk_001.mp4
    - /path/to/chunk_002.mp4
    - /path/to/chunk_003.mp4
    - /path/to/chunk_004.mp4
  output: /path/to/stitched/file.mp4
```


[random/int](src/main/java/com/creactiviti/piper/taskhandler/random/RandomInt.java)

```
  name: someRandomNumber
  type: random/int
  startInclusive: 1000 # default 0
  endInclusive: 9999 # default 100
```

[random/rogue](src/main/java/com/creactiviti/piper/taskhandler/random/Rogue.java)

```
  type: random/rogue
  probabilty: 0.25 # default 0.5
```

[s3/getObject](src/main/java/com/creactiviti/piper/taskhandler/s3/S3GetObject.java)

```
  type: s3/getObject
  uri: s3://my-bucket/path/to/file.mp4
  filepath: /path/to/my/file.mp4
```

[s3/listObjects](src/main/java/com/creactiviti/piper/taskhandler/s3/S3ListObjects.java)

```
  type: s3/listObjects
  bucket: my-bucket
  prefix: some/path/
```

[s3/getUrl](src/main/java/com/creactiviti/piper/taskhandler/s3/S3GetUrl.java)

```
  type: s3/getUrl
  uri: s3://my-bucket/path/to/file.mp4
```

[s3/presignGetObject](src/main/java/com/creactiviti/piper/taskhandler/s3/S3PresignedGetObject.java)

```
  name: url
  type: s3/presignGetObject
  uri: s3://my-bucket/path/to/file.mp4
  signatureDuration: 60s
```

[s3/putObject](src/main/java/com/creactiviti/piper/taskhandler/s3/S3PutObject.java)

```
  type: s3/putObject
  uri: s3://my-bucket/path/to/file.mp4
  filepath: /path/to/my/file.mp4
```

[shell/bash](src/main/java/com/creactiviti/piper/taskhandler/shell/Bash.java)

```
  name: listOfFiles
  type: shell/bash
  script: |
        for f in /tmp
        do
          echo "$f"
        done
```

[time/sleep](src/main/java/com/creactiviti/piper/taskhandler/time/Sleep.java)

```
  type: time/sleep
  millis: 60000
```

# Expression Functions

[boolean](src/main/java/com/creactiviti/piper/core/task/Cast.java)

```
  type: core/var
  value: "${boolean('false')}"
```

[byte](src/main/java/com/creactiviti/piper/core/task/Cast.java)

```
  type: core/var
  value: "${byte('42')}"
```

[char](src/main/java/com/creactiviti/piper/core/task/Cast.java)

```
  type: core/var
  value: "${char('1')}"
```

[short](src/main/java/com/creactiviti/piper/core/task/Cast.java)

```
  type: core/var
  value: "${short('42')}"
```

[int](src/main/java/com/creactiviti/piper/core/task/Cast.java)

```
  type: core/var
  value: "${int('42')}"
```

[long](src/main/java/com/creactiviti/piper/core/task/Cast.java)

```
  type: core/var
  value: "${long('42')}"
```

[float](src/main/java/com/creactiviti/piper/core/task/Cast.java)

```
  type: core/var
  value: "${float('4.2')}"
```

[double](src/main/java/com/creactiviti/piper/core/task/Cast.java)

```
  type: core/var
  value: "${float('4.2')}"
```

[systemProperty](src/main/java/com/creactiviti/piper/core/task/SystemProperty.java)

```
  type: core/var
  value: "${systemProperty('java.home')}"
```

[range](src/main/java/com/creactiviti/piper/core/task/Range.java)

```
  type: core/var
  value: "${range(0,100)}" # [0,1,...,100]
```

[join](src/main/java/com/creactiviti/piper/core/task/Join.java)

```
  type: core/var
  value: "${join('A','B','C')}" # ABC
```

[concat](src/main/java/com/creactiviti/piper/core/task/Concat.java)

```
  type: core/var
  value: "${join('A','B','C')"}
```

[concat](src/main/java/com/creactiviti/piper/core/task/Concat.java)

```
  type: core/var
  value: ${concat(['A','B'],['C'])} # ['A','B','C']
```

[flatten](src/main/java/com/creactiviti/piper/core/task/Flatten.java)

```
  type: core/var
  value: ${flatten([['A'],['B']])} # ['A','B']
```

[sort](src/main/java/com/creactiviti/piper/core/task/Sort.java)

```
  type: core/var
  value: ${sort([3,1,2])} # [1,2,3]
```

[tempDir](src/main/java/com/creactiviti/piper/core/task/TempDir.java)

```
  type: core/var
  value: "${tempDir()}"  # e.g. /tmp
```

[uuid](src/main/java/com/creactiviti/piper/core/task/Uuid.java)

```
  name: workDir
  type: core/var
  value: "${tempDir()}/${uuid()}"
```

[stringf](src/main/java/com/creactiviti/piper/core/task/StringFormat.java)

```
  type: core/var
  value: "${stringf('%03d',5)}"  # 005
```

[now](src/main/java/com/creactiviti/piper/core/task/Now.java)

```
  type: core/var
  value: "${dateFormat(now(),'yyyy')}"  # e.g. 2020
```

[timestamp](src/main/java/com/creactiviti/piper/core/task/Timestamp.java)

```
  type: core/var
  value: "${timestamp()}"  # e.g. 1583268621423
```

[dateFormat](src/main/java/com/creactiviti/piper/core/task/DateFormat.java)

```
  type: core/var
  value: "${dateFormat(now(),'yyyy')}"  # e.g. 2020
```

[config](src/main/java/com/creactiviti/piper/core/task/Config.java)

```
  type: core/var
  value: "${config('some.config.property')}"
```

# Tutorials

## Hello World

Start a local Postgres database:

```
./scripts/database.sh
```

Start a local RabbitMQ instance:

```
./scripts/rabbit.sh
``` 

Build Piper:

```
./scripts/build.sh
```

Start Piper:

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

The `/jobs` endpoint lists all jobs that are either running or were previously run on Piper.

Start a demo job:

```
curl -s \
     -X POST \
     -H Content-Type:application/json \
     -d '{"workflowId":"demo/hello","inputs":{"yourName":"Joe Jones"}}' \
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
  "workflowId": "demo/hello",
  "status": "CREATED",
  "tags": []
}
```


If you'll refresh your browser page now you should see the executing job. 

In case you are wondering, the `demo/hello` workflow is located at <a href="https://github.com/creactiviti/piper/blob/master/piper-core/src/main/resources/workflows/demo/hello.yaml" target="_blank">here</a>


## Writing your first workflow

Create the directory `~/piper/workflows` and create a file in there called `myworkflow.yaml`.

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

## Scaling Piper

Depending on your workload you will probably exhaust the ability to run Piper on a single node fairly quickly. Good, because that's where the fun begins. 

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

Execute the demo workflow: 

```
curl -s \
     -X POST \
     -H Content-Type:application/json \
     -d '{"workflowId":"demo/hello","inputs":{"yourName":"Joe Jones"}}' \
     http://localhost:8080/jobs
```


## Transcoding a Video

Note: You must have [ffmpeg](https://ffmpeg.org) installed on your worker machine to get this demo to work


Transcode a source video to an SD (480p) output:

```
curl -s \
     -X POST \
     -H Content-Type:application/json \
     -d '{"workflowId":"video/transcode","inputs":{"input":"/path/to/video/input.mov","output":"/path/to/video/output.mp4","profile":"sd"}}' \
     http://localhost:8080/jobs
```

Transcode a source video to an HD (1080p) output:

```
curl -s \
     -X POST \
     -H Content-Type:application/json \
     -d '{"workflowId":"video/transcode","inputs":{"input":"/path/to/video/input.mov","output":"/path/to/video/output.mp4","profile":"hd"}}' \
     http://localhost:8080/jobs
```

## Transcoding a Video (Split & Stitch)

See [Transcoding video at scale with Piper](https://medium.com/@arik.c.mail/transcoding-video-at-scale-with-piper-dca23eb26fd2)

## Adaptive Streaming

See [Adaptive Streaming with Piper](https://medium.com/@arik.c.mail/adaptive-streaming-with-piper-b37e55d95466)

# Using Git as a Workflow Repository backend

Rather than storing the workflows in your local file system you can use Git to store them for you. This has great advantages, not the least of which is workflow versioning, Pull Requests and everything else Git has to offer.

To enable Git as a workflow repository set the `piper.workflow-repository.git.enabled` flag to `true` in `./scripts/development.sh` and restart Piper. By default, Piper will use the demo repository [piper-workflows](https://github.com/creactiviti/piper-workflows).

You can change it by using the `piper.workflow-repository.git.url` and `piper.workflow-repository.git.search-paths` configuration parameters.  

# Configuration

```ini
# messaging provider between Coordinator and Workers (jms | amqp | kafka) default: jms
piper.message-broker.provider=jms
# turn on the Coordinator process
piper.coordinator.enabled=true
# turn on the Worker process and listen to tasks.
piper.worker.enabled=true
# when worker is enabled, subscribe to the default "tasks" queue with 5 concurrent consumers. 
# you may also route workflow tasks to other arbitrarilty named task queues by specifying the "node"
# property on any give task. 
# E.g. node: captions will route to the captions queue which a worker would subscribe to with piper.worker.subscriptions.captions
# note: queue must be created before tasks can be routed to it. Piper will create the queue if it isn't already there when the worker
# bootstraps.
piper.worker.subscriptions.tasks=5 
# enable a git-based workflow repository
piper.workflow-repository.git.enabled=true
# The URL to the Git Repo
piper.workflow-repository.git.url=https://github.com/myusername/my-workflows.git
piper.workflow-repository.git.branch=master
piper.workflow-repository.git.username=me
piper.workflow-repository.git.password=secret
# folders within the git repo that are scanned for workflows.
piper.workflow-repository.git.search-paths=demo/,video/
# enable file system based workflow repository
piper.workflow-repository.filesystem.enabled=true
# location of workflows on the file system.
piper.workflow-repository.filesystem.location-pattern=$HOME/piper/**/*.yaml
# data source
spring.datasource.platform=postgres # only postgres is supported at the moment
spring.datasource.url=jdbc:postgresql://localhost:5432/piper
spring.datasource.username=piper
spring.datasource.password=piper
spring.datasource.initialization-mode=never # change to always when bootstrapping the database for the first time
```

# Docker
[creactiviti/piper](https://hub.docker.com/r/creactiviti/piper)
Hello World in Docker:

Start a local Postgres database:

```
./scripts/database.sh
```

Create an empty directory: 
```
mkdir workflows
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
  --name=piper \
  --link postgres:postgres \
  --rm \
  -it \
  -e spring.datasource.url=jdbc:postgresql://postgres:5432/piper \
  -e spring.datasource.initialization-mode=always \
  -e piper.worker.enabled=true \
  -e piper.coordinator.enabled=true \
  -e piper.worker.subscriptions.tasks=1 \
  -e piper.workflow-repository.filesystem.enabled=true \
  -e piper.workflow-repository.filesystem.location-pattern=/workflows/**/*.yaml \
  -v $PWD:/workflows \
  -p 8080:8080 \
  creactiviti/piper
```
```
curl -s \
     -X POST \
     -H Content-Type:application/json \
     -d '{"workflowId":"hello","inputs":{"name":"Joe Jones"}}' \
     http://localhost:8080/jobs
```
# License
Piper is released under version 2.0 of the [Apache License](LICENSE). 
