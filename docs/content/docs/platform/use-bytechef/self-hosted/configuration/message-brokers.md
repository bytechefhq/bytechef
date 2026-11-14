---
title: Message Brokers
description: Choose the broker that carries task dispatches between the coordinator and the workers - memory, Redis, RabbitMQ, Kafka, JMS, or AWS SQS.
---

The message broker carries task executions from ByteChef's coordinator to the workers and carries
the results back. The broker is a runtime setting: the same workflow definitions, component code and
execution history work with any supported broker, and switching brokers is a configuration change.

## Supported providers

Set `BYTECHEF_MESSAGE_BROKER_PROVIDER` (property `bytechef.message-broker.provider`). The
single-node distribution defaults to `MEMORY`.

| Provider | Backing system | Best fit |
|---|---|---|
| `MEMORY` | None - in-process | Dev, evaluation, and single-JVM production. Cannot be used across processes. |
| `REDIS` | Redis streams | Small to mid production deployments, and instances that already run Redis. |
| `AMQP` | RabbitMQ | Production deployments that want a dedicated broker. The only provider that configures a dead-letter queue (see below). |
| `KAFKA` | Apache Kafka | High-throughput deployments, or deployments that already run Kafka for other event streams. |
| `JMS` | Any JMS provider | Enterprises with existing JMS infrastructure. |
| `AWS` | AWS SQS | AWS-native deployments. **Enterprise Edition** - the SQS provider lives in `server/ee/libs/core/message/message-broker/message-broker-aws`. |

Every provider above is compiled into the server image, so switching is a configuration change and
never a rebuild.

The `MEMORY` provider is in-process only, so it cannot carry work between processes. A single
instance running the whole platform is exactly what it is for; run more than one instance against a
shared database and you need one of the other providers instead, or a task dispatched on one node
will never reach another.

## What the broker carries

- **Task dispatches** - "run this task execution on a worker".
- **Task completions and errors** - the worker's result, returned to the coordinator.
- **Job control events** - start, stop, and resume job events.
- **Trigger events** - the scheduler and the webhook service publish trigger events onto the broker.

The broker does **not** carry large payloads. Task outputs above the inline threshold are written to
[file storage](/platform/use-bytechef/self-hosted/configuration/file-storage) and only the reference
travels on the broker.

## How the abstraction works

The abstraction lives in `server/libs/core/message`:

- A `MessageRoute` is a name plus an exchange.
- The `MessageBroker` interface has exactly one method - `send(MessageRoute route, Object message)`.
  Implementations are responsible for guaranteed delivery.
- Subscription is wired separately, through `MessageBrokerListenerRegistrar` and
  `MessageBrokerConfigurer`, so each provider registers its own listener endpoints.

Coordinator and worker code never references a provider. Each backend is a self-contained adapter,
so a workflow behaves the same locally on `MEMORY` as it does in production on Kafka.

## Picking a provider

| Situation | Provider |
|---|---|
| You already run Kafka | `KAFKA`, to reuse infrastructure you already operate. |
| AWS-only deployment | `AWS` (SQS, Enterprise Edition), a managed service controlled through IAM. |
| Single region, mid-scale, no existing broker | `REDIS`. If you already run Redis, it adds no new dependency. |
| Existing JMS or RabbitMQ infrastructure | `JMS` or `AMQP`. |

In Enterprise Edition, every provider except `MEMORY` also carries cache invalidation between nodes
(with `AWS`, only when `BYTECHEF_MESSAGE_BROKER_AWS_CONTROL_PROVIDER=REDIS`).

## Operational notes

| Concern | Behaviour |
|---|---|
| **Backpressure** | Handled by the broker. Workers consume at their own pace; the broker buffers. |
| **Delivery semantics** | At-least-once. Components should tolerate a redelivered task. |
| **Worker crash redelivery** | On `AMQP` and `KAFKA`, an unacknowledged task is redelivered by the broker itself. |
| **Dead-letter routing** | Configured for `AMQP` only. `AmqpMessageBrokerConfiguration` declares the `system.dlq` queue and the listener registrar sets `x-dead-letter-exchange` / `x-dead-letter-routing-key` on each task queue. The other providers have no ByteChef-configured dead-letter path - configure one in the broker itself if you need it. |
| **Worker concurrency** | Per-queue, via `BYTECHEF_WORKER_TASK_SUBSCRIPTIONS_DEFAULT` (default `10`) and per-queue variants. |

## AWS SQS control messages

Some messages must reach every node, not one: task cancellation and the events behind SSE streams
are examples. SQS queues deliver each message to a single consumer, so the `AWS` provider can send
these messages over a different transport, chosen with `bytechef.message-broker.aws.control-provider`
(environment variable `BYTECHEF_MESSAGE_BROKER_AWS_CONTROL_PROVIDER`).

| Value | Behaviour |
|---|---|
| `SQS` (default) | Control messages go through SQS and reach only one node. Fine for a single node. |
| `REDIS` | Control messages go over Redis pub/sub (for example Amazon ElastiCache), so every node receives them. Required when you run more than one node. Set the connection with `BYTECHEF_REDIS_*`. |

With `SQS` and more than one node, each control message reaches a single node, often not the one that needs it:

- Cancelling a task or a job does not stop a task running on another worker.
- Cancelling a trigger execution does not stop it on another node.
- SSE clients connected to one node miss the events of webhook runs handled by another node.
- WebSocket webhook clients (Twilio call status) miss updates handled by another node.
- EE cache invalidation between nodes is off, so other nodes keep cached entries for up to 10 minutes after a change.
- EE shared state caches (the editor's webhook test, embedded JWT public keys) stay in each node's memory, so with more than one coordinator these can fail when a request lands on another coordinator.

At startup, the `AWS` provider logs a warning when `control-provider` is `SQS`.

## Switching providers

1. Drain in-flight tasks on the old broker.
2. Change `BYTECHEF_MESSAGE_BROKER_PROVIDER` and the broker-specific connection settings
   (`BYTECHEF_KAFKA_*`, `BYTECHEF_RABBITMQ_*`, `BYTECHEF_REDIS_*`, or the AWS credentials under
   `BYTECHEF_CLOUD_AWS_*`).
3. Restart the instance (every instance, if you run more than one).

For example, these environment variables point every node at a shared Redis:

```bash
BYTECHEF_MESSAGE_BROKER_PROVIDER=REDIS
BYTECHEF_REDIS_HOST=redis.internal.example.com
BYTECHEF_REDIS_PORT=6379
BYTECHEF_REDIS_PASSWORD=<redis-password>
```

RabbitMQ uses the `AMQP` provider with the `BYTECHEF_RABBITMQ_*` variables:

```bash
BYTECHEF_MESSAGE_BROKER_PROVIDER=AMQP
BYTECHEF_RABBITMQ_HOST=rabbitmq.internal.example.com
BYTECHEF_RABBITMQ_PORT=5672
BYTECHEF_RABBITMQ_USERNAME=bytechef
BYTECHEF_RABBITMQ_PASSWORD=<rabbitmq-password>
```

Kafka takes a comma-separated list of bootstrap servers:

```bash
BYTECHEF_MESSAGE_BROKER_PROVIDER=KAFKA
BYTECHEF_KAFKA_BOOTSTRAP_SERVERS=kafka-1.internal.example.com:9092,kafka-2.internal.example.com:9092
BYTECHEF_KAFKA_CONSUMER_GROUP_ID=bytechef
```

Workflow definitions, execution history, and component code are unchanged by the swap - the broker
is the substrate, not the record.

## See also

- [Environment variables](/platform/use-bytechef/self-hosted/configuration/environment-variables) - the exact variables and defaults.
- [Architecture](/platform/use-bytechef/self-hosted/architecture) - where the broker sits in the Atlas engine.
