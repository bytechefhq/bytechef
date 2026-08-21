# Personal Agent — attach all AI Hub composer resource kinds

**Date:** 2026-05-21
**Status:** Design
**Area:** EE — AI Hub Personal Agents

## 1. Problem

The New/Edit Personal Agent form (`AiHubPersonalAgentForm`) lets the user attach only
**Tools**. The AI Hub composer's "Search resources…" dropdown, by contrast, lets a user
reference **nine** resource kinds into a conversation: Workflows, Files, Data Tables,
Knowledge Bases, Tools, Workflow Executions, MCP Servers, API Collections, and Previous
Tasks.

A personal agent should be able to pre-declare any of those nine kinds, so every task
spawned from the agent starts with those resources attached — exactly the way the agent's
tool template is copied onto every spawned task today.

## 2. Goals

- Extend the agent form so it can attach all **9** composer resource kinds (Tools already
  works; this adds the other 8).
- When a task is spawned from the agent, auto-attach the agent's resources to that task —
  parity with the existing tool-template copy.
- Present the picker as a single unified, composer-style nested dropdown rather than nine
  separate cards.
- Extract the composer's resource-picker dropdown into a shared component reused by both
  the composer and the agent form.

## 3. Non-goals

- **Composer manual-@-mention persistence for the 4 non-tabbable kinds.** Making the
  composer persist manually @-mentioned MCP servers / API collections / workflow executions
  / previous tasks as artifacts would require new right-panel tab kinds and viewer panels
  (the tab store has only `file`/`workflow`/`dataTable`/`knowledgeBase` today). That is a
  composer enhancement independent of personal agents and is left to a separate follow-up
  spec. The agent feature does not need it — see §6.
- **Per-resource configuration.** Tools carry a pinned `connectionId` and a pre-set
  `parameters` map. The 8 reference-kinds are plain `(kind, id, name)` references with no
  connection or parameter surface; no config UI is added for them.
- **Renaming agents, sharing agents, or any change to the schedule tab.**

## 4. Decisions (locked during brainstorming)

| Decision | Choice |
|----------|--------|
| Which kinds | All 8 non-tool kinds (Workflows, Files, Data Tables, Knowledge Bases, MCP Servers, API Collections, Workflow Executions, Previous Tasks). |
| Form layout | One unified card with a single "Add" button opening a composer-style nested dropdown. |
| Task-spawn wiring | Server copy — parity with tools. The agent's resources are copied onto each spawned task as `ai_hub_task_artifact` rows. |
| Picker reuse | Extract a shared `ResourcePickerMenu` from the composer, used by both the composer and the agent form. |
| §9 composer tabs/viewers | Out of scope — separate follow-up. |

> Note on "Previous Tasks": an AI Hub *task* is a chat conversation/thread. Pinning a
> specific past conversation onto a reusable agent template is the weakest conceptual fit
> of the eight kinds, but it is supported for completeness and uniformity.

## 5. Architecture

The personal agent already follows a **template → copy** pattern: `ai_hub_personal_agent_tool`
rows are copied into `ai_hub_task_tool` whenever a task is spawned
(`AiHubTaskServiceImpl.createAiHubPersonalAgentChat` → `copyAgentToolTemplate`). This design
replicates that pattern for resources with a parallel table and a parallel copy step,
rather than inventing a new mechanism.

Tools and the 8 reference-kinds are kept on separate rails. A tool row is a
`(componentName, componentVersion, operationName)` triple plus optional connection and
parameters; a reference-kind row is a flat `(kind, resourceId, resourceName)`. Merging them
into one table or one picker would require pervasive conditional handling, so:

- Tools stay in `ai_hub_personal_agent_tool` (unchanged) and copy into `ai_hub_task_tool`.
- The 8 reference-kinds get a new `ai_hub_personal_agent_resource` table and copy into
  `ai_hub_task_artifact`.

### 5.1 Server — persistence

New EE entity and table **`ai_hub_personal_agent_resource`** (module
`automation-ai-hub-service`):

| Column | Type | Notes |
|--------|------|-------|
| `id` | PK | |
| `ai_hub_personal_agent_id` | FK → `ai_hub_personal_agent` | `ON DELETE CASCADE` — rows live as long as the agent. |
| `kind` | INT ordinal | `AiHubPersonalAgentResourceKind` enum. |
| `resource_id` | VARCHAR | The referenced entity's id. VARCHAR because composer resource ids are strings and workflow ids are non-numeric — uniform string storage avoids per-kind columns. |
| `resource_name` | VARCHAR | Display-name snapshot. The composer's `ReferencedResourceI` already carries `name`; snapshotting avoids N live lookups when rendering the form. |
| `created_at` | timestamp | |

- Unique constraint `(ai_hub_personal_agent_id, kind, resource_id)` — makes `addResource`
  idempotent, mirroring the tool table's behaviour.
- New enum **`AiHubPersonalAgentResourceKind`** in `platform-ai-hub-api`:
  `WORKFLOW, FILE, DATA_TABLE, KNOWLEDGE_BASE, MCP_SERVER, API_COLLECTION,
  WORKFLOW_EXECUTION, TASK`. Persisted as INT ordinals; **append-only**; pinned by
  `EnumOrdinalStabilityTest`.
- New API entity `AiHubPersonalAgentResource` in `platform-ai-hub-api`.
- New `AiHubPersonalAgentResourceRepository` (Spring Data JDBC) in `automation-ai-hub-service`.
- New service methods on `AiHubPersonalAgentService` / `AiHubPersonalAgentServiceImpl`:
  `listResources(agentId)`, `addResource(agentId, workspaceId, userId, kind, resourceId,
  resourceName)`, `removeResource(resourceId, workspaceId, userId)`.
- New Liquibase changelog under
  `.../changelog/automation/aihub/` (e.g. `20260521000001_ai_hub_personal_agent_resource.xml`),
  registered in the aihub master changelog.

The `ai_hub_personal_agent_tool` table and entity are **unchanged**.

### 5.2 Server — task-spawn copy

`AiHubTaskArtifactKind` (in `platform-ai-hub-api`) currently has four `*_REFERENCED` values:
`FILE_REFERENCED, WORKFLOW_REFERENCED, DATA_TABLE_REFERENCED, KB_REFERENCED`. Extend it with
four appended values so all eight kinds have an artifact kind:

- `MCP_SERVER_REFERENCED`
- `API_COLLECTION_REFERENCED`
- `WORKFLOW_EXECUTION_REFERENCED`
- `TASK_REFERENCED`

All four are `reversible = false`, matching the existing `*_REFERENCED` block (un-referencing
is UI bookkeeping, not a side effect to undo). They are appended at the **end** of the enum
to preserve ordinal stability; `EnumOrdinalStabilityTest#testTaskArtifactKindOrdinals` is
updated to pin the new ordinals.

In `AiHubTaskServiceImpl.createAiHubPersonalAgentChat`, after the existing
`copyAgentToolTemplate` call, add `copyAgentResourceTemplate(taskId, aiHubPersonalAgentId)`:

- Reads the agent's `ai_hub_personal_agent_resource` rows.
- For each, writes one `ai_hub_task_artifact` row, mapping
  `AiHubPersonalAgentResourceKind` → `AiHubTaskArtifactKind` (`WORKFLOW → WORKFLOW_REFERENCED`,
  `MCP_SERVER → MCP_SERVER_REFERENCED`, etc.), carrying `resource_id` and `resource_name`.
- Uses the same resilience pattern as `copyAgentToolTemplate`: `ObjectProvider`-guarded
  dependency lookups so a deployment without the relevant beans degrades to a no-op, and
  per-row try/catch so one stale resource does not abort task creation.

This is a snapshot: editing the agent's resources later does not rewrite artifact rows on
already-spawned tasks.

### 5.3 Server — GraphQL

In `ai-hub-personal-agent.graphqls`:

```graphql
enum AiHubPersonalAgentResourceKind {
    WORKFLOW
    FILE
    DATA_TABLE
    KNOWLEDGE_BASE
    MCP_SERVER
    API_COLLECTION
    WORKFLOW_EXECUTION
    TASK
}

type AiHubPersonalAgentResource {
    id: ID!
    aiHubPersonalAgentId: Long!
    kind: AiHubPersonalAgentResourceKind!
    resourceId: String!
    resourceName: String!
    createdAt: Long
}
```

- New field on `AiHubPersonalAgent`: `resources: [AiHubPersonalAgentResource!]!` (resolver
  returns an empty list, never null, matching the `tools` resolver).
- New mutations, mirroring the tool mutations:
  - `addAiHubPersonalAgentResource(input: AddAiHubPersonalAgentResourceInput!): AiHubPersonalAgentResource!`
    — idempotent (adding a duplicate returns the existing row).
  - `removeAiHubPersonalAgentResource(workspaceId: ID!, id: ID!): Boolean!`
    — idempotent (removing a non-existent id is a no-op). The `id` argument is the
    `ai_hub_personal_agent_resource` **row** id, not the referenced entity's id —
    deliberately named `id` (not `resourceId`) to avoid colliding with
    `AddAiHubPersonalAgentResourceInput.resourceId`, which carries the *referenced
    entity's* id.
- `AddAiHubPersonalAgentResourceInput { workspaceId: ID!, aiHubPersonalAgentId: ID!,
  kind: AiHubPersonalAgentResourceKind!, resourceId: String!, resourceName: String! }`
  — here `resourceId` is the referenced entity's id (a workflow id, file id, …).
- `AiHubPersonalAgentGraphQlController` gets the matching `@MutationMapping`s and a
  `resources` `@SchemaMapping`, each behind the existing
  `WorkspaceAccessGuard.verifyUserCanAccessWorkspace` check.
- `createAiHubPersonalAgentTask` is unchanged at the schema level — the resource copy
  happens inside `createAiHubPersonalAgentChat`.

### 5.4 Client — shared `ResourcePickerMenu`

The composer's "Search resources…" dropdown is a `Command`-based nested menu currently
inline inside the 71 KB `AiHubComposer.tsx`. Extract it into a shared component (proposed
location `client/src/pages/automation/ai-hub/resource-picker/`).

`ResourcePickerMenu` owns:

- The per-kind data queries — already factored into shared hooks
  (`useGetAssetFilesQuery`, `useDataTablesQuery`, `useKnowledgeBasesQuery`,
  `useWorkspaceMcpServersQuery`, `useGetApiCollectionsQuery`,
  `useGetWorkspaceProjectWorkflowExecutionsQuery`, `useAiHubTasksQuery`, and the
  `useAllWorkspaceWorkflows` helper).
- Debounced filtering, the `MenuPath` drill-down state, the workflow → project two-level
  grouping, and the "Show N more" pagination.

`ResourcePickerMenu` does **not** own @-mention behaviour, right-panel tab opening, or any
store writes — those stay caller concerns. It exposes a single callback,
`onSelect(kind, id, name)`; the workflow branch additionally passes `projectId` and
`projectWorkflowId` so a caller that wants to open a workflow tab still can.

**Tools stays per-context — it is not part of `ResourcePickerMenu`.** The composer attaches
tools through `ai_hub_task_tool` via `useAiHubTaskToolableComponentsQuery` + `TaskToolDialog`;
the agent form attaches through `ai_hub_personal_agent_tool` via `useGetComponentDefinitionsQuery`
+ a component → action drill-down. These are genuinely different flows, so each context
supplies its own Tools branch and `ResourcePickerMenu` covers only the 8 uniform
reference-kinds.

`AiHubComposer` is refactored to render `ResourcePickerMenu` + keep its own Tools branch
and its `handleSelect` / `handleSelectWorkflow` wiring. Composer behaviour must remain
identical; the composer's existing tests are the regression guard.

### 5.5 Client — agent form

`AiHubPersonalAgentToolsCard` is replaced by `AiHubPersonalAgentResourcesCard` ("Resources
this agent can use:"). A single "Add" button opens a menu listing all 9 kinds:

- **Tools** → the existing component → action drill-down plus the per-tool Configure dialog
  (`AiHubPersonalAgentToolConfigDialog`). Logic is unchanged, just relocated under the
  unified menu.
- **The 8 reference-kinds** → `ResourcePickerMenu` branches.

Added items render as a single list grouped by kind: each row shows a kind icon, the
resource name, and a remove control. Tool rows additionally keep their Configure entry.

Create / edit parity mirrors today's tool flow:

- **Create mode** — picks are collected in a local `pendingResources` state and bulk-attached
  after `createAiHubPersonalAgent` returns, in the same post-create loop as the existing
  `pendingTools`. Per-item failures aggregate into the existing partial-failure toast.
- **Edit mode** — picks fire `addAiHubPersonalAgentResource` / `removeAiHubPersonalAgentResource`
  directly against the persisted agent.

New GraphQL operation files under `client/src/graphql/ai/aihub/personal-agent/`
(`addAiHubPersonalAgentResource.graphql`, `removeAiHubPersonalAgentResource.graphql`); the
`aiHubPersonalAgent.graphql` query gains a `resources { ... }` selection. Run
`graphql-codegen` to regenerate `src/shared/middleware/graphql.ts`.

## 6. Surfacing the agent's resources in the LLM context (resolved)

The agent's resources must reach the spawned task's **LLM context**, not just the database.
This is solved **server-side, per turn** — no client-side hydration.

**How `referencedResources` reaches the LLM today.** `AiHubSpringAIAgent.createSystemMessage`
runs every turn and renders `state.get("referencedResources")` as a `Context("Referenced
Resources", …)` block (`AiHubSpringAIAgent.java:280-284`). The composer puts that list into
the AG-UI `State` per message; it being per-message is purely a client artifact.

**Mechanism — per-turn router overlay.** `AiHubRoutingAgent.applyAiHubPersonalAgentOverlay`
already runs every turn for `kind = PERSONAL_AGENT` tasks, resolves the agent via
`findOwned`, and writes the instructions/title state keys. It additionally:

1. calls `aiHubPersonalAgentService.listResources(agentId)` — a **live** read of the
   agent's current `ai_hub_personal_agent_resource` rows every turn (so editing the
   agent's resources affects in-flight conversations on the next turn, consistent with how
   the instructions overlay already behaves);
2. converts each `AiHubPersonalAgentResource` to the `{id, kind, name}` map shape the
   composer's `referencedResources` entries use, mapping `AiHubPersonalAgentResourceKind`
   (`WORKFLOW`, `DATA_TABLE`, …) to the composer's lowercase `ReferencedResourceKindType`
   strings (`workflow`, `dataTable`, …) via a small 8-case map;
3. merges them into the `state` `referencedResources` list — appending to whatever the
   client sent, deduped by `(kind, id)` so an @-mention of an already-pinned resource is
   not doubled.

**Rendering — no change to `AiHubSpringAIAgent`.** The existing `createSystemMessage`
rendering absorbs the merged list automatically; agent-pinned resources and per-message
@-mentions appear together in the one "Referenced Resources" block. The implementation
plan must confirm the exact key shape `formatReferencedResources`
(`AiHubSpringAIAgent.java:435`) expects and produce maps of that shape.

**Edge cases** (all mirror the existing instructions overlay): agent deleted mid-conversation
→ `findOwned` empty → no-op; agent has no resources → empty list → nothing appended.

**Relationship to the task-artifact copy (§5.2).** The `copyAgentResourceTemplate` copy
into `ai_hub_task_artifact` is **independent of this path and unchanged** — it serves the
right-panel artifact *sidebar* (which renders `ai_hub_task_artifact` rows). The LLM-context
path reads the agent's resources live and never consults the copied artifacts. Both
consumers are legitimate; the copy is not redundant.

## 7. Testing

**Server**

- `AiHubPersonalAgentServiceTest` — add / list / remove resources, including the
  idempotent-add and no-op-remove paths.
- `EnumOrdinalStabilityTest` — pin the 4 new `AiHubTaskArtifactKind` ordinals and the new
  `AiHubPersonalAgentResourceKind` ordinals.
- `AiHubTaskServiceImpl` test — `createAiHubPersonalAgentChat` copies resource rows into
  `ai_hub_task_artifact`; a stale resource is skipped without aborting task creation.
- `AiHubPersonalAgentGraphQlController` wiring test for the new mutations and resolver.
- `AiHubRoutingAgent` overlay test (§6) — the agent's resources merge into the `state`
  `referencedResources` list, dedup against client-sent entries works, and the overlay is a
  no-op when the agent has no resources.

**Client**

- `AiHubPersonalAgentResourcesCard` — add / remove per kind, pending (create) vs persisted
  (edit) flows.
- `ResourcePickerMenu` — per-kind list rendering, filtering, workflow → project drill-down.
- The composer's existing tests must still pass after the `ResourcePickerMenu` extraction.

## 8. Affected modules

**Server (EE)**

- `platform-ai-hub-api` — `AiHubPersonalAgentResource`, `AiHubPersonalAgentResourceKind`,
  `AiHubTaskArtifactKind` (+4 values), `AiHubPersonalAgentService` interface.
- `automation-ai-hub-service` — `AiHubPersonalAgentResourceRepository`,
  `AiHubPersonalAgentServiceImpl`, `AiHubTaskServiceImpl`, `AiHubRoutingAgent` (§6 overlay),
  Liquibase changelog.
- `automation-ai-hub-graphql` — `ai-hub-personal-agent.graphqls`,
  `AiHubPersonalAgentGraphQlController`.

**Client**

- `pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx` (new).
- `pages/automation/ai-hub/composer/AiHubComposer.tsx` (refactored to use the shared menu).
- `pages/automation/ai-hub/personal-agents/AiHubPersonalAgentResourcesCard.tsx`
  (replaces `AiHubPersonalAgentToolsCard.tsx`).
- `pages/automation/ai-hub/personal-agents/AiHubPersonalAgentForm.tsx`,
  `hooks/useAiHubPersonalAgents.ts`.
- `graphql/ai/aihub/personal-agent/` — new operation files; `graphql.ts` regenerated.

## 9. Follow-ups (out of scope)

- Composer right-panel tabs and viewer panels for `workflowExecution`, `mcpServer`,
  `apiCollection`, and `task`, plus extending `useRecordReferencedArtifacts` so manual
  composer @-mentions of those kinds persist as artifacts. The artifact enum kinds added in
  §5.2 already exist, so this follow-up is purely client-side (new tab kinds + viewers +
  recorder branches). "Workflow execution opens as a tab" is the cleanest standalone slice.
