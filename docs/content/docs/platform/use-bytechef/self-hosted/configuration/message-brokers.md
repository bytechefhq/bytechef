---
title: Message Brokers
description: Choose the broker that carries task dispatches between the coordinator and the workers - memory, Redis, RabbitMQ, Kafka, JMS, or AWS SQS.
---

The message broker is how ByteChef's coordinator hands task executions to workers and how workers
hand results back. It is a runtime choice: the same workflow definition, the same component code,
and the same execution history run on top of any supported broker. Switching brokers is a
configuration change, not a rewrite.

## Supported providers

Set `BYTECHEF_MESSAGE_BROKER_PROVIDER` (property `bytechef.message-broker.provider`). The
single-node distribution defaults to `MEMORY`.

| Provider | Backing system | Best fit |
|---|---|---|
| `MEMORY` | None - in-process | Dev, evaluation, and single-JVM production. Cannot be used across processes. |
| `REDIS` | Redis streams | Small to mid production deployments, and instances that already run Redis as their cache provider. |
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
which is why a bug in one cannot leak into another and why a workflow behaves the same locally on
`MEMORY` as it does in production on Kafka.

## Picking a provider

- **Already running Kafka?** Use `KAFKA`. Reusing infrastructure you already operate beats
  introducing a new piece.
- **AWS-only deployment?** `AWS` (SQS) - managed, and the IAM story is straightforward.
- **Single-region, mid-scale, no existing broker?** `REDIS`. If you already set
  `BYTECHEF_CACHE_PROVIDER=REDIS` to share cache state across instances, this adds no new dependency.
- **Existing JMS or RabbitMQ estate?** `JMS` or `AMQP`.

## Operational notes

| Concern | Behaviour |
|---|---|
| **Backpressure** | Handled by the broker. Workers consume at their own pace; the broker buffers. |
| **Delivery semantics** | At-least-once. Components should tolerate a redelivered task. |
| **Worker crash redelivery** | On `AMQP` and `KAFKA`, an unacknowledged task is redelivered by the broker itself. |
| **Dead-letter routing** | Configured for `AMQP` only. `AmqpMessageBrokerConfiguration` declares the `system.dlq` queue and the listener registrar sets `x-dead-letter-exchange` / `x-dead-letter-routing-key` on each task queue. The other providers have no ByteChef-configured dead-letter path - configure one in the broker itself if you need it. |
| **Worker concurrency** | Per-queue, via `BYTECHEF_WORKER_TASK_SUBSCRIPTIONS_DEFAULT` (default `10`) and per-queue variants. |

## Switching providers

1. Drain in-flight tasks on the old broker.
2. Change `BYTECHEF_MESSAGE_BROKER_PROVIDER` and the broker-specific connection settings
   (`BYTECHEF_KAFKA_*`, `BYTECHEF_RABBITMQ_*`, `BYTECHEF_REDIS_*`, or the AWS credentials under
   `BYTECHEF_CLOUD_AWS_*`).
3. Restart the instance (every instance, if you run more than one).

Workflow definitions, execution history, and component code are unchanged by the swap - the broker
is the substrate, not the record.

## See also

- [Environment variables](/platform/use-bytechef/self-hosted/configuration/environment-variables) - the exact variables and defaults.
- [Architecture](/platform/use-bytechef/self-hosted/architecture) - where the broker sits in the Atlas engine.
