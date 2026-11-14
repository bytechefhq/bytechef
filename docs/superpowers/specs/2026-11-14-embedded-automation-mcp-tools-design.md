# Embedded automations as MCP tools

Date: 2026-11-14. Status: draft for review (builder check and Fable review folded in).

## Intent

An embedded MCP server can already expose integration workflows as tools, per connected user. The admin offers
an integration configuration's workflows on a server; every connected user who has that integration gets their own
row with an on/off switch; the connected user sheet's MCP Servers tab shows those rows and can provision the server
for the user; the user's own app can switch a row on or off through the public API.

Embedded automations (admin templates a connected user activates) should work the same way: the admin offers an
automation configuration's workflows on an embedded MCP server, each connected user who activated the automation
gets a per-user switch, and the MCP Servers tab lists and provisions them.

A user activates an automation in any of three ways, and all of them produce the same reference and deployment:
the Automation Hub, the ActivationDialog opened directly from the host app through the SDK (`useActivationDialog`),
or the admin provisioning it from the connected user sheet. This spec treats them alike.

## Goals

- Admin attaches an automation configuration to an embedded MCP server and picks which of its workflows are tools,
  with the same per-workflow settings integration workflows have (tool name, description, `fromAi` parameter
  mapping, enabled).
- A connected user's activated automation shows up as a tool on that server, behind a per-user switch.
- The connected user sheet's MCP Servers tab lists automation tools with toggles, and the Provision MCP server dialog
  counts automations as covered (activated) or missing (not activated).
- Two switches, like integrations (user decision): the automation's own on/off controls its event triggers only,
  and a separate MCP switch controls the tool. Both the admin (connected user sheet) and the connected user (public
  API and SDK) can flip the MCP switch.
- Calling the tool runs the user's own activated deployment and returns the result the same way integration
  workflow tools do (including approval and suspended outcomes).

## Non-goals

- A connected user's copies of templates and automations they built themselves. Only activated references to admin
  templates become tools: the admin decides what the tool does.
- Activating an automation from MCP. A user who has not activated the automation does not see the tool.
- Per-user tool inputs for automations (the integration `PUT .../mcp-workflows/{uuid}`). An automation's inputs
  already live on the user's deployment and are edited with the ActivationDialog
  (`/automation/workflows/{workflowUuid}/inputs`); the tool uses them.
- Changing how integration workflow tools or component tools work, apart from sharing the server membership logic
  (below) and the attach rule for integration workflows (Admin section).
- Tool-name uniqueness on a server. Nothing guards it today for any tool kind (`FilterableMcpAsyncServer` picks a
  `tools/call` target with `findAny()` on the name). A server-wide check for all kinds is a separate follow-up.
- Distributed EE: `configuration-app` wires the throwing `RemoteMcpIntegrationInstanceRowsClient`; the new rows
  hooks have the same limitation as the integration ones today.
- The sample app's MCP chat page. It is a separate step; its layout comes from the user.

## Anti-goals

- A tool that runs for a user whose automation is switched off, or whose configuration the admin turned off.
- A tool that survives the user being moved to another configuration with stale links.
- One connected user reaching another user's deployment.

## Constraints

- Embedded automations are unreleased: no data migration or back-compat work.
- New Liquibase changelog in its own commit (`<ticket> Upgrade - ...`); never edit merged changelogs.
- EE code: Enterprise license header and `@version ee` on every class.
- Facades never use repositories; services never call services; `@PreAuthorize("isTenantAdmin()")` on admin
  facades, like `ConnectedUserMcpServerFacadeImpl`.
- EE microservice apps need `@ConditionalOnEEVersion` remote client stubs for new facades, services and resolvers,
  like `RemoteMcpIntegrationInstanceRowsClient`.
- GraphQL enums in SCREAMING_SNAKE_CASE; `.graphql` operations and the regenerated `graphql.ts` in separate commits.
- `openapi.yaml` changes in their own commit, followed by `generateOpenAPI` output in its own commit.
- Only a workflow whose enabled trigger is `workflow/v1/newWorkflowCall` can be a tool (same rule as
  `EmbeddedMcpToolFacade.getMcpToolCallableTrigger`).

## Approaches

- **A. Mirror the integration tables for automations (picked).** New admin link tables and a per-user row table,
  shaped like `mcp_integration_instance_configuration`, `mcp_integration_instance_configuration_workflow` and
  `mcp_integration_instance_workflow`. More code, but the same model that already shipped and is tested, so the
  admin page, the connected user tab and the tool listing all follow a known path.
- B. Generalize the integration tables with a source-type column. Less schema, but rewrites a feature that just
  landed and loses foreign keys. Dropped.
- C. An "expose as MCP tool" flag on the automation template picked up by every server. No per-server choice, which
  breaks parity with integrations. Dropped.

The user picked A. A fourth model was discussed and not taken: MCP-owned deployments per user, like the automation
(non-embedded) MCP server's `McpProject`. It would skip activation, but give every user a second deployment of the
same template outside the configuration rollout.

## Design

### How an activated automation is reached today

- `AutomationProjectConfiguration`: per environment, pins a published `projectVersion` of the template project,
  `enabled`, several per environment. Its workflows (`AutomationProjectConfigurationWorkflow`: `workflowUuid`,
  `enabled`, provided connections and inputs) are a `@MappedCollection`, so Spring Data JDBC deletes and re-inserts
  them on every save of the configuration: their ids are not stable.
- `ConnectedUserProjectConfiguration` assigns a connected user to one configuration per template project.
  Activation always assigns.
- An activated template is a `ConnectedUserProjectWorkflow` reference: `automationUuid` is the template workflow's
  uuid, `projectDeploymentId` is the user's deployment. `dangling` marks a reference whose workflow is gone from the
  pinned version (it is only ever set to true).
- All activation paths go through `ConnectedUserWorkflowReferenceFacadeImpl.provisionReference`, which saves the
  reference and then applies the workflow. The apply step can throw `MissingConnectionException` /
  `MissingInputException`, and those are `noRollbackFor`: the reference stays saved.
- Admin and user state is folded into `ProjectDeploymentWorkflow.enabled` (user on AND configuration on AND
  workflow on), but asynchronously: `AutomationProjectConfigurationUpdatedEventListener` runs on `workerExecutor`
  after commit, and a failed rollout for one deployment is logged and skipped. The flag can lag behind the admin,
  so the tool also reads the configuration directly (below).

### Data model (new, in `embedded-ai-mcp-service`)

- `mcp_automation_project_configuration`: `id`, `mcp_server_id` (FK `mcp_server`), `automation_project_configuration_id`
  (FK with `ON DELETE CASCADE`), audit columns, `version`. Unique on (`mcp_server_id`,
  `automation_project_configuration_id`).
- `mcp_automation_project_configuration_workflow`: `id`, `mcp_automation_project_configuration_id` (FK,
  `ON DELETE CASCADE`), `workflow_uuid`, `parameters` (JSON, same shape as the integration link: `toolName`,
  `toolDescription`, parameter values with `fromAi(...)`), `enabled`, audit columns, `version`. Unique on
  (configuration link, `workflow_uuid`). Keyed by `workflow_uuid`, not by an `automation_project_configuration_workflow`
  id, because those ids change on every configuration save and on version rollout.
- `mcp_connected_user_project_workflow`: `id`, `connected_user_project_workflow_id` (FK, `ON DELETE CASCADE`, the
  user's reference), `mcp_automation_project_configuration_workflow_id` (FK, `ON DELETE CASCADE`), `enabled`, audit
  columns, `version`. Unique on the pair.

The cascades are load-bearing, not just tidy:
- deleting a template project removes its configurations by a database cascade, so no Spring Data event fires;
- `AutomationProjectConfigurationAdminFacadeImpl.deleteUnassignedConfiguration` treats a
  `DataIntegrityViolationException` as "in use, try again", so a restricting FK would block configuration deletes.

Domain classes, repositories and services mirror the integration ones: `McpAutomationProjectConfiguration`,
`McpAutomationProjectConfigurationWorkflow`, `McpConnectedUserProjectWorkflow`, each with a service in
`embedded-ai-mcp-api` / `-service`. `embedded-ai-mcp-service` gains `automation-configuration-api` in main scope (it
is test-only today), for the template project name.

### Server membership, shared by every row kind

Today "a user is on a server" is derived only from integration rows (`McpIntegrationInstanceRowsImpl`:
`getConnectedUserIds`, `getMcpServerIds`, `hasEnabledRow`). With automation rows beside them, that would break both
ways: an automation-only user would be skipped when a component tool or integration link is added, and their new
integration rows would default to off.

So membership is defined once, in one component used by both the integration rows and the automation rows:

- a user is on a server when they have at least one row of any kind for it (component tool row, integration
  workflow row, automation row);
- the user's state on the server is "on" when any of those rows is on.

The integration rows class switches to it (it is the one change to integration code). Whether this is a new
`McpServerMembership` component or `McpIntegrationInstanceRows` growing into a general `McpServerRows` is left to
the builder.

### Per-user automation rows lifecycle (`McpAutomationRows`)

An interface in `embedded-ai-mcp-api`, implemented in `embedded-ai-mcp-service`, called from
`embedded-configuration-service` the way `McpIntegrationInstanceRows` already is.

Rows are created from stable facts only: the link exists, the user is assigned to the link's configuration, and the
user has a non-dangling reference whose `automationUuid` equals the link's `workflow_uuid`. Whether the
configuration, its workflow or the deployment is currently on is not a row concern; it is filtered when listing
tools and in the sheet, as `ConnectedUserMcpServerFacadeImpl` already filters integration rows. This keeps rows out
of the async rollout entirely.

The row is the admin's per-user, per-server override, not the user's switch: the user's switch is the automation's
own on/off (below). So activation alone must make the tool available, without an admin step or a user-side MCP
toggle. That is why automation rows are created for every covered user, not only for users already on the server.

- **Add missing rows** (for a user and a server): create a row for every covered link that has none. `enabled`
  follows the user's state on that server (shared membership above) when they are already on it, and is on
  otherwise. Triggers:
  - a link is created → for every connected user covered by it, on the server or not;
  - a reference is created, by any activation path → for every server that offers a link it covers. Hook point:
    inside `provisionReference`, right after the reference is saved, before `applyWorkflow`, so a
    missing-connection or missing-input throw after it cannot skip the hook;
  - a user moves to another configuration (Move users, Change configuration, `upgradeConnectedUsers`) → remap,
    below. Hook point: `AutomationProjectConfigurationAdminFacadeImpl.moveConnectedUser` after its save;
    `upgradeConnectedUsers` funnels through it.
- **Provision** (from the sheet): create or enable rows for every covered link on the server.
- **Remap** on a move: delete the user's rows bound to links of the old configuration and create rows against the
  same server's links of the new configuration with the same `workflow_uuid`, keeping `enabled`, then add missing
  rows. Mirrors `remapIntegrationInstance`.
- **Delete:** rows go with their reference and with their link by FK cascade, links go with their configuration by FK
  cascade. Only the MCP server delete needs a listener, next to `EmbeddedMcpServerBeforeDeleteEventListener`.

### Resolving a user's callable automation (`embedded-configuration-api`)

`embedded-ai-mcp-server` cannot reach the reference and deployment logic, which lives in
`embedded-configuration-service` (`ConnectedUserReferenceDeploymentManager`). Add one resolver interface in
`embedded-configuration-api` (plus a remote stub) that, for (connectedUserId, automationProjectConfigurationId,
workflowUuid), returns what a tool needs or nothing:

- the configuration is enabled, its workflow with that uuid is enabled, and the user is assigned to it;
- the user's reference for that uuid exists and is not dangling;
- the deployment workflow (deployment = the reference's `projectDeploymentId`, workflow id resolved from the uuid at
  the deployment's project version) exists. Its `enabled` flag is NOT checked: it carries the automation's own
  on/off, which controls only the event triggers (user decision, like integrations, whose tool ignores the instance
  workflow's own switch). The configuration and its workflow are checked directly above, so the async rollout lag
  of that flag does not matter;
- returns the deployment id, the workflow id, the deployment workflow's inputs, and the name of a component whose
  connection is missing, if any (from the same resolution the reference uses).

Ownership invariant: the reference is always resolved from the caller's connected user id (from the JWT), never from
a row id or a request argument. A two-users-one-link IntTest proves one user cannot run the other's deployment.

### Tool listing and calling (`EmbeddedMcpToolFacade`)

A third source next to component tools and integration workflows, wired into the `toolFilter` in
`EmbeddedMcpServerConfiguration`. `tools/call` already re-runs the whole `toolFilter` before it dispatches, so listing
and calling share one path through the resolver; there is no separate call-time re-check to write.

For an authenticated connected user (anonymous callers get no automation tools), for each automation configuration
link on the server and each enabled link workflow:

1. the user has an enabled `mcp_connected_user_project_workflow` row for it;
2. the resolver returns a callable automation;
3. the workflow in the user's deployment version has an enabled `workflow/v1/newWorkflowCall` trigger.

Tool name and description come from the link's `parameters`, falling back to the label and description of the
workflow in the user's deployment version, as for integration workflows. The input schema comes from the link
workflow's `parameters` (the `fromAi` mapping the admin set). The trigger check, the name and description fallback
and the job's workflow id follow the user's deployment version, not the configuration's pinned one, because a user's
deployment can lag a rollout. The link workflows carry only tool parameters, never connections or inputs, which
live in the configuration.

If the resolver reports a missing connection, the tool is still listed and a call answers with the same
connection-required response, without starting a job. Unlike integration workflow tools, the answer has no setup
link: it tells the user to connect the app for the automation in the host app.

On call:

- inputs = the deployment workflow's inputs, plus the trigger inputs resolved from the link parameters and the call
  arguments, keyed by the trigger name, plus `JobInputConstants.TRIGGER_NAME_INPUT`;
- `principalJobFacade.createJob(new JobParametersDTO(workflowId, inputs), projectDeploymentId,
  PlatformType.AUTOMATION)`, as `AutomationMcpToolFacade` does;
- automation service reads and `createJob` run under `AutomationAuthorizationContext.callSkippingChecks`, as the
  existing embedded calls do, because the caller is a connected user, not a workspace member;
- await and map the result with the existing helpers (`getWorkflowRunResult`, approval and suspended handling);
- record an `ExternalInvocationEvent` (`PlatformType.EMBEDDED`, `MCP_SERVER`, `WORKFLOW`) with the connected user,
  environment and MCP server ids and the job id.

### Admin: embedded MCP server page

- GraphQL in `embedded-ai-mcp-graphql`, mirroring `mcp-integration-instance-configuration.graphqls`:
  `mcpAutomationProjectConfigurationsByServerId(mcpServerId)`, the properties query
  `mcpAutomationProjectConfigurationWorkflowProperties` (feeds the tool properties popover), `createMcpAutomationProjectConfiguration`,
  `updateMcpAutomationProjectConfiguration` (selected workflow uuids + per-workflow configuration),
  `deleteMcpAutomationProjectConfiguration`, plus the per-link update and enable mutations the integration links
  have.
- Validation at attach time: the configuration's environment equals the server's environment; only workflows with a
  workflow-call trigger at the pinned version can be selected.
- Attaching has no side effect on the automation configuration (user decision): an automation link never enables
  a workflow in its configuration, which would start a rollout and change what every assigned user can activate.
  The dialog only lets the admin select workflows that are already enabled in the configuration; disabled ones are
  shown greyed out with "Turned off in the configuration".
- Integration links get the same rule (user decision). Today `McpIntegrationInstanceConfigurationFacadeImpl`
  (`saveIntegrationInstanceConfigurationWorkflow`) switches the configuration workflow on when a link is created,
  and again when a link is saved with connections or inputs. After this change only workflows already enabled in
  the integration configuration can be selected, and neither creating nor editing a link changes the configuration
  workflow's `enabled`. Turning a workflow off in the configuration keeps hiding its tool for every user, as today.
- For simplicity (user decision), the integration attach dialog also stops editing the workflow's connections and
  inputs: they are set only in the integration configuration. The MCP link keeps only what is MCP-specific (tool
  name, description, `fromAi` parameter mapping, enabled). The `workflowConfigurations` argument goes away from the
  MCP integration link mutations; the shared `WorkflowConfigurationDTO` stays for its other users (A2A).
- A link whose workflow is later turned off in the configuration, or is no longer callable after a version change,
  stays, offers no tool, and the admin list marks it unavailable.
- Client (`client/src/ee/pages/embedded/mcp-servers`): an "Automations" section beside the integration
  configurations list, with its own add dialog (pick configuration, pick workflows, edit tool name, description and
  parameter mapping). Reuse the integration workflow form (`useMcpIntegrationWorkflowsForm`,
  `McpIntegrationInstanceConfigurationWorkflowDialog`) where it fits.

### Connected user sheet: MCP Servers and Automations tabs

- `ConnectedUserMcpServer` gains `automations: [ConnectedUserMcpServerAutomation!]!` with `id` (the per-user row),
  `connectedUserProjectWorkflowId`, `automationName` (template project name), `workflowUuid`, `name` (tool name),
  `description`, `enabled` (the per-user MCP row), `available` (the resolver returns a callable automation),
  `lastExecutionDate`.
- `lastExecutionDate` queries jobs with `PlatformType.AUTOMATION` for the reference's deployment id, across the
  workflow ids that uuid had in the deployment's versions (the workflow id changes per project version).
- `enableConnectedUserMcpAutomation(id, enable)` toggles one per-user MCP row (this server only), like
  `enableConnectedUserMcpWorkflow` for integration workflows. The Automations tab keeps its own switch
  (`enableConnectedUserProjectWorkflow`), which controls the event triggers. The two are independent (user decision).
- A tool-only automation (its only trigger is the workflow call) shows no own switch in the Automations tab, with a
  "Tool only" hint pointing to the MCP Servers tab: its own switch would control nothing. It stays listed, because
  that tab is where it is activated and edited.
- `enableConnectedUserMcpServer` and `deleteConnectedUserMcpServer` also cover automation rows; "enabled for user"
  counts automation rows too.
- A server with only automation rows for the user still appears in the user's list.
- `ProvisionableConnectedUserMcpServer` gains `coveredAutomationNames` and `missingAutomationNames`, one entry per
  offered workflow (its tool name). Covered = the user has activated it; missing = the user is assigned to that
  configuration but has not activated it. Links of configurations the user is not assigned to are not listed: the
  user cannot activate them without being moved first. Provisioning is allowed when anything (component,
  integration or automation) is covered. The Provision dialog shows them next to covered and missing components.
- Client: `ConnectedUserSheetPanelMcpServerList` and `ConnectedUserMcpServerListItem` render an automation group with
  a row per tool and a toggle, the way `ConnectedUserMcpServerListItemWorkflowRow` does for integration workflows,
  greyed out when not available; `ProvisionMcpServerDialog` lists covered and missing automations.

### Connected user: the MCP switch (public API and SDK)

Mirrors the integration MCP switch (`McpIntegrationInstanceWorkflowApiController`,
`McpIntegrationInstanceWorkflowFacadeImpl.enableMcpIntegrationInstanceWorkflow`), which is per workflow across all
servers:

- Public REST in `embedded-configuration-public-rest/openapi.yaml`: `POST` and `DELETE
  /automation/workflows/{workflowUuid}/mcp/enable`, plus `/{externalUserId}/automation/workflows/{workflowUuid}/mcp/enable`.
  It sets every per-user MCP row of the caller's reference for that uuid, on every server that offers it. Ownership
  comes from the caller's external user id. 404 when the user has no such reference.
- The user's automation in the public automation workflow list gains `mcpEnabled` (null when no server offers it;
  otherwise true when any of its MCP rows is on) and `toolOnly`.
- SDK (`sdks/frontend/embedded/library`): where the automation's own switch is shown (Automation Hub, ActivationDialog),
  add an "Available to AI agents" switch when `mcpEnabled` is not null, calling the new endpoint. For a `toolOnly`
  automation show only that switch. The Automation Hub card switch lives in the main client
  (`client/src/ee/pages/embedded/automation-hub`), not in the SDK library.

So for a mixed-trigger automation the user can keep its event trigger running and hide it from AI agents, or the
other way round. A tool-only automation has one switch, the MCP one, like a tool-only integration workflow.

### Audit

Reuse `ConnectedUserMcpServerAuditEvents` for the server-level actions and add an event for the per-row automation
toggle, like `CONNECTED_USER_MCP_WORKFLOW_ENABLED_UPDATED`, and one for the public per-workflow switch. Admin link changes get events in line with
`McpIntegrationInstanceConfigurationAuditEvents`.

### Docs

`docs/content/docs/platform/embedded/configure/mcp-servers.md`: a section on exposing automations (including that the
workflow must be on in its configuration and the user must have activated and switched it on), and the connected
user tab's automation rows and provisioning.

## Testing

- IntTests for the rows lifecycle: link created, activation through each path (including a reference saved before a
  missing-connection throw) creating enabled rows on a server the user was not on yet, provisioning, remap on
  configuration move, reference delete, configuration delete, template project delete, server delete.
- Shared membership: an automation-only user gets new integration and component rows with the right `enabled`, and
  an integration-only user gets new automation rows likewise.
- Resolver IntTests: each condition off returns nothing; two users, one link, each resolves only their own
  deployment.
- `EmbeddedMcpToolFacade` tests: listing filters (each admin switch and the MCP row off hide the tool; the
  automation's own switch off does NOT), connection-required answer, job created on the user's
  deployment with `PlatformType.AUTOMATION`, schema from the link workflow's parameters, approval and suspended
  results.
- Facade tests for `ConnectedUserMcpServerFacadeImpl` covering automation rows, `available`, and provisioning
  coverage.
- Client tests for the new list group, toggle and provision dialog lines, and the tool-only hint in the Automations tab.
- Public REST IntTests for the MCP switch (ownership, every server's row, 404) and SDK tests for the new switch.
- A live test through the embedded MCP endpoint with a JWT (`tools/list`, `tools/call`), as in the existing embedded
  MCP test recipe.

## Left to the builder

- Exact Liquibase file names and index names (63-character limit).
- New membership component vs. growing `McpIntegrationInstanceRows` into a general one.
- Whether the link and row services share code with the integration ones or stay parallel.
- Whether `ExternalInvocationEvent` gets a project deployment or reference id field, or the job id is enough.
- Whether `getDeleteImpact` for a configuration also lists the MCP links it would remove.
- Client component split inside the existing folders.

## My calls (to confirm)

- Only activated references to admin templates become tools; copies and self-built automations do not.
- A user who has not activated the automation does not see the tool at all, unlike integration workflows, which
  list and answer "connection required": activation needs the host app (Automation Hub or ActivationDialog) or the
  admin, and the MCP server cannot link to either. A missing connection on an activated automation is different: the
  tool is listed and answers "connection required" without a link.
- Links are keyed by `workflow_uuid` because configuration workflow ids are not stable.
- Rows come from stable facts only; on/off state of the configuration, workflow and deployment is checked when
  listing and in the sheet, never written into rows. No rollout hook.
- Server membership is shared across all row kinds; this is the one change to integration code.
- Activation creates enabled rows even on servers the user was not on yet, so no admin step is needed; rows on a
  server the user is already on follow their state there.
- A missing connection on the user's deployment lists the tool and answers "connection required", like integrations.
- A tool-only automation stays in the Automations tab and in the Hub (it is activated and edited there) but shows no
  own switch, only the MCP one.
- No tool-name uniqueness check in this change; a server-wide check for all tool kinds is a follow-up.
- Provision dialog coverage is per offered workflow; unassigned configurations are not listed as missing.

## User decisions

- Approach A, mirror the integration tables per connected user.
- Provisioning from the connected user sheet's MCP Servers tab covers automations.
- Attaching an automation workflow to a server never enables it in the automation configuration.
- Keep the user's activated automation as what the tool runs (not MCP-owned deployments).
- Activation by any path counts: Automation Hub, ActivationDialog, admin provisioning.
- Two switches, like integrations: the automation's own on/off controls only its event triggers; a separate MCP
  switch controls the tool. (Replaces an earlier "one switch" decision.)
- The MCP switch can be flipped by the admin (connected user sheet, per server) and by the connected user (public
  API and SDK, per workflow across servers).
- Tools from a disabled integration configuration are hidden too: fixed separately on branch
  `integration-mcp-configuration-enabled`.
- Integration workflows that are off in their configuration cannot be attached, and attaching or editing a link
  never enables them; turning one off in the configuration disables its tool for all users.
- The integration MCP attach dialog no longer edits connections and inputs; the configuration is the only place
  they are set, matching the automation side.
