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
| The webhook paths from the table above | The webhook nodes, with the coordinators as backup. |
| Everything else | The coordinators, round robin. |

Workers serve no HTTP traffic and stay out of the load balancer pool.

The coordinator backup means webhooks keep working when every webhook node is down. Set `BYTECHEF_WEBHOOK_ENABLED=false` on the coordinators to remove that fallback and keep all webhook traffic on webhook nodes. With every webhook node down, webhooks then fail, and the coordinator answers `404`.

In the reference nginx configuration, a request that cannot reach a coordinator within 2 seconds is retried on the next one, so a coordinator failure is visible for about 2 seconds. A request that was already sent is never replayed if it is not idempotent.

## One coordinator in CE

A CE coordinator takes a PostgreSQL advisory lock when it starts and holds it for as long as it runs. A second CE coordinator on the same database stops at startup with:

```
Another coordinator is already running. Running more than one coordinator requires ByteChef Enterprise Edition (bytechef.edition=ee, BYTECHEF_EDITION=ee).
```

If a coordinator loses its database connection, it reconnects and takes the lock again. If another coordinator took the lock in between, the node shuts down. EE takes no lock.

## Scheduler clustering

Only coordinators start the Quartz scheduler and fire triggers. Other nodes can still add and remove scheduled jobs in the shared job store.

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

Workers and webhook nodes do not use sessions.

## Cache invalidation

Caffeine is the only cache provider, so every node keeps its own caches. In EE, evictions are broadcast to every other node over the message broker as a control message, and no setting is needed: it is active whenever the broker is `AMQP`, `REDIS` or `AWS`, and off with the in-memory broker. CE runs one coordinator and has nothing to invalidate across nodes.

## Database migrations

Run Liquibase once, before the nodes start, and turn it off on every node:

1. Run a one-shot container from the same image with the `liquibase` command and `BYTECHEF_UPGRADE_ENABLED=true`.
2. Start every node with `BYTECHEF_UPGRADE_ENABLED=false`, after the one-shot run has completed successfully.

This avoids concurrent migration attempts when many nodes start together.

## Reference Compose file

[`docker-compose.cluster.yml`](https://github.com/bytechefhq/bytechef/blob/master/docker-compose.cluster.yml) covers every topology on this page through variables: coordinator, worker and webhook replicas behind nginx, RabbitMQ or Redis, PostgreSQL, and a `migrate` service that runs Liquibase once. The `migrate` service waits for PostgreSQL, RabbitMQ and Redis, and every node starts after it completes.

```bash
export POSTGRES_PASSWORD=... BYTECHEF_ENCRYPTION_KEY=$(openssl rand -base64 16)
export BYTECHEF_SECURITY_REMEMBER_ME_KEY=...

# CE: 1 coordinator (also serving webhooks), 3 workers
docker compose -f docker-compose.cluster.yml up -d

# EE high availability: 2 coordinators
BYTECHEF_EDITION=ee BYTECHEF_COORDINATORS=2 docker compose -f docker-compose.cluster.yml up -d

# Webhooks only on 2 webhook nodes, no coordinator fallback
BYTECHEF_WEBHOOKS=2 BYTECHEF_COORDINATOR_WEBHOOKS=false docker compose -f docker-compose.cluster.yml up -d
```

The file sets `BYTECHEF_SESSION_STORE=redis` on every node. In EE with the `redis` session store, the Redis connection opens at startup, so the nodes fail to start when Redis is unreachable. PostgreSQL, the message broker and the load balancer are single containers in this file; for production, use managed or clustered services.
