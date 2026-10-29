# Configuration rollout follow-ups: blocked destructive rollback, serialized rollouts, turned-off state on enable, hub listing filter, paused workflows resume, version impact warning, provided connections and inputs

Issue: bytechefhq/bytechef#6786. Follow-up to #6756 (automation workflow project configurations).

## Intent

Three gaps left by #6756's final review, all in how a configuration change reaches connected users' reference
deployments:

1. Rolling a configuration back to an older version is destructive: references to workflows the older version lacks
   dangle for good and lose their inputs and connections. An administrator who rolls back by mistake and forward again
   breaks those customers' automations.
2. Two configuration changes in quick succession roll out concurrently and can leave deployments in the older state.
3. A customer who enables a workflow the administrator turned off gets a plain success and no hint that it stays paused.

## Goals

- A configuration cannot be moved to a version that lacks workflows connected users still use, so a rollback never
  dangles references.
- Rollouts for one (project, environment) never interleave, and each works from the configuration as committed when it
  runs.
- The public enable endpoints tell the caller when the workflow stays paused by the administrator.

## Non-goals

- Liquibase identifiers longer than 63 characters stay as they are (user decision). No Liquibase change at all.
- Recovering references that are already dangling (from before this change, or deleted projects): those stay terminal.
- Making enable fail when the administrator has turned the workflow off. The user's intent is still stored and resumes
  automatically; that behaviour from #6756 stays.
- Cross-node locking beyond what the database row lock gives.

## Anti-goals

- A rejected version change must not write anything (configuration, references or deployments).
- The enable change must not break existing SDK callers that expect a 2xx with no body to mean success.

## 1. Block destructive rollbacks

User decision (2026-10-29): instead of making a rollback recoverable, prevent the rollback that would destroy
connected users' workflows. No schema change, no new Liquibase changeset.

### Rule

`AutomationWorkflowProjectConfigurationAdminFacadeImpl.updateConfiguration` (the only place a configuration's
`projectVersion` changes) rejects a move to an **older** `projectVersion` when connected users in that environment still
reference workflows the older version lacks (user decision, 2026-10-29: moving to a newer version that deliberately
removes a workflow keeps today's behaviour, its references dangle with `"Not in the version released to this
environment"`, because that is how a workflow is retired; turning a workflow off in the configuration does not unlock
the rollback, because the rollback would still destroy the users' state):

1. Collect the workflow uuids of the requested version (`projectWorkflowService.getProjectWorkflows(projectId, version)`).
2. Collect the automation workflow uuids of the project's non-dangling references in the configuration's environment
   (`ConnectedUserWorkflowReferenceAdminFacade.getReferences(...)` filtered by environment and `dangling == false`, the
   same source `#6770`'s execution dates use).
3. If any referenced uuid is missing from the requested version, throw `ConfigurationException` with a new
   `AutomationWorkflowProjectConfigurationErrorType.VERSION_LACKS_REFERENCED_WORKFLOWS` (next free key) and a message
   naming the missing workflows by label: `Version %d lacks workflows connected users still use: %s. Turn them off in
   this configuration or remove them from those users first.` Nothing is written.

Moving to a newer version (even one that removes workflows), and moving to an older version that contains every
referenced workflow, behave as today.
Creating a configuration is unaffected (a new configuration has no references yet). Deleting a configuration stays
reversible as in #6756.

Turning a workflow off in the configuration does not remove the user's reference, so the error stays until the
references are gone or a version containing those workflows is chosen. The message tells the administrator so.

### UI

The configuration dialog's existing "older version" warning (from #6756) is replaced: when the chosen version is older
than the current one, the dialog says that versions lacking workflows connected users still use cannot be selected,
and a rejected save shows the server message through the global error toast. The rollout's existing
`NOT_IN_RELEASED_VERSION_DANGLING_REASON` path stays as a safety net for references created between the check and the
rollout.

### Tests

- Admin facade IntTest: moving to an older version lacking a referenced workflow throws
  `VERSION_LACKS_REFERENCED_WORKFLOWS` naming it and leaves the configuration unchanged; moving to an older version
  containing every referenced workflow succeeds; dangling references, references in other environments and references
  whose workflow is turned off in the configuration are handled as specified (the first two do not block, the last
  still blocks); moving to a newer version that removes a referenced workflow succeeds and dangles it as today.
- Client: the dialog's new warning copy.

## 2. Serialized rollouts

The listener today calls `rollOut(projectId, environmentId)` then `applyConfigurationState(projectId,
environmentId)`, neither with a transaction of its own; each deployment and each reference is processed in its own
`REQUIRES_NEW` transaction with per-item failures caught and logged. A new `@Transactional` method
`rollOutAndApply(projectId, environmentId)` (on a Spring proxy) wraps both calls and the listener calls it instead. It
first takes a row lock on the `automation_workflow_project_configuration` row for (project, environment) through a new
repository query `findByProjectIdAndEnvironmentIdForUpdate` written as a native `@Query("SELECT * ... FOR UPDATE")`,
following `ConnectedUserProjectRepository.findByIdForUpdate`, exposed on `AutomationWorkflowProjectConfigurationService`
as `fetchConfigurationForUpdate`. It then reads the configuration and rolls out from that state. A second rollout for
the same pair waits on the lock and then sees the newer committed configuration. When the configuration no longer
exists (deleted), the deleted-event path runs as today without a lock.

The per-deployment and per-reference `REQUIRES_NEW` transactions and their failure handling stay as they are, so one
deployment's failure still does not undo the others. The cost is a second database connection held by each running
rollout (builder's call: configuration changes are rare administrator actions, so this is bounded in practice by
`workerExecutor`'s limit and acceptable). Different pairs still run in parallel.

## 3. Turned-off state on enable

Both public enable endpoints (`POST /automation/workflows/{workflowUuid}/enable` and
`POST /{externalUserId}/automation/workflows/{workflowUuid}/enable`) answer `200` with a body instead of `204`:

```
EnableConnectedUserWorkflowResponse { enabled: boolean, attentionReason: string | null }
```

`enabled` is the reference's stored intent after the call (always true for enable); `attentionReason` is what the
existing `ConnectedUserReferenceAttentionResolver` reports for the reference with its existing precedence, so
`TURNED_OFF_BY_ADMINISTRATOR` comes back to the caller. Both public controllers call
`ConnectedUserProjectFacade.enableProjectWorkflow(externalUserId, workflowUuid, enable, environmentId)`, which today
returns `void`; it returns the attention reason (`@Nullable String`) instead, and its remote client stub changes with
it. The error responses enable gives today for a missing connection or input (`MissingConnectionException`,
`MissingInputException` -> 409) stay unchanged. The disable endpoints stay `204`. The generated public REST models are
regenerated from `openapi.yaml`; the frontend/backend SDKs under `sdks/` are not changed in this issue. 2xx-checking SDK callers keep working;
the OpenAPI spec lists the new response schema, and the sample app's playground shows the attention reason when present
(it already maps `TURNED_OFF_BY_ADMINISTRATOR` to a message).

## 4. Hidden projects are filtered by the listing endpoint

User decision (2026-10-29): the configuration now controls exposure, so the Automation Hub component stops filtering.
Both listing endpoints, `GET /automation/projects` (`getFrontendProjects`, frontend JWT) and
`GET /{externalUserId}/automation/projects` (`getProjects`, backend), leave out projects whose configuration for the
caller's environment has `automationHubVisible = false`. The filter lives in that listing path only (controller
or a listing-specific facade method); `AutomationWorkflowProjectFacade.getPublishedProjects(externalUserId,
environment)` is unchanged, because provisioning (`getOrCreateReference`) and the admin "New Workflow" read from #6770
use it.

So "Show in Automation Hub" off means: not listed by `GET /automation/projects` (neither in the hub nor in a customer's
own UI), still provisionable by workflow UUID through the public API and from the connected user sheet (user
confirmed this is the intended meaning). Turning the configuration or a workflow off remains the switch that stops
provisioning and running. A user's already provisioned workflows still appear in `GET /automation/workflows`.

Client: the `select` filter on `automationHubVisible` in
`client/src/ee/pages/embedded/automation-hub/queries/automationHub.queries.ts` is removed. Docs:
`activation-dialog.mdx` (and any page describing "Show in Automation Hub") says the flag controls listing through the
API, not provisioning. This reverses #6756's final-review ruling that kept the flag a client-side hint.

Tests: a public REST / facade test that a project with `automationHubVisible = false` is absent from
`GET /automation/projects` and still provisionable by `POST /automation/workflow-templates/{workflowUuid}/provision`; the hub query test no
longer filters.

Embedded sample app (`bytechef-embedded-sample-app`, `front-end/src/app/automation-workflow/page.tsx`): the
"Find catalog workflow UUID" card lists from `GET /automation/projects`, so hidden projects disappear from it; the page
says that a workflow of a project with "Show in Automation Hub" off is provisioned by pasting its UUID (copied from the
administrator's Automation Workflows page) into the "Catalog workflow UUID" field, which already calls
`POST /automation/workflow-templates/{workflowUuid}/provision`. The live end-to-end check covers it.

## 5. A workflow paused by a rollout resumes when the user completes it

Today `ConnectedUserReferenceRolloutManager.rollOutDeployment` does `reference.setEnabled(rowSpec.enabled())`. When a new
version adds a required input or a required connection the user lacks, `resolveReference` returns `enabled = false`, so
the rollout overwrites the user's own switch with off. Filling the input later (`updateReferenceInputs` →
`ConnectedUserReferenceDeploymentManager.updateInputs`) keeps the row disabled, so the user must re-enable by hand.

Change (user decision, 2026-10-29): `reference.enabled` is the user's intent and a rollout never changes it. The
deployment row is enabled when intent, resolution (`resolved.isComplete() && missingInputName == null &&
workflowErrorCount == 0`) and `isAdministrativelyEnabled` all hold, exactly as for the administrator switch.

Resume points, each re-resolving through `resolveReference` with the reference's intent:
- `ConnectedUserWorkflowReferenceFacade.updateReferenceInputs` (the user fills the missing input): when the reference's
  intent is on, the row is re-resolved and enabled if complete; the "enabled row requires every required input"
  validation in `updateInputs` stays for rows that are already enabled.
- A connection the user creates for a component (wherever the connected user connection is created — the builder finds
  the existing creation path or event): every reference of that user whose attention reason is
  `MISSING_CONNECTION:<that component>` is re-resolved. If no creation hook can be attached without a new event, the
  next rollout or enable re-resolves it and the plan records that ruling.

The attention reasons stay as today (`INPUT_REQUIRED:<name>`, `MISSING_CONNECTION:<component>`), so a paused workflow
shows why; the user's switch shows on.

## 6. Warn the administrator before a version change pauses users

New read on the configuration admin facade, `getVersionImpact(long configurationId, int projectVersion)` →
`AutomationWorkflowProjectConfigurationVersionImpactDTO(List<Workflow> workflows)` with
`Workflow(String workflowUuid, String label, List<String> newRequiredInputs, List<String> newRequiredConnections,
int affectedConnectedUserCount)`, comparing the configuration's current version with the requested one for workflows
present in both:

- `newRequiredInputs`: inputs required in the requested version that are absent or optional in the current one.
- `newRequiredConnections`: component names of required connection slots in the requested version that the current
  version's workflow does not have.
- `affectedConnectedUserCount`: non-dangling references in the configuration's environment to that workflow whose
  stored inputs lack a value for one of `newRequiredInputs`, plus, when `newRequiredConnections` is non-empty, every
  reference to it (connection entitlement is not checked per user; the copy says "may pause").

Only workflows with at least one new required input or connection are returned. Exposed through GraphQL
(`automationWorkflowProjectConfigurationVersionImpact(id: ID!, projectVersion: Int!)`). The configuration dialog, when
editing and the chosen version differs from the current one, shows one warning line per returned workflow, e.g.
`Version 4 adds required input Slack Channel to Notify Slack: 12 connected users will pause until they fill it.` and
`Version 4 adds a required Jira connection to Sync Issues: connected users without a Jira integration will pause.`
Saving is not blocked.


## 7. Connections and inputs provided by the configuration

User decision (2026-10-29): an administrator can predefine, per workflow of a configuration, connections and input
values. Every connected user's workflow uses them, and the user cannot change them through the UI or the API. Example:
the administrator attaches their own, non-shared OpenAI connection to "Summarize Email"; every user's "Summarize Email"
runs on it.

### Storage (new changeset)

Two nullable `TEXT` columns on `automation_workflow_project_configuration_workflow` (new changeset in the
`embedded/configuration` changelog directory, `columnExists` preconditions, rollback drops them; existing identifiers
untouched):

- `provided_connections` — JSON map `componentName -> connectionId`.
- `provided_inputs` — JSON map `inputName -> value`.

Mapped as `MapWrapper` fields on `AutomationWorkflowProjectConfigurationWorkflow`. A configuration workflow row is
created on demand when a workflow has none yet (enabled defaults to true, matching `isWorkflowEnabled`'s default).

### Admin

- Facade: `AutomationWorkflowProjectConfigurationAdminFacade.updateConfigurationWorkflow(long id, String workflowUuid,
  Map<String, Long> providedConnections, Map<String, Object> providedInputs)`. Validation: every component name is a
  connection slot of the workflow at the configuration's pinned version; every connection exists, belongs to that
  component and to the configuration's environment; every input name is an input of that workflow. Violations throw
  `ConfigurationException` with a new `AutomationWorkflowProjectConfigurationErrorType.INVALID_PROVIDED_VALUE`. Saving
  publishes `AutomationWorkflowProjectConfigurationUpdatedEvent`.
- The configuration DTO's `Workflow` gains `providedConnections` and `providedInputs`; GraphQL exposes both and a mutation
  `updateAutomationWorkflowProjectConfigurationWorkflow(id: ID!, workflowUuid: ID!, providedConnections: Map,
  providedInputs: Map): Boolean`.
- UI: each workflow row of a configuration card gets an "Edit" action (in its existing actions menu) opening a dialog
  that lists the workflow's connection slots (one connection picker per component, listing the administrator's
  connections of that component in the configuration's environment, plus "Let each user connect") and its inputs (a
  value field per input, empty means "Let each user fill it").

### Resolution

- `ConnectedUserWorkflowConnectionResolver.resolve` gains the provided map: a slot whose component has a provided
  connection takes it without the entitlement check; other slots resolve as today.
- `resolveReference` merges inputs as `userInputs` overridden by `providedInputs`; provided inputs never count as
  missing.
- Every path that resolves a reference (first provisioning, enable, rollout, `applyConfigurationState`, inputs update,
  #6770's admin provisioning) reads the configuration workflow's provided values for the reference's environment.
- Because `rollOut` only visits deployments that are behind the released version, a change of provided values (same
  version) must still reach every reference: `applyConfigurationState` re-resolves rows, not just the enabled flag,
  when the configuration's provided values changed (simplest correct rule: always re-resolve in
  `applyConfigurationState`).

### Locked for the user

- Public API: a requested connection id for a provided component (provisioning with `connections`) and an input value
  for a provided input (provisioning with `inputs`, `PUT .../inputs`) are rejected with a new 409
  `ProvidedValueLockedError` naming the slot or input. Requests that omit them succeed.
- Reads: `ConnectedUserWorkflowTemplateDTO.Input` gains `boolean provided`; the template components and the connected
  user workflow connection rows gain `boolean provided`. The OpenAPI schemas carry `provided`.
- The provided connection is never added to the user's connections, never shared with them, and its credentials are
  never returned to them.
- Automation Hub: provided connections show "Provided by your administrator" with no picker; provided inputs are not
  editable (shown read-only with the same label, or hidden from the inputs dialog).

### Tests

- Admin facade IntTests: validation errors; saving publishes the event; values round-trip.
- Resolution IntTests: a non-shared administrator connection is used for every user's workflow; user-owned connections
  are ignored for that slot; provided inputs override and never count as missing; changing provided values reaches
  existing references without a version change; removing a provided connection hands the slot back to the user
  (auto-wire or `MISSING_CONNECTION`).
- Public REST tests: requested connection or input for a provided slot → 409 `ProvidedValueLockedError`; reads carry
  `provided: true`; the provided connection never appears in the user's connection list.
- Client: configuration workflow dialog; hub read-only rendering.

## Tests

- Blocked rollback: see section 1.
- Concurrency IntTest: two rollouts for the same pair started together end with the deployments in the newer
  configuration's state (drive with two threads and a latch; assert final state, not timing).
- Public REST tests: enable while turned off returns 200 with `attentionReason: TURNED_OFF_BY_ADMINISTRATOR`; enable
  while on returns 200 with `attentionReason: null`; disable stays 204.

## Left to the builder

- Whether `rollOutAndApply` lives on the rollout manager or a thin new service (it must be a Spring proxy for
  `@Transactional`).
