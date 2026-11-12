# Embedded Trigger API - Design

Issue: [#1732 Connectivity Webhook/Trigger API](https://github.com/bytechefhq/bytechef/issues/1732)
Date: 2026-11-11
Status: implemented. Implementation notes below record where the code deviates from this design.

## Intent

An embedded developer (a SaaS company embedding ByteChef) wants to know when something changes in an app
one of their end users has connected - a new HubSpot contact, a new Slack message in a channel - without
building an integration workflow inside ByteChef. They call ByteChef from their own backend to **subscribe a
connected user (`externalUserId`) to a component trigger** with parameters; ByteChef registers the webhook with
the target app (or polls it) using that user's connection, and **POSTs every event to the developer's
application**.

This is the counterpart of the existing embedded Action API
(`POST /api/embedded/v1/{externalUserId}/components/{componentName}/versions/{componentVersion}/actions/{actionName}`,
`ActionApiController` -> `ActionFacadeImpl`), which runs one component action for a connected user.

Prior art the user pointed at: Paragon ActionKit triggers and Composio triggers. Both model a trigger
*instance* per connected account, deliver every event to **one signed webhook URL per project**, and let the
developer enable/disable/delete instances independently. Paragon also lets triggers be turned on from the
dashboard's Connected Users page.

**Rely on existing infrastructure** (user's explicit constraint): the trigger lifecycle
(`TriggerLifecycleFacade`: dynamic webhook registration + refresh, polling scheduler, listeners), webhook
ingress (`/webhooks/{id}`), the trigger coordinator, the workflow engine (jobs, execution history), outbound
`WebhookDeliveryService`, and the external invocation log. No parallel trigger runtime.

## Goals

- Public REST API to subscribe a connected user to any component trigger, and to list, get, update, enable,
  disable and delete subscriptions; plus listing the triggers available to subscribe to.
- All trigger types work: dynamic webhook (auto-registered), polling, hybrid, listener, and static webhook
  (the subscribe response returns the ByteChef URL the end user must paste into the app).
- Each fired event is delivered to the developer as an HMAC-signed POST.
- Many subscriptions per user and trigger (e.g. two Slack channels) - each subscribe call creates a new
  subscription with its own id and parameters.
- Each trigger execution (event fired -> delivered) appears in the embedded external invocation log and UI.
- Admin UI: configure the delivery destination per environment; see and manage a connected user's
  subscriptions.

## Non-goals (v1)

- Normalizing payloads across apps (unified event model). The event carries the trigger output as-is; may come
  later.
- Recording the management calls (subscribe/update/enable/disable/delete) in the external invocation log.
  Only executions are logged.
- Frontend (JS) SDK helpers.
- A local-development event stream (Composio's `subscribe()`).
- Durable (persisted) outbound delivery queue - see "Delivery" for what we get from `WebhookDeliveryService`.

## Anti-goals

- Subscription workflows must never surface as user-facing workflows: not in the connected user's workflow
  list, the Automation Hub, workspace project listings, or anywhere a connected user or admin edits workflows.
- No second trigger runtime: a subscription must not enable/schedule/register a trigger through any path other
  than `TriggerLifecycleFacade`.
- No subscription may keep firing after it is disabled or deleted, or after its connection, integration instance
  or connected user is disabled/deleted/invalid. Unregistering at the target app is attempted every time but is
  best-effort when credentials are already dead (see designer calls).
- Subscribing, updating or deleting one subscription must never touch any other workflow, deployment or trigger.

## Decisions made with the user

1. **Granularity: component triggers**, mirroring the Action API (not triggers of integration workflows built in
   ByteChef).
2. **Instances:** many subscriptions per `externalUserId` + trigger; each has its own id and parameters.
3. **Trigger types:** all. Static webhook returns the ByteChef webhook URL in the response.
4. **Delivery destination:** a Webhook URL + signing secret **per environment** (configured in embedded
   Settings), with an **optional per-subscription `webhookUrl` override** on subscribe/update.
5. **Payload:** an envelope with the trigger output as-is (`data`); no normalization now.
6. **Broken connection:** when the connection is deleted or becomes invalid, the subscription is
   auto-disabled (trigger actually unregistered/unscheduled), its status set to a connection-error state, and a
   lifecycle event is delivered to the developer's webhook so they can re-prompt the user.
   Implementation note: an invalid connection behaves as described (`CONNECTION_ERROR`, `subscription.disabled`).
   A deleted connection does not disable the subscription: every subscription of the connection is deleted,
   whatever its status, and `subscription.deleted` with `CONNECTION_DELETED` is sent (see "Connection lifecycle").
7. **External invocation log:** record **trigger execution calls** (each fired event and its delivery), not the
   management calls; visible in the existing embedded External Invocations page and the connected-user sheet.
8. **Scope:** public REST API + docs + admin UI (settings section and per-connected-user subscription list).
9. **Placement: one hidden project per subscription.** Not the connected user's automation project (publishing
   that copies every workflow, including unpublished drafts, and re-registers every other trigger in it).
10. **Throughput:** `WebhookDeliveryService` as-is is acceptable for v1 (one delivery at a time per destination
    URL, 10 in flight globally, 1000 pending per URL, excess dropped); limits stay configurable in yml.

## Calls made by the designer (not the user) - check these

- **Delivery is asynchronous**: the delivery task enqueues and completes; the outcome callback writes the
  invocation log row and the subscription's last-delivery status. A failed delivery therefore does **not** fail
  the job. Reason: one developer URL is shared by all users, and blocking a `workerExecutor` permit through
  per-endpoint serialization and retries could starve every workflow (CLAUDE.md executor rules).
- **Auto-disable triggers**: connection INVALID or deleted, integration instance disabled or deleted, connected
  user disabled. Deleting a connected user deletes their subscriptions. Re-enabling is always explicit.
  Implementation note: a deleted connection deletes the subscriptions instead of disabling them.
- **Unregistration is best-effort** when credentials are dead: disabled locally, ingress rejects, a webhook may
  remain orphaned at the target app. A revoked API key is not detected in v1.
  Implementation note: a bulk disable runs each subscription in its own REQUIRES_NEW transaction. When unregistering
  fails, that transaction rolls back and `recordDisableFailure` sets the subscription `FAILED` with the error, not
  `DISABLED`/`CONNECTION_ERROR`; the deployment workflow stays enabled, and events stop only because the delivery
  task skips subscriptions that are not `ENABLED`. Deleting a connection or connected user logs a failed unregister
  and deletes the subscription anyway.
- **Subscriptions bind to the integration instance** whose connection the Action API would use
  (`ConnectionIdHelper`), and the connection is set explicitly on the generated workflow/deployment.
  Implementation note: `TriggerSubscriptionFacadeImpl` resolves the instance through
  `IntegrationInstanceService.fetchIntegrationInstance` / `getIntegrationInstance` instead of `ConnectionIdHelper`,
  with the same effect. Without `X-Instance-Id` the first matching instance is used, and an instance of another
  connected user answers 403.

## Architecture (proposed; builder may adjust details, not the shape)

### A subscription is a hidden single-workflow project

Each subscription gets **its own hidden automation project** (user decision 9) with exactly one workflow and one
`ProjectDeployment`, owned by a new `ProjectOwner` value (e.g. `EMBEDDED_TRIGGER_SUBSCRIPTION`) so it is excluded
from workspace listings the same way `EMBEDDED_CONNECTED_USER_PROJECT` projects are. It is **not** the connected
user's automation project: `ConnectedUserProjectFacadeImpl.publishProjectWorkflow` -> `ProjectFacadeImpl.publishProject`
copies every workflow in a project (drafts included), and `ProjectDeploymentFacadeImpl.updateProjectDeployment` /
`checkProjectDeploymentWorkflows` re-enable the triggers of every enabled workflow in the deployment. With one
workflow per project, the existing create -> publish -> deploy -> enable paths can be reused unchanged and only
ever touch that subscription.

The generated workflow has exactly two nodes:

1. the requested trigger (`{componentName}/v{componentVersion}/{triggerName}`) with the subscription's
   parameters and the user's connection;
2. a **trigger-delivery task** (below) that receives the trigger output and the subscription id.

Enabling goes through `ProjectDeploymentFacade.enableProjectDeploymentWorkflow`, which builds
`WorkflowExecutionId.of(AUTOMATION, projectDeploymentId, workflowUuid, triggerName)` and calls
`TriggerLifecycleFacade.executeTriggerEnable(...)`. Everything downstream is existing machinery: dynamic webhook
registration and refresh, polling via the scheduler, `/webhooks/{id}` ingress, `TriggerCoordinator` ->
`TriggerCompletionHandler` -> one job per output item, execution history. Disable = disable the deployment
workflow; update = new workflow version, republish and redeploy that one project; delete = disable, then remove
the deployment, project and workflow.

The builder may reuse pieces of `ConnectedUserProjectWorkflowManager` / `ConnectedUserProjectFacadeImpl` but must
not route through them in a way that touches the user's automation project.

**Connection:** resolved the same way the Action API does it -
`ConnectionIdHelper.getConnectedUserConnection(externalUserId, componentName, instanceId, environment)` (optional
`X-Instance-Id` header, as on `executeAction`) - and stored on the subscription together with the integration
instance id. It is set **explicitly** on the deployment workflow's connections; do not rely on
`ConnectedUserProjectWorkflowManager.checkWorkflowNodeConnection`, which binds the first EMBEDDED connection in the
tenant whose component name matches, not the user's own. A missing required connection fails the subscribe call.

Known wrinkle to settle while building: `ConnecteduserProjectTriggerDispatcherPreSendProcessor` and
`ProjectTriggerDispatcherPreSendProcessor` are both `@Order(2)` and both match AUTOMATION, and
`TriggerDispatcher.preProcess` uses only the first match. Subscription workflows must resolve their connection
deterministically from their deployment - verify which processor runs and fix the ordering if needed.

Hiding (anti-goal above): the new `ProjectOwner` keeps these projects out of workspace listings; verify that no
other listing (connected-user sheet, Automation Hub, provisioning dialogs, public
`ConnectedUserProjectWorkflowApiController`, project/deployment pickers) shows them. Job executions of
subscription workflows may appear in workflow-execution views; label them by subscription if they do.

### `connected_user_trigger_subscription` (new EE table, Liquibase)

The developer-facing record. Suggested columns: `id`, `connected_user_id`, `environment`, `component_name`,
`component_version`, `trigger_name`, `name` (optional label), `external_id` (optional, the developer's own id),
`parameters` (JSON), `webhook_url` (optional override), `status`, `error_code`, `error_message`,
`integration_instance_id`, `connection_id`, `project_id` / `project_deployment_id` of the hidden project,
`last_event_date`,
`last_delivery_status`, audit columns, `version`.

Status: `ENABLED`, `DISABLED`, `CONNECTION_ERROR` (auto-disabled because of the connection), `FAILED` (enable
failed at subscribe/update/enable time; error code/message set). `name`, `externalId` and the error codes come
from the commented-out `TriggerSubscription` sketch already in
`server/ee/libs/embedded/embedded-execution/embedded-execution-public-rest/openapi.yaml`; error codes:
`USER_INPUT_ERROR`, `API_ERROR`, `TIMEOUT_ERROR`, `OAUTH_REFRESH`, `NO_TRIGGER`, `INTERNAL_ERROR`.

Implementation note: the error codes map to HTTP as follows. `API_ERROR`, `TIMEOUT_ERROR` and `OAUTH_REFRESH` answer
499 with `meta.origin` `remote-provider` and are produced only for component or provider failures. `INTERNAL_ERROR`
is any local failure, answers 500 with origin `bytechef` and a fixed message (the cause stays in the log).
`USER_INPUT_ERROR` and `NO_TRIGGER` answer 400 with origin `bytechef`, and an enable that finds a required
connection missing is a `USER_INPUT_ERROR` too. `lastDeliveryError` and `error_message` are cut to 1024
characters. A `ConfigurationException` that is not a trigger subscription error (another entity class, or an
unknown error key) answers 500 `INTERNAL_ERROR` with the generic detail "Internal error while processing the trigger
subscription" and is logged; a failure while registering the trigger keeps "Internal error while enabling the
trigger subscription". `NO_TRIGGER` is produced only on create, and `checkTriggerExists` runs before the
connection-required check, so an unknown component answers `NO_TRIGGER`. Only a `TriggerSubscriptionEnableException`
carries `triggerSubscriptionId`; validation errors and 404 never do.

Liquibase changelogs go in their own commit (`<ticket> Upgrade - ...`).

### Trigger-delivery task

A dedicated internal task handler (not `http-client`), run on the worker like any task. At run time it:

1. loads the subscription by id (so later URL/secret changes apply without regenerating workflows);
2. resolves the destination: subscription `webhookUrl` override, else the environment Webhook URL; if neither
   is set, the task fails with a clear error (implementation note: the task never fails. A missing URL or secret
   records a `NO_DESTINATION` / `NO_SIGNING_SECRET` invocation row and sets `lastDeliveryStatus` to `FAILED`, and a
   subscription that is not `ENABLED` is skipped silently);
3. builds the envelope and submits it via `WebhookDeliveryService.submit(WebhookDelivery)` with the
   environment signing secret, `eventType = "trigger.event"`, and a delivery id;
4. **completes without waiting** for the delivery (designer call; never hold a `workerExecutor` permit on
   the shared, serialized developer endpoint);
5. on the returned `CompletableFuture<WebhookDeliveryOutcome>` completing (success, failure, or dropped because
   the per-URL pending limit was hit): updates `last_event_date` / `last_delivery_status` / last error on the
   subscription and records the execution in the external invocation log (below). The callback runs off the
   worker lane and must be cheap.

A failed delivery does not fail the job; it is visible as an `ERROR` row in the invocation log and on the
subscription.

It must not appear in the component catalogue, workflow editor palette, Action API, tools, or MCP. Builder
picks the mechanism (an internal `TaskHandler` registered in the handler map rather than a public component is
preferred; there is no existing "hidden component" flag).

### Delivery

- Transport: `WebhookDeliveryService` (platform-notification-delivery) on the `webhookDeliveryExecutor` lane.
  It already provides HMAC signing (`X-ByteChef-Signature: t=<ms>,v1=HMAC-SHA256(secret, ts + "." + json)`,
  plus `X-ByteChef-Event`, `X-ByteChef-Timestamp`, `X-ByteChef-Delivery` when an event type is set), retries
  (`WebhookRetry`), per-endpoint serialization, endpoint pause after repeated failures, and URL validation
  against `bytechef.webhook-delivery.allowed-hosts`. Retry defaults belong in `application.yml`.
  Implementation note: the signed timestamp is the send time of each attempt, so `X-ByteChef-Timestamp` and `t=`
  change on every retry, while the `timestamp` in the body stays the event time. Receivers check the replay window
  against the header, never the body.
- Not durable and serialized per destination URL (one at a time; 10 in flight globally and 1000 pending per URL
  by default in `application-bytechef.yml`; excess dropped). Accepted for v1 (user decision 10); drops and
  failures become `ERROR` rows in the invocation log.
- Event envelope (`trigger.event`):
  ```json
  {
    "type": "trigger.event",
    "id": "<delivery id>",
    "subscriptionId": "...",
    "externalId": "... (if set)",
    "externalUserId": "...",
    "environment": "PRODUCTION",
    "componentName": "hubspot",
    "componentVersion": 1,
    "triggerName": "newContact",
    "jobId": 123,
    "timestamp": "2026-11-11T10:00:00Z",
    "data": { "...": "trigger output as-is" }
  }
  ```
- Lifecycle envelope (`subscription.disabled`, and `subscription.deleted` when a connected user or a connection is
  deleted):
  same identifying fields, `reason` (`CONNECTION_DELETED`, `CONNECTION_INVALID`, `INTEGRATION_INSTANCE_DISABLED`,
  `INTEGRATION_INSTANCE_DELETED`, `CONNECTED_USER_DISABLED`, `CONNECTED_USER_DELETED`), no `data`/`jobId`.
  Implementation note: `subscription.disabled` carries `CONNECTION_INVALID`, `INTEGRATION_INSTANCE_DISABLED`,
  `INTEGRATION_INSTANCE_DELETED` or `CONNECTED_USER_DISABLED`; `subscription.deleted` carries `CONNECTION_DELETED`
  or `CONNECTED_USER_DELETED`. Nothing disables with `CONNECTION_DELETED` any more, although
  `TriggerSubscriptionManager.getDisabledStatus` still maps it to `CONNECTION_ERROR`. Admin disable and delete from
  the UI send no lifecycle event.

### Environment settings (Webhook URL + signing secret)

There is no embedded per-environment webhook setting today. Store it in the existing encrypted `property`
table (`Property`, `Scope.EMBEDDED`, nullable `environment`, `EncryptedMapWrapper` value,
`PropertyService.fetchProperty(key, scope, scopeId, environmentId)`) - currently unused for EMBEDDED.
Per environment: `webhookUrl`, `signingSecret` (generated server-side; rotatable). The secret is shown in the
admin UI (reveal/copy) and never returned by the public API. Implementation note: the settings class is
`TriggerSubscriptionWebhookSettingsAccessor` (not a service). Rotation applies to new events at once; deliveries
already queued or retrying keep the secret they were submitted with.

### Connection lifecycle

- **Invalid connection:** `ConnectionAfterSaveEventListener` (automation-connection-service) already reacts to a
  status change to `INVALID` but only calls `disableProjectDeploymentWorkflowByConnectionId`, which flips
  `enabled=false` without disabling triggers. For subscriptions this is not enough: every subscription using the
  connection must be properly disabled through the normal disable path (trigger unregistered/unscheduled),
  set to `CONNECTION_ERROR`, and a `subscription.disabled` event delivered. Builder decides whether to fix the
  general gap for all project deployment workflows or scope the fix to subscriptions; at minimum subscriptions
  must be covered. Lower layers must not depend on embedded: hook in from the embedded side (e.g. an event or
  SPI the automation listener publishes/calls), not by importing embedded code into automation-connection.
  `INVALID` is only set when an OAuth token refresh fails (`TokenRefreshHandler`); other auth types are not
  detected in v1.
- **Deleted connection:** `ConnectedUserConnectionFacadeImpl.deleteConnectedUserConnection` must disable the
  affected subscriptions the same way before the connection is removed (`ConnectionFacadeImpl.delete` refuses
  deletion while a connection is in use, so the order matters).
  Implementation note: this was changed to deleting. `ConnectedUserConnectionFacadeImpl.deleteConnectedUserConnection`
  and `ConnectionAdminFacadeImpl.deleteConnection` first call `ConnectionFacade.checkDeletable`, so a refused delete
  touches no subscription and makes no remote call, then `TriggerSubscriptionManager.deleteByConnectionId` deletes
  every subscription of the connection, whatever its status, and sends `subscription.deleted` with
  `CONNECTION_DELETED`. Deleting a connected user checks all the user's connections first, in the same way.
- **Integration instance** (the one the subscription is bound to): `IntegrationInstanceFacadeImpl.enableIntegrationInstance`
  (disable) and `deleteIntegrationInstance` disable the subscription with `INTEGRATION_INSTANCE_DISABLED` /
  `INTEGRATION_INSTANCE_DELETED`. Implementation note: a subscription disabled with `INTEGRATION_INSTANCE_DELETED`
  cannot be enabled again, because its instance no longer exists (enable and update answer 400).
- **Connected user:** `ConnectedUserFacadeImpl.enableConnectedUser` (disable) disables all their subscriptions
  (`CONNECTED_USER_DISABLED`); deleting a connected user deletes them (`subscription.deleted`,
  `CONNECTED_USER_DELETED`). Implementation note: the bulk disables caused by another change (integration instance
  disabled or deleted, connected user disabled, credentials invalidated) run after that change commits, each
  subscription in its own transaction, so a rolled-back change disables nothing. A subscription whose disable fails
  becomes `FAILED` with the error recorded and gets no `subscription.disabled` event; the others are still disabled.
  A crash between the commit and the after-commit work leaves subscriptions `ENABLED`, and nothing disables them
  later: the delivery task checks only the subscription's own status, so subscriptions of a disabled integration
  instance or connected user keep delivering events, and a connection that is already `INVALID` publishes no new
  `ConnectionCredentialsInvalidatedEvent`.
- Every auto-disable goes through the normal disable path (`TriggerLifecycleFacade.executeTriggerDisable`), which
  is best-effort at the target app when credentials are dead (it logs and clears local state); ingress for a
  disabled subscription must be rejected. Implementation note: see the note under "Unregistration is best-effort":
  a failed unregister leaves the subscription `FAILED` with the deployment workflow still enabled, and the delivery
  task drops its events.
- Implementation note: deleting a connection or connected user unregisters each `ENABLED` subscription's trigger
  (REQUIRES_NEW), logs and skips an unregister failure, and deletes the rows inside the caller's transaction. When
  that transaction rolls back, an `afterCompletion` synchronization marks the subscriptions whose trigger was
  already unregistered `FAILED` / `INTERNAL_ERROR` with "Trigger unregistered; delete was rolled back".
- Implementation note: disabling, updating or deleting an `ENABLED` subscription goes through
  `enableProjectDeploymentWorkflow(false)`, which stops the deployment's running jobs, so an event captured but not
  yet delivered is dropped without an invocation row. Every event is an AUTOMATION job of the hidden deployment;
  no retention removes them, and `deleteProjectDeployment` deletes them all synchronously when the subscription is
  deleted. The hidden deployments are excluded from the embedded Workflow Executions listing.
- Re-enabling after the user reconnects or the instance/user is re-enabled is an explicit developer action
  (`enable` / `update`); no automatic re-enable in v1. Implementation note: only `enable` re-enables. `update`
  registers the trigger again only for an `ENABLED` subscription and keeps any other status.

### External invocation log

- Append `API_TRIGGER` to `ExternalInvocationSource` **at the end** (persisted as ordinal). Kind: `COMPONENT`.
- One row per **trigger execution**: written by the delivery task with `componentName`, `componentVersion`,
  `operationName = triggerName`, `connectionId`, `environment`, `externalUserId`, `connectedUserId`, `jobId`,
  outcome (`SUCCESS` when the developer endpoint accepted the event, `ERROR` otherwise, with error type/message)
  and duration. Implementation note: duration runs from handing the event to the delivery queue until the delivery
  ended, so it includes queue wait and retry back-off, and is `0` when nothing was submitted.
- Management calls are not logged (non-goal).
- Update the public `ExternalInvocationSource` enum in the embedded public OpenAPI (it also currently lacks
  `A2A_SERVER`; fixing that is in scope since the enum is being touched) and its description of what the
  endpoint returns.
- Client: add `API_TRIGGER` to `SOURCE_LABELS` ("API (Trigger)") and `SOURCE_TYPES` (icon + "Trigger") in
  `client/src/pages/automation/external-invocations/components/ExternalInvocationsTable.tsx`; ensure the source
  filter on the embedded External Invocations page and the connected-user sheet list offers it.

## Public REST API

Lives in `embedded-execution-public-rest` (`/api/embedded/v1`), same auth (`bearerAuth`, `jwtBearerAuth`) and
`X-Environment` / `X-Instance-Id` headers as `executeAction`. The commented-out trigger/subscription sketch in
that `openapi.yaml` is prior art; replace it, keeping its useful fields. After editing `openapi.yaml`, run
`./gradlew -p <module> generateOpenAPI` (see `.claude/rules/openapi.md`).

Proposed paths (builder may refine naming, keep the shape):

- `GET /{externalUserId}/components/{componentName}/versions/{componentVersion}/triggers` - triggers of a
  component (name, title, description, type, input properties, output schema/dynamic-output flag). Uncomment/adapt
  the existing `getComponentTriggers` sketch. Implementation note: the path carries `externalUserId` and the call
  is restricted to that connected user (403 otherwise), like the other endpoints.
- `POST /{externalUserId}/components/{componentName}/versions/{componentVersion}/triggers/{triggerName}/subscriptions`
  - body `{input, name?, externalId?, webhookUrl?}`; returns the subscription. For static-webhook triggers the
  response includes `triggerWebhookUrl` (the ByteChef URL to paste into the app). Implementation note: the field
  is `staticWebhookUrl`.
- `GET /{externalUserId}/trigger-subscriptions` - list (filter by component/trigger/status optional).
  Implementation note: no filters are implemented.
- `GET /{externalUserId}/trigger-subscriptions/{id}`
- `PUT /{externalUserId}/trigger-subscriptions/{id}` - update `input`, `name`, `externalId`, `webhookUrl`;
  re-registers the trigger (implementation note: only when the subscription is `ENABLED`).
- `POST /{externalUserId}/trigger-subscriptions/{id}/enable` and `/disable`
- `DELETE /{externalUserId}/trigger-subscriptions/{id}` - unregister and remove (including the generated
  workflow).

A subscription belongs to one connected user and one environment; ids from another user or environment answer
404. Subscribe/update/enable failures from the target app (e.g. dynamic webhook registration rejected) return
an error with the error code and leave the subscription `FAILED` (builder decides whether a failed subscribe
persists the row or rolls back; the old sketch's `CREATING_FAILED` suggests persisting).
Implementation note: a failed subscribe deletes the row (`TriggerSubscriptionFacadeImpl.removeFailedTriggerSubscription`)
and the error carries no `triggerSubscriptionId`, unless that delete fails too. Only enable and the update of an
`ENABLED` subscription leave a row `FAILED`; a failed update of any other status changes nothing and carries no id.
For an `ENABLED` subscription, a failure to save and redeploy the new configuration re-registers the previous
trigger (`restoreTrigger`): the subscription stays `ENABLED` on the old configuration and the error still carries
the id. It ends `FAILED` when the old trigger cannot be unregistered, the restore fails, or the new trigger cannot
be registered. The create transaction commits the row as `FAILED` with no error code before the trigger is
registered, so a concurrent read sees it that way, and a crash in that window leaves a `FAILED` row and its hidden
project behind.

Implementation note: enable (public and admin) refuses a disabled connected user with 400 `USER_INPUT_ERROR`.
Create beyond `bytechef.embedded.trigger-subscription.max-per-connected-user` (per connected user and environment,
100 in the shipped yml, 0 = no limit) answers 400 `USER_INPUT_ERROR`. A caller whose `ConnectedUserAuthentication` is
not API-key authenticated (a connected user's JWT) may not set `webhookUrl` on create or change it on update (blank
counts as null); that answers 400 `USER_INPUT_ERROR`. The environment webhook URL can be cleared (a blank value
deletes it); the signing secret is kept.

Authorization goes on the facade (`@PreAuthorize` per project conventions), not the controller.

## Admin UI (client, EE)

- **Embedded Settings - "Trigger events"** per environment: Webhook URL, signing secret (reveal, copy,
  rotate), "Send test event" (uses `WebhookDeliveryService.sendTest`). Builder picks the exact settings page;
  it should sit with other embedded per-environment settings. Served over GraphQL like the rest of the admin UI.
- **Connected user sheet - "Triggers"** list: each subscription with component icon, trigger title, name,
  status, last event time, last delivery status/error; actions enable/disable/delete. Read-mostly; creating
  subscriptions from the UI is not required. Follow existing sheet list patterns
  (`ConnectedUserSheetPanel*List.tsx`).
- **External Invocations** pages show `API_TRIGGER` rows (see above).

## Documentation

- New page under `docs/content/docs/platform/embedded/build/` (e.g. `trigger-subscriptions.mdx`), or a
  "Triggers" section in `component-kit.mdx` next to "Calling an action" - builder's call; link both ways.
  Cover: configuring the environment webhook, subscribing, the envelope, verifying the signature, lifecycle
  events, static-webhook setup, and status/error codes.
- Update `monitor/external-invocations.mdx` for the new source.
- Docs describe only what the code does.

## Testing

- Facade/service unit tests for subscription lifecycle (subscribe, update, enable, disable, delete, connection
  invalid/deleted) - one test class per source class.
- IntTests (`*IntTest`, Testcontainers) for the repository and for an end-to-end path: subscribe a polling or
  webhook trigger -> fire -> delivery task POSTs a signed envelope to a stub endpoint -> invocation log row with
  `API_TRIGGER` and `jobId`.
- Controller tests as WebMvc IntTests.
- Client: Vitest tests for the new list, settings section, and the `API_TRIGGER` label.

## Left to the builder

- Exact module placement inside `server/ee/libs/embedded/` (expected: facade + service in
  `embedded-configuration` or `embedded-execution`, controller in `embedded-execution-public-rest`), keeping the
  repo's layering rules (facades never use repositories; services never call services; no upward deps).
- The internal delivery task registration mechanism.
- Secret rotation semantics (immediate vs grace period) and how batch triggers' list outputs map to `data`.
- Whether the INVALID-connection gap is fixed generally or only for subscriptions.
- Retry defaults and whether duration in the log covers delivery only.
- Naming of GraphQL operations and client components.
