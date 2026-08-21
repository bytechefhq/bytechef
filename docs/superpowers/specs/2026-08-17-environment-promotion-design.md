# Environment Promotion for API Collections, MCP Servers and A2A Servers — Design

**Date:** 2026-08-17
**Branch:** 0_732
**Status:** Draft — pending user review (written autonomously; every judgement call is marked ⚑ in §4
so it can be flipped before the plan is executed)

## 1. Goal

Let a user promote an **API collection**, an **MCP server**, or an **A2A server** from one
environment to another (`DEVELOPMENT → STAGING → PRODUCTION`) with one action, instead of
re-creating the counterpart by hand and then re-doing the work every time the pinned project
version changes.

Concretely, after this work:

- **First promotion** creates the counterpart in the target environment: same project, same
  pinned version, same endpoints / tools / skills and their mapping metadata, with connections
  re-bound to the target environment's connections.
- **Re-promotion** (after a version bump or after endpoints/tools/skills changed in the source)
  updates the existing counterpart in place — it does not create a second one.
- The user sees, before applying, what will happen: create vs. update, version move, which
  connections were auto-mapped and which they must pick, and any URL/contract consequences.

The user-facing docs already describe this promise
(`docs/content/docs/platform/enterprise/collaboration-devops/build-once-deploy-many.mdx`,
`…/environments.mdx` — "Draft ──publish──▶ v18 ──promote──▶ STAGING ──promote──▶ PRODUCTION").
Nothing implements it today.

## 2. Current state (verified on 0_732)

### 2.1 The three entities share one shape

Each is an environment-stamped root row that owns one or more **synthetic `ProjectDeployment`s**
and a set of **mapping rows that FK to `project_deployment_workflow`**:

| Surface | Root row (env) | Synthetic deployment name | Mapping rows → `project_deployment_workflow` |
|---|---|---|---|
| API collection (EE) | `api_collection` — env only via joined `project_deployment` | `__API_COLLECTION__<projectId>` (one per collection) | `api_collection_endpoint(http_method, path, name, project_deployment_workflow_id)` |
| MCP server (CE) | `mcp_server.environment` | `__MCP_SERVER__<projectId>_v<version>` (one per `mcp_project`) | `mcp_project_workflow(project_deployment_workflow_id, parameters)` — `toolName`, `toolDescription`, per-input `fromAi(...)` values; plus `mcp_component(connection_id, component_name, component_version)` + `mcp_tool(name, parameters, enabled)` |
| A2A server (CE) | `a2a_server.environment` | `__A2A_SERVER__<projectId>_v<version>` (one per `a2a_project`) | `a2a_project_workflow(project_deployment_workflow_id, parameters)` — `skillName`, `skillDescription`, `skillTags` |

Key files:

- `ApiCollectionFacadeImpl.createApiCollection` (`server/ee/libs/automation/automation-api-platform/automation-api-platform-configuration/automation-api-platform-configuration-service/…/facade/ApiCollectionFacadeImpl.java:109-141`) mints the deployment via `projectDeploymentService.create` (bypassing `ProjectDeploymentFacade`'s published/non-draft guards) with `enabled` unset (false).
- `McpProjectFacadeImpl.createMcpProject` (`server/libs/automation/automation-ai/automation-ai-mcp/automation-ai-mcp-service/…/facade/McpProjectFacadeImpl.java:73-114`) mints the deployment with `mcpServer.getEnvironment()`, `enabled=true`, no connections.
- `A2aProjectFacadeImpl.createA2aProject` (`server/libs/automation/automation-ai/automation-ai-a2a/automation-ai-a2a-service/…/facade/A2aProjectFacadeImpl.java:65-89`) mints the deployment with **`Environment.DEVELOPMENT` hardcoded** — a PRODUCTION A2A server runs its workflows against a DEVELOPMENT deployment. Pre-existing bug; prerequisite fix (§7.6).

### 2.2 Cross-version identity already exists; cross-environment identity does not

- `project_workflow.uuid` is stable across project versions (`ProjectWorkflowServiceImpl.publishWorkflow`
  re-inserts the historical row with the same uuid). `ProjectDeploymentFacadeImpl.checkProjectDeploymentWorkflows`
  (`…/automation-configuration-service/…/facade/ProjectDeploymentFacadeImpl.java:516-632`) uses that uuid to
  **update `project_deployment_workflow` rows in place** on a version change, which is why endpoint /
  tool / skill mapping rows survive "Change Project Version" today.
- Nothing links an `api_collection` / `mcp_server` / `a2a_server` in DEVELOPMENT to its
  counterpart in STAGING. There is no column, no naming convention, no service.

### 2.3 What blocks a naive "copy the row"

1. `uk_api_collection_name (name)` and `uk_mcp_server_name (name)` are **global** unique constraints
   (released — both init changelogs are in `v1.1.5`), so a same-named counterpart in another
   environment cannot exist. `a2a_server` has no name constraint (its init changelog is not on
   `master` and in no release tag).
2. Connections are environment-scoped (`connection.environment`) and
   `ProjectDeploymentFacadeImpl.validateDeploymentConnectionEnvironments` (`:958-975`) throws
   `INVALID_CONNECTION` when a bound connection's environment ≠ the deployment's. Every connection
   binding must be re-mapped; there is no cross-environment link between connections either.
3. Every MCP/A2A server row generates its own `secret_key` (`TenantKey.of()`), so a promoted server
   necessarily has a **different URL**. Clients must be repointed once per environment. (Runtime
   auth also requires the API key's environment to equal the server's:
   `AutomationMcpServerApiKeyAuthenticationProvider.java:96`, `AutomationA2AServerApiKeyAuthenticationProvider.java:95`.)
4. API collections resolve at runtime by `(environment, contextPath, collectionVersion, path)`
   (`ApiPlatformHandlerController.java:229-265`, env from `x-environment`, default PRODUCTION) — the
   same context path in two environments already dispatches correctly. Only the DB name
   constraint stands in the way.

### 2.4 Prior art in the repo

- `PromoteWorkflowToolCallback` (`automation-ai-tool`, tool `promoteWorkflow`) — creates a plain
  `ProjectDeployment` in a target environment with **zero connections**; a human finishes it in the
  dialog. Good naming rationale in its javadoc; not reusable as the algorithm.
- `CloneApiCollectionToolCallback` (EE `automation-ai-tool`, tool `cloneApiCollection`) — copies the
  collection shell to a target environment, **not** the endpoints, drops tags, requires a new context
  path (its javadoc wrongly says context paths are unique per environment; the real constraint is the
  global name UK, which its same-name default hits).
- `McpProjectFacade.cloneMcpProject` — re-runs `createMcpProject` against a target server, **not**
  copying `parameters` (a workflow without `toolName` is silently not served:
  `AutomationMcpToolFacade.java:209-217`) and keyed by legacy `workflowId`.
- `AssetFileFacade.cloneToEnvironment` — the cleanest facade-level "copy to environment" pattern
  (validate target env, verify workspace, resolve unique name, drop provenance).
- `ProjectDeploymentDialog` (client) with `environmentEditable` + `changeProjectVersion` +
  `filterWorkflowUuids` is ~90% of a promotion connection-mapping step: its connections step already
  filters candidate connections by the **form's** environment (`…StepItemConnection.tsx:49`).

## 3. Non-goals

- Agent/MCP tools for promotion (`promoteApiCollection`, `promoteMcpServer`, …) — follow-up once the
  facade exists; the existing `cloneApiCollection`/`cloneMcpProject`/`promoteWorkflow` tools are left
  as they are except for the doc fix in §7.7.
- Promotion-status badges on list rows ("promoted to STAGING v3 ✓") — the lineage uuid enables it;
  not built here.
- Workspace scoping for A2A servers (they are tenant-global today; unchanged).
- Approval gates / "must bake in STAGING for N minutes" policy — the docs describe these as policy
  on top of the promote operation; out of scope.
- Promoting plain project deployments (non-synthetic). `promoteWorkflow` + the deployment dialog
  cover that today; the shared `ProjectDeploymentPromoter` (§7.2) is written so a later
  "promote deployment" surface can reuse it, but no such surface ships here.
- Custom environments. `Environment` remains the fixed 3-value enum; "environmentId" stays the
  ordinal.
- Denormalising `environment` onto `api_collection` (§4 ⚑5).

## 4. Decisions and assumptions (⚑ = made autonomously, flip before executing the plan)

1. **Lineage identity = a `uuid` column on the root row** (`api_collection.uuid`, `mcp_server.uuid`,
   `a2a_server.uuid`). A promoted counterpart inherits the source's uuid; `(uuid, environment)`
   identifies "the same thing in another environment". Chosen over name-matching (a rename in the
   target silently forks a second row on the next promote) and over a `promoted_from_id` parent
   pointer (DEV→PROD direct plus DEV→STAGING→PROD yields two PROD rows). Precedent:
   `project_workflow.uuid`.
2. ⚑ **Any target environment other than the source is allowed**; the dialog defaults to the next
   one up. Matches the docs ("promote v18 straight to PRODUCTION, skipping STAGING if your policy
   allows"). No "forward only" rule — a PRODUCTION→STAGING promote is a legitimate way to
   reproduce prod in staging.
3. ⚑ **First promotion copies everything; re-promotion syncs only the exposed surface.** After the
   first promotion the following are **environment-local and never overwritten**: name,
   description, tags, enabled flags (server, deployment, endpoint, tool), authentication settings
   (`authenticationRequired`, `enforceToolAuthorization`, component authorities), secret keys, and
   connection bindings / workflow inputs that already exist in the target. What re-promotion
   **does** sync: pinned project version, the *set* of endpoints / workflow tools / component tools /
   skills, their mapping metadata (`http_method`, `path`, endpoint `name`; `toolName`,
   `toolDescription`, `fromAi` input mappings; `mcp_tool.parameters`; `skillName`,
   `skillDescription`, `skillTags`), and `contextPath` / `collectionVersion` (part of the URL
   contract — the preview warns when they differ from the target's current values).
4. ⚑ **Connection auto-mapping is exact-match, unambiguous-only.** A source connection maps to a
   target-environment connection when exactly one visible connection in the same workspace has the
   same `(componentName, connectionVersion, name)`; zero or several candidates → unresolved. On
   re-promotion, an existing target binding for the same node wins over the name match. The dialog
   pre-selects the suggestion and lets the user change it or leave it unresolved.
5. ⚑ **Unresolved connections do not block promotion.** The counterpart is created **disabled**
   (§6.3), the result lists the unresolved connections, and the dialog warns before apply. A
   promotion that must be preceded by "create every staging connection first, then come back" is
   what users do by hand today; leaving holes the user can fill through the existing
   "Change Project Version" / component dialogs is the better default.
6. ⚑ **`api_collection` gets no denormalised `environment` column.** `environment` stays on the
   joined deployment (as today). Consequence: `uk_api_collection_name` is **dropped without a DB
   replacement**; name uniqueness per `(workspace, environment)` and lineage uniqueness per
   `(uuid, environment)` are enforced by the facade (a repository query joining `project_deployment`).
   `mcp_server` and `a2a_server` carry `environment` on the row, so they get real
   `(name, environment)` / `(uuid, environment)` constraints. The asymmetry is documented, not
   hidden. (Alternative — add `environment INT` to `api_collection`, backfilled from the deployment,
   for a proper composite UK — is a one-changeset follow-up if the app-level check proves too weak.)
7. **The feature is EE.** Multi-environment is EE (`EnvironmentServiceImpl` CE returns only
   `DEVELOPMENT`; the client's `EnvironmentSelect` renders only for `edition === 'EE'`) and the
   project has been strict about EE feature code not shipping in the CE artifact
   (`2026-06-13-connection-visibility-to-ee-design.md`). All promotion logic lives in a new EE
   module (§7.1). The only CE touches are the `uuid` columns/fields on `mcp_server`/`a2a_server`
   (a neutral identity column), the A2A environment bug fix, and small pass-through additions to
   existing create paths (§7.5).
8. ⚑ **One generic GraphQL surface**, `environmentPromotionPreview` / `promoteToEnvironment`, over an
   enum `PromotionResourceType { API_COLLECTION, MCP_SERVER, A2A_SERVER }`, rather than three typed
   query/mutation pairs. One client dialog consumes it; adding a resource type = one handler bean +
   one enum value. API collections are REST elsewhere; the new surface is GraphQL because the client
   dialog is one component and MCP/A2A are GraphQL already.
9. ⚑ **Authorization mirrors each surface's create path**: `hasPermission(#projectId, 'Project',
   'DEPLOYMENT_PUSH')` for API collections (their create path has no guard of its own;
   `ProjectDeploymentServiceImpl.create` already requires `DEPLOYMENT_PUSH` on the project, so
   this is the power a promotion actually exercises), `hasPermission(#workspaceId, 'Workspace',
   'MCP_CREATE')` for MCP servers, `hasAuthority('ROLE_ADMIN')` for A2A servers.
10. **Promotion requires a published, non-draft project version.** The shared promoter validates
    up front (`project.isPublished()` and `version != project.getLastProjectVersion()` — the same
    rule `ProjectDeploymentFacadeImpl.createProjectDeployment` enforces at `:180-190`) before any
    write. A collection/server whose synthetic deployment pins the current draft (possible today
    because those create paths bypass the facade) is rejected with `PROJECT_NOT_PUBLISHED` /
    `INVALID_PROJECT_VERSION`.
12. **Target deployments are always minted by the surface's own create path** (`createApiCollection`,
    `createMcpProject`, `createA2aProject`) and then **synced** by the shared promoter. The promoter
    has no create branch. This keeps one creation path per surface (listeners, naming, cascades
    unchanged) and makes the core a single reconciliation routine.
11. **A2A `createA2aProject` must use the server's environment.** Fixed as a prerequisite; no data
    migration (A2A is unreleased and not on `master`).

## 5. Data model changes

### 5.1 `api_collection` (EE, released → new changeset)

```
addColumn uuid UUID                     -- backfill: UPDATE api_collection SET uuid = gen_random_uuid()
addNotNullConstraint uuid
createIndex idx_api_collection_uuid (uuid)      -- non-unique (see §4 ⚑6)
dropUniqueConstraint uk_api_collection_name
```

`ApiCollection` gains `@Column private UUID uuid` (+ `getUuidAsString()`), `ApiCollectionDTO` gains
`uuid`; `createApiCollection` uses the DTO's uuid when present, else `UUID.randomUUID()`.
`ApiCollectionServiceImpl.create/update` enforce name uniqueness within `(workspace, environment)`
via a new `CustomApiCollectionRepository` query (join `project_deployment` for environment,
`project` for workspace) → `ApiCollectionErrorType.NAME_ALREADY_EXISTS` (new). Lineage lookup:
`fetchApiCollection(UUID uuid, Environment environment)` (same join).

### 5.2 `mcp_server` (CE platform-mcp, released → new changeset)

```
addColumn uuid UUID                     -- backfill gen_random_uuid()
addNotNullConstraint uuid
dropUniqueConstraint uk_mcp_server_name
addUniqueConstraint uk_mcp_server_name_environment (name, environment)
addUniqueConstraint uk_mcp_server_uuid_environment (uuid, environment)
```

`McpServer` gains `uuid` (constructor assigns `UUID.randomUUID()`; setter for promotion);
`McpServerRepository.findByUuidAndEnvironment(UUID, int)`. `McpServerServiceImpl.update` keeps
ignoring `environment` (immutable) and also ignores `uuid`.

### 5.3 `a2a_server` (CE, unreleased → edit init changelog in place, per the repo rule)

```
column uuid UUID NOT NULL
uk_a2a_server_uuid_environment (uuid, environment)
```

`A2aServer` gains `uuid`; `A2aServerRepository.findByUuidAndEnvironment(UUID, int)`. Run
`scripts/dev/sync-local-schema-after-collapse.sh` for local DBs (init edit).

### 5.4 Nothing else changes shape

Endpoints, `mcp_project*`, `mcp_component`, `mcp_tool`, `a2a_project*`, `project_deployment*`
keep their tables. Enum ordinals untouched.

## 6. Semantics

### 6.1 Identity and idempotence

`promote(resourceType, sourceId, targetEnvironment, connectionMappings)`:

1. Load source; require `source.environment != targetEnvironment` and `targetEnvironment ∈
   environmentService.getEnvironments()` (CE never passes this).
2. Look up the counterpart by `(source.uuid, targetEnvironment)` — for MCP additionally within the
   same workspace (`workspace_mcp_server`) — → **create** if absent, **update** if present.
3. Re-running with the same inputs is a no-op update (all reconciliations are diff-based).

### 6.2 What is copied on create / synced on update

Per §4 ⚑3. Concretely, per surface (S = source, T = target):

**API collection**
- create: `name`, `description`, `contextPath`, `collectionVersion`, tags, `projectId`,
  `projectVersion`, all endpoints (`httpMethod`, `path`, `name`, `enabled`, `workflowUuid`),
  deployment `enabled=false`, workflow connections/inputs from mapping.
- update: `projectVersion`, `contextPath`, `collectionVersion`; endpoints reconciled by
  `(httpMethod, path)` — create missing, update `name`/`workflowUuid` on existing, delete those
  absent from S. Endpoint `enabled` is set from S only when the endpoint is created in T.

**MCP server**
- create: `name`, `type` (always `AUTOMATION` here), `authenticationRequired`,
  `enforceToolAuthorization`, tags, `enabled=false`, workspace assignment; every `mcp_project`
  (deployment in T env, pdw rows, `mcp_project_workflow` rows **with `parameters`**); every
  `mcp_component` (`componentName`, `componentVersion`, `connection_id` via mapping — may be null)
  and its `mcp_tool`s (`name`, `parameters`, `enabled`). `McpComponentAuthority` rows are **not**
  copied (security posture is environment-local).
- update: per S `mcp_project` matched to a T `mcp_project` by `projectId` (§6.5): deployment
  version + pdw reconciliation, `mcp_project_workflow` reconciled by workflow uuid with
  `parameters` **overwritten from S**; S projects absent in T are created; T projects absent in S
  are deleted. `mcp_component` reconciled by `(componentName, componentVersion)`, `mcp_tool` by
  `name` with `parameters` overwritten; T components/tools absent in S are deleted; T
  `connection_id` kept unless the mapping supplies one; T `mcp_tool.enabled` kept.

**A2A server**
- create: `name`, `description`, `type`, `authenticationRequired`, `enabled=false`; every
  `a2a_project` + `a2a_project_workflow` with `parameters`.
- update: as MCP projects; `a2a_project_workflow.parameters` overwritten from S.

### 6.3 Enabled state and activation

Created counterparts are **disabled** (server row / collection deployment). Rationale: the target
may have unresolved connections, and the user should look at the preview result before traffic
flows. Re-promotion never changes any enabled flag. For MCP, `McpServerAfterSaveEventListener`
already cascades server `enabled` onto every `mcp_project` deployment on save — the promoter does
not duplicate that.

### 6.4 Connections and inputs

- `ProjectDeploymentPromoter` collects every `ProjectDeploymentWorkflowConnection.connectionId`
  from the source deployment(s) plus every `mcp_component.connection_id`; distinct ids form the
  preview's connection list.
- Mapping resolution order per source connection: explicit mapping from the request → existing T
  binding for that node (update only) → unambiguous name match → unresolved (binding omitted).
- Mapped target connections are validated: exist, `environment == targetEnvironment`, visible to
  the caller in the workspace (resolved through the workspace connection read path so
  `ResourceVisibility` applies, not raw `ConnectionService`). `validateDeploymentConnectionEnvironments`
  remains the last line of defence.
- `ProjectDeploymentWorkflow.inputs`: copied on create; on update, an existing T pdw keeps its
  inputs, a newly created pdw takes S's.

### 6.5 Matching MCP/A2A projects across environments

`mcp_project` / `a2a_project` have no identity of their own beyond `(server, deployment)`. Within
one server the UI creates at most one project per `projectId`; the promoter matches by
`deployment.projectId`. If a server has several projects for the same `projectId` (schema-legal),
they are matched in ascending id order and the preview lists a warning naming the ambiguity.

### 6.6 URLs and secret keys

A created MCP/A2A counterpart mints its own `secret_key`; the preview and result carry the new URL
so the dialog can show it. Re-promotion never touches `secret_key`. API collections have no
per-collection secret; the runtime URL is `(x-environment, contextPath, collectionVersion, path)`,
so promoting keeps the same path shape and consumers switch environments through the header/API
key, which is the intended contract.

## 7. Architecture

### 7.1 Module

`server/ee/libs/automation/automation-promotion/`
- `automation-promotion-api` — `com.bytechef.ee.automation.promotion`: `PromotionResourceType`,
  `EnvironmentPromotionFacade`, DTOs (`EnvironmentPromotionPreview`, `EnvironmentPromotionResult`,
  `PromotionConnectionMapping`, `PromotionProjectPreview`), `EnvironmentPromotionErrorType`.
- `automation-promotion-service` — facade impl, `EnvironmentPromotionHandler` SPI + three handlers,
  `ProjectDeploymentPromoter`, `ConnectionEnvironmentMapper`, metrics.
- `automation-promotion-graphql` — `environment-promotion.graphqls` (`extend type Query/Mutation`),
  `EnvironmentPromotionGraphQlController`.

All classes: EE license header + `@version ee`, `@ConditionalOnEEVersion` on beans. Wired into
`server-app` (and `configuration-app` if that app hosts the MCP/API-collection GraphQL — the plan
verifies) beside `automation-workflow-alert-*`, the layout precedent.

Dependencies (api/service): `automation-configuration-api` (deployments, project workflows,
environments), `platform-connection-api`, EE `automation-configuration-api` (workspace connection
read facade), `automation-api-platform-configuration-api`, `automation-ai-mcp-api`,
`platform-mcp-api`, `automation-ai-a2a-api`, `platform-tag-api`, `platform-security-api`.
This module is a fan-in facade over domain APIs, like `automation-ai-tool`; it never imports a
`*-service` module.

### 7.2 Components

```
EnvironmentPromotionGraphQlController
  └─ EnvironmentPromotionFacade (impl: EnvironmentPromotionFacadeImpl)
       │  validates env, dispatches on PromotionResourceType, records metric
       └─ EnvironmentPromotionHandler (SPI, one bean per resource type)
            ├─ ApiCollectionPromotionHandler   @PreAuthorize DEPLOYMENT_PUSH on project
            ├─ McpServerPromotionHandler       @PreAuthorize MCP_CREATE on workspace
            └─ A2aServerPromotionHandler       @PreAuthorize ROLE_ADMIN
                 each uses:
                 ├─ ProjectDeploymentPromoter   clone/sync a synthetic deployment across envs
                 └─ ConnectionEnvironmentMapper suggest/validate connection re-binding
```

**`EnvironmentPromotionHandler`**
```java
public interface EnvironmentPromotionHandler {
    PromotionResourceType getResourceType();
    EnvironmentPromotionPreview preview(long sourceId, Environment targetEnvironment);
    EnvironmentPromotionResult promote(
        long sourceId, Environment targetEnvironment, Map<Long, Long> connectionMappings);
}
```
`@PreAuthorize` sits on the handler impl methods (they are Spring beans; SpEL resolves the
project/workspace id from the loaded source, exposed as a method parameter or via a small
`@PreAuthorize("@promotionAuthz.canPromoteApiCollection(#sourceId)")`-style bean if a parameter is
not available — the plan picks the mechanically simplest form the existing `PermissionEvaluator`
supports). `PermissionAuditAspect` therefore audits every promotion call automatically.

**`ProjectDeploymentPromoter`** (the shared core — one routine, no create branch; §4 12)
```java
record SyncResult(Map<Long, Long> workflowIdMapping,      // source pdw id → target pdw id
                  List<Long> unresolvedConnectionIds)

void validatePromotable(long projectId, int projectVersion);          // §4 10, throws
SyncResult sync(ProjectDeployment source, ProjectDeployment target,   // target already exists,
                ConnectionMappingResolver resolver, boolean targetIsNew); // in the target env
```
- `sync` sets `target.projectVersion = source.projectVersion`, builds the pdw list for that version
  from `projectWorkflowService.getProjectWorkflows(projectId, version)`, and for each workflow
  (matched to source/target pdw rows by `project_workflow.uuid`):
  - connections: the resolver decides per source binding (explicit mapping → existing target
    binding → name match → omitted, §6.4);
  - inputs / enabled: the target's own values when the target pdw exists and `targetIsNew` is
    false; the source's otherwise;
  then calls `ProjectDeploymentFacade.updateProjectDeployment(target, workflows, List.of())` →
  `checkProjectDeploymentWorkflows` updates pdw rows **in place by uuid** (ids stable — this is
  what keeps `api_collection_endpoint` / `mcp_project_workflow` / `a2a_project_workflow` FKs valid),
  creates rows for workflows new to the target and deletes rows whose workflow left the version.
- Returns `workflowIdMapping` computed by uuid after the update. Handlers use it to point mapping
  rows at the right target pdw.

`★` This deliberately reuses the version-migration algorithm instead of copying rows: cross-version
re-binding (`checkProjectDeploymentWorkflows`) and cross-environment re-binding are the same uuid
pivot with a different environment stamped on the parent. `targetIsNew` is what makes "first
promotion copies inputs/enabled, re-promotion keeps the target's" fall out of one code path.

**`ConnectionEnvironmentMapper`**
```java
List<PromotionConnectionMapping> suggest(long workspaceId, Set<Long> sourceConnectionIds,
                                         Environment target, Map<Long, Long> existingTargetBindings);
Map<Long, Long> validate(long workspaceId, Environment target, Map<Long, Long> requested); // throws
```
Uses the workspace connection read facade (visibility-aware) to list target-env connections for
each `(componentName, connectionVersion)`; exact `name` match with exactly one candidate.

### 7.3 Handler algorithms

**`ApiCollectionPromotionHandler`**
1. `source = apiCollectionFacade.getApiCollection(id)`; `existing = apiCollectionService
   .fetchApiCollection(source.uuid, target)`.
2. preview: create/update, project version move, `contextPath`/`collectionVersion` diff warning,
   connection suggestions from the source deployment's pdw connections, endpoint counts
   (create/update/delete).
3. promote (`@Transactional`): `promoter.validatePromotable(projectId, version)` first, then
   - create: `apiCollectionFacade.createApiCollection(dto with uuid=source.uuid, environment=target,
     name/description/contextPath/collectionVersion/tags copied)` (mints the disabled deployment as
     today), then `promoter.sync(sourceDeployment, createdDeployment, resolver, targetIsNew=true)`
     so connections/inputs land through the one shared path; then for each S endpoint
     `apiCollectionFacade.createApiCollectionEndpoint(dto{apiCollectionId=T, workflowUuid, …})`
     (which find-or-creates the pdw and stores its id).
   - update: `promoter.sync(sourceDeployment, targetDeployment, resolver, targetIsNew=false)`;
     `apiCollectionService.update` for `contextPath`/`collectionVersion`; endpoint reconciliation by
     `(httpMethod, path)` via `create/update/deleteApiCollectionEndpoint`.

**`McpServerPromotionHandler`**
1. `source = mcpServerService.getMcpServer(id)`; workspace via
   `workspaceMcpServerService.fetchWorkspaceIdByMcpServerId`; `existing =
   mcpServerService.fetchMcpServer(source.uuid, target)` filtered to the same workspace.
2. preview: create/update, per-project version moves (`PromotionProjectPreview[]`), connection
   suggestions (pdw connections ∪ component connections), new-URL warning on create, ambiguity
   warning (§6.5).
3. promote (`@Transactional`):
   - create: `workspaceMcpServerFacade.createWorkspaceMcpServer(name, AUTOMATION, target,
     enabled=false, authenticationRequired, workspaceId, uuid=source.uuid)` (new overload carrying
     uuid; also copies `enforceToolAuthorization` and tags via `McpServerFacade.updateMcpServerTags`).
   - for each S project: if no T project for that `projectId`, `mcpProjectFacade.createMcpProject(
     targetServerId, projectId, version, workflowIdsOfSourceSelection@version)` mints the T
     deployment + `mcp_project` + `mcp_project_workflow` rows; then `promoter.sync(sourceDeployment,
     targetDeployment, resolver, targetIsNew)` (version + connections/inputs); then reconcile
     `mcp_project_workflow` by workflow uuid using `workflowIdMapping` (add/remove selections via
     `updateMcpProject(selectedWorkflowIds)` where the set changed) and `updateParameters` from S.
   - components: reconcile `mcp_component` by `(componentName, componentVersion)` through
     `McpServerFacade.create/update(mcpComponent, mcpTools)` with `connection_id` from the mapping;
     tools by name.
   - Deletions in T for anything absent in S go through the existing facades (`deleteMcpProject`,
     `deleteMcpComponent`) so their cascades run.

**`A2aServerPromotionHandler`** — same as MCP minus workspace/tags/components:
`a2aServerService.create(A2aServer with uuid, target env, enabled=false)`,
`a2aProjectFacade`/`a2aProjectWorkflowService.updateParameters` for the mapping rows.

### 7.4 Facade + GraphQL

```graphql
enum PromotionResourceType { API_COLLECTION MCP_SERVER A2A_SERVER }

type PromotionConnectionMapping {
  sourceConnectionId: ID!
  sourceConnectionName: String!
  componentName: String!
  connectionVersion: Int!
  suggestedTargetConnectionId: ID
  usedBy: [String!]!               # "workflowLabel / nodeName" or "component:<name>" labels for the UI
}

type PromotionProjectPreview {
  projectId: ID!
  projectName: String!
  sourceProjectVersion: Int!
  targetProjectVersion: Int      # null when the counterpart does not exist yet
}

type EnvironmentPromotionPreview {
  resourceType: PromotionResourceType!
  sourceId: ID!
  sourceEnvironmentId: ID!
  targetEnvironmentId: ID!
  existingTargetId: ID           # null → will create
  existingTargetName: String
  projects: [PromotionProjectPreview!]!
  connections: [PromotionConnectionMapping!]!
  warnings: [String!]!
}

input PromotionConnectionMappingInput { sourceConnectionId: ID!, targetConnectionId: ID! }

input PromoteToEnvironmentInput {
  resourceType: PromotionResourceType!
  sourceId: ID!
  targetEnvironmentId: ID!
  connectionMappings: [PromotionConnectionMappingInput!]!
}

type EnvironmentPromotionResult {
  targetId: ID!
  created: Boolean!
  targetUrl: String              # MCP/A2A only
  unresolvedConnectionIds: [ID!]!
}

extend type Query {
  environmentPromotionPreview(resourceType: PromotionResourceType!, sourceId: ID!,
                              targetEnvironmentId: ID!): EnvironmentPromotionPreview!
}
extend type Mutation {
  promoteToEnvironment(input: PromoteToEnvironmentInput!): EnvironmentPromotionResult!
}
```
Controller methods are `@PreAuthorize("isAuthenticated()")` (real guards are on the handlers, per
the "API facade owns authorization" convention — here the handler is the facade of record).

### 7.5 Small changes to existing modules

- `WorkspaceMcpServerFacade.createWorkspaceMcpServer(...)`: overload with `UUID uuid` (existing
  overload delegates with `null` → random). `McpServer` gets `uuid`; remote-client stubs (if any)
  updated to the new signature.
- `ApiCollection`/`ApiCollectionDTO`: `uuid`; `ApiCollectionServiceImpl`: name-per-env check;
  `ApiCollectionRepository`/custom repo: uuid+env fetch.
- `A2aServer`: `uuid`; `A2aServerService.create(String, String, PlatformType, Environment)`
  assigns a random uuid; new `create(A2aServer)` already exists for the promoter.
- `A2aProjectFacadeImpl.createA2aProject`: use `a2aServerService.getA2aServer(a2aServerId)
  .getEnvironment()` (§7.6).
- `CloneApiCollectionToolCallback` javadoc/description: replace the "context paths are unique per
  environment" claim with the real rule (name unique per workspace+environment; context path free).
  `McpProjectFacade.cloneMcpProject` javadoc: drop the stale "implicitly bound to DEVELOPMENT"
  sentence.

### 7.6 Prerequisite bug fix — A2A deployment environment

`A2aProjectFacadeImpl.java:74` sets `Environment.DEVELOPMENT` unconditionally. Change to the
owning server's environment (inject `A2aServerService`). Pinned by a unit test. Without this, an
A2A server promoted to PRODUCTION would still run DEVELOPMENT deployments and PRODUCTION API keys
would be rejected at auth time (`AutomationA2AServerApiKeyAuthenticationProvider.java:95`).

### 7.7 Docs

- `docs/content/docs/platform/automation/deploy/api-platform.mdx`, `mcp-servers.mdx`,
  `a2a-servers.mdx`: a "Promote to another environment" section each (what is copied, what stays
  local, the URL/secret consequence, unresolved connections).
- `docs/content/docs/platform/enterprise/collaboration-devops/build-once-deploy-many.mdx`: replace
  the promote-dialog screenshot TODO with the shipped dialog once it exists (plan task, not blocking).
- CLAUDE.md: a short "Environment promotion (EE)" section naming the module, the lineage-uuid rule,
  the sync-vs-local rule (§4 ⚑3), and the API-collection app-level uniqueness caveat.

## 8. Client

- **`client/src/ee/shared/components/environment-promotion/EnvironmentPromotionDialog.tsx`** (EE).
  Props: `{resourceType, sourceId, sourceName, sourceEnvironmentId, workspaceId, onClose,
  onPromoted?}`. Flow: target-environment `Select` (all EE environments except the source; default =
  next ordinal, wrapping to the first non-source) → `useEnvironmentPromotionPreviewQuery` → summary
  (Create/Update badge, per-project "v3 → v5", warnings list) → connection section: one row per
  `connections[]` entry with a `Select` of target-env connections for that `(componentName,
  connectionVersion)` (reusing `useGetWorkspaceConnectionsQuery({componentName, connectionVersion,
  environmentId: target, id: workspaceId})` exactly as `ProjectDeploymentDialogWorkflowsStepItemConnection`
  does), pre-selected from `suggestedTargetConnectionId`, "Unresolved" allowed → **Promote**.
  On success: toast "Promoted to STAGING" with a **View in STAGING** action that calls
  `useEnvironmentStore.setCurrentEnvironmentId(target)`; invalidate the surface's list query
  (`apiCollections`, `workspaceMcpServers`, `a2aServers`).
- **Menu items**: "Promote to environment…" in `ApiCollectionListItemDropDownMenu` (EE page —
  static import), `McpServerListItemDropdownMenu` and `A2aServerListItem` (CE pages —
  `React.lazy(() => import('@/ee/shared/components/environment-promotion/EnvironmentPromotionDialog'))`
  rendered inside `<EEVersion hidden>`, the ProjectGit seam; the menu item itself is wrapped in
  `<EEVersion>` and additionally hidden when `useEnvironmentsQuery()` returns < 2 environments).
- Codegen: add the new `.graphqls` to `client/codegen.ts`, operations under
  `client/src/graphql/automation/promotion/`, regenerate `graphql.ts` (separate commit).
- No changes to `ProjectDeploymentDialog`.

## 9. Error handling

`EnvironmentPromotionErrorType` (new, in `automation-promotion-api`):
`SAME_ENVIRONMENT`, `ENVIRONMENT_NOT_AVAILABLE`, `SOURCE_NOT_FOUND`, `TARGET_CONNECTION_INVALID`
(wrong env / not visible / wrong component), `TARGET_NAME_CONFLICT` (a *different* lineage already
owns `(name, target env)` — the dialog offers to retry with a suffixed name; ⚑ v1 surfaces the
error and lets the user rename the source or the existing target). Deployment guard errors
(`PROJECT_NOT_PUBLISHED`, `INVALID_PROJECT_VERSION`, `INVALID_CONNECTION`) propagate as today.
All promotion is `@Transactional` — a failure mid-way leaves nothing half-created; the MCP
`McpServerAfterSaveEventListener` cascade runs after commit, unaffected.

Client: errors surface through the global fetch interceptor toast (GraphQL errors are parsed
centrally); the dialog only adds inline handling for `TARGET_CONNECTION_INVALID` (highlight the row).

## 10. Observability

- Metric `bytechef_environment_promotion{resource=api_collection|mcp_server|a2a_server,
  outcome=created|updated|failed}` via `ObjectProvider<MeterRegistry>` (no-op without actuator).
- Audit: every handler method is `@PreAuthorize`-guarded, so `PermissionAuditAspect` records the
  call with principal, decision and arguments (`sourceId`, `targetEnvironment`). No new audit aspect.
- Log line at INFO on success: resource type, source id, target id, created/updated, unresolved
  count.

## 11. Testing

- **Unit** (`automation-promotion-service`):
  - `ProjectDeploymentPromoterTest` — create branch builds a DTO with mapped connections and env;
    update branch keeps target bindings/inputs, adopts source's for new rows, returns a uuid-keyed
    `workflowIdMapping`; unresolved ids reported.
  - `ConnectionEnvironmentMapperTest` — exact match, ambiguity → unresolved, existing target
    binding wins, cross-env/invisible target rejected by `validate`.
  - One `*PromotionHandlerTest` per surface — create vs update detection by `(uuid, env)`, what
    is copied vs. left alone (§4 ⚑3 pinned field by field), reconciliation by
    `(httpMethod, path)` / workflow uuid / `(componentName, componentVersion)` / tool name,
    deletions, `parameters` overwrite, MCP project ambiguity warning.
  - `EnvironmentPromotionFacadeTest` — env validation, dispatch, metric outcomes.
  - Authorization pin test in the style of `ApiClientServiceAuthorizationTest`: asserts the exact
    `@PreAuthorize` expression on each handler method.
- **Unit** (existing modules): `A2aProjectFacadeImplTest` pins the server-environment fix;
  `ApiCollectionServiceTest` pins name-per-env uniqueness; `McpServerServiceTest` pins that
  `update` ignores `uuid`/`environment`.
- **Integration** (Testcontainers): `EnvironmentPromotionIntTest` in `automation-promotion-service`
  assembling the real repositories + facades — DEV→STAGING create then re-promote after a
  project publish + version bump: pdw ids stable, endpoint / `mcp_project_workflow` FKs valid,
  `parameters` synced, connections re-bound, `(uuid, environment)` constraints hold; plus a
  Liquibase proof that `uk_api_collection_name` / `uk_mcp_server_name` are gone and the new
  constraints exist.
- **Client**: `EnvironmentPromotionDialog.test.tsx` (vitest) — default target = next env, preview
  rendering, suggestion pre-selection, unresolved warning, mutation payload shape, success toast
  action switches the environment store (wait on store state, never fixed sleeps).

## 12. Migration and compatibility

- Existing rows get random uuids on migration — no lineage exists between rows created by hand
  before this feature. Two hand-made counterparts stay unlinked; a promotion from DEV creates a
  third unless the user deletes the hand-made STAGING one first. The preview makes this visible
  ("will create"). ⚑ No "adopt existing row as counterpart" affordance in v1 (follow-up:
  `linkAsPromotionTarget`).
- Dropping `uk_api_collection_name` / `uk_mcp_server_name` loosens uniqueness; the app-level
  (workspace, env) name check for API collections and the `(name, environment)` UK for MCP servers
  are strictly tighter than or equal to any collision that could arise from existing data (all
  existing names are globally unique, so no backfill conflict).
- The `cloneApiCollection` tool's default same-name clone becomes valid once the global name UK is
  gone (its `newContextPath` requirement stays as it is — separate tool, separate contract).
- Distributed EE apps: the promotion module ships in `server-app` and whichever EE app serves the
  automation GraphQL surface today (the plan verifies `configuration-app`); remote-client stubs for
  any newly added facade methods throw `UnsupportedOperationException` per convention.

## 13. Follow-ups (not in this spec)

- Agent tools `promoteApiCollection` / `promoteMcpServer` / `promoteA2aServer` (catalog tier) on the
  respective specialists, once the facade exists.
- Promotion-status badges on list rows (siblings by uuid).
- `linkAsPromotionTarget` for hand-made counterparts (§12).
- Composite DB uniqueness for `api_collection` if the app-level check proves insufficient (§4 ⚑6).
- Extending `ProjectDeploymentPromoter` to plain project deployments (a "Promote…" item on the
  Project Deployments page).

## 14. Decisions log

| # | Decision | Alternatives rejected | Why |
|---|---|---|---|
| 1 | Lineage `uuid` on the root row | name matching; `promoted_from_id` | robust to renames; supports non-linear promotion; precedent `project_workflow.uuid` |
| 2 | Any target env ≠ source | forward-only | docs promise skip-STAGING; PROD→STAGING is a legit reproduce step |
| 3 | First promotion copies all; re-promotion syncs only the exposed surface | full overwrite; sync nothing | env-local settings (enabled, auth, names, connections) must not be clobbered by a version bump |
| 4 | Unambiguous exact-name auto-map, user picks the rest, unresolved allowed | block on unresolved; fuzzy match | fewer round-trips than "create all connections first"; fuzzy match risks binding prod to the wrong account |
| 5 | Created counterpart is disabled | copy source's enabled | unresolved connections + user review before traffic |
| 6 | New EE module `automation-promotion` (fan-in facade) | per-domain facades in CE modules; EE twins of mcp/a2a modules | EE feature; one place; six new EE twin modules is heavier than one |
| 7 | Generic GraphQL query+mutation over `PromotionResourceType` | three typed pairs; REST | one dialog; adding a surface = enum + handler |
| 8 | Drop `uk_api_collection_name`, app-level per-env checks; real UKs on `mcp_server`/`a2a_server` | denormalise `environment` onto `api_collection` | avoids a duplicated column; asymmetry documented; follow-up path kept |
| 9 | Reuse `checkProjectDeploymentWorkflows` via `ProjectDeploymentFacade.updateProjectDeployment` | copy pdw rows directly | in-place-by-uuid keeps mapping-row FKs valid — the same reason "Change Project Version" works |
| 12 | Surface create paths mint target deployments; promoter only syncs | promoter creates via `createProjectDeployment` | one creation path per surface; core is one reconciliation routine; the published/non-draft rule is an explicit up-front check instead |
| 10 | Guards mirror each create path | one shared `PROMOTE` scope | no new permission scope; `PermissionAuditAspect` covers audit |
| 11 | Fix A2A hardcoded DEVELOPMENT as prerequisite | leave | promotion is meaningless for A2A otherwise |
