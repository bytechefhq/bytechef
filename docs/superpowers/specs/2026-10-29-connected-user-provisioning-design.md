# Provisioning MCP servers and automation workflows from the connected user sheet

Issue: bytechefhq/bytechef#6770. Follow-up to #6756 and #6762.

## Intent

A support agent setting up one specific customer opens the connected user sheet in the embedded admin UI. On the
Integrations tab they can already provision an integration instance for that user (`+ New Integration`,
`ProvisionIntegrationDialog`, `IntegrationInstanceAdminFacade.provisionIntegrationInstance`). The MCP Servers and
Automation Workflows tabs only list what the customer self-served through the public API; the agent cannot add
anything there. This design gives those two tabs the same "add it for the customer" capability, reusing the
provisioning paths that already exist for the public API.

## Goals

- `+ New Workflow` on the Automation Workflows tab provisions automation workflow references for the user, the same
  way a call to the public `POST /api/embedded/v1/automation/workflows/{workflowUuid}` would.
- `+ New MCP Server` on the MCP Servers tab makes an MCP server available to the user again, using the integration
  instances the user already has. It is also the way to undo an earlier "Delete" on that tab.
- Both dialogs follow `ProvisionIntegrationDialog`: title, one-line subtitle, the user's environment implied, a
  disabled primary button until a valid choice exists, the tab refreshed after saving.
- Admin mutations and reads live on admin facades guarded with `@PreAuthorize("isTenantAdmin()")` and talk to
  services and other facades only.

## Non-goals

- Provisioning an integration instance from inside the MCP dialog. When the user lacks an integration the server
  needs, the agent goes through the Integrations tab first; the MCP dialog only explains what is missing.
- Picking connections per workflow in the workflow dialog. Connections resolve from the user's integration
  instances exactly as on the public path; a missing connection becomes the reference's attention reason.
- New tables or Liquibase changes. Everything is expressed with existing rows (`connected_user_project_workflow`,
  `mcp_integration_instance_tool`, `integration_instance_workflow`).
- Bulk onboarding across many users.
- Creating user-owned copies (`ConnectedUserProjectWorkflowDTO.Kind.COPY`). The dialog provisions references
  (`Kind.REFERENCE`) only: the user runs the template at the version pinned by the environment's configuration,
  and follows the configuration when it moves. Copies remain something the customer makes through the hub.

## Anti-goals

- A workflow provisioned by the agent must not behave differently from one the customer provisioned: same
  reference row, same attention reasons, same `TURNED_OFF_BY_ADMINISTRATOR` derivation from the configuration.
- The MCP dialog must never create tool rows for an integration instance that does not belong to this user, or for
  a tool/component the server does not expose.

## Vocabulary

- *Connected user*: `ConnectedUser` (id, `externalId`, `environment`). Every admin call takes `connectedUserId`; the
  facade turns it into `(externalId, environment)` where the public path needs those.
- *Reference*: `ConnectedUserProjectWorkflow`, the row that says "this user has automation workflow template X";
  created by `ConnectedUserWorkflowReferenceFacade.getOrCreateReference(externalUserId, automationWorkflowUuid,
  environment, requestedConnectionIds, inputs)`.
- *Configuration*: `AutomationWorkflowProjectConfiguration` (project, environment, pinned published version,
  enabled, per-workflow enabled). A template is provisionable for a user only when the project has a
  configuration for the user's environment and that configuration lists the workflow as enabled.
- *MCP server coverage*: an embedded `McpServer` exposes components (`McpComponent`, matched to the user's
  integration instances by `Integration.componentName`) and workflows (`McpIntegrationInstanceConfiguration` ->
  integration instance configuration, matched to the user's instances by configuration id). The user "has" the
  server when per-user tool rows (`McpIntegrationInstanceTool`) or enabled MCP workflow rows exist on their
  instances; `ConnectedUserMcpServerFacade.getConnectedUserMcpServers` rebuilds the list from those rows.

## Automation Workflows tab

### Read: what can be provisioned

`ConnectedUserProjectAdminFacade.getProvisionableProjects(long connectedUserId)` returns exactly the projects and
workflows the user could provision through the public API: it calls
`AutomationWorkflowProjectFacade.getPublishedProjects(externalId, environment)`, which already applies the
environment's configuration (enabled, pinned version, per-workflow switches) and the project and template
permission expressions for this user. Builder's call: the agent cannot override a permission expression; a
reference provisioned past one would 404 for the customer on the public API, so the dialog lists only what the
save would accept.

```
ProvisionableAutomationWorkflowProjectDTO(
    long projectId, String name, int projectVersion,
    List<Workflow> workflows)
Workflow(String workflowUuid, String label, List<ConnectedUserWorkflowTemplateDTO.Input> inputs,
    boolean provisioned, boolean dangling)
```

`provisioned` is true when the user already has a reference for that uuid (from
`ConnectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUserId)`), dangling or not; a dangling one
is reported with `dangling = true` so the dialog can say "Needs attention" instead of "Provisioned" (builder's
call: a dangling reference is the customer's existing row, re-provisioning it would be a no-op, so it is never
checkable here). Workflow labels and inputs come from the pinned `projectVersion`, not the draft, so the agent sees
what the customer would get.

GraphQL (in `connected-user-project.graphqls`):

```
provisionableConnectedUserProjects(connectedUserId: ID!): [ProvisionableConnectedUserProject!]!
```

### Write

```
provisionConnectedUserProjectWorkflows(connectedUserId: ID!, workflows: [ProvisionConnectedUserProjectWorkflowInput!]!): Boolean
input ProvisionConnectedUserProjectWorkflowInput { workflowUuid: ID!, inputs: JSON }
```

`ConnectedUserProjectAdminFacade.provisionProjectWorkflows(long connectedUserId, List<WorkflowProvisionRequest>)`
loads the connected user and, for each requested workflow, calls
`connectedUserWorkflowReferenceFacade.getOrCreateReference(externalId, workflowUuid, environment, Map.of(),
inputs)`, passing `null` (not an empty map) when the dialog sent no inputs, which is what "leave inputs alone"
means on that path. Requested uuids that are not in `getProvisionableProjects` for this user are rejected up front
with `ConfigurationException` and a new `ConnectedUserProjectWorkflowErrorType.WORKFLOW_NOT_PROVISIONABLE` (the
existing error type in `embedded-configuration-service`, next key after the ones in use) before anything is
written, so a dialog that raced a configuration or permission change does not half-provision. Already provisioned
workflows are accepted and left as they are (the public path's "get or create" semantics), unless inputs are
supplied, in which case the inputs are updated, again like the public path.

The facade method is not `@Transactional`: each `getOrCreateReference` runs in its own transaction and commits
with the trigger registrations its deployment update makes (including provider-side dynamic webhooks), exactly
like N calls on the public path. One outer transaction would roll the earlier rows back on a later failure while
their triggers stayed registered. `getOrCreateReference` saves the reference and then throws
`MissingConnectionException` when a component has no connection for this user; its own proxy is `noRollbackFor`
that exception. Builder's call: the admin facade catches `MissingConnectionException` per workflow and continues
with the next one, so the reference stays and shows the missing-connection attention reason on the tab, which is
the behaviour the customer would see. Any other exception aborts the call at that workflow: the references
provisioned before it stay committed and provisioned, and the ones after it are not attempted. Required
inputs are enforced by the dialog only; the server path does not reject an empty required input
(`throwOnMissingInput` is false there), same as the public API.

The existing `getOrCreateReference` already copies the pinned version, creates the reference deployment and
applies the configuration state, so a reference created here is indistinguishable from one created by the
customer.

### Dialog

`ProvisionProjectWorkflowDialog` under
`client/src/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-project-workflow-dialog/`,
opened from a `+ New Workflow` button (`size="sm" variant="secondary"`, `PlusIcon`) in the Automation Workflows
tab header, which `ConnectedUserSheetPanelWorkflowList` renders in the same `flex items-center justify-between
pb-2` header row as the Integrations tab, with the heading "Automation Workflows".

- Title "New Workflow", subtitle "Provision automation workflows from a released configuration for this connected
  user."
- A project select (name, with "V{projectVersion}" after it). When no project is provisionable, the dialog shows
  "No project has a configuration released to this user's environment." and only a Close button.
- Under the select, the configuration's workflows as a checkbox list. Already provisioned workflows are checked
  and disabled with a "Provisioned" badge, or a "Needs attention" badge when the reference is dangling. Checking a workflow reveals its declared inputs as fields below it
  (label, required marker, text input; same block `ProvisionIntegrationDialog` uses for its workflow inputs).
- Save is enabled when at least one not-yet-provisioned workflow is checked and every required input of the
  checked workflows has a value. Save calls the mutation with the checked, not-yet-provisioned workflows and
  their inputs, invalidates `connectedUserProjects` and `connectedUsers`, and closes.

## MCP Servers tab

### Read: what can be provisioned

`ConnectedUserMcpServerFacade.getProvisionableMcpServers(long connectedUserId)` (the existing admin-only facade
in `embedded-ai-mcp-api`, every method already `@PreAuthorize("isTenantAdmin()")`) returns every enabled embedded
MCP server (`PlatformType.EMBEDDED`, `McpServer.enabled`, `McpServer.environment` equal to the user's environment;
builder's call, the tab is per environment) with its coverage for this user:

```
ProvisionableConnectedUserMcpServerDTO(
    long id, String name, boolean provisioned,
    List<String> coveredComponentNames, List<String> missingComponentNames)
```

- `coveredComponentNames`: component names of the server's `McpComponent`s and of the integrations behind its
  `McpIntegrationInstanceConfiguration`s for which the user has at least one integration instance (component
  match for tools; configuration id match for workflows). De-duplicated, sorted. Only an `McpComponent` with at
  least one enabled `McpTool`, and only a configuration with at least one enabled
  `IntegrationInstanceConfigurationWorkflow`, counts; one with nothing enabled is left out of both lists, because
  the write below would enable nothing through it.
- `missingComponentNames`: the rest.
- `provisioned`: the server is already in `getConnectedUserMcpServers(connectedUserId)`.

GraphQL (in `connected-user-mcp-server.graphqls`):

```
provisionableConnectedUserMcpServers(connectedUserId: ID!): [ProvisionableConnectedUserMcpServer!]!
```

### Write

```
provisionConnectedUserMcpServer(connectedUserId: ID!, mcpServerId: ID!): Boolean
```

`ConnectedUserMcpServerFacade.provisionMcpServer(long connectedUserId, long mcpServerId)`:

1. Rejects a server that is not in the provisionable list for this user (disabled, or of another environment;
   `ConfigurationException` with `ConnectedUserMcpServerErrorType.MCP_SERVER_NOT_PROVISIONABLE`, "MCP server %s
   cannot be provisioned for connected user %s"), or has no covered component (`ConfigurationException` with
   `ConnectedUserMcpServerErrorType.NO_COVERED_INTEGRATION`). This is the first `AbstractErrorType` in the
   embedded-ai modules, so it gets its own class and key range next to the facade. Together the two rejections
   mean the mutation cannot create an empty server entry.
2. For every `McpComponent` of the server and every `McpTool` of it with `McpTool.enabled` (the same flag the read
   path and the self-service `enableMcpIntegrationInstanceTool` honour; "exposed" means exactly that flag): for
   each integration instance of the user whose integration's component name matches, create the
   `McpIntegrationInstanceTool` row with `enabled = true` if it does not exist, otherwise set it enabled. This is
   the ownership rule `McpIntegrationInstanceToolFacadeImpl.belongsToIntegrationInstance` applies to the
   self-service path; it is private there, so the builder re-implements it in the facade (one `Objects.equals` on
   the component name).
3. For every `McpIntegrationInstanceConfiguration` of the server whose configuration the user has an instance of,
   and every linked `McpIntegrationInstanceConfigurationWorkflow` whose
   `IntegrationInstanceConfigurationWorkflow.enabled` is true (the filter `getWorkflowsByMcpServerId` applies):
   enable the user's instance workflow via `integrationInstanceFacade.enableIntegrationInstanceWorkflow`, the call
   `enableConnectedUserMcpServer` on this same facade already makes from an admin principal, so the ownership check
   inside it is known to pass for an admin.

Partially covered servers are provisioned for what is covered; the dialog has already told the agent what is
missing. Re-provisioning an already provisioned server is allowed and just re-enables everything, which is a
cheap way to recover a server the agent disabled tool by tool.

### Dialog

`ProvisionMcpServerDialog` under `.../connected-user-sheet/provision-mcp-server-dialog/`, opened from
`+ New MCP Server` in the MCP Servers tab header (same header row and heading pattern, heading "MCP Servers").

- Title "New MCP Server", subtitle "Expose an MCP server through the integrations this connected user already
  has."
- A radio list of servers, one row each: name, then a line in muted text. Fully covered: "Uses {covered}".
  Partially covered: "Uses {covered}. Missing {missing}: add those integrations first to expose them." Not covered
  at all: the row is disabled, line "Add {missing} on the Integrations tab first." Already provisioned servers are
  listed last with a "Provisioned" badge and stay selectable (re-provisioning re-enables).
- When no embedded MCP server exists, the dialog says "No MCP servers are defined." with only a Close button.
- Save is enabled when a selectable server is chosen. It calls the mutation, invalidates
  `connectedUserMcpServers` and `connectedUsers`, and closes.

## Security

- Both admin facades: `@PreAuthorize("isTenantAdmin()")` on every method, `@ConditionalOnEEVersion`.
- The GraphQL controllers only parse ids and delegate; no logic there.
- The connected user is loaded by id first; a missing user fails with the existing not-found behaviour before any
  write.

## Where things go

Server, under `server/ee/libs/embedded/`, ByteChef Enterprise license header and `@version ee`:

- `embedded-configuration-api`: `ConnectedUserProjectAdminFacade` (new methods), DTOs
  `ProvisionableAutomationWorkflowProjectDTO`, `ProvisionConnectedUserProjectWorkflowDTO`.
- `embedded-configuration-service`: `ConnectedUserProjectAdminFacadeImpl` (exists, today injects only
  `ConnectedUserProjectFacade`; gains the two methods and injects `AutomationWorkflowProjectFacade`,
  `ConnectedUserService`, `ConnectedUserWorkflowReferenceFacade`), `WORKFLOW_NOT_PROVISIONABLE` on the existing
  `exception/ConnectedUserProjectWorkflowErrorType`.
- `embedded-configuration-graphql`: `connected-user-project.graphqls`, `ConnectedUserProjectGraphQlController`.
- `embedded-ai-mcp-api`: `ConnectedUserMcpServerFacade` (new methods), `ProvisionableConnectedUserMcpServerDTO`.
- `embedded-ai-mcp-service`: `ConnectedUserMcpServerFacadeImpl` (already has every service the two methods need),
  new `exception/ConnectedUserMcpServerErrorType`.
- `embedded-ai-mcp-graphql`: `connected-user-mcp-server.graphqls`, `ConnectedUserMcpServerGraphQlController`
  (already injects `ConnectedUserMcpServerFacade`).
- No new SPI interfaces, so no remote client stubs.

Client, under `client/src/ee/pages/embedded/connected-users/components/connected-user-sheet/`:

- `ConnectedUserSheetPanelWorkflowList.tsx`, `ConnectedUserSheetPanelMcpServerList.tsx`: header row with heading
  and button, dialog mount.
- `provision-project-workflow-dialog/ProvisionProjectWorkflowDialog.tsx`,
  `provision-mcp-server-dialog/ProvisionMcpServerDialog.tsx`.
- GraphQL operations in `client/src/graphql/embedded/configuration/` (two queries, two mutations), regenerated
  `src/shared/middleware/graphql.ts` in its own commit.

Docs: `docs/src/content/docs/embedded/...` connected users page gets a short paragraph per tab, mirroring the
existing "Provision an integration" paragraph.

## Tests

- `ConnectedUserProjectAdminFacadeIntTest`: provisionable list honours environment, configuration enabled,
  per-workflow enabled flags and permission expressions, marks provisioned and dangling uuids; provisioning creates
  references equal to the public path's (same deployment, same enabled state); a non-provisionable uuid rejects
  the whole call before any write; a workflow with a missing connection is still created and the batch continues;
  inputs reach the deployment workflow and an omitted inputs map leaves existing inputs alone.
- `ConnectedUserMcpServerFacadeIntTest` (exists): coverage lists for component-only, configuration-only, mixed and
  uncovered servers; provisioning creates tool rows only for matching instances and re-enables existing ones;
  uncovered server rejected; MCP workflows enabled; the server then appears in `getConnectedUserMcpServers`.
- GraphQL controller IntTests for the two queries and two mutations (mocked facades, like the existing ones).
- Client: dialog tests (empty states, disabled Save, provisioned rows disabled, mutation variables, invalidation)
  and panel tests for the header button, following `ProvisionIntegrationDialog.test.tsx`.

## Left to the builder

- Exact copy of error toasts (the global fetch interceptor shows the server message).
- Whether the project select in the workflow dialog uses the native `<select>` `ProvisionIntegrationDialog` uses or
  the shared `Select`; match the integration dialog.
- Ordering of servers and projects (alphabetical by name unless the existing lists do otherwise).
