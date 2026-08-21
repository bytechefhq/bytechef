---
title: Plan Limits
description: Cap what a tenant consumes — rate limits, concurrency slots, a monthly cost cap, and resource quotas — enforced at the request boundary and at job admission.
comingSoon: true
---

A **plan tier** is a named bundle of limits applied to a tenant. Self-hosted deployments run
unlimited by default.

## Tiers and the meaning of "unset"

`BYTECHEF_PLAN_TIER` (property `bytechef.plan.tier`) selects the tier: `SELF_HOSTED` (the default),
`FREE`, `PRO`, `TEAM`, or `ENTERPRISE`.

Every individual limit is nullable, and **null means unlimited — never zero**. An unset limit is
simply not enforced. `SELF_HOSTED` leaves every limit null, which is exactly the pre-plan behaviour:
enforcement is registered but is a no-op until you configure a tier or an individual override.

Override any single field with `BYTECHEF_PLAN_LIMITS_*` regardless of tier — see the
[Plan Limits Configuration](/platform/use-bytechef/self-hosted/configuration/environment-variables#plan-limits-configuration)
variables for the full list. A billing integration can replace the default policy provider bean to
resolve tiers per tenant instead of reading them from configuration.

## What gets enforced

Enforcement lives **outside** the workflow engine — at the HTTP request boundary and at job
admission — so the Atlas engine stays limit-agnostic.

| Category | Behaviour | Rejection |
|---|---|---|
| **Rate limits** | Token buckets for login attempts per IP, synchronous surfaces (webhook trigger calls plus the MCP and A2A secret-key endpoints), public API calls, and per-tenant asynchronous submissions. | HTTP **429** with `Retry-After`. |
| **Concurrency slots** | A bounded number of concurrently running async jobs per tenant. A slot is taken at admission and released on terminal job status. The rate check runs first, so a rate rejection never leaks a slot. | HTTP **429**. |
| **Monthly cost cap** | A per-tenant spend ceiling over accumulated execution cost for the calendar month (UTC). Over-cap submissions are rejected unless the tenant's on-demand overage terms admit them. | HTTP **429**. |
| **Quotas** | `maxWorkspaces`, `maxMembers` (pending invitations hold a seat), and `maxStorageBytes` (tenant-wide asset-file storage). | HTTP **403**, no `Retry-After` — a capacity ceiling is not retryable. |
| **Run governance windows** | `syncRunTimeout` caps a synchronous run and can only tighten the configured default, never extend it; `asyncRunTimeout` fails over-long async runs; `logRetentionDays` drives execution-history purging. | See [Crash recovery and run governance](/platform/use-bytechef/self-hosted/management/crash-recovery). |

Synchronous submissions that bypass dispatch are deliberately ungated, to avoid leaking concurrency
slots.

## Local versus global enforcement

`BYTECHEF_PLAN_ENFORCEMENT_PROVIDER` selects where the buckets and counters live:

- `local` (default) — in-memory buckets per node. An N-node deployment therefore admits up to N×
  the budget.
- `redis` — a shared Redis token bucket and a bounded concurrency gate, for strict global limits
  across nodes.

Both Redis paths **fail open** on a Redis outage: availability is preferred over hard-blocking when
the coordinating store is unreachable.

The monthly cost cap and the quota checks read shared database state, so they are naturally global
and need no Redis provider.

Turn the whole mechanism off with `BYTECHEF_PLAN_ENFORCEMENT_ENABLED=false`.

## Observability

Every rejection increments the `bytechef_plan_limit_rejection` counter, tagged `limit` with one of:

`login`, `sync`, `api`, `preauth`, `resume`, `async`, `concurrency`, `cost`, `timeout`, `workspace`,
`member`, `storage`

so you can see which ceiling a tenant is hitting rather than only that something was rejected. The
counter is a no-op when no meter registry is present.

## See also

- [Environment variables](/platform/use-bytechef/self-hosted/configuration/environment-variables#plan-limits-configuration) — every tier and override variable.
- [Crash recovery and run governance](/platform/use-bytechef/self-hosted/management/crash-recovery) — the timeout and retention monitors that consume these limits.
