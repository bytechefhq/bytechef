# Workflow-Agent Auto-Memory — Design

Date: 2026-06-05
Status: Proposed
Branch: `claude/quizzical-driscoll-8d47a4`

## Problem

`AutoMemoryTools` (`com.bytechef.platform.ai.agent.memory`, CE/Apache) is a plain
`@Tool`-annotated POJO that gives an agent persistent memory (view/create/replace/insert/
delete/rename). Today it is wired only in the EE AI Hub agent, backed by DB adapters that
scope memory to `(workspaceId, userId, environment)` pulled from AI-Hub-specific `ToolContext`
keys.

We want to expose it in the **CE `aiAgentUtils`** component so the **workflow agent** can use
it. The blocker is **scoping**: the `AiAutoMemoryService` contract requires a non-null
`long userId`, but a workflow run has **no user** — it is owned by a *project deployment*
(`ActionContextAware.getJobPrincipalId()`), not a person. Stuffing a deployment id into the
`user_id` column is unsafe because `ai_auto_memory` is shared with AI Hub (which stores real
user ids there); deployment-id and user-id spaces could numerically collide within the same
workspace/environment/name and cross a workflow's memory with a user's.

## Goals

- Let the CE workflow agent read/write persistent auto-memory, isolated per **project
  deployment**.
- Guarantee workflow (deployment-owned) memory can **never** collide with AI Hub
  (user-owned) memory, even with identical numeric ids.
- Reuse the existing CE `AiAutoMemoryService` persistence; no second memory store.

## Non-goals (v1)

- **Embedded** platform support. Embedded runs have no `workspace_id` derivation path
  (`jobPrincipalId` is an integration-instance id, not a deployment→project→workspace chain).
  In embedded/editor contexts the tool is **inert** (returns a clear "auto-memory unavailable"
  message) rather than mis-scoping.
- Streaming, auth, or any change to the `AutoMemoryTools` POJO itself.
- A management UI for deployment-owned memory.

## Design

### 1. Owner discriminator on `AiAutoMemory`

Turn `ai_auto_memory` from a single-owner-axis table (always a user) into a
polymorphic-owner table, using the discriminator-on-the-key pattern already used by
`ConversationKind` on `ai_hub_task`.

New enum (CE, in `platform-ai-auto-memory-api`):

```java
public enum AiAutoMemoryPrincipalType {
    USER,        // ordinal 0 — AI Hub per-user memory (existing behavior)
    DEPLOYMENT   // ordinal 1 — workflow-agent memory, owned by a project deployment
}
```

- INT ordinal, **append-only**, pinned by `EnumOrdinalStabilityTest`
  (`testAiAutoMemoryPrincipalTypeOrdinalsAreStable`). `USER = 0` so every existing row
  backfills to USER with a column default — no data-migration query.

Liquibase follow-up migration on `ai_auto_memory`:

- Rename column `user_id` → `principal_id` (it now holds a user id **or** a deployment id;
  leaving it named `user_id` is a future footgun — someone will join it to the `user` table).
- Add `principal_type INT NOT NULL DEFAULT 0`.
- Replace index `idx_ai_auto_memory_user_env` `(user_id, environment, memory_type)` with
  `idx_ai_auto_memory_principal_env` `(principal_type, principal_id, environment, memory_type)`.
- The `workspace_ai_auto_memory` relation table is unaffected (it keys on
  `ai_auto_memory_id`).
- There is **no DB unique constraint** to change — uniqueness is enforced in
  `AiAutoMemoryServiceImpl` (see §2).

Entity (`AiAutoMemory`, Spring Data JDBC):

- Rename field `userId` → `principalId` (`@Column("principal_id")`), keep the bind-once
  constructor + package-private setter pattern.
- Add `@Column("principal_type") private int principalType;` with a bounds-checked
  `getPrincipalType()`/`setPrincipalType()` converting the ordinal to/from
  `AiAutoMemoryPrincipalType` (same pattern as `memoryType`).

### 2. Service contract

Thread `AiAutoMemoryPrincipalType principalType` through every `AiAutoMemoryService` method as
part of the ownership key (explicit required param — no overloads that default to USER, which
would risk a silent wrong-owner write). `principalId` replaces `userId` in the signatures:

```java
AiAutoMemory create(long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId,
    int environment, String name, String title, @Nullable String description,
    AiAutoMemoryType memoryType, String content);
Optional<AiAutoMemory> read(long workspaceId, AiAutoMemoryPrincipalType principalType,
    long principalId, int environment, String name);
// update / updateById / delete / deleteById / rename / list / findById /
// listByPrincipalAndWorkspace — same treatment.
```

- `AiAutoMemoryServiceImpl.create()`'s duplicate-name check now includes `principalType`, so a
  USER and a DEPLOYMENT may share a memory name without colliding.
- Ownership errors keep their existing not-found shape (anti-enumeration).

### 3. Repository

Derived queries gain the discriminator and the rename:

```java
List<AiAutoMemory> findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentOrderByUpdatedAtDesc(
    long workspaceId, int principalType, long principalId, int environment);
List<AiAutoMemory> findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndMemoryTypeOrderByUpdatedAtDesc(
    long workspaceId, int principalType, long principalId, int environment, int memoryType);
List<AiAutoMemory> findAllByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndName(
    long workspaceId, int principalType, long principalId, int environment, String name);
```

The JDBC implementations keep their explicit `JOIN workspace_ai_auto_memory` SQL, with
`m.user_id` → `m.principal_id` and an added `m.principal_type = :principalType` predicate.

### 4. CE backing adapters

Add CE implementations of the two SPIs (`MemoryResourceResolver`, `AutoMemoryDirectoryOps`),
ported from the EE `Db*` adapters (which already call only CE service methods). Key
simplification: **scoping is fixed for the whole agent run**, so the adapters are constructed
with concrete `(workspaceId, principalType=DEPLOYMENT, principalId, environment)` values and
ignore the per-call `ToolContext` — no AI-Hub-style ToolContext-key extraction needed.

- `CeMemoryResourceResolver(AiAutoMemoryService, workspaceId, principalId, environment)` →
  returns a `WritableResource` (port `DbMemoryResource`) bound to those fixed values, with
  `principalType = DEPLOYMENT`.
- `CeAutoMemoryDirectoryOps(AiAutoMemoryService, workspaceId, principalId, environment)` →
  list/exists/delete/rename delegating with the fixed values.
- Move/relocate `AutoMemoryFrontmatter` (zero EE deps, pure frontmatter render/parse) to a CE
  home shared by both EE and CE adapters, or duplicate minimally if a shared module is
  undesirable.

### 5. CE tool (cluster element)

`AiAgentUtilsAgentClientTool`-style wrapper, `AiAgentUtilsAutoMemoryTool`, in `aiAgentUtils`:

- `.type(TOOLS)`, handler-constructed (injects `AiAutoMemoryService` + the services needed for
  the workspace-id hop — `ProjectDeploymentService`, `ProjectService`).
- `apply(inputParameters, connectionParameters, context)` resolves scoping **once** from the
  agent `context` (cast to `ActionContextAware`):
  - `platformType = getPlatformType()` — if not `AUTOMATION` (or workspace can't be derived) →
    return an inert provider (no memory tools) so the agent degrades gracefully.
  - `principalId = getJobPrincipalId()` (the project deployment id).
  - `environment` = `Environment` ordinal derived from `getEnvironmentId()` (default
    `DEVELOPMENT = 0` when null/editor — **verify** whether `getEnvironmentId()` is already the
    ordinal or an entity id needing a lookup).
  - `workspaceId` via job metadata if present, else
    `projectService.getProject(projectDeploymentService.getProjectDeployment(principalId).getProjectId()).getWorkspaceId()`.
  - Build `new AutoMemoryTools(ceResolver, ceDirectoryOps)` and return
    `ToolCallbackProvider.from(ToolCallbacks.from(autoMemoryTools))`.
- Register in `AiAgentUtilsComponentHandler`; regenerate `ai_agent-utils_v1.json`.
- New module dependency: `aiAgentUtils` → automation-configuration api (for the workspace
  hop). If that coupling is undesirable, the hop can move behind a small CE
  `WorkflowWorkspaceResolver` SPI.

### 6. Caller migration

All existing `AiAutoMemoryService` callers pass `USER`:

| Caller | Edition | Change |
| --- | --- | --- |
| `AiAutoMemoryGraphQlController` | CE | pass `USER` + `principalId = currentUser.id` |
| `DbMemoryResource` (AI Hub) | EE | pass `USER` |
| `DbAutoMemoryDirectoryOps` (AI Hub) | EE | pass `USER` |
| `AiAutoMemoryServiceTest` | CE | pass `USER` |
| `DbAutoMemorySeamTest` | EE | pass `USER` |
| **new** CE workflow adapters | CE | pass `DEPLOYMENT` |

## Testing

- `EnumOrdinalStabilityTest` pin for `AiAutoMemoryPrincipalType` (USER=0, DEPLOYMENT=1).
- Service tests: a USER row and a DEPLOYMENT row with identical
  `(workspaceId, principalId, environment, name)` coexist and never read each other.
- Repository tests for the new derived queries (JOIN + principal_type predicate).
- CE adapter tests: fixed-scope construction; resolve/list/delete/rename round-trip against a
  stubbed `AiAutoMemoryService`.
- `aiAgentUtils` snapshot regen; handler test updated for the new constructor dependency.

## Open questions / risks

- **`getEnvironmentId()` semantics**: confirm it maps to the `Environment` ordinal (0/1/2) the
  memory column expects, vs. an entity id needing conversion. Default `DEVELOPMENT` when
  null/editor.
- **`aiAgentUtils` → automation-configuration dependency** for the workspace hop. Acceptable,
  or hide behind a CE resolver SPI?
- **`AutoMemoryFrontmatter` location**: relocate to a shared CE module vs. duplicate.
- Liquibase: this is a follow-up migration that renames a column + index; ensure no stale
  build/resources copies (per the migration-rename lesson) and that the init file is not
  edited in place.

## Out of scope

Embedded platform, streaming, auth, management UI for deployment memory, and any cross-run
(shared) memory semantics. v1 is per-deployment, AUTOMATION-only, isolated by `principal_type`.
