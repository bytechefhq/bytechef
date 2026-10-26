---
title: Node Roles and High Availability
description: Run ByteChef as separately scalable coordinator, worker and webhook nodes, and which parts of high availability need Enterprise Edition
---

A ByteChef node can play three roles. Each role has its own switch, every switch defaults to on, and a node can combine roles. To scale a cluster, run several nodes and give each one the roles it needs.

## The three roles

| Role | Variable | What it serves |
|---|---|---|
| **Coordinator** | `BYTECHEF_COORDINATOR_ENABLED` | The REST and GraphQL API, the UI, job coordination, and the Quartz scheduler that fires triggers. |
| **Worker** | `BYTECHEF_WORKER_ENABLED` | Executes tasks. A worker serves no API. |
| **Webhook node** | `BYTECHEF_WEBHOOK_ENABLED` | The public webhook endpoints: `/webhooks/{id}` (including `/webhooks/{id}/sse`, `/webhooks/*/wss` and `/webhooks/twilio/**`), `/callback`, `/approvals/{id}`, `/job/resume/{id}` and `/file-entries/{id}/content`. |

A coordinator with `BYTECHEF_WEBHOOK_ENABLED=true` serves the webhook endpoints too, so a separate webhook node is optional. Add webhook nodes when webhook traffic needs its own capacity.

A webhook node runs with:

```bash
BYTECHEF_COORDINATOR_ENABLED=false
BYTECHEF_WORKER_ENABLED=false
BYTECHEF_WEBHOOK_ENABLED=true
```

How each webhook mode runs on a webhook node:

- **Asynchronous** webhooks publish the trigger event to the message broker, and a coordinator consumes it.
- **Synchronous** webhooks run the trigger and the job on the webhook node itself, because the caller is waiting on that node. The webhook node therefore executes component code for them.
- **SSE streams** create the job on the webhook node and receive the stream events over the message broker.

## What each edition includes

| Capability | Community (CE) | Enterprise (EE) |
|---|---|---|
| 1 coordinator | Yes | Yes |
| N workers | Yes | Yes |
| N separate webhook nodes (optional) | Yes | Yes |
| N coordinators (high availability) | No | Yes |
| Clustered Quartz | Not applicable | Yes |
| Cluster-wide cache invalidation over the message broker | Not applicable | Yes |
| Shared HTTP sessions (no sticky sessions) | No, memory only | Yes, Redis or JDBC |

Select the edition with `BYTECHEF_EDITION`. Multi-tenancy (`BYTECHEF_TENANT_MODE=MULTI`) also requires EE.

Between the nodes, a self-hosted cluster uses RabbitMQ (`AMQP`) or Redis (`REDIS`) as the [message broker](/platform/use-bytechef/self-hosted/configuration/message-brokers). On AWS, use `AWS` (SQS, EE) and set `BYTECHEF_MESSAGE_BROKER_AWS_CONTROL_PROVIDER=redis` (for example Amazon ElastiCache), which is required for more than one node - see [AWS SQS control messages](/platform/use-bytechef/self-hosted/configuration/message-brokers#aws-sqs-control-messages).

## Load balancer routing

Put a load balancer in front of the cluster and route by path:

| Traffic | Goes to |
|---|---|
| The webhook paths from the table above, except `/webhooks/{id}/test/**` | The webhook nodes, with the coordinators as backup. |
| `/webhooks/{id}/test/**` (the editor's webhook test) | The coordinators. Only coordinators serve it. |
| Everything else | The coordinators, round robin. |

Workers serve no HTTP traffic and stay out of the load balancer pool.

Every node must also share file storage: a volume all nodes mount (NFS, EFS) or the AWS provider, so any node can read what another node wrote. The encryption key and the remember-me key are the same on every node.

The coordinator backup means webhooks keep working when every webhook node is down. Set `BYTECHEF_WEBHOOK_ENABLED=false` on the coordinators to remove that fallback and keep all webhook traffic on webhook nodes. With every webhook node down, webhooks then fail, and the coordinator answers `404`.

In the reference nginx configuration, a request that cannot reach a coordinator within 2 seconds is retried on the next one, so a coordinator failure is visible for about 2 seconds. A request that was already sent is never replayed if it is not idempotent.

## One coordinator in CE

A CE coordinator takes a PostgreSQL advisory lock when it starts and holds it for as long as it runs. A second CE coordinator on the same database stops at startup with:

```
Another coordinator is already running. Running more than one coordinator requires ByteChef Enterprise Edition (bytechef.edition=ee, BYTECHEF_EDITION=ee).
```

A starting coordinator waits for the lock before it gives up: it retries every 2 seconds for up to `BYTECHEF_COORDINATOR_LOCK_WAIT` (default `60s`) and only then stops with the message above. A running coordinator that loses its lock connection shuts down at once, so it never runs alongside a coordinator that took the lock in the meantime. The orchestrator restarts it (the reference Compose file uses `restart: unless-stopped`), and on startup it waits for the lock as above. EE takes no lock.

The lock connection asks PostgreSQL for TCP keepalives, so the database releases the lock within about 25 seconds after a crashed host or a network partition instead of after the operating system's default timeout.

**Rolling deployments.** Stop the old CE coordinator before the new one starts: use the Recreate strategy in Kubernetes, or stop the old container first in Docker. The new coordinator then waits up to the lock wait for the old one to release the lock. A rolling update that keeps the old coordinator running for longer than the lock wait makes the new one stop with the edition message.

## Scheduler clustering

Only coordinators start the Quartz scheduler and fire triggers. Other nodes keep the scheduler in standby: they do not fire triggers, but they can still add and remove scheduled jobs in the shared job store.

| Variable | Default | Purpose |
|---|---|---|
| `BYTECHEF_SCHEDULER_QUARTZ_CLUSTERED` | `false` | Run Quartz in clustered mode on the shared job store, so each trigger fires on exactly one coordinator. |
| `BYTECHEF_SCHEDULER_QUARTZ_CLUSTER_CHECKIN_INTERVAL` | `15000` | How often, in milliseconds, a clustered scheduler checks in with the job store. |

**More than one coordinator requires `BYTECHEF_SCHEDULER_QUARTZ_CLUSTERED=true`.** Without it, each coordinator fires every trigger. Setting it with a single coordinator is harmless, and the reference Compose file always sets it.

Quartz serialises its job store writes through database row locks, whether or not clustering is on.

## Shared sessions

By default, login state lives in the memory of the coordinator that served the login, so a load balancer would have to keep each browser on one coordinator. `BYTECHEF_SESSION_STORE` moves the sessions to a shared store:

| Value | Store | Notes |
|---|---|---|
| `MEMORY` | The coordinator's memory | The default, and the only store in CE. |
| `REDIS` (EE) | Redis, using the `BYTECHEF_REDIS_*` connection | Sessions are kept under the key namespace `bytechef:session` and expire by Redis TTL. Startup fails if Redis is unreachable. |
| `JDBC` (EE) | The `spring_session` and `spring_session_attributes` tables in the `public` schema of the main database | For deployments without Redis. In multi-tenant mode the tables stay in `public`, not in the tenant schema. |

With `REDIS` or `JDBC`, any coordinator serves any request, so the load balancer needs no sticky sessions (no `ip_hash`), and a coordinator failure does not log users out. Remember-me tokens and CSRF tokens do not depend on the session store, but every node still needs the same `BYTECHEF_SECURITY_REMEMBER_ME_KEY`.

A CE node started with `REDIS` or `JDBC` does not fail. It logs at INFO `bytechef.session.store=<value> requires ByteChef Enterprise Edition; using memory sessions` and keeps memory sessions, so one configuration works in both editions.

Workers and webhook nodes do not use sessions, so they keep the session store disabled even when `BYTECHEF_SESSION_STORE` is set. Only nodes with the coordinator role open the Redis connection or use the JDBC tables.

## Cache invalidation

Caffeine is the only cache provider, so every node keeps its own caches. In EE, evictions are broadcast to every other node over the message broker as a control message, and no setting is needed; webhook nodes take part like coordinators. CE runs one coordinator and does not broadcast evictions, so a CE webhook node without the coordinator role does not cache workflow definitions and always reads the current version from the database.

The broadcast needs a broker whose control routes reach every node: `AMQP`, `REDIS`, or `AWS` with `BYTECHEF_MESSAGE_BROKER_AWS_CONTROL_PROVIDER=redis`. Kafka and JMS deliver a control message to one node only, and the in-memory broker has a single node, so with those brokers there is no cluster-wide invalidation.

## Known limitations

With more than one EE coordinator, the editor's webhook test and the embedded JWT checks can fail intermittently. Both keep state in node-local caches, so a request that lands on a different coordinator than the one that stored the state does not find it. Moving that state out of the node-local caches is tracked in [#6632](https://github.com/bytechefhq/bytechef/issues/6632). Until then, run a single coordinator if you rely on those two features.

## Database migrations

Run Liquibase once, before the nodes start, and turn it off on every node:

1. Run a one-shot container from the same image with the `liquibase` command and `BYTECHEF_UPGRADE_ENABLED=true`.
2. Start every node with `BYTECHEF_UPGRADE_ENABLED=false`, after the one-shot run has completed successfully.

This avoids concurrent migration attempts when many nodes start together.

## Reference Compose file

[`docker-compose.cluster.yml`](https://github.com/bytechefhq/bytechef/blob/master/docker-compose.cluster.yml) covers every topology on this page through variables: coordinator, worker and webhook replicas behind nginx, RabbitMQ or Redis, PostgreSQL, and a `migrate` service that runs Liquibase once. The `migrate` service waits for PostgreSQL, RabbitMQ and Redis, and every node starts after it completes.

```bash
export POSTGRES_PASSWORD=... RABBITMQ_PASSWORD=... BYTECHEF_ENCRYPTION_KEY=$(openssl rand -base64 16)
export BYTECHEF_SECURITY_REMEMBER_ME_KEY=...

# CE: 1 coordinator (also serving webhooks), 3 workers
docker compose -f docker-compose.cluster.yml up -d

# CE with 2 separate webhook nodes, the coordinator stays the fallback for webhook traffic
BYTECHEF_WEBHOOKS=2 docker compose -f docker-compose.cluster.yml up -d

# EE high availability: 2 coordinators
BYTECHEF_EDITION=ee BYTECHEF_COORDINATORS=2 docker compose -f docker-compose.cluster.yml up -d

# Webhooks only on 2 webhook nodes, no coordinator fallback
BYTECHEF_WEBHOOKS=2 BYTECHEF_COORDINATOR_WEBHOOKS=false docker compose -f docker-compose.cluster.yml up -d
```

`POSTGRES_PASSWORD`, `RABBITMQ_PASSWORD`, `BYTECHEF_ENCRYPTION_KEY` and `BYTECHEF_SECURITY_REMEMBER_ME_KEY` have no defaults, and every node gets the same values. The other variables and their defaults:

| Variable | Default | Values |
| -------- | ------- | ------ |
| `BYTECHEF_EDITION` | `ce` | `ce`, `ee` |
| `BYTECHEF_COORDINATORS` | `1` | more than 1 requires `ee` |
| `BYTECHEF_WORKERS` | `3` | |
| `BYTECHEF_WEBHOOKS` | `0` | with 0, the coordinators serve webhooks |
| `BYTECHEF_COORDINATOR_WEBHOOKS` | `true` | with `false` and every webhook node down, webhooks fail |
| `BYTECHEF_MESSAGE_BROKER_PROVIDER` | `amqp` | `amqp` (RabbitMQ), `redis` |
| `BYTECHEF_SESSION_STORE` | `redis` | `redis`, `jdbc` (`ee` only) |
| `BYTECHEF_TENANT_MODE` | `single` | `single`, `multi` (`ee` only) |
| `BYTECHEF_IMAGE` | `docker.bytechef.io/bytechef/bytechef:latest` | a locally built image |
| `BYTECHEF_PORT` | `8080` | published port of the load balancer |
| `RABBITMQ_MANAGEMENT_PORT` | `15672` | published port of the RabbitMQ management UI (user `bytechef`, password `RABBITMQ_PASSWORD`) |

With `BYTECHEF_EDITION=ce` and more than one coordinator, the second coordinator keeps restarting with the edition message, and the load balancer never starts because it waits for every coordinator to be healthy. The file storage is a volume all nodes mount; in production, mount shared storage (NFS, EFS) or use the `aws` provider.

The file sets `BYTECHEF_SESSION_STORE=redis` on every node. In EE with the `redis` session store, the Redis connection opens at startup, so the nodes fail to start when Redis is unreachable. PostgreSQL, the message broker and the load balancer are single containers in this file; for production, use managed or clustered services.
