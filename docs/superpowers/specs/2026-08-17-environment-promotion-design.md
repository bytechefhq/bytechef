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

- ~~Agent/MCP tools for promotion~~ — **built as a follow-up; see §16.** Four catalog-tier tools
  (`promoteApiCollection`, `promoteMcpServer`, `promoteA2aServer`, `promoteProjectDeployment`) landed
  once the facade existed. `cloneApiCollection` was retired with them; `cloneMcpProject` and
  `promoteWorkflow` are unchanged.
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

---

## 15. Amendment (2026-08-18): plain project deployments become a fourth resource type

**Status:** approved by the user on 2026-08-18, mid-execution (Tasks 1-6 already landed).

§3 previously listed "promoting plain project deployments (non-synthetic)" as a non-goal and §13
carried it as a follow-up. The user has moved it into scope. This amendment supersedes both entries;
everything else in this document stands unchanged.

### 15.1 Why it is cheap now

`ProjectDeploymentPromoter` (§7.2) already *is* the deployment promoter — moving the pinned version,
re-binding connections through `ConnectionEnvironmentMapper`, and reconciling
`project_deployment_workflow` rows in place by `project_workflow.uuid`. The three existing handlers
are thin wrappers that mint their surface's synthetic deployment and then hand it to that routine.

A plain deployment is the *simplest* of the four callers, because it is the only one with **no
mapping rows to reconcile afterwards** — there is no `api_collection_endpoint`,
`mcp_project_workflow` or `a2a_project_workflow` child set. It needs no
`ProjectWorkflowMappingReconciler` (pre-flight ruling R7) at all.

### 15.2 What exists today, and why it is not enough

The `promoteWorkflow` AI tool (`PromoteWorkflowToolCallback`, CE) builds a `ProjectDeploymentDTO`
with only `projectId`, `projectVersion`, `environment`, `name`, `description`, `enabled(false)` and
calls `createProjectDeployment`. It wires **no connections and no workflow inputs**, is not
idempotent (running it twice creates two deployments), carries no lineage, and re-syncs nothing when
the project version moves. There is no client affordance at all. The tool stays as it is; it is a
one-shot convenience for the LLM, not a promotion surface.

### 15.3 Lineage — `project_deployment.uuid`

`ProjectDeployment` has no uuid today (verified). It gets one, exactly as `api_collection`,
`mcp_server` and `a2a_server` did:

```
addColumn uuid UUID                     -- backfill gen_random_uuid()
addNotNullConstraint uuid
addUniqueConstraint uk_project_deployment_uuid_environment (uuid, environment)
```

`project_deployment` is long released (it predates `v1.1.5`, having been renamed from
`project_instance` in changelog `20240604153081`), so this is a NEW changeset — never an init edit.

A `(uuid, environment)` unique constraint is safe here in a way it was not for `api_collection`:
`project_deployment` carries `environment` on the row. Matching by `(projectId, environment)` was
rejected — `ProjectDeploymentService.fetchProjectDeployment(projectId, environment)` assumes one
deployment per project+environment, an assumption the embedded automation bridge already had to work
around with `fetchProjectDeploymentByName`, so it is not a safe identity.

### 15.4 Prerequisite: `__A2A_SERVER__` deployments leak into the deployments list

`CustomProjectDeploymentRepositoryImpl` excludes `__API_COLLECTION__` and `__MCP_SERVER__` deployment
names from the Project Deployments listing but **not** `__A2A_SERVER__` — `A2aProjectFacadeImpl`
defines its own private `A2A_SERVER_NAME_PREFIX` constant instead of adding one to `SystemProjects`,
so the shared filter never learned about it. This is a pre-existing bug (A2A synthetic deployments
are visible in the Project Deployments list today), and it becomes actively harmful the moment the
list grows a "Promote…" item: a user could promote an A2A server's synthetic deployment directly,
producing a second deployment shell that its owning `a2a_server` row knows nothing about.

Fix as a prerequisite: add `A2A_SERVER_DEPLOYMENT_NAME_PREFIX` to `SystemProjects`, have
`A2aProjectFacadeImpl` use it, and add the third `notLikePredicate` to the listing query.

### 15.5 Synthetic deployments are refused at the handler, not only hidden

The listing filter is a UI concern and not an authorization boundary. `ProjectDeploymentPromotionHandler`
independently REJECTS any source deployment whose name starts with a synthetic prefix, with a new
`EnvironmentPromotionErrorType.SYNTHETIC_DEPLOYMENT_NOT_PROMOTABLE` (key 106), so a direct GraphQL
call cannot do what the hidden menu item cannot. The correct action for those rows is to promote the
owning API collection / MCP server / A2A server, and the error message says so.

### 15.6 Semantics

Identical to §6.1-§6.4, with the surface-specific parts collapsing:

- **create**: `ProjectDeploymentFacade.createProjectDeployment(dto)` — the surface's own create path,
  per decision 12, which also enforces the published/non-draft rule of decision 10 — with `name`,
  `description`, `projectId`, `projectVersion`, `uuid = source.uuid`, `enabled = false`, and the
  per-workflow connections/inputs/enabled produced by `ProjectDeploymentPromoter.sync`
  (`targetIsNew = true`).
- **update**: `promoter.sync(source, target, requested, suggested, targetIsNew = false)` and nothing
  else. Per §4 ⚑3, `name`, `description`, tags and every `enabled` flag stay environment-local.
- **no mapping-row reconciliation**, and therefore no ordering constraint around
  `updateProjectDeployment`'s pdw deletion — the reason that ordering matters for MCP and A2A
  (pre-flight ruling R6) simply does not arise.
- **name conflict**: the handler rejects with `TARGET_NAME_CONFLICT` when a *different* lineage
  already owns `(name, projectId, targetEnvironment)`. Whether a database constraint already
  enforces deployment-name uniqueness at that grain must be VERIFIED during implementation rather
  than assumed; the handler check is required either way.

### 15.7 Authorization

`@PreAuthorize("hasPermission(@promotionAuthorizer.projectIdOfProjectDeployment(#sourceId), 'Project', 'DEPLOYMENT_PUSH')")`
— the same guard as API collections (§4 ⚑9, as revised by pre-flight ruling R5), and exactly the
permission `ProjectDeploymentServiceImpl.create` already demands. `promotionAuthorizer` gains a third
lookup method.

### 15.8 Surface

`PromotionResourceType` gains `PROJECT_DEPLOYMENT`, **appended last** so no existing constant moves.
The enum is a GraphQL enum and a Java enum only — it is not persisted as an ordinal — but appending
keeps it consistent with the repo's enum discipline. The generic
`environmentPromotionPreview` / `promoteToEnvironment` pair (§4 ⚑8) needs no schema change beyond the
new enum value, which is the payoff of having chosen one generic surface over three typed pairs.

Client: a "Promote to environment…" item on the Project Deployments page's list-item dropdown,
reusing the same `EnvironmentPromotionDialog` through the same `<EEVersion hidden>` + `React.lazy`
seam as the MCP and A2A pages, and hidden when fewer than two environments exist.

### 15.9 Not in scope even now

- Promoting a deployment to a project that does not exist in the target environment. Projects are
  workspace-scoped, not environment-scoped, so this cannot arise.
- Bulk promotion of every deployment in a project.
- Retiring or rewriting `promoteWorkflow`.

## 16. As built

Recorded after implementation (Tasks 1-22). Where the design assumed something that turned out to need a
different shape, the assumption is corrected here rather than quietly dropped.

- **Promotion ships in `server-app` only.** `automation-promotion-graphql`/`-service` are wired into
  `server/apps/server-app/build.gradle.kts`, not into `configuration-app` or any other distributed EE app.
  §7.1 asked the plan to "verify" whether `configuration-app` hosts the MCP/API-collection GraphQL surface
  and wire the module there too; it does not carry `automation-api-platform-configuration-service` or
  `automation-ai-a2a-service` at all, so the handlers' collaborators (`ApiCollectionService`,
  `A2aServerService`, …) don't exist in that app's context — there is nothing to wire in. Distributed EE
  deployments therefore have no promotion surface at all, in the same spirit as orphaned-job recovery and
  the embedded-automation error-workflow handler being monolith-only. A future distributed rollout needs
  either remote-client stubs for every collaborator the handlers touch, or a dedicated promotion
  microservice — neither is attempted here.
- **Authorization goes through a separate `promotionAuthorizer` bean**
  (`com.bytechef.ee.automation.promotion.security.PromotionAuthorizer`), not the self-referencing
  `@apiCollectionPromotionHandler.projectIdOf(...)` form §7.2 sketched. A `@PreAuthorize` expression that
  calls back into the very bean the annotation guards makes that bean invoke itself through its own
  freshly-entered security proxy — the guard is still being evaluated, so the call either deadlocks the
  proxy or bypasses the check depending on proxy mode. `PromotionAuthorizer` exposes one
  `<ownerKind>Of<ResourceKind>(id)` lookup per resource type instead. Because every lookup runs *before*
  any authorization verdict exists, it cannot distinguish "no such resource" from "a resource this caller
  may not see" without creating an id-enumeration oracle, so an unresolvable id throws
  `AccessDeniedException` (403) rather than a typed `SOURCE_NOT_FOUND` — a real typo or a concurrently
  deleted resource is reported the same way as "not yours to promote."
- **`ConnectionEnvironmentMapper.suggest` diverges from §7.2's signature.** It is
  `Map<Long, Long> suggest(long workspaceId, Set<Long> sourceConnectionIds, Environment targetEnvironment)`
  — three parameters, no `existingTargetBindings` map, returning a plain id-to-id map — rather than the
  spec's `List<PromotionConnectionMapping>` built from four parameters. Resolving which target connection
  a source connection maps to, and assembling the `PromotionConnectionMapping` preview DTO (with
  `sourceConnectionName`, `usedBy` labels, etc.) for the client, turned out to be different jobs done at
  different times; the latter moved into a new `PromotionPreviews` helper that each handler's `preview()`
  calls after resolving ids. "Existing target wins over name match" is handled by each handler passing its
  own `existingTargetBindings` into that assembly step, not by the mapper.
- **§6.5's duplicate-project matching is per-`projectId` deque pairing (i-th source project to i-th
  target project), not first-wins.** A first-wins scheme is monotonically corrupting across repeated
  re-promotions: once a target project is claimed by an earlier source project in the iteration, a later
  source project with the same `projectId` would never find it, mint a fresh target project every time,
  and never delete the one that lost the race (a skipped target is never recorded as matched, so nothing
  ever marks it stale). Grouping each side's projects by `projectId` into an `ArrayDeque` (target side
  ordered by ascending `mcp_project`/`a2a_project` id, per §6.5) and polling one target per source project
  keeps the pairing stable run over run, and any target left over after all source projects are paired is
  deleted as genuinely orphaned.
- **Name-conflict guards differ per surface, by design, not by oversight.** `api_collection`
  (`(workspace, environment)`, app-level check) and `mcp_server` (`(name, environment)`, a real DB unique
  constraint) both convert a collision with a *different* lineage's row into
  `EnvironmentPromotionErrorType.TARGET_NAME_CONFLICT` before creating a counterpart. `a2a_server` carries
  no name-uniqueness constraint at all, so `A2aServerPromotionHandler` adds no such guard — there is
  nothing to collide with. `project_deployment` also carries no name constraint, but
  `ProjectDeploymentPromotionHandler` adds the check anyway (§15.6): `fetchProjectDeploymentByName` is a
  real disambiguation path elsewhere in the codebase (the embedded automation bridge depends on being able
  to find "the" deployment for a project+environment by name), and the same bridge can legitimately produce
  more than one deployment per `(project, environment)` — so a plain deployment promotion silently creating
  a second same-named row would poison that lookup in a way the other three surfaces can't reproduce.
- **`api_collection`'s app-level name check is a known, accepted check-then-insert race.** Dropping
  `uk_api_collection_name` (§5.1) removes the database backstop the old constraint gave; no partial unique
  index replaces it because `environment` is not a column on `api_collection` — it is read through the
  joined `project_deployment` (⚑5/⚑6), which a partial index cannot express. Two concurrent creates for the
  same `(workspace, environment, name)` can therefore both pass the read and both insert, producing a
  duplicate name pair the old global constraint would have rejected. Ruled ACCEPTED RISK rather than fixed
  (progress log, Task 2) — rare in practice, and no worse than the app-level checks other resources already
  rely on elsewhere in the codebase.
- **Two user-facing REST behavior changes shipped as prerequisites, not as promotion features per se:**
  - `ApiCollectionEndpointServiceImpl.update` previously copied only `httpMethod`, `name`, and `path` onto
    the re-read row, silently dropping any `projectDeploymentWorkflowId` the caller supplied — editing an
    endpoint's workflow in the UI compiled, saved, and toasted success, but the endpoint kept calling the
    old workflow. Promotion's own endpoint reconciliation (§6.2, "update `name`/`workflowUuid` on
    existing") needed this to actually work, so `update` now copies `projectDeploymentWorkflowId` when the
    caller supplies one (a DTO-converted entity supplies none, so a plain re-save is unaffected).
  - A stale or foreign workflow uuid on that same update path used to succeed as a silent no-op (the
    lookup failure was swallowed); it now throws a typed `ApiCollectionErrorType.WORKFLOW_NOT_FOUND` (400).
    Both are pinned by `ApiCollectionFacadeTest`.

### 16.x Agent tools (follow-up, landed after the main feature)

**Topology correction to §13.** §13 said the tools go "on the respective specialists", written when
`AiHubAgentType` was believed to hold them. It does not, and never did — the automation-owned
specialists live in a **separate enum**, `AutomationSubAgentType` (`MCP_AGENT`,
`PROJECT_DEPLOYMENT_AGENT`, `API_COLLECTION_AGENT`). Nothing had been dissolved; two enums were being
read as one.

They were still the wrong home. There is no `a2a_agent`, so A2A would have had nowhere to live, and
promotion is one capability across four resource types rather than four resource-specific features.
All four tools are registered in the **AI Hub BUILD searchable catalog** instead.

**Principal propagation was already solved by that placement.** The concern was real — agent tools run
on worker threads with no `SecurityContext`, and every handler is `@PreAuthorize`-guarded — but
`ToolSearchAdvisorConfiguration` already wraps every catalog tool in
`AiHubToolCallbackWrappers.wrap(...)`, which includes `RehydrateContextToolCallback`. Choosing the
catalog tier for token-cost reasons happened to be the same choice that supplies the principal.

**Four concrete verbs, one implementation.** `PromoteToEnvironmentToolCallback` is constructed once per
`PromotionResourceType`. Concrete names rather than one generic tool taking a type argument: ids are
per-table, so a generic tool that paired a correct `sourceId` with the wrong `resourceType` would
usually find a real row of the other type and promote something the user never named. Four names make
that unrepresentable for the cost of four catalog entries.

**One shot, previewed internally.** The tool calls `preview` before `promote` purely to enrich its own
result with the handler's warnings, then promotes. No separate dry-run tool to keep in sync. The result
states explicitly that a created counterpart is DISABLED, and lists `unresolvedConnectionIds` — the
prompt requires the model to relay both.

**Distributed deployments (ruling R4).** Registered through `ObjectProvider<EnvironmentPromotionFacade>
.ifAvailable`. This is load-bearing, not defensive: `ai-hub-service` also ships in `ai-copilot-app`,
which does not carry `automation-promotion-service`, so the bean is genuinely absent there and the
tools are simply not registered rather than failing startup.

**`cloneApiCollection` retired.** It copied five fields, set endpoints to `List.of()` deliberately, and
called the same facade method `createApiCollection` calls — its whole marginal value was saving one
lookup, while its cross-environment case produced the half-configured shell that `promoteApiCollection`
now produces properly. Eight references removed, not the three the plan listed: the plan missed the
copilot `ApiCollectionToolCallbacksFactory`, that factory's test, and both prompt files.
`cloneMcpProject` is unchanged — its axis is the MCP server, not the environment.
