# Apps — Sub-project D: the coding-agent loop (design)

Sub-project D of [Apps](2026-10-06-apps-governed-ai-built-apps-design.md). Builds on A (server), A4
([workflow bindings and SDK](2026-10-06-apps-workflows-and-sdk-design.md)), B (`@bytechef/app`) and C
([the app template](2026-10-07-apps-template-design.md)). Approved 2026-10-08 and revised the same day after review.
This design **replaces** the parent spec's "Design — D. Deploy" section (see "Changes to the parent spec").

## Goal

A coding agent (Claude Code or any agent with a shell and MCP) working in an app made from the template can, without
opening the ByteChef UI except to create sign-in connections:

1. create the app and get its key into `.env.local` without the key passing through the model;
2. bind connections, allowing **actions** for the app's code and **tools** for the app's own AI;
3. optionally build workflows, deploy them, and bind them to the app;
4. see what the app can call (`npm run bytechef:status`, `bytechef automation app status`), and call it through
   `@bytechef/app`.

An app that only calls actions and tools needs no workflows. Workflows are an optional extension of the same loop.

## Decisions (with the alternatives that were rejected)

| # | Decision | Rejected |
|---|----------|----------|
| D1 | Workflows are authored through the management MCP's `buildWorkflow` (ByteChef's own model generates them) | the agent writing definition JSON through a deterministic write tool; offering both |
| D2 | The **management MCP** builds and deploys workflows; the **CLI** manages apps | app tools on the MCP (the parent spec's `createApp`/`bindAppConnection`/`rotateAppKey`); everything through the CLI |
| D3 | A connection is picked automatically when exactly one candidate exists, otherwise explicitly; a listing on each side helps choose | always explicit; always automatic |
| D4 | The agent loop is **DEVELOPMENT only**; staging and production stay a person's decision (existing promotion) | any environment per command; development by default with a flag |
| D5 | Imperative, idempotent commands (binds are upserts) | a declarative `bytechef.app.json` manifest reconciled by a sync command (possible later, built on these commands) |
| D6 | Tools are for AI, actions for regular calls from code; no "actions as AI tools" | an `actions: true` option on `toAiSdkTools` |
| D7 | No hosting deploy in D; the template already deploys with `vercel deploy` / Docker | the parent spec's `app deploy --target vercel|self` and `key rotate --vercel` |
| D8 | No tool to test-run a workflow; the agent tests through the app after binding, or in the ByteChef editor | an MCP run/test tool |

Why the key goes through the CLI (D2): an MCP tool returning the app key puts a credential into the model's context,
transcript and logs; the MCP server runs on the ByteChef host and cannot write the developer's files. A CLI command
writes the key straight into `.env.local` and the agent only sees "written to .env.local". The cost: two
credentials (an admin API key for the MCP, an automation API key for the CLI), and agents without a shell manage apps
in the UI.

## Plans

D is built as two plans. The seam between them is the REST contract in
`automation-app-public-rest/openapi-management.yaml`: D2 builds against the contract as D1 merged it.

- **Plan D1 (server):** the three new MCP tools and the `buildWorkflow` app-callable mode, the new facade methods,
  the four REST routes, and the end-to-end IntTest.
- **Plan D2 (CLI, template, plugin):** the `bytechef automation app …` and `bytechef automation connection list`
  commands with their client, the template's `AGENTS.md`/`README.md` and the command check, then the plugin skill and
  the regenerated MCP instructions. The plugin comes last: its `cli` fences need the `@Command` names and its `mcp`
  fences need D1's tools.

## Architecture

```
coding agent ── MCP (admin API key, DEVELOPMENT) ──▶ management MCP: createProject, buildWorkflow{appCallable},
     │                                                publishProject, deployProject, listConnections,
     │                                                listAppCallableWorkflows                 (DEVELOPMENT)
     │
     └─ shell ── bytechef CLI (automation API key, DEVELOPMENT) ──▶ /api/automation/v1: apps, bindings, key,
                    │                                                 bindable-*, component-operations, connections
                    └─ writes .env.local (BYTECHEF_URL, BYTECHEF_APP_KEY) + .bytechef/app.json
app (template) ── app key ──▶ /api/app/v1 (unchanged): actions, tools, workflows, proxy
```

### Terms

- **Usable connection:** its `credentialStatus` is `VALID` (`Connection.CredentialStatus`) and the caller holds
  `CONNECTION_USE` on it (`PermissionService.hasResourceScope(id, "Connection", "CONNECTION_USE")`).
- **AI-provider connection:** one of the virtual connections `WorkspaceConnectionFacade.getConnections` appends from
  `ConnectionFacade.getAiProviderConnections` (`AiProviderConnectionId.isAiProviderConnectionId(id)`). Listings show
  them with `aiProvider: true`; they are never a candidate for a deployment connection slot.
- **Deployment inputs** are the workflow's own `inputs` (`Workflow.getInputs()`): set once per deployment, stored on
  the deployment-workflow row, required ones checked by
  `ProjectDeploymentFacadeImpl.validateProjectDeploymentWorkflowInputs`.
  The **call input** is the JSON the app sends on each call, described by the Call-workflow trigger's `inputSchema`.
  `deployProject` sets only deployment inputs.
- **Workflow uuid:** a workflow's identity across project versions (`ProjectWorkflow` uuid). Publishing duplicates
  every workflow: the published version keeps the old `workflowId` and the new DRAFT version gets a new one
  (`ProjectFacadeImpl.publishProject`, `ProjectWorkflowServiceImpl.publishWorkflow`). Every new tool input and result
  keys workflows by uuid; a `workflowId` appears only as the DRAFT id to pass to `buildWorkflow`.
- **Unpublished changes:** the Data Sync rule (`DataSyncFacadeImpl.toDataSyncDTO`) applied to every workflow of the
  project. Each DRAFT workflow must have a workflow with the same uuid in the latest published version, with an equal
  definition; a workflow present in only one of the two versions is a change too. A project never published has
  unpublished changes. (`Project.publish` always appends a new DRAFT version, so the last version's status says
  nothing.)
- **App-callable workflow:** triggered by `workflow/v1/newWorkflowCall` with an `inputSchema` that is readable JSON
  Schema — the trigger and schema half of `AppCallableWorkflowResolver`'s definition. A
  `workflow/v1/responseToWorkflowCall` task is optional: with it the app receives its output, described by its
  `outputSchema`; without it the app receives the job outputs (A4's rule).

## Management MCP (workflow half)

### Common rules

- `deployProject`, `listConnections` and `listAppCallableWorkflows` are EE only: `@Tool` methods of a new class
  `AppLoopTools` in `server/ee/libs/automation/automation-ai/automation-ai-tool` (package
  `com.bytechef.ee.automation.ai.tool`), registered by a new `AppLoopMcpContributorConfiguration` whose
  `McpServerToolCallbackContributor` bean `appLoopMcpContributor` wraps each in `WorkspaceScopedFlatToolCallback`.
- They run as the MCP key's user (their authorities) and go through facades only. The workspace is the wrapper's
  `workspaceId` (`workspace_required` when the user has several). An `environment` argument other than `DEVELOPMENT`
  is refused with "The app loop runs in DEVELOPMENT only".
- `createProject` is not workspace-wrapped and falls back to the default workspace, so the loop always passes
  `workspaceId` to it.
- MCP setup for the loop: an admin API key created in DEVELOPMENT, with the client sending `X-ENVIRONMENT:
  DEVELOPMENT` (the key's environment must equal the header).

### `buildWorkflow` — app-callable mode

`buildWorkflow` (`ProjectWorkflowAgentToolCallback`) is registered on the management MCP by the CE bean
`copilotAgentToolCallbackContributor` (`ToolCallbackContributorConfiguration`), wrapped in
`WorkspaceScopedSubAgentToolCallback`, which replaces the input schema with `{request, workspaceId, environment}` and
forwards only `request`. The app-callable mode therefore lives on the wrapper:

- A new CE SPI, `CallableWorkflowChecker` (`automation-configuration-api`, package
  `com.bytechef.automation.configuration.workflow`), has one method, `CallableWorkflowCheck check(String workflowId)`,
  returning `record CallableWorkflowCheck(List<String> problems, @Nullable String inputSchema)`; no problems means
  app-callable. The EE implementation is `AppCallableWorkflowResolver`, built on its existing callable-trigger and
  readable-schema checks, so binding, listing, execution and this check share one definition.
- `copilotAgentToolCallbackContributor` takes an `ObjectProvider<CallableWorkflowChecker>` and passes the checker to
  the wrapper of `buildWorkflow` only (a new nullable constructor argument). With a checker, the wrapper's schema
  gains an optional boolean `appCallable`; without one (CE) the schema is unchanged. The wrapper consumes
  `appCallable` itself and still forwards `{"request": …}` only; its javadoc's "exactly `{request}`" invariant is
  amended to say the wrapper may own inputs it never forwards.
- When `appCallable` is true, the wrapper:
  1. appends a fixed instruction to the request: the trigger is `workflow/v1/newWorkflowCall` with an `inputSchema`
     (JSON Schema) describing every value the caller supplies; the workflow ends with a
     `workflow/v1/responseToWorkflowCall` task carrying an `outputSchema`; only DRAFT workflows are edited;
  2. puts a capture holder and an app-callable flag in the forwarded `ToolContext`.
     `WorkflowPersistCaptureUtils.withCaptureHolder` keeps a holder that is already present instead of replacing it,
     and gains `getCapturedWorkflowIds(Map)`, so the wrapper reads what `ProjectWorkflowTools.capturePersistedWorkflow`
     recorded inside the delegate;
  3. under the flag, `ProjectWorkflowTools.updateWorkflow`, `ProjectWorkflowTools.deleteWorkflow` and
     `ScriptTools.updateScriptComponentCode` refuse a workflow that is not in its project's DRAFT (last) version; the
     refusal names the DRAFT `workflowId` with the same uuid (`ProjectWorkflowService.getLastWorkflowId`);
  4. after the delegate returns, checks every distinct captured `workflowId`:
     - nothing captured (the specialist asked a question, rendered with `SubAgentQuestionRenderer.PLAIN_TEXT` on this
       surface, or only answered) — the reply passes through unchanged;
     - every captured workflow callable — the reply, plus each `workflowId` marked app-callable;
     - any failure — an error naming each failing `workflowId` and what is missing (no Call-workflow trigger,
       unreadable `inputSchema`). The workflow stays saved; the error tells the agent to call `buildWorkflow` again
       naming that `workflowId`. The tool never reports success for an unbindable workflow.
- AI model: unchanged. `buildWorkflow` is registered only when `bytechef.ai.copilot.enabled` is `true`: without it
  `CopilotConfiguration` (`@ConditionalOnProperty`) does not create `workflowEditorBuildSubAgentChatClientFactory`
  and `IntelligentToolCatalog` skips the definition. No new runtime check is added; the generated MCP instructions'
  app-loop section says that when `buildWorkflow` is missing, an admin must set `bytechef.ai.copilot.enabled: true`
  with a chat model provider (optionally chosen with `bytechef.ai.copilot.provider`).

Other surfaces (Copilot panels, AI Hub) keep their schema: only the management MCP wrapper carries `appCallable`.

### `deployProject(projectId, projectDeploymentId?, enabledWorkflows?, connections?, inputs?)`

Creates or updates the project's **single DEVELOPMENT deployment** at the latest published version, then enables it.
`enabledWorkflows` is a list of workflow uuids; `connections` is a list of `{workflowUuid, workflowNodeName, key,
connectionId}`; `inputs` maps a workflow uuid to that workflow's deployment inputs.

The tool:

1. Refuses a project whose workspace differs from the resolved `workspaceId`.
2. Picks the deployment from `ProjectDeploymentFacade.getWorkspaceProjectDeployments(workspaceId,
   Environment.DEVELOPMENT.ordinal(), projectId, null)`: several and no `projectDeploymentId` — refused, listing them
   (id, name); exactly one — that one, updated in place, so existing app workflow bindings (deployment id + workflow
   uuid) stay valid; none — create.
3. Defaults `enabledWorkflows`, when omitted, to the app-callable workflows of the version being deployed
   (`CallableWorkflowChecker`).
4. Calls the new facade method below, then `ProjectDeploymentFacade.enableProjectDeployment(id, true)` as a separate
   call.

New facade method `ProjectDeploymentFacade.deployProject(long projectId, @Nullable Long projectDeploymentId,
List<String> enabledWorkflowUuids, List<DeploymentConnectionChoiceDTO> connections, Map<String, Map<String, ?>>
inputs)` returning `DeployProjectResultDTO`, guarded:

```
hasResourceScopeInEnvironment(#projectId, 'Project', 'WORKFLOW_EDIT',
    T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT)
and (#projectDeploymentId == null
    or hasPermission(#projectDeploymentId, 'ProjectDeployment', 'DEPLOYMENT_EDIT'))
```

Its body:

- **Deployment:** a given `projectDeploymentId` must belong to `projectId` and be in DEVELOPMENT; without one, the
  project must have no DEVELOPMENT deployment. Anything else is refused.
- **Version:** the latest published version. A project with unpublished changes is refused with "call
  `publishProject` first". Publishing stays explicit.
- **Rows:** every workflow of that version gets a deployment-workflow row, never a subset, because
  `checkProjectDeploymentWorkflows` deletes every existing row missing from the list it is given. Listed uuids are
  enabled; an unlisted workflow keeps its current row's enabled state, and a new row starts disabled. So the default
  enabled set is the app-callable workflows plus those already enabled. These are the rows today's
  `createProjectDeployment`/`promoteWorkflow` never create, which is why nothing they deploy is bindable.
- **Connection slots:** per workflow, from `ComponentConnectionFacade.getWorkflowComponentConnections(workflowId)`
  (triggers, nested tasks, cluster elements). That method is the body of the EE embedded `WorkflowConnectionSlots`
  moved into `platform-configuration-api` / `platform-configuration-service`; the embedded callers
  (`ConnectedUserWorkflowConnectionResolver`, `ConnectedUserReferenceAttentionResolver`) switch to it and the EE class
  is removed.
- **Candidates** for a slot: the usable connections returned by `WorkspaceConnectionFacade.getConnections(
  projectWorkspaceId, componentName, connectionVersion, DEVELOPMENT.ordinal(), null)` (guarded and
  visibility-filtered), where `connectionVersion` comes from
  `ConnectionDefinitionService.getConnectionDefinition(componentName, componentVersion)`, AI-provider connections
  excluded.
- **Choice** per slot: the explicit `connections` entry, which must be a candidate or the call is refused; else the
  existing row's connection for the same slot while it is still a candidate; else the only candidate. An optional
  slot without a choice stays empty.
- **Deployment inputs** per workflow: the explicit `inputs[uuid]`, else the existing row's inputs; required ones
  must be present.
- **All or nothing:** an enabled workflow with an unresolved required slot (no candidate, or several) or a missing
  required input means nothing is written; the result lists every unresolved slot as `{workflowUuid, workflowName,
  workflowNodeName, key, componentName, required, candidates: [{id, name}]}` and every missing input as
  `{workflowUuid, name}`. A disabled workflow's slots are resolved where possible and never block.
- **Write**, in one transaction: create the deployment (saved disabled), or for an update disarm its enabled
  workflows and mark it disabled, then write version, rows, connections and inputs through
  `checkProjectDeploymentWorkflows`.

Arming: the tool's separate `enableProjectDeployment` call arms the triggers. If it fails, the written configuration
stays, the deployment stays disabled, and the tool returns `arming failed` with the deployment id and the cause;
calling `deployProject` again retries.

Result: `{projectDeploymentId, projectVersion, enabled, workflows: [{workflowUuid, name, enabled, appCallable,
inputSchema?, draftWorkflowId, connections: [{workflowNodeName, key, connectionId, connectionName}]}],
removedWorkflows: [{workflowUuid, name}]}`. `draftWorkflowId` is the id to pass to `buildWorkflow` for the next
change. `removedWorkflows` lists workflows of the previously deployed version that the new version no longer has;
app bindings to them become unavailable, and the result says to check `bytechef automation app status`. For each
enabled app-callable workflow the result includes the exact CLI command `bytechef automation app bind workflow
--workflow-key <KEY> --deployment-id <id> --workflow-uuid <uuid>`, with a key suggested from the workflow name.

### `listConnections(componentName?)`

Calls the new `AppFacade.getConnections(workspaceId, Environment.DEVELOPMENT, componentName)` (see "Server REST").
Each connection: `{id, name, componentName, connectionVersion, authorizationType, credentialStatus, aiProvider,
usable}`. Never parameters or credentials.

### `listAppCallableWorkflows(projectId?)`

Calls the new `AppWorkflowFacade.getCallableWorkflows(long workspaceId, @Nullable Long projectId)`, guarded
`hasWorkspaceScopeInEnvironment(#workspaceId, 'DEPLOYMENT_VIEW',
T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT)`. It is built on
`AppCallableWorkflowResolver.getBindableWorkflows`, refactored to take `(long workspaceId, Environment environment)`
instead of an `App` (and `resolve` likewise); the App-based callers pass the app's workspace and environment. The
listing now also drops system deployments and system projects, as `resolve` does. The facade then keeps only projects
the caller can see (the visible-project filter `getBindableWorkflows(appId)` already applies, changed to take a
workspace id) and the given project. Each workflow: `{projectDeploymentId, workflowUuid, workflowName, projectId,
projectName, inputSchema}`.

### Existing deployment tools

Unchanged in behaviour. `createProjectDeployment`'s and `promoteWorkflow`'s descriptions point to `deployProject` for
deployments an app will call.

## Server REST (for the CLI)

New read-only routes in `automation-app-public-rest/openapi-management.yaml`: base `/api/automation/v1`, an automation
API key (bound to the environment it was created in), `X-Environment`, workspace as a query parameter like the
existing app routes. Controllers carry no `@PreAuthorize`; each route is guarded on its facade. Every controller
extends `AbstractAppApiController`, whose `@ExceptionHandler(AccessDeniedException.class)` answers 403 (the global
handler would answer 500).

- `GET /apps/{id}/bindable-connections?componentName=` — `AppApiController` (tag `app`) →
  `AppFacade.getBindableConnections(appId, componentName)`, which gains the optional filter. Returns `id, name,
  componentName, connectionVersion, componentVersion, usable`; same rules as GraphQL `appBindableConnections`.
- `GET /apps/{id}/bindable-workflows` — `AppWorkflowBindingApiController` (tag `app-workflow-binding`) →
  `AppWorkflowFacade.getBindableWorkflows(appId)`. Returns `projectDeploymentId, projectDeploymentName, projectName,
  workflowUuid, workflowName, description`; same rules as GraphQL `appBindableWorkflows`.
- `GET /apps/component-operations?componentName=&componentVersion=` — `AppApiController` (tag `app`) →
  `AppFacade.getComponentOperations`. Returns `actionNames, toolNames`; same rules as GraphQL `appComponentOperations`.
- `GET /connections?workspaceId=&componentName=` — new `WorkspaceConnectionApiController` (tag
  `workspace-connection`) → new `AppFacade.getConnections(workspaceId, environment, componentName)`. Returns the
  `listConnections` fields.

Facade and controller details:

- `AppBindableConnectionDTO` gains `usable`; the GraphQL `appBindableConnections` schema is unchanged.
- `AppFacade.getConnections` is guarded `hasWorkspaceScopeInEnvironment(#workspaceId, 'CONNECTION_VIEW',
  #environment)` and delegates to `WorkspaceConnectionFacade.getConnections(workspaceId, componentName, null,
  environment.ordinal(), null)`, adding `usable` and `aiProvider` per connection (new `AppWorkspaceConnectionDTO`).
- `WorkspaceConnectionApiController` lives in `automation-app-public-rest` (package
  `com.bytechef.ee.automation.app.public_.web.rest`) with the other management routes, so the CLI gets one client
  from one spec.

## CLI (app half)

New `AutomationAppCommand` and `AutomationConnectionCommand` in `cli/commands/automation`, registered in
`CliApplication`, calling a new client `cli/clients/automation-app`. Its `openapi.yaml` is a committed copy of
`openapi-management.yaml`, regenerated with the client's `generateClient` task when the server spec changes (as
`cli/clients/automation-configuration` does). Commands use the profile and the per-command overrides every automation
command has (`--profile`, `--host`, `--token`, `--environment`, `--workspace-id`) and `--output json|table` (default
`json`).

- `bytechef automation app create --name … [--description …] [--hosting-target VERCEL|SELF_HOSTED]
  [--env-file .env.local]` — creates the app (`--hosting-target` defaults to `VERCEL`); writes `BYTECHEF_URL` and
  `BYTECHEF_APP_KEY` into the env file and `.bytechef/app.json` (`id`, `uuid`, `workspaceId`, `environment`, `host` —
  no secret, safe to commit) in the current directory. Never prints the key.
- `bytechef automation app link --app-id N` — writes `.bytechef/app.json` for an existing app in the current
  directory; no key.
- `bytechef automation app list` — the workspace's apps in DEVELOPMENT.
- `bytechef automation app status` — the app; its connection bindings (connection, component version — none when it
  cannot be resolved — and allow-lists); its workflow bindings with `available`.
- `bytechef automation app bind connection --binding-key K [--component-name …] [--connection-id N] [--actions a,b]
  [--tools x,y] [--proxy GET,POST]` — full replace; see below.
- `bytechef automation app bind workflow --workflow-key K --deployment-id N --workflow-uuid U` — upsert.
- `bytechef automation app unbind connection --binding-key K` and `bytechef automation app unbind workflow
  --workflow-key K` — remove the binding.
- `bytechef automation app key rotate [--env-file .env.local]` — rotates and rewrites `BYTECHEF_APP_KEY`; never
  prints it.
- `bytechef automation connection list [--component-name …]` — `GET /connections`: the `listConnections` fields.

Rules:

- **DEVELOPMENT only:** every `automation app` command and `automation connection list` refuses (exit 1) when the
  resolved environment (option, then `BYTECHEF_ENVIRONMENT`, then profile) is not DEVELOPMENT, telling the user to
  use a DEVELOPMENT profile with an automation API key created in DEVELOPMENT. Automation API keys are bound to one
  environment (`AutomationApiKeyAuthenticationProvider` looks the key up by environment).
- **Finding the app:** `--app-id`, else `.bytechef/app.json` found by walking up from the current directory (like
  git), so one repository can hold several app directories. The directory holding `.bytechef/` is the app directory;
  `--env-file` is resolved relative to it. A file whose host, environment or workspace differs from the resolved
  profile is an error. An `--app-id` that is not the linked app is resolved relative to the current directory, and
  every command that changes it first reads the app and refuses one outside DEVELOPMENT or outside the profile's
  workspace; `key rotate` with such an `--app-id` inside a linked directory also needs an explicit `--env-file`, so
  it never overwrites the linked app's key.
- **`create` and `link`** refuse when walking up finds an existing `.bytechef/app.json`. If writing a file fails after
  the server created the app, `create` prints the app id and what is missing: `automation app link --app-id <id>`
  restores `.bytechef/app.json`, and `automation app key rotate` puts a new key in the env file. It never prints the
  key.
- **Env file writing:** replaces only the `BYTECHEF_URL` / `BYTECHEF_APP_KEY` lines and keeps every other line; writes
  a temporary file with owner-only permissions (0600) in the same directory and renames it over the env file; warns
  when the env file is not git-ignored.
- **`bind connection`:** the binding becomes exactly what the command names: the PUT body replaces the connection and
  every allow-list, and an omitted `--actions`, `--tools` or `--proxy` means none. `--component-name` is required
  unless `--connection-id` is given (the component is then the connection's). Without `--connection-id`, the CLI
  picks among the usable connections of `GET /apps/{id}/bindable-connections?componentName=`: exactly one is used;
  otherwise it fails listing the candidates (id, name, usable). Action and tool names are checked against
  `GET /apps/component-operations` before the PUT; an unknown name fails listing the allowed ones. Nothing is written
  on failure.
- Errors say what to do next and list allowed values where the server returns them.

## Template (agent-facing docs)

`create/template/AGENTS.md` gains the loop:

1. One-time, by a person: connect the management MCP server (URL, an admin API key created in DEVELOPMENT, header
   `X-ENVIRONMENT: DEVELOPMENT`); create an automation API key in DEVELOPMENT and run `bytechef configure` with it,
   the workspace and `--environment DEVELOPMENT`; create sign-in (OAuth) connections in the ByteChef UI.
2. App first: `bytechef automation app create`, `bytechef automation connection list`,
   `bytechef automation app bind connection …` (actions for code, tools for AI); `npm run bytechef:status`.
3. Workflows only if needed: MCP `createProject` (with `workspaceId`) → `buildWorkflow` (`appCallable: true`) →
   `publishProject` → `deployProject` → the printed `bytechef automation app bind workflow …`. Changing a workflow:
   `buildWorkflow` naming the `draftWorkflowId` from the last `deployProject` result → `publishProject` →
   `deployProject` (the binding stays valid). If `buildWorkflow` is missing, ask an admin to enable Copilot.
4. Use from the app with the existing recipes: actions from server functions; tools (and optionally workflows) for an
   in-app agent via `toAiSdkTools`.
5. Rules: never print or echo `BYTECHEF_APP_KEY`; stay in DEVELOPMENT; ask a person to create sign-in connections.

`AGENTS.md`'s Deploy section (Vercel / Docker) stays as it is (D7). `README.md`'s mention of `bytechef app create`
becomes `bytechef automation app create`. A Java test, `AppTemplateCliCommandDriftTest` in
`server/apps/server-app/src/test/java/com/bytechef/ai/mcp/server/config/` next to `PluginSkillTagDriftIntTest`,
checks that every `bytechef …` command `AGENTS.md` names matches a `@Command(name = …)` literal under
`cli/commands/**`. `check:template` is unchanged: it runs in the Node SDK job, which has no CLI binary.

## Plugin

A `bytechef-dev` skill, `bytechef-app-builder`, carries the loop with transport fences:

- `mcp`, `edition: ee`, `uses: deployProject, listConnections, listAppCallableWorkflows`. The section is keyed only on
  these EE-only tools, so a CE server, which registers `buildWorkflow` but none of them, never receives it
  (`ManagementMcpServerConfiguration.buildInstructions` includes a section when any of its tools is registered). The
  body names `createProject`, `buildWorkflow` (`appCallable`) and `publishProject` in prose; the plugin README's rule
  that `uses:` lists every tool a block mentions gains this exception for `edition: ee` sections keyed on EE-only
  tools. It explains the Call-workflow trigger, the DEVELOPMENT setup and what to do when `buildWorkflow` is missing.
- `cli`, `uses:` the `automation app …` and `automation connection list` command paths and `configure`.

`bytechef-mcp-setup` gains one line: the app loop needs a DEVELOPMENT admin key and `X-ENVIRONMENT: DEVELOPMENT`.
The generated MCP instructions are regenerated (`./gradlew :claude-code-plugin:plugin-tools:generateMcpInstructions`);
`PluginSkillTagDriftIntTest` and `McpInstructionsStalenessTest` cover the new skill.

## Error handling summary

- Every refusal names the next step (publish first; pass `connections` or `--connection-id` from this list; use the
  DRAFT workflow id; use a DEVELOPMENT profile; ask an admin to enable Copilot; add the Call-workflow trigger).
- `deployProject` is all-or-nothing up to the write; an arming failure leaves the written deployment disabled and is
  reported as `arming failed`. `automation app bind connection` checks everything before its single PUT.
- A failed app-callable check leaves the built workflow saved and names its `workflowId` for the retry.
- The MCP tools refuse any environment but DEVELOPMENT; the CLI refuses a non-DEVELOPMENT profile.
- No command or tool ever prints an app key; the CLI writes it only to the env file.

## Testing

Plan D1:

- `AppLoopTools` unit tests: `deployProject` — automatic vs explicit connections; ambiguity refused with nothing
  saved; several deployments require an id; unpublished changes refused (never published, changed definition, added
  workflow, removed workflow); default enabled set; non-DEVELOPMENT `environment` refused; workspace mismatch refused;
  arming failure reported with the deployment left disabled. `listConnections` marks AI-provider connections and
  `usable`.
- `ProjectDeploymentFacadeDeployProjectIntTest` (automation-configuration-service): update in place keeps an
  existing app binding valid; a subset of `enabledWorkflows` keeps every other row and its enabled state; an explicit
  `connectionId` without `CONNECTION_USE` is refused; a PRIVATE connection invisible to the caller is neither a
  candidate nor accepted explicitly; a `projectDeploymentId` of a PRODUCTION deployment or of another project is
  refused; `removedWorkflows` is reported. The guard expression is pinned in `ProjectDeploymentFacadeAuthorizationTest`.
- `buildWorkflow` app-callable, wrapper tests with the inner model mocked: callable passes; a manual trigger and an
  unreadable schema fail with the workflow left saved; nothing persisted passes the reply through; editing a published
  `workflowId` is refused naming the DRAFT id; no `appCallable` in the schema without a checker.
- `AppCallableWorkflowResolver` after the refactor: existing tests pass; the listing excludes system deployments.
  `WorkflowConnectionSlotsTest` moves with the method.
- REST: `@WebMvcTest` IntTests per new route, including 403 (not 500) on `AccessDeniedException`; facade guard tests
  for `AppFacade.getConnections`, `AppFacade.getBindableConnections` (`usable`) and
  `AppWorkflowFacade.getCallableWorkflows` in `AppFacadeTest` / `AppWorkflowFacadeTest`.
- End-to-end `AppAgentLoopIntTest` (server-app): build (mocked model) → publish → `deployProject` → bind workflow →
  the app lane's `GET /api/app/v1/workflows` lists it as available; then the second iteration — rebuild through
  `draftWorkflowId` → publish → `deployProject` — keeps the binding and runs the changed definition.
- Generated instructions: on a CE tool set `buildInstructions` omits the app-loop section.

Plan D2:

- CLI command tests against a mocked server (`StubApi`, as the existing automation command tests): env file merge,
  atomic write and owner-only permissions, key never in output, walk-up lookup and `--env-file` relative to the app
  directory, `create`/`link` refused when `.bytechef/app.json` exists, the post-create failure message, `list`,
  DEVELOPMENT refusal, `bind connection` — automatic pick among usable candidates, ambiguity error, full-replace body,
  unknown action names listed — and the `--output json` shape.
- `AppTemplateCliCommandDriftTest`.
- `PluginSkillTagDriftIntTest` and `McpInstructionsStalenessTest` with the new skill.

## Changes to the parent spec

`2026-10-06-apps-governed-ai-built-apps-design.md`, edited in Plan D2 (line numbers as of this revision):

- **Line 23** (sub-project table): the row "D, Deploy: `bytechef app …` CLI commands and management MCP tools; Vercel
  and self-host targets" becomes "D, Coding-agent loop: the management MCP builds and deploys workflows; the
  `bytechef automation app …` CLI manages apps", pointing to this spec.
- **Lines 54-55** (goal): "to a deployed app on Vercel with one CLI command" becomes "to a running app bound to its
  connections with `bytechef automation app create` and `bind`; hosting deploy stays the template's `vercel deploy` /
  Docker".
- **Line 78** (non-goal "A Claude Code plugin. Optional later as one skill in `bytechef-dev`"): D adds the optional
  `bytechef-app-builder` skill; nothing depends on it.
- **Line 107** ("deploy does not rotate (see D)"): only an explicit rotation replaces the key; D has no hosting
  deploy.
- **Lines 133-135** (decision 8): "The deploy path that works in every coding agent" becomes "The agent loop that
  works in every coding agent".
- **Line 511** ("For the CLI and MCP, under the existing automation API-key auth"): "For the CLI" — the MCP has no app
  tools.
- **Lines 550-579** ("Design — D. Deploy", its CLI and MCP subsections): replaced by a pointer to this spec. App
  management moves from MCP tools to the CLI (D2), `app deploy` / `key rotate --vercel` are dropped (D7), workflow
  building and deployment are added.
- **Lines 665-666** (end to end: `bytechef app create` … `bytechef app deploy --target vercel`): `bytechef automation
  app create` → `bytechef automation app bind connection …` → run in development … → `vercel deploy --prod` (the
  template's own deploy).
- **Line 677** ("7. **D** CLI and MCP deploy."): "7. **D** Coding-agent loop (D1 server; D2 CLI, template, plugin)."
- **Line 679** ("A1–A3 are one implementation plan; A4 and B–D one each."): "A1–A3 are one implementation plan; A4, B
  and C one each; D two (D1, D2)."

Other documents:

- **`.agents/apps.md:13`** ("The CLI/MCP deploy commands are a separate sub-project and do not exist yet."): Plan D1
  replaces it with a D section on the server half (the three MCP tools, `buildWorkflow` app-callable mode, the REST
  routes) pointing to this spec; Plan D2 adds the CLI, template and plugin half.
- **`2026-10-07-apps-template-design.md` lines 4-5, 30, 44 and 272** name `bytechef app …` and say D replaces the
  template's deploy section: in Plan D2 they become `bytechef automation app …`, and line 272 says the deploy section
  stays (D7).

## Out of scope

Hosting deploy (an `app deploy` command), a workflow test-run tool, app tools on the MCP, staging/production (and
promotion) in the agent loop, creating sign-in connections, actions as AI tools, a declarative app manifest.
