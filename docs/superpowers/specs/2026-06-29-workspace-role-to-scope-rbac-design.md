# Workspace RBAC: collapse role-rank gates into scope checks + resource-typed tokens

- **Date:** 2026-06-29
- **Branch:** 0_732
- **Status:** Design (pre-plan)
- **Author:** Ivica Cardic

## 1. Motivation

`@PreAuthorize` authorization against a workspace id is expressed two ways today:

- `hasPermission(#workspaceId, 'WorkspaceScope', '<PermissionScope>')` — fine-grained capability, routed to `PermissionService.hasWorkspaceScope`.
- `hasPermission(#workspaceId, 'WorkspaceRole', '<VIEWER|EDITOR|ADMIN>')` — coarse role-rank, routed to `PermissionService.hasWorkspaceRole`.

Two problems:

1. **The rank check excludes custom-role members.** A `WorkspaceUser` row carries *either* a built-in `workspaceRole` ordinal *or* a `customRoleId` (XOR invariant). `hasWorkspaceRole` reads the built-in ordinal; for a custom-role member it is null, so `toWorkspaceRole(null)` collapses the `Optional` to `false`. A custom-role member therefore fails *every* `WorkspaceRole` gate regardless of how privileged their custom role's scopes are. This is a latent footgun: an org that mints an "admin-equivalent" custom role still can't pass a `WorkspaceRole('ADMIN')` gate.

2. **Both role kinds already resolve to `PermissionScope` sets.** `BuiltInRoleScopes` maps VIEWER/EDITOR/ADMIN to scope sets (`VIEWER ⊂ EDITOR ⊂ ADMIN`, ADMIN = `allOf`); custom roles resolve via `CustomRoleScopeResolver`. The scope model is already the common substrate. The rank check is redundant *and* the only thing that breaks for custom roles.

The fix: express every workspace gate as a scope check. Rank semantics fall out of scope containment for free (a VIEW scope placed in the VIEWER tier is automatically held by EDITOR/ADMIN). Custom-role members become first-class.

A second, independent cleanup: once the rank check is gone, the `targetType` no longer needs a `Scope`/`Role` discriminator — there is only one kind of check against a workspace id. So the token collapses from `'WorkspaceScope'` to the bare resource type `'Workspace'`, aligning with Spring's `hasPermission(targetId, targetType, permission)` semantics:

```
@PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'KNOWLEDGE_BASE_VIEW')")
```

reads as *"does the caller have KNOWLEDGE_BASE_VIEW on Workspace #workspaceId?"*.

## 2. Scope of the change

The gap audit (45 `WorkspaceRole` gates) showed the `PermissionScope` enum today covers **only the project-artifact domain** (workflows, executions, connections, agents, deployments push/pull, API keys, project settings/delete). Workspace-level concerns and several feature domains have no scope, which is exactly why they fell back to rank. Closing the gap needs new scopes.

Granularity decision: **hybrid** — per-domain scopes where a custom role would plausibly differentiate (data tables, knowledge bases, AI gateway, project create, deployment view, workspace management/membership); trivial workspace-scoped tag/list reads fold into a single `WORKSPACE_VIEW`.

## 3. New `PermissionScope` values (10)

Appended at the **end** of the enum (ordinals are pinned; JDBC stores enums as INT ordinals — never reorder).

| Scope | Meaning |
|---|---|
| `WORKSPACE_VIEW` | read the workspace, its non-sensitive settings, member list, and misc workspace-scoped tag/list reads (asset files, API collections) |
| `WORKSPACE_MANAGE` | update the workspace entity + sensitive settings (git config incl. secrets, AI Hub settings) |
| `WORKSPACE_MEMBER_MANAGE` | add / remove / change-role of members |
| `DATA_TABLE_VIEW` | read data tables / tags |
| `DATA_TABLE_CREATE` | create data tables |
| `KNOWLEDGE_BASE_VIEW` | read knowledge bases / tags |
| `KNOWLEDGE_BASE_CREATE` | create knowledge bases |
| `AI_GATEWAY_VIEW` | read the AI gateway / observability / eval / prompt surface |
| `DEPLOYMENT_VIEW` | read deployments / tags (rounds out the existing `DEPLOYMENT_PUSH`/`DEPLOYMENT_PULL`) |
| `PROJECT_CREATE` | create / import a project (fills a genuine model gap — only `PROJECT_SETTINGS`/`PROJECT_DELETE` existed) |

## 4. `BuiltInRoleScopes` tier assignment

Preserves `VIEWER ⊂ EDITOR ⊂ ADMIN`. Because every new VIEW scope goes in the VIEWER tier, the weakest built-in role keeps everything it has today → **no built-in user loses access**. The only behavioral change is additive: custom-role members gain access they were previously (wrongly) denied.

- **VIEWER delta (+):** `WORKSPACE_VIEW`, `DATA_TABLE_VIEW`, `KNOWLEDGE_BASE_VIEW`, `AI_GATEWAY_VIEW`, `DEPLOYMENT_VIEW`
- **EDITOR delta (+):** `DATA_TABLE_CREATE`, `KNOWLEDGE_BASE_CREATE`, `PROJECT_CREATE`
- **ADMIN:** `WORKSPACE_MANAGE`, `WORKSPACE_MEMBER_MANAGE` arrive automatically via `EnumSet.allOf` (never added to VIEWER/EDITOR deltas — "admin-only" is expressed by tier omission, not a separate mechanism)

## 5. Token collapse (resource-typed `targetType`)

`targetType` becomes the pure resource type for the fixed singleton resources; the `permission` slot always carries the scope.

| Old token | New token | PermissionService method (unchanged) |
|---|---|---|
| `'WorkspaceScope'` and `'WorkspaceRole'` | `'Workspace'` | `hasWorkspaceScope(id, value)` |
| `'ProjectScope'` | `'Project'` | `hasWorkspaceScopeForProject(id, value)` |
| `'WorkflowScope'` | `'Workflow'` | `hasWorkflowScope(stringId, value)` |
| `'<Type>:ResourceScope'` | *unchanged* | `hasResourceScope(type, id, value)` |
| `'<Type>:ResourceRole'` | *unchanged* | `hasResourceRole(type, id, value)` |

The dynamic `<Type>:ResourceScope` / `<Type>:ResourceRole` tokens keep their suffix: a single type (e.g. `KnowledgeBase`) may be checked by *role* while `ApiKey` is checked by *scope*, so the kind genuinely disambiguates the routing method. Only the fixed singletons (`Workspace`/`Project`/`Workflow`) shed the suffix.

### Evaluator changes (`AutomationPermissionEvaluator`)

- Constants: `WORKSPACE_SCOPE` → `WORKSPACE` (`"Workspace"`), `PROJECT_SCOPE` → `PROJECT` (`"Project"`), `WORKFLOW_SCOPE` → `WORKFLOW` (`"Workflow"`); **remove** `WORKSPACE_ROLE`.
- String-id branch: `case WORKFLOW -> hasWorkflowScope(stringId, value)`.
- Numeric-id plain switch: `case WORKSPACE -> hasWorkspaceScope`, `case PROJECT -> hasWorkspaceScopeForProject`; **remove** the `WORKSPACE_ROLE` case.
- `isSkipChecks()` chokepoint and prefix-token (`ResourceScope`/`ResourceRole`) handling are unchanged. No new SpEL root built-ins (consistent with the prior decision to keep value-carrying checks on the evaluator so the skip bypass stays centralized).

## 6. Per-gate mapping (the 45 `WorkspaceRole` gates)

VIEWER reads:

| Method | → scope |
|---|---|
| `ProjectWorkflowExecutionFacadeImpl.getWorkflowExecutions` | `EXECUTION_VIEW` (existing) |
| `ProjectFacadeImpl.getWorkspaceProjects`, `getWorkspaceProjectWorkflows`; `ProjectTagFacadeImpl.getProjectTags` | `WORKFLOW_VIEW` (existing) |
| `WorkspaceConnectionFacadeImpl.getConnectionTags` | `CONNECTION_VIEW` (existing) |
| `WorkspaceMcpServerFacadeImpl.getWorkspaceMcpProjects`, `getWorkspaceMcpServers`, `getWorkspaceMcpServerTags` | `AGENT_VIEW` (existing) |
| `WorkspaceDataTableFacadeImpl.getDataTableTags`, `listTables` | `DATA_TABLE_VIEW` |
| `WorkspaceKnowledgeBaseFacadeImpl.getKnowledgeBaseTags`, `getWorkspaceKnowledgeBases` | `KNOWLEDGE_BASE_VIEW` |
| `ProjectDeploymentFacadeImpl.getProjectDeploymentTags`, `getWorkspaceProjectDeployments` | `DEPLOYMENT_VIEW` |
| `AiPromptFacadeImpl.getPromptsByWorkspace`; `AiGatewayWorkspaceSettingsFacadeImpl.findByWorkspaceId`; `AiGatewayRequestLogFacadeImpl.getRequestLogsByWorkspace`; `AiEvalRuleFacadeImpl.getEvalRulesByWorkspace`; `AiEvalScoreConfigFacadeImpl.getScoreConfigsByWorkspace`; `AiEvalScoreFacadeImpl.getScoresByWorkspace`; `AiObservability{AlertRule,Trace,WebhookSubscription,NotificationChannel,Session,ExportJob}FacadeImpl.get*ByWorkspace` | `AI_GATEWAY_VIEW` |
| `WorkspaceServiceImpl.getWorkspace`; `WorkspaceUserServiceImpl.getWorkspaceWorkspaceUsers`; `ApiCollectionFacadeImpl.getApiCollectionTags`; `AssetFileTagServiceImpl.getAllTags`; `AiHubWorkspaceSettingsFacadeImpl.findByWorkspaceId`; `ProjectGitApiController.getWorkspaceProjectGitConfigurations` | `WORKSPACE_VIEW` |

EDITOR creates:

| Method | → scope |
|---|---|
| `WorkspaceDataTableFacadeImpl.createTable` | `DATA_TABLE_CREATE` |
| `WorkspaceKnowledgeBaseFacadeImpl.createWorkspaceKnowledgeBase` | `KNOWLEDGE_BASE_CREATE` |
| `WorkspaceMcpServerFacadeImpl.createWorkspaceMcpServer` | `AGENT_CREATE` (existing) |
| `ProjectFacadeImpl.createProject`, `importProject`, `importProjectTemplate` | `PROJECT_CREATE` |

ADMIN:

| Method | → scope |
|---|---|
| `WorkspaceServiceImpl.update`; `GitConfigurationFacadeImpl.fetchGitConfiguration`, `save`; `AiHubWorkspaceSettingsFacadeImpl.updateVoiceWebhookUrl` | `WORKSPACE_MANAGE` |
| `WorkspaceUserServiceImpl.addWorkspaceUser`, `removeWorkspaceUser`, `updateWorkspaceUserRole` | `WORKSPACE_MEMBER_MANAGE` |

Note `fetchGitConfiguration` is an ADMIN-gated *read* (git config holds secrets) → `WORKSPACE_MANAGE`, not `WORKSPACE_VIEW`, to preserve admin-only access. The separate VIEWER-gated `getWorkspaceProjectGitConfigurations` (non-sensitive per-project listing) maps to `WORKSPACE_VIEW`.

The 6 pre-existing `'WorkspaceScope'` gates keep their scope value and only lose the suffix (`'WorkspaceScope'/'X'` → `'Workspace'/'X'`).

## 7. Custom roles & client

- New scopes become custom-role-assignable automatically server-side (`CustomRoleScopeResolver` resolves from the custom role's `PermissionScope` set). Verify the client custom-role editor enumerates `PermissionScope` dynamically (so new scopes appear) vs. a hardcoded list.
- CE behavior unchanged: the CE `PermissionService` impl is permissive for these checks; this design only touches EE routing/scopes.

## 8. Testing

- `BuiltInRoleScopesTest` (if present) — assert the new VIEW scopes land in VIEWER, CREATE in EDITOR, MANAGE only in ADMIN; assert subset invariant still holds.
- `PermissionScope` ordinal-stability test — extend the pin with the 10 appended values (append only).
- `PreAuthorizeAnnotationTest` / the per-module `*AuthorizationTest` reflection tests — update expected strings to the new `'Workspace'/'Project'/'Workflow'` tokens + scope values.
- `PreAuthorizeProxyEnforcementIntTest` — add/adjust fixtures for a `'Workspace'/'WORKSPACE_MEMBER_MANAGE'` admin gate and a `'Workspace'/'KNOWLEDGE_BASE_VIEW'` viewer gate; assert deny for a non-member and a positive control.
- Add a regression test proving a **custom-role member** with `KNOWLEDGE_BASE_VIEW` now passes a `'Workspace'/'KNOWLEDGE_BASE_VIEW'` gate (the bug this fixes).

## 9. Rollout (commit plan)

1. Add the 10 `PermissionScope` values + `BuiltInRoleScopes` tier deltas + ordinal-pin/tier tests. (Self-contained; no annotation changes yet.)
2. Evaluator token rename: add `WORKSPACE`/`PROJECT`/`WORKFLOW`, keep old `*Scope` tokens temporarily as aliases routing to the same methods, drop `WORKSPACE_ROLE` routing only after step 3. (Allows incremental annotation migration without a flag day.)
3. Migrate annotations per the table (regex-driven, per module) + update each module's pinning test in the same commit.
4. Once no annotation references the old tokens, remove the alias cases and `hasWorkspaceRole` from the SpEL routing surface (keep the `PermissionService.hasWorkspaceRole` method + `WorkspaceRole` enum — still the membership/scope-set source).

## 10. Risks & non-goals

- **Risk:** an ADMIN-gated *read* mis-mapped to a VIEW scope would leak (e.g. git secrets). Mitigated by routing `fetchGitConfiguration`/settings reads to `WORKSPACE_MANAGE`.
- **Risk:** silent semantic drift if a new VIEW scope is accidentally omitted from the VIEWER tier — caught by the tier test.
- **Non-goal:** unifying the dynamic `<Type>:ResourceScope`/`:ResourceRole` tokens (kept as-is — the kind suffix is load-bearing there).
- **Non-goal:** moving these checks to SpEL root built-ins (rejected earlier to keep `isSkipChecks()` centralized in the evaluator).
- **`WorkspaceRole` enum is retained** — it remains the per-membership role and the built-in scope-set source via `BuiltInRoleScopes`; only its use as an *authorization check* is removed.

## 11. Extension: `ResourceScope` and `ResourceRole` (grand unification)

Usage audit:

- **`ResourceScope`** — 2 types, already capability checks: `ApiKey`→`API_KEY_DELETE`; `Connection`→`CONNECTION_VIEW/EDIT/USE/DELETE`. ~18 gates. No semantic issue, no new scopes.
- **`ResourceRole`** — 9 collaborative types, ~115 gates, all `VIEWER`/`EDITOR`: `DataTable`, `Job`, `KnowledgeBase`, `McpServer`, `McpProject`, `McpComponent`, `McpProjectWorkflow`, `McpTool`, `ProjectDeployment`. **Same rank-vs-custom-role bug as `WorkspaceRole`** (`hasResourceRole` resolves the owning workspace and rank-compares the caller's *workspace role*, excluding custom-role members) — and it's the larger instance.

Key fact: **no resource type uses both kinds.** `ApiKey`/`Connection` are scope-only; the 9 collaborative types are role-only. So eliminating `ResourceRole` (convert to `hasResourceScope`) leaves every dynamic type on a single mechanism, after which the `:ResourceScope` suffix is redundant.

`hasResourceRole` and `hasResourceScope` share the same resolver registry and the same owning-workspace resolution — they differ only in role-rank vs scope-lookup. Converting a gate is therefore "pick the scope the role stood in for"; the plumbing is identical.

### Unified target

- One shape for every gate: `hasPermission(#id, '<Type>', '<SCOPE>')`.
- Evaluator routing: `type ∈ {Workspace, Project, Workflow}` → dedicated `PermissionService` method; else → resolver registry → `hasResourceScope(type, id, scope)`. The `:` parsing and scope-vs-role dispatch are removed.
- `WorkspaceRole` and `ResourceRole` both eliminated as checks. `isCurrentUser`/`isTenantAdmin`/`isResourceOwner` remain value-less built-ins.

### `ResourceRole → scope` mapping (decided)

| Type | VIEWER → | EDITOR → |
|---|---|---|
| `DataTable` | `DATA_TABLE_VIEW` | `DATA_TABLE_EDIT` *(new)* |
| `KnowledgeBase` | `KNOWLEDGE_BASE_VIEW` | `KNOWLEDGE_BASE_EDIT` *(new)* |
| `Job` (= workflow execution) | `EXECUTION_VIEW` | `EXECUTION_DELETE` *(new; only gate is `deleteLogEntries`)* |
| `ProjectDeployment` | `DEPLOYMENT_VIEW` | `DEPLOYMENT_EDIT` *(new; lifecycle: create/enable/update/delete — NOT `DEPLOYMENT_PUSH`, which is version publish)* |
| `McpServer`/`McpProject`/`McpProjectWorkflow`/`McpTool`/`McpComponent` | `MCP_VIEW` *(new)* | `MCP_EDIT` *(new)* |

**Decisions locked:**
- `Job` is workflow execution. VIEWER reads (logs, execution/task-execution detail) → `EXECUTION_VIEW`. The lone EDITOR gate `deleteLogEntries` → new `EXECUTION_DELETE` (no execution-delete scope existed; deletion ≠ `EXECUTION_DATA` view).
- `ProjectDeployment` EDITOR is full lifecycle management → new `DEPLOYMENT_EDIT`. `DEPLOYMENT_PUSH`/`PULL` are reserved for version publish/fetch.
- Mcp* uses a **common** scope family (`MCP_VIEW`/`MCP_CREATE`/`MCP_EDIT`) shared across all five Mcp types — not per-type. Kept distinct from `AGENT_*` (so a custom role can grant MCP without agents; `AGENT_*` reverts to agents-only). **Phase A consequence:** the WorkspaceMcpServer gates re-map off `AGENT_*` — `getWorkspaceMcpServers`/`getWorkspaceMcpProjects`/`getWorkspaceMcpServerTags` → `MCP_VIEW`; `createWorkspaceMcpServer` → `MCP_CREATE` *(new)*.

### Additional new scopes (Phase B + Phase-A MCP re-map)

`DATA_TABLE_EDIT`, `KNOWLEDGE_BASE_EDIT`, `EXECUTION_DELETE`, `DEPLOYMENT_EDIT`, `MCP_VIEW`, `MCP_CREATE`, `MCP_EDIT` — 7 scopes.

Combined A+B new-scope total: ~17 (custom-role editor will surface ~33 scopes overall).

### Tier assignment for Phase B scopes (`BuiltInRoleScopes`)

- **VIEWER delta (+):** `MCP_VIEW` (resource VIEWER gates are workspace-member reads). `DATA_TABLE_VIEW`/`KNOWLEDGE_BASE_VIEW`/`EXECUTION_VIEW`/`DEPLOYMENT_VIEW` already in VIEWER from Phase A / pre-existing.
- **EDITOR delta (+):** `DATA_TABLE_EDIT`, `KNOWLEDGE_BASE_EDIT`, `EXECUTION_DELETE`, `DEPLOYMENT_EDIT`, `MCP_CREATE`, `MCP_EDIT`.
- **ADMIN:** via `allOf`.

This preserves current behavior (resource VIEWER gates ⇒ member-level read, EDITOR ⇒ editor-level mutate) while making each capability independently grantable to a custom role.

### Phasing

- **Phase A** (this spec, §1–10): `WorkspaceRole`→scope + `Workspace`/`Project`/`Workflow` token collapse. 10 new scopes.
- **Phase B**: `ResourceRole`→`hasResourceScope` conversion for the 9 collaborative types (+ ~3 new `*_EDIT` scopes). ~115 gates. Fixes the custom-role bug for collaborative resources.
- **Phase C**: collapse `'<Type>:ResourceScope'` → bare `'<Type>'` (safe once `ResourceRole` is gone). ~133 gates token rename; removes `:` parsing from the evaluator.

Phases B and C may be combined. Phase A is independently shippable.
