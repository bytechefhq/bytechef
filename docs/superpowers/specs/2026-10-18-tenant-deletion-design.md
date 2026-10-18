# Tenant deletion (issue 6239)

## Intent

Issue 6239 asks for a way to delete "his/her own ByteChef account and all the account associated data easily
through the app itself." In multi-tenant ByteChef (the cloud), an account *is* a tenant: every sign-up gets its own
Postgres schema and is its admin. So the feature is **a tenant admin deletes their organization**, and everything
the tenant owns, inside and outside its schema, is torn down.

Individual users are not self-service: a tenant admin removes a particular user from Settings → Users.

## Goals

- A tenant admin can request deletion of their tenant from the app.
- From the moment of the request, the tenant does nothing: no trigger fires, no schedule runs, no webhook stays
  registered at a provider, nobody can log in, and billing stops.
- After a grace period, every byte the tenant owns is gone: its schema, its vector-store schema, its files, chat
  memory, credential-store secrets, and the public-schema rows created at registration.
- A deleted tenant's id is never handed to a new tenant.
- Admin and SCIM user deletion clean up the user's dependent rows (already built; see "Already built").

## Non-goals

- Self-service deletion of an individual user. The per-user `POST /api/account/delete` endpoint and its UI are
  removed.
- Tenant deletion in single-tenant mode. A self-hosted owner deletes the installation; the option is hidden there.
- A UI to cancel a pending deletion. Cancellation is a support operation during the grace period.
- Distributed EE deployment. Tenant deletion runs in `server-app` only, like environment promotion.
- Fixing the pre-existing bugs listed under "Found along the way".

## Anti-goals

- Leaving anything that can act on the tenant's behalf after the request: a provider webhook still registered, a
  Quartz/db-scheduler/EventBridge job still firing, an OAuth2 refresh still running.
- A dropped tenant whose stale callers silently read and write the **public** schema. Every tenant datasource
  appends `public` to `search_path` (`BaseDataSource.getSearchPath`), and the public schema carries the full table
  set, so an orphaned webhook or scheduled job does not fail: it corrupts public data. Teardown must be complete
  *before* the drop, and the drop must not be the only line of defence (see "Pending tenants are inert").
- A new sign-up inheriting a deleted tenant's id, and with it every key, prefix, bucket and URL built from that id.

## Already built (branch 6239)

`UserDeletingEvent(userId, login)` is published synchronously by `UserServiceImpl.delete` before the user row goes,
and listeners delete or detach the user's rows in the same transaction: persistent tokens, API keys, approval-task
assignee (set null), workspace memberships, and embedded signing keys. Admin GraphQL and SCIM deletes get this
cleanup; before it they failed on the foreign keys from those tables.

There is no self-service per-user deletion endpoint, per the non-goal above. The last-admin guard belongs in the
admin delete path (see "UI").

## Lifecycle

```
ACTIVE ──request──▶ PENDING_DELETION ──grace period elapses──▶ PURGED
                        │
                        └──support cancels──▶ ACTIVE (deployments stay disabled)
```

### Request (tenant admin, synchronous)

Runs inside `TenantContext.runWithTenantId(tenantId, …)` with the tenant schema intact, because trigger state,
connections and deployment rows all live there.

1. Record the request in a new public-schema table (below) with status `PENDING_DELETION`.
2. Disable every enabled automation project deployment through `ProjectDeploymentFacade.enableProjectDeployment(id,
   false)`. That is the path a manual disable takes: per trigger it calls `TriggerLifecycleFacade.executeTriggerDisable`,
   which unregisters dynamic/hybrid webhooks at the provider (using the connection and the stored
   `WebhookEnableOutput`), cancels polling and schedule jobs, and deletes trigger state. Agents, agent channels, MCP
   servers, A2A and API collections all sit on project deployments, so this covers them.
3. Disable every enabled embedded integration instance (`IntegrationInstanceFacade`), connected-user project
   workflow, and integration instance configuration the same way.
4. Cancel the OAuth2 refresh job of every connection (`ConnectionLifecycleFacade.deleteScheduledConnectionRefresh`).
5. Cancel the one-time resume task of every suspended job. One-time tasks are keyed by the bare job id (Quartz group
   `OneTimeTask`, EventBridge `OneTimeTask_resume_<jobId>`), with no tenant in the key, so a prefix sweep cannot find
   them; they are enumerated from the tenant's job table while it still exists and cancelled one by one through
   `TriggerScheduler.cancelOneTimeTask` (my call: this closes the hole without changing the key shape, which is
   listed under "Found along the way").
6. Sweep the scheduler store for anything left under the tenant (see "Scheduler sweep").
7. Snapshot the Stripe customer and subscription ids onto the deletion row, then cancel the subscription
   immediately. `StripeClient` only has `cancelAtPeriodEnd` today; add an immediate cancel. The customer is **not**
   deleted, at request or at purge: its invoices stay in Stripe for accounting.
8. Delete every persistent token, then log the requester out. Other sessions are not enumerable (container
   `HttpSession` only, no Spring Session or `SessionRegistry`); they are rejected per request instead (see "Pending
   tenants are inert").

`executeTriggerDisable` logs and swallows failures by design. The request must not: collect per-deployment failures
and record them on the deletion row, so support can see which provider webhooks may still be registered. The
request still succeeds, since the tenant is inert regardless.

### Pending tenants are inert

While `PENDING_DELETION` (and, for the request-path guards, `PURGED` too):

- Login is refused for the tenant's users, for password, remember-me, social and SSO login alike. The tenant is
  excluded from `TenantRepository.findTenantIds()`, which every by-email/login/reset-key/activation-key lookup
  iterates, so the refusal falls out of that one exclusion; the same exclusion makes per-tenant background jobs
  (billing usage reporting, and any later tenant-looping monitor) skip it.
- Every request that enters a tenant by credential rather than by login is rejected by one filter keyed on
  `TenantContext` against the pending set: existing sessions (session attribute `CURRENT_TENANT_ID`; the filter
  invalidates the session), API keys (`ApiKeyAuthenticationFilter`, `TenantAwareApiKeyAuthenticationFilter`), MCP
  JWTs (`TenantAwareJwtAuthenticationFilter`), SCIM bearer tokens (`ScimBearerTokenAuthenticationFilter`), inbound
  webhooks, and scheduled executions. This is the guard against the public-schema fallback: a straggler must be
  rejected by tenant id, not left to fail on a missing schema.
- Stripe webhook events whose `metadata.tenantId` is in `tenant_deletion` are acknowledged and dropped;
  `BillingWebhookFacadeImpl` otherwise runs them inside the tenant.
- Registration's duplicate-email check consults `tenant_deletion` as well as live tenants, so an address cannot
  re-register during the grace period and end up owning two tenants after a support cancellation (my call).

### Purge (scheduled, after the grace period)

A platform-coordinator sweep picks up rows whose grace period has elapsed:

1. Delete credential-store secrets for the tenant (AWS Secrets Manager / HashiCorp Vault paths built by
   `CredentialPathResolver` with the tenant id). Needs the schema to enumerate connections, so this runs first.
   External credential stores are not on master yet; this step applies once they land.
2. Drop the tenant schema (`TenantService.deleteTenant`).
3. Drop the vector-store schema `bytechef_vectorstore_<tenantId>` on the **pgvector datasource**, which is a separate
   datasource from the main one.
4. Delete the tenant's file storage prefix: `{baseDir}/{tenantId}/` on the filesystem, `s3://{bucket}/{tenantId}/` on
   S3. `FileStorageService` has no prefix delete today; add one.
5. Delete chat memory: the per-tenant S3 bucket (`<bucket-prefix>-<tenantId>`), the Redis index and
   `chat-memory-<tenantId>:` keys, and the in-memory map entries. Steps 3 and 5 are provider-conditional: the pgvector
   datasource exists only when `bytechef.ai.vectorstore.pgvector.url` is set, and chat memory is one of
   aws | in_memory | jdbc | redis.
6. Delete the tenant's users' registration rows in the public schema, unless the login still belongs to another
   tenant.
7. Mark the row `PURGED`. Keep it forever as the tombstone.

Each step is idempotent and the row records the last completed step, so a crashed purge resumes on the next sweep.

### Support cancellation

Support flips the row to `CANCELLED` directly in the database; there is no endpoint and no UI (my call: the only
ops principal is the basic-auth system user gating `/actuator/**`, and an API for a rare manual operation is not
worth a new security surface). The code honours the status: login and the request-path guards reopen. Deployments, instances and connection
refreshes are **not** re-enabled; the admin re-enables what they want. The Stripe subscription stays cancelled.

## Data

A new table in the **public** schema (it must survive the tenant's schema), e.g. `tenant_deletion`:

| column | notes |
|---|---|
| `tenant_id` | primary key |
| `status` | `PENDING_DELETION`, `PURGED`, `CANCELLED` (ordinal; append only) |
| `requested_by` | login of the tenant admin |
| `requested_date` | |
| `purge_after` | `requested_date` + grace period |
| `stripe_customer_id`, `stripe_subscription_id` | snapshotted at request time so a retried cancel still has them |
| `last_completed_step` | purge resume point |
| `teardown_failures` | JSON list of deployments/instances whose trigger disable threw |

Liquibase context: the public-schema contexts the scheduler tables use, not `multitenant`.

### Tenant id allocation

`MultiTenantService.createTenant` takes `MAX(schema_name) + 1`, so deleting the highest tenant hands its id to the next
sign-up. Allocation must take the max over live schemas **and** `tenant_deletion.tenant_id`.

## Scheduler sweep

A safety net after the facade disables, for jobs nothing in the schema can enumerate (editor test webhooks only live
in a cache) or that a failed disable left behind. Every trigger job name is `WorkflowExecutionId.toString()`, the
base64 of `tenantId:…`; a 6-character tenant id is exactly two base64 groups, so the first 8 characters identify the
tenant.

- **Quartz** (public schema, forced by `MultiTenantDriverDelegate`): groups `ScheduleTrigger`, `PollingTrigger`,
  `DynamicWebhookTriggerRefresh` by that prefix; one-time tasks `tenantId:jobId`; connection refresh
  `tenantId + connectionId`.
- **db-scheduler** (`scheduled_tasks`): the same instance-id shapes. Not on master yet; covered once it lands.
- **AWS EventBridge**: `ListSchedules(NamePrefix=<group> + base64prefix)` per group, `OneTimeTask_resume_<tenantId>_`,
  and the connection-refresh names.

Add a `cancelAllForTenant(tenantId)` to the `TriggerScheduler` SPI with one implementation per scheduler. OAuth2
refresh jobs live on a separate SPI, `ConnectionRefreshScheduler` (Quartz and AWS implementations, keys
`tenantId + connectionId`), which gets the same method.

## UI

- Settings → Users: the admin delete already exists; it gains the last-admin guard (an admin cannot delete the last
  `ROLE_ADMIN`, or themselves).
- The profile page gets a **"Delete organization"** section, shown only to tenant admins in multi-tenant mode. The
  client does not know the tenant mode today; `/api/account` (or the application-info endpoint) exposes it. The
  confirmation asks the admin to type their own email (there is no organization name in the data model; the
  `organization`/`company` tables the tenant repository references do not exist) and, for local accounts, their
  password; social/SSO admins confirm with the email alone. It states the grace period and that billing stops now.

## Constraints

- Monolith only (`server-app`); remote-client stubs throw.
- Grace period: a `bytechef.tenant.deletion.grace-period` property, default 30 days (my call).
- Purge sweep: a Spring `@Scheduled` method under `@ConditionalOnCoordinator`, every 15 minutes, reading
  `tenant_deletion` in the public schema (my call; the only cross-tenant periodic job on this branch is Stripe usage
  reporting, implemented once as a Quartz job and once as an SQS listener, and doubling the purge that way buys
  nothing since the sweep reads one public table).
- No explanatory code comments; rationale goes in commit messages.

## Left to the builder

- Package placement of the orchestration facade (it spans platform-user, automation-configuration, embedded,
  connection, scheduler, file storage, billing and tenant modules; it likely needs SPIs so the tenant module does not
  depend on all of them).
- Where the single request-path filter sits in the security chain, and the exact dispatch points for webhooks and
  scheduled executions. `MultiTenantInternalFilter` throws when a session has no tenant on `/api/internal` and
  `/graphql`, so the deletion request endpoint must sit on a path that filter covers.
- Whether the sweep holds a distributed lock (one coordinator purges a tenant at a time).

## Found along the way (out of scope)

- One-time resume tasks are keyed by bare job id with no tenant, so a job id collides across tenants and
  `ResumeJobEvent` runs under whatever tenant the scheduler thread carries.
- `QuartzAlertScheduler` / `QuartzExportScheduler` key jobs by bare `alertRuleId` / `exportJobId`, so jobs collide
  across tenants.
- `AwsTriggerScheduler` names schedules from the first 16 base64 characters of the workflow execution id, which only
  covers `tenantId:type:` plus three digits of the principal id; schedules of different workflows can collide.
- Connection refresh keys concatenate `tenantId + connectionId` without a separator.
