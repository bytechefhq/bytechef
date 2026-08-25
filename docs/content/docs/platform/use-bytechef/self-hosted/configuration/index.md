---
title: Configuration
description: Learn how to configure your ByteChef instance
---

ByteChef is configured entirely through **externalized configuration** - there is no settings screen inside the application for instance-wide options. Everything from the database connection to the AI provider keys is supplied before the process starts, so the same container image behaves differently per environment based on the values you pass in.

## How configuration is supplied

ByteChef is a Spring Boot application, so it reads configuration from the standard Spring sources, in order of increasing precedence:

1. The bundled defaults (documented on the [Environment Variables](/platform/use-bytechef/self-hosted/configuration/environment-variables) page).
2. An external `application.yml` / `application.properties` placed next to the process or pointed at with `--spring.config.additional-location`.
3. **Environment variables** - the recommended mechanism for containers, and the form every example in this documentation uses.

Environment variables and YAML properties are interchangeable through Spring's **relaxed binding**: the property `bytechef.datasource.url` is the same setting as the environment variable `BYTECHEF_DATASOURCE_URL`. Uppercase the name, and replace dots and dashes with underscores. Indexed list entries use a trailing number, e.g. `bytechef.feature-flags[0]` becomes `BYTECHEF_FEATURE_FLAGS_0`.

```yaml tab="application.yml"
bytechef:
  datasource:
    url: jdbc:postgresql://postgres:5432/bytechef
    username: postgres
    password: postgres
```

```bash tab="Environment variables"
BYTECHEF_DATASOURCE_URL=jdbc:postgresql://postgres:5432/bytechef
BYTECHEF_DATASOURCE_USERNAME=postgres
BYTECHEF_DATASOURCE_PASSWORD=postgres
```

See the [Environment Variables](/platform/use-bytechef/self-hosted/configuration/environment-variables) reference for the complete, categorized list of every setting and its default.

## Minimum configuration for a production instance

Beyond the defaults, a single-node deployment needs only a handful of settings:

| Setting | Why it matters |
|---|---|
| `BYTECHEF_DATASOURCE_URL`, `BYTECHEF_DATASOURCE_USERNAME`, `BYTECHEF_DATASOURCE_PASSWORD` | Point ByteChef at your PostgreSQL 15+ database. Schema migrations run automatically on startup. |
| `BYTECHEF_SECURITY_REMEMBER_ME_KEY` | A fixed secret used to sign "remember me" tokens. Set it to a stable random value so existing sessions survive restarts. |
| `BYTECHEF_PUBLIC_URL` | The externally reachable base URL of the instance (default `http://127.0.0.1:8080`). It is the base for webhook URLs, the OAuth2 redirect URI, and links in outgoing mail. |
| `BYTECHEF_ENCRYPTION_PROVIDER` / `BYTECHEF_ENCRYPTION_PROPERTY_KEY` | Controls how stored credentials are encrypted at rest - see below. |

## Encryption of stored credentials

Connection credentials and other secrets are encrypted at rest. The unencrypted form exists only in
memory, at execution time.

### The cipher

New values are encrypted with **AES-GCM** using a fresh random 12-byte IV per value, and stored as
`v2:` + base64(IV + ciphertext + auth tag). GCM gives confidentiality, integrity (the auth tag
detects tampering), and no pattern leakage across identical plaintexts. Values written by older
versions under AES-ECB carry no version prefix and are still decryptable, so an upgrade needs no
re-encryption pass.

### The key

There is **one instance-wide encryption key**, chosen with `BYTECHEF_ENCRYPTION_PROVIDER`:

- **`FILESYSTEM`** (default) - ByteChef generates an AES key on first start and writes it to the
  local filesystem. This is fine for a single, persistent node, but it is **not** suitable for
  containers with ephemeral disks or for multi-instance deployments, because each replica would
  generate its own key and could not decrypt data written by the others.
- **`PROPERTY`** - you supply the key yourself via `BYTECHEF_ENCRYPTION_PROPERTY_KEY`. Use this for
  Kubernetes and any multi-replica setup so every instance shares one stable key. Store the key in a
  secret manager, never in source control.

Back the key up with the same rigour as the database. A restored database without its key is a
database of undecryptable credentials - see
[Upgrades and backups](/platform/use-bytechef/self-hosted/management/upgrades). The key is not
workspace- or tenant-scoped; isolation between tenants comes from the schema boundary, not from
separate key material.

### What counts as a credential

A connection's **authorization parameters** - OAuth access and refresh tokens, API keys and bearer
tokens, basic-auth pairs, certificate material and private keys used for client auth, and any custom
auth fields a component definition declares. A component author declares the connection's properties
once, and the platform treats them as credentials end-to-end. The REST controllers obfuscate
`authorizationParameters` on the way out, so a connection's secret is never returned to a client that
reads the connection back.

Encryption governs *where the secret lives*; it says nothing about who may use the connection. That is
[connection visibility](/platform/settings/connections), which is configured separately.

### Rotation

Rotate a credential in one place - the connection edit screen. Workflows reference a connection by
id rather than inlining its secret, so every running and future execution picks up the new value with
no workflow change and no redeploy.

> **Coming soon.** Integration with external secret managers (HashiCorp Vault, AWS Secrets Manager),
> so that secret material lives in your vault rather than in ByteChef's database, is on the upcoming
> release track and is not yet available in the latest released version of ByteChef.

## Data retention

ByteChef does not delete your data unless you tell it to, and there is no consolidated retention
settings screen today - retention is configured per data type through the properties below.

> **Coming soon.** Execution-history retention (`BYTECHEF_WORKFLOW_EXECUTION_RETENTION_*`) is on the
> upcoming release track and is not yet available in the latest released version of ByteChef. It
> permanently deletes finished runs past a configured window, along with their task executions,
> output blobs, and context rows. See
> [Crash recovery](/platform/use-bytechef/self-hosted/management/crash-recovery) for the monitor's
> cadence, its variables, and their defaults.

Two things to plan for once you set a window. Deletion is not instantaneous - a scheduled monitor
sweeps on a fixed cadence and removes expired records in batches. And expired execution records take
their [file storage](/platform/use-bytechef/self-hosted/configuration/file-storage) artifacts with
them, so storage cost tracks the retention policy rather than growing without bound.

## More configuration topics

| Topic | What it covers |
|---|---|
| [Environment variables](/platform/use-bytechef/self-hosted/configuration/environment-variables) | The complete, categorized reference for every setting and its default. |
| [Message brokers](/platform/use-bytechef/self-hosted/configuration/message-brokers) | Choosing the broker that carries task dispatches between the coordinator and the workers. |
| [File storage](/platform/use-bytechef/self-hosted/configuration/file-storage) | Where the opaque bytes a workflow produces are kept - S3, the filesystem, or the database. |
| [Plan limits](/platform/use-bytechef/self-hosted/configuration/plan-limits) | Rate limits, concurrency slots, cost caps, and resource quotas per tenant. |

## Edition, tenancy, and sign-up

A few instance-wide switches shape the whole deployment:

| Setting | Effect | Default |
|---|---|---|
| `BYTECHEF_EDITION` | Selects Community (`CE`) or Enterprise (`EE`) behavior. EE-only features (SSO, connection visibility scopes, S3 file storage) require `EE`. | `EE` |
| `BYTECHEF_TENANT_MODE` | `SINGLE` for one tenant per instance, `MULTI` for multi-tenant. | `SINGLE` |
| `BYTECHEF_ENVIRONMENT` | Optionally pin the instance to a single environment (`DEVELOPMENT`, `STAGING`, `PRODUCTION`). | - |
| `BYTECHEF_SIGN_UP_ENABLED` | Whether visitors can self-register from the login screen. | `true` |
| `BYTECHEF_SIGN_UP_ACTIVATION_REQUIRED` | Whether new accounts must confirm via email before logging in (requires mail to be configured). | `false` |

## Verifying the resolved configuration

Once the instance is running, the effective, merged configuration is exposed through the Actuator `env` endpoint at `/actuator/env`. That endpoint is protected - it is reachable only with the system administrator credentials set via `BYTECHEF_SECURITY_SYSTEM_USERNAME` / `BYTECHEF_SECURITY_SYSTEM_PASSWORD`. See [Observability](/platform/use-bytechef/self-hosted/management/observability) for the full Actuator surface.

## Running multiple instances

To scale horizontally, run several ByteChef instances against the same database. A few settings must be aligned so the replicas cooperate rather than conflict:

| Concern | What to configure |
|---|---|
| **Encryption key** | Use `BYTECHEF_ENCRYPTION_PROVIDER=PROPERTY` with the same `BYTECHEF_ENCRYPTION_PROPERTY_KEY` on every instance, so all replicas can decrypt the same stored credentials. |
| **Schema migrations** | Keep `BYTECHEF_UPGRADE_ENABLED=true` on one instance only during upgrades to avoid concurrent migration attempts. |
| **Message broker** | Move off the default in-memory broker to a shared one (`BYTECHEF_MESSAGE_BROKER_PROVIDER` = `REDIS`, `KAFKA`, `AMQP`, …) so tasks dispatched on one node can be executed on another. |
| **Cache** | Set `BYTECHEF_CACHE_PROVIDER=REDIS` so cache state is shared instead of per-node. |
| **Remember-me key** | Set a single fixed `BYTECHEF_SECURITY_REMEMBER_ME_KEY` across all instances so sessions remain valid regardless of which node serves a request. |

### Splitting coordinator and worker roles

Every instance runs both halves of the workflow engine by default: the **coordinator**, which
advances jobs and fires triggers, and the **worker**, which executes tasks. Each half has its own
switch, and both default to on:

| Variable | Default | Effect when `false` |
|---|---|---|
| `BYTECHEF_WORKER_ENABLED` | `true` | The node stops executing tasks. It keeps coordinating jobs and keeps serving the API and UI. |
| `BYTECHEF_COORDINATOR_ENABLED` | `true` | The node becomes a **headless task executor**: no API, no GraphQL, no UI - see below. |

> **Disabling the coordinator also disables the API.** The switch does not gate job coordination
> alone - it gates the application surface. Of the 224 classes behind it, 211 are controllers: 87
> REST API controllers and 104 GraphQL controllers, including account, workflow, connection and
> project endpoints. A node with `BYTECHEF_COORDINATOR_ENABLED=false` executes tasks and answers
> nothing else.

That gives you one useful topology: **one node with both halves on**, serving the API and UI and
coordinating jobs, plus **as many `BYTECHEF_COORDINATOR_ENABLED=false` nodes as you need** to
execute tasks. Task execution is normally what needs the capacity, so this is the axis worth
scaling - add executors without adding application nodes.

Three things to get right:

- **Do not route HTTP traffic to executor nodes.** They serve no API, so a load balancer that
  round-robins across every instance will fail a large share of requests. Keep executors out of the
  pool entirely, and health-check the application nodes only.
- **A shared message broker is required**, not optional. With the default in-memory broker an
  executor never receives work and an application node has nothing to dispatch to, so the split
  silently produces a system that accepts jobs and never runs them. Configure the broker and cache
  from the table above *before* changing these switches.
- **Do not turn both off on the same node.** Nothing prevents it, and the result is an instance
  that does nothing at all.

Running more than one node with the coordinator enabled is supported - they are ordinary replicas
once the table above is satisfied - but the coordinator is rarely the bottleneck, so scale the
executors first.

#### Routing tasks to specific workers

Workers subscribe to named task queues, with a concurrency per queue:

```bash
BYTECHEF_WORKER_TASK_SUBSCRIPTIONS_DEFAULT=10
```

A task carrying a `node` property is routed to the queue of that name, so a worker subscribing to
`BYTECHEF_WORKER_TASK_SUBSCRIPTIONS_CAPTIONS` receives every task marked `node: captions`. Use it
to give heavy or long-running work its own pool rather than letting it occupy the default one. The
queue is created on demand when a worker subscribing to it starts.
