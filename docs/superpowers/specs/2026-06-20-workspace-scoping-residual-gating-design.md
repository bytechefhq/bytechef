# Workspace-Scoping & Residual-Gating Pass — Design

**Date:** 2026-06-20
**Branch:** `0_732`
**Status:** Draft (awaiting review)
**Predecessors:** `2026-06-19-centralized-idor-authorization-design.md`, `2026-06-20-approval-token-signing-and-search-scoping-design.md`

## 1. Problem

The gecko IDOR remediation (T17–T25) closed every **single-id, user-context** access path by
introducing a central `hasPermission` evaluator + `ResourceOwnershipResolver` SPI. That layer is
structurally unable to close two remaining classes of leak, which were therefore deferred as
documented residuals across T20, T22, and T24:

1. **Global `findAll` list endpoints.** A tenant-wide table is queried with no workspace predicate
   and the result feeds a *non-admin* workspace page. There is no resource id to evaluate, so a
   `hasPermission` gate cannot apply — the endpoint needs a `workspaceId` **parameter** (an API
   change) plus scoping, after which a gate becomes possible.

2. **Per-id reads on a runtime-shared SPI/executor path.** The method is also invoked from a worker
   / async-executor / editor-option-load thread that has **no `SecurityContext`**, so annotating it
   with `@PreAuthorize` would break legitimate non-user callers. These need a per-controller facade
   split (web-facing gated wrapper over the shared service) or the owning workspace threaded through
   the SPI.

This spec consolidates every such residual into one coherent pass with two fix-archetypes and
reuses the workspace resolvers already built during the search-scoping work.

## 2. Goals

- Every list endpoint that feeds a workspace page returns only the calling user's accessible
  workspace data, and carries an explicit `WorkspaceRole VIEWER` (or stricter) gate.
- Every per-id read currently reachable without ownership validation is either gated at a
  web-facing facade or has its owning workspace validated before returning data.
- No regression to worker / async-executor / embedded-execution paths that legitimately run without
  a `SecurityContext`.
- Reuse existing resolvers (`WorkspaceConnectionRepository.findByConnectionId`,
  `KnowledgeBaseWorkspaceResolver`, `ApiPlatformWorkspaceResolver`,
  `ProjectService.getWorkspaceProjectIds`) rather than inventing parallel mappings.

## 3. Non-Goals

- The tenant-wide `tag` table is **not** re-partitioned by workspace. Tags remain tenant-global
  entities; scoping happens at the *listing* layer (which tag-ids are reachable from a given
  workspace's entities), not in storage.
- No change to the already-correct admin-only embedded tag endpoints (`getIntegrationTags`,
  `getIntegrationInstanceConfigurationTags` — gated `Tenant ADMIN` in T25) or the already-scoped
  `getContextStoreTags(workspaceId)`.
- No change to the higher-severity non-IDOR backlog (T1–T16, T26–T27).
- Search-provider scoping (already shipped in the predecessor spec) is not revisited.

## 4. Inventory

### Archetype 1 — global `findAll` → workspace-scoped list

| # | Endpoint (current) | Module | Entity→workspace hop | Consumed by |
|---|---|---|---|---|
| 1 | `ProjectTagFacadeImpl.getProjectTags()` | automation-configuration | `ProjectService.getWorkspaceProjectIds(workspaceId)` | non-admin Projects page |
| 2 | `ProjectDeploymentFacadeImpl.getProjectDeploymentTags()` | automation-configuration | workspace-scoped `getProjectDeployments(...)` | non-admin Deployments page |
| 3 | `DataTableTagServiceImpl.getAllTags()` | platform-data-table | `workspace_data_table` relation | non-admin Data Tables page |
| 4 | `AssetFileTagServiceImpl.getAllTags()` | automation | asset→workspace | non-admin Assets page |
| 5 | `ConnectionFacadeImpl.getConnectionTags(PlatformType)` | platform-connection | `WorkspaceConnectionRepository.findByConnectionId` (reuse) | non-admin Connections page |
| 6 | `KnowledgeBaseTagFacadeImpl.getAllTags()` | automation-knowledge-base | `KnowledgeBaseWorkspaceResolver` (reuse) | non-admin KB page |
| 7 | `ApiCollectionFacadeImpl.getApiCollectionTags()` | EE api-platform | `ApiPlatformWorkspaceResolver` (reuse) | non-admin API Platform page |
| 8 | `McpServerGraphQlController.mcpServers(type, orderBy)` → `getMcpServers(...)` | platform-mcp | **sibling already exists**: `WorkspaceMcpServerGraphQlController.workspaceMcpServers(workspaceId)` | MCP Servers page |
| 9 | `McpServerTagGraphQlController.mcpServerTags(type)` | platform-mcp | via mcpServers→workspace | MCP Servers page filter |
| 10 | `McpProjectGraphQlController.mcpProjects()` → `getMcpProjects()` | automation-ai-mcp | mcpProject→mcpServer→workspace | MCP Projects page |
| 11 | `SubflowDataSourceImpl.getSubWorkflows(platformType, triggerName, search)` | platform-workflow | current workflow's workspace | editor subflow dropdown |

### Archetype 2 — per-id read on a runtime-shared SPI/executor path

| # | Method | Module | Why it can't take a plain `@PreAuthorize` |
|---|---|---|---|
| A | `SubflowDataSourceImpl.getSubWorkflowInputSchema(workflowUuid)` / `getSubWorkflowOutputSchema(workflowUuid)` | platform-workflow | Reachable from the T22a cluster-element option-load seam (shared with worker/embedded execution, no `SecurityContext`). Today takes a raw uuid and returns the workflow's I/O schema with **zero ownership check** — a true per-id IDOR read. |
| B | AI-agent **eval** read queries (the executor-shared reads left ungated in T24) | EE automation-ai eval | The same service methods are called by the async eval-run executor on a worker thread with no `SecurityContext`; gating in place breaks the run. |

## 5. Design

### 5.1 Archetype 1 — REST tag endpoints (#1–#7)

For each REST tag endpoint, mirror the existing `getWorkspaceProjects` pattern (which lives at
`/workspaces/{id}/projects`, operationId `getWorkspaceProjects`, `id` path param = workspaceId):

1. **OpenAPI** (`automation-configuration-rest-impl/openapi.yaml` and each domain's REST module):
   relocate the operation under `/workspaces/{id}/<entity>-tags` with `id` as a required int64 path
   parameter. (Query-param alternative — keep the path and add `?workspaceId=` — is rejected: the
   path form matches the established `getWorkspace*` convention and makes the gate SpEL read
   `#id`.) Regenerate the REST API interface for that module.

2. **Facade/service:** change the signature to accept `long workspaceId`, collect tag-ids only from
   that workspace's entities, and return `tagService.getTags(tagIds)`. Add
   `@PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')")` on the facade impl
   (per the agreement that `hasPermission` gates live on facade/service, not controllers).

   Example (`ProjectTagFacadeImpl`):
   ```java
   @Override
   @PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')")
   public List<Tag> getProjectTags(long workspaceId) {
       List<Long> projectIds = projectService.getWorkspaceProjectIds(workspaceId);

       List<Project> projects = projectService.getProjects(projectIds);

       List<Long> tagIds = projects.stream()
           .flatMap(project -> project.getTagIds().stream())
           .distinct()
           .toList();

       return tagService.getTags(tagIds);
   }
   ```

3. **Client:** regenerate the middleware; update the two existing hooks
   (`projectTags.queries.ts`, `projectDeploymentTags.queries.ts`) and the equivalents for
   DataTable/AssetFile/Connection/KB/ApiCollection to pass the **current** workspaceId (from the
   current-workspace store — same source `useGetWorkspaceProjectsQuery` already reads) and add it to
   the query key, e.g.:
   ```ts
   export const ProjectTagKeys = {
       projectTags: (id: number) => ['projectTags', id] as const,
   };

   export const useGetProjectTagsQuery = (id: number) =>
       useQuery<Tag[], Error>({
           queryKey: ProjectTagKeys.projectTags(id),
           queryFn: () => new ProjectTagApi().getProjectTags({id}),
       });
   ```

### 5.2 Archetype 1 — MCP GraphQL endpoints (#8–#10)

- **#8 `mcpServers(type)`:** a workspace-scoped sibling `workspaceMcpServers(workspaceId)` already
  exists (`WorkspaceMcpServerGraphQlController` → `workspaceMcpServerFacade.getWorkspaceMcpServers`).
  **Decision:** point the client at `workspaceMcpServers` and **delete** the global `mcpServers`
  query mapping (it is redundant attack surface), or — if any non-workspace caller remains — gate it
  `Tenant ADMIN`. Confirm callers during planning before deleting.
- **#9 `mcpServerTags(type)`:** add a `workspaceId` argument; resolve the workspace's mcp servers
  (via the same workspace facade), collect their tag-ids, return `tagService.getTags(tagIds)`. Gate
  `hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')` on the backing facade method.
- **#10 `mcpProjects()`:** an mcpProject belongs to an mcpServer which belongs to a workspace. Add a
  `workspaceId` argument and scope via the workspace's mcp servers
  (`mcpProjectsByServerId` already exists as the per-server primitive to compose over), or add a
  `getWorkspaceMcpProjects(workspaceId)` facade method. Gate on the new id.

### 5.3 Archetype 1 — Subflow list (#11)

`SubflowDataSourceImpl.getSubWorkflows(platformType, triggerName, search)` currently calls
`projectWorkflowService.getLatestProjectWorkflows()` (global) then resolves each via
`projectService.getProject(...)`. A subflow call may only target workflows in the **same workspace**
as the workflow being edited. The SPI signature carries no workspace, and the caller path
(`WorkflowCallWorkflowTool` options function, `SubflowTaskDispatcherDefinitionFactory`) runs through
the T22a cluster-element seam, so deriving "current user" inside the impl is unreliable.

**Decision:** thread the owning workspace into the SPI. Add a `Long workspaceId` parameter to
`SubflowDataSource.getSubWorkflows(...)` and filter `getLatestProjectWorkflows()` to that workspace
(`projectService.getWorkspaceProjectIds(workspaceId)`). The callers already know the workflow being
edited; plumb its workspace from the option-function context. If plumbing the workspace through the
option context proves infeasible in this pass, fall back to filtering by the *current user's
accessible workspaces* **only** when a `SecurityContext` is present, and document the editor-only
assumption. The threaded-workspace form is preferred.

### 5.4 Archetype 2 — Subflow schema reads (#A)

`getSubWorkflowInputSchema(workflowUuid)` / `getSubWorkflowOutputSchema(workflowUuid)` resolve any
workflow by uuid with no ownership check. Mirror the T24 `ProjectWorkflowExecutionFacade` split:

- Keep the existing `SubflowDataSourceImpl` methods unchanged for the worker/execution path
  (self-invocation continues to bypass the proxy, per the established pattern).
- Add a **web-facing facade** for the editor's schema-preview calls that, before delegating,
  validates the resolved workflow's workspace against the caller using the existing
  `@permissionService.hasWorkflowScope(workflowId, 'VIEW')` SpEL helper (the uuid resolves to a
  `workflowId` via `projectWorkflowService.getLastWorkflowId(workflowUuid)`). Route the editor's
  GraphQL/REST schema-preview through this facade; leave SPI consumers on the ungated path.

### 5.5 Archetype 2 — Eval read queries (#B)

Apply the per-controller-facade rule (from project memory): the shared eval service stays ungated
(executor calls it on a worker thread); introduce a thin web-facing eval facade that wraps the read
methods consumed by the eval **controllers**, gated `Tenant ADMIN` (eval authoring is already
admin-only in T24). Reroute the eval read controllers through the new facade; the async run executor
keeps calling the shared service directly.

## 6. Reuse map

| Resolver / method | Built in | Reused for |
|---|---|---|
| `ProjectService.getWorkspaceProjectIds(long)` | existing | #1, #2, #11 |
| `WorkspaceConnectionRepository.findByConnectionId` | ConnectionOwnershipResolver / search | #5 |
| `KnowledgeBaseWorkspaceResolver` | KB search-scoping | #6 |
| `ApiPlatformWorkspaceResolver` | API-platform search-scoping | #7 |
| `@permissionService.hasWorkflowScope(workflowId, 'SCOPE')` | central evaluator | #A |
| `workspaceMcpServerFacade.getWorkspaceMcpServers(workspaceId)` | existing | #8, #9, #10 |

## 7. Testing

- **Per facade (unit):** Mockito test asserting (a) only the target workspace's tag-ids are passed
  to `tagService.getTags`, and (b) cross-workspace entities are excluded. Mirror
  `AutomationSearchFacadeImplTest`.
- **Gate (negative):** a non-member principal hitting the workspace-scoped endpoint is denied
  (`AccessDeniedException`); a worker/executor path (no `SecurityContext`) on the Archetype-2 shared
  service still succeeds (pins the facade-split boundary).
- **Subflow SPI:** `getSubWorkflows(workspaceId, ...)` returns only same-workspace subflows;
  `getSubWorkflowInputSchema` via the facade denies a foreign-workspace uuid while the SPI path
  remains open.
- **Client:** typecheck + the existing query-hook tests updated for the new `id` argument.

## 8. Rollout

Pure scoping/gating — **no** feature flag or `required`-style migration toggle (unlike the
approval-token work). Each domain ships as an independent vertical slice (server + openapi +
client codegen + test) so partial delivery is safe; a half-shipped pass leaves earlier domains fully
correct. Server and client commits split per the repo convention (`gecko ...` / `gecko client - ...`).

## 9. Risks & deviations

- **Client breakage from signature change.** Adding a required `workspaceId` to a generated client
  method breaks every caller until regenerated + updated. Mitigation: do server+openapi+codegen+hook
  as one atomic slice per domain; never commit the server change alone.
- **MCP `mcpServers` deletion.** Deleting the global query is only safe if no non-workspace caller
  remains; planning must enumerate callers (GraphQL operation files) before removal — otherwise gate
  `Tenant ADMIN` instead.
- **Subflow workspace plumbing.** Threading workspace through the option-function context may touch
  the cluster-element option call chain; if that proves too invasive for this pass, the documented
  fallback (current-user-when-present) is acceptable but weaker, and must be called out honestly per
  the "don't relabel spec intent" rule.
- **Tag storage stays tenant-global.** A user can still *coin* a tag name visible only via entities
  they own; we scope listing, not the tag namespace. Documented as accepted (Non-Goal §3).

## 10. Open questions (resolve in planning)

1. Do DataTable / AssetFile tag endpoints expose a workspace-scoped entity-id list method today, or
   must one be added (analogous to `getWorkspaceProjectIds`)?
2. Are there non-workspace callers of `mcpServers(type)` / `mcpProjects()` (delete vs. admin-gate)?
3. Is the eval read surface reachable from any non-admin page (would force `WorkspaceRole` instead of
   `Tenant ADMIN` on the new eval facade)?
