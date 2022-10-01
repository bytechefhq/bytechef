---
id: environment-variables
title: Environment Variables
---

:::note

You can provide a configuration file for ByteChef. For more details check [Configuration guide](configuration.md#configuration-by-file).

:::

## DataSource

| Variable                                   | Type    | Default                                   | Description            |
|:-------------------------------------------|:--------|:------------------------------------------|:-----------------------|
| SPRING_DATASOURCE_URL                      | string  | jdbc:postgresql://localhost:5432/bytechef | The used database url. |
| SPRING_DATASOURCE_USERNAME                 | string  | root                                      | The database username. |
| SPRING_DATASOURCE_PASSWORD                 | number  |                                           | The database password. |

## Server

| Variable                                   | Type    | Default                                              | Description                            |
|:-------------------------------------------|:--------|:-----------------------------------------------------|:---------------------------------------|
| SERVER_PORT                                | string  | 8080                                                 | The server port.                       |

## Message Broker

| Variable                                   | Type                                | Default                                              | Description                                          |
|:-------------------------------------------|:------------------------------------|:-----------------------------------------------------|:-----------------------------------------------------|
| WORKFLOW_MESSAGE-BROKER_PROVIDER              | enum: `jms`, `amqp`, `kafka`, `jms` | jms  | Messaging provider between Coordinator and Workers.

## Workflow Repository

| Variable                                   | Type    | Default                                              | Description                                                                         |
|:-------------------------------------------|:--------|:-----------------------------------------------------|:------------------------------------------------------------------------------------|
| WORKFLOW_WORKFLOW-REPOSITORY_GIT_ENABLED      | boolean | false                                                |                                                                                     |
| WORKFLOW_WORKFLOW-REPOSITORY_GIT_URL          | string  | https://github.com/bytechefhq/bytechef-workflows.git | The URL to the Git repository.                                                      |
| WORKFLOW_WORKFLOW-REPOSITORY_GIT_BRANCH       | string  | master                                               | The Git repository branch to use.                                                   |
| WORKFLOW_WORKFLOW-REPOSITORY_GIT_USERNAME     | string  |                                                      | The git username.                                                                   |
| WORKFLOW_WORKFLOW-REPOSITORY_GIT_PASSWORD     | string  |                                                      | The git password.                                                                   |
| WORKFLOW_WORKFLOW-REPOSITORY_GIT_SEARCH-PATHS | string  |                                                      | Comma-separated list of folders within the git repo that are scanned for workflows. |

## Worker

| Variable                                | Type   | Default | Description                                                                                                                                                                                                                                                                                                                                                                                                         |
|:----------------------------------------|:-------|:-------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| WORKFLOW_WORKER_SUBSCRIPTIONS_TASKS        | number | 10     | When worker is enabled, subscribe to the default `tasks` queue with 10 concurrent consumers.                                                                                                                                                                                                                                                                                                                          |
| WORKFLOW_WORKER_SUBSCRIPTIONS_[queue name] | string |        | You may also route workflow tasks to other arbitrarily named task queues by specifying the "node" property on any given task. E.g. node: captions will route to the captions queue which a worker would subscribe to with `ATLAS_WORKER_SUBSCRIPTIONS_CAPTIONS`. Note: queue must be created before tasks can be routed to it. ByteChef will create the queue if it isn't already there when the worker bootstraps. |

## Storage

| Variable                             | Type   | Default             | Description                                                        |
|:-------------------------------------|:-------|:--------------------|:-------------------------------------------------------------------|
| FILE_STORAGE_PROVIDER         | enum: `filesystem` | filesystem                | The environment for storing files used during workflow executions. |
| FILE_STORAGE_FILE_STORAGE_DIR | string | /tmp/bytechef/files | The path to directory for storing files.                           |
