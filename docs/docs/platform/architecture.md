---
id: architecture
title: How ByteChef works
---

In ByteChef, work to be done is defined as **a set of tasks** called a **workflow**. Workflows can be sourced from many locations including a Git repository where they can be versioned and tracked. ByteChef can run on one or a multiple machines depending on your scaling needs.

## Tasks

Tasks are the basic building blocks of a workflow. Each task has a `type` property which maps to a `TaskHandler` implementation, responsible for carrying out the task.

For example here's the `RandomInt` `TaskHandler` implementation:

```
  public class RandomInt implements TaskHandler<Object> {

    @Override
    public Object handle(Task task) throws Exception {
      int startInclusive = task.getInteger("startInclusive", 0);
      int endInclusive = task.getInteger("endInclusive", 100);
      
      return RandomUtils.nextInt(startInclusive, endInclusive);
    }
    
  }
```

While it doesn't do much beyond generating a random integer, it does  demonstrate how a `TaskHandler` works. a `Task` instance is passed as  an argument to
the `TaskHandler` instance which contains all the Key-Value pairs of that task.

The `TaskHandler` instance is then responsible for executing the task using this input and optionally returning an output which can be used by other workflow tasks downstream.

## Flow Controls

TODO

## Workflows

ByteChef workflows are authored in YAML or JSON format.

Here is an example of a basic workflow definition in YAML format:

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
    type: time/sleepTaskHandler        --+
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

## Architecture

ByteChef is composed of the following components:

**Coordinator**: The Coordinator is the like the central nervous system of ByteChef. It keeps tracks of jobs, dishes out work to be done by Worker machines, keeps track of failures, retries and other job-level details. Unlike Worker nodes, it does not execute actual work but delegate all task activities to Worker instances.

**Worker**: Workers are the work horses of ByteChef. These are the ByteChef nodes that actually execute tasks requested to be done by the Coordinator machine. Unlike the Coordinator, the workers are stateless, which by that is meant that they do not interact with a database or keep any state in memory about the job or anything else. This makes it very easy to scale up and down the number of workers in the system without fear of losing application state.

**Message Broker**:  All communication between the Coordinator and the Worker nodes is done through a messaging broker. This has many advantages:
1. if all workers are busy the message broker will simply queue the message until they can handle it.
2. when workers boot up they subscribe to the appropriate queues for the type of work they are intended to handle
3. if a worker crashes the task will automatically get re-queued to be handle by another worker.
4. Last but not least, workers and `TaskHandler` implementations can be written in any language since they decoupled completely through message passing.

**Database**: This piece holds all the jobs state in the system, what tasks completed, failed etc. It is used by the Coordinator as its "mind".

**Workflow Repository**: The component where workflows (workflows) are created, edited etc. by workflow engineers.
