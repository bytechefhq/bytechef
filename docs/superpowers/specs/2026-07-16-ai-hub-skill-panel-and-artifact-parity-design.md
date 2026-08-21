# AI Hub Skill Panel, Delete Tool, and Artifact Parity — Design

**Date:** 2026-07-16
**Status:** Approved (design) — revised after deeper investigation of the artifact-recording path
**Area:** EE AI Hub (`server/ee/libs/ai/ai-hub`), `automation-ai-tool`, AI Hub client (`client/src/pages/automation/ai-hub`)

## Problem

The AI Hub agent can **create** and **update** skills (via the `skills_build` sub-agent, which
wraps `SkillsTools`), but two gaps remain:

1. **No delete.** `SkillsTools.deleteAiSkill` exists but its `@Tool` annotation is commented out,
   so the agent cannot delete a skill — inconsistent with `deleteProject` / `deleteWorkflow`,
   which are exposed and return a confirmation message.
2. **No "open skill in the right panel."** The agent can open workflows, data tables, and
   knowledge bases into the AI Hub resource panel via `openWorkflowTab` / `openDataTableTab` /
   `openKnowledgeBaseTab`, but there is no `openSkillTab`. Skills are only reachable through the
   composer @-mention picker and the settings page — and are absent from the task artifact list.

## Artifact recording architecture (as-is)

Understanding this is load-bearing for the design.

- **Client hook (primary path).** `useRecordReferencedArtifacts` watches the tabs store and, for
  each open tab, calls the GraphQL `recordReferencedAiHubTaskArtifact` mutation. It maps tab kinds to
  artifact kinds: `file→FILE_REFERENCED`, `workflow→WORKFLOW_REFERENCED`,
  `dataTable→DATA_TABLE_REFERENCED`, `knowledgeBase→KB_REFERENCED`. The mutation
  (`AiHubTaskArtifactService.recordReference`) **dedups** on `(taskId, kind, artifactId)`. **Skills
  are not in this map**, so opening a skill records nothing.
- **Server robustness layer (workflow only).** `OpenWorkflowTabToolCallback` *also* records the
  workflow server-side via `AiHubTaskArtifactRecorder.recordWorkflowReference` →
  `AiHubTaskArtifactService.recordWorkflowArtifact` (which enforces one-workflow-one-row). This is a
  robustness layer "that no longer depends on the client tab-watching hook firing."
- **Data tables / KB** record via the client hook only — no server robustness layer.
- The generic `AiHubTaskArtifactService.record(...)` **does not dedup** (it saves unconditionally);
  it is used for event-log kinds like `DATA_TABLE_ROW_ADDED` and `MEMORY_CREATED` that intentionally
  allow multiple rows. It therefore **cannot** be used for the server robustness layer of a
  `*_REFERENCED` kind without producing duplicates against the client hook.

## Goals

- Expose an agent-callable **delete skill** tool matching the existing delete convention.
- Add **`openSkillTab`** so the agent can open a skill in the resource panel, rendering the existing
  `AiSkillDetail` viewer (read-only in-panel), and have the opened skill recorded as a task artifact.
- **Full workflow parity for the server robustness layer:** opening a skill, a data table, or a
  knowledge base each dual-record (client hook + a dedup-aware server layer), exactly like workflows.

## Non-goals (YAGNI)

- In-panel skill **editing** (viewer is read-only; edits go through the agent's `updateAiSkill*`
  tools or the skills settings page).
- New `SKILL_CREATED` / `SKILL_UPDATED` kinds — only `SKILL_REFERENCED`.
- Any pre-delete confirmation gate (matches `deleteProject` / `deleteWorkflow`).

## Design decisions

- **Skill panel content:** reuse `AiSkillDetail`, read-only. It currently takes no props and reads
  the skill id from the route; refactor it to accept an optional `skillId` prop (falling back to the
  route param) so it can be embedded in the resource panel.
- **Delete:** uncomment `@Tool`, change `void` → `String`, return `"Deleted skill <id>."`.
- **Artifact kind:** add `SKILL_REFERENCED` to **both** enums (Java `AiHubTaskArtifactKind` and the
  GraphQL `ai-hub-artifact.graphqls` enum), append-only.
- **Dedup-aware server layer:** add a new recorder method for reference kinds rather than reuse the
  non-dedup generic `record` — see server change #4.

## Server changes

1. **`SkillsTools.deleteAiSkill`** — uncomment `@Tool(description = "Delete an AI skill by its ID.
   Returns a confirmation message.")`; change `void` → `String`; on success return
   `"Deleted skill " + id + "."` (keep the existing `ExecutionException` on failure).
2. **`AiHubTaskArtifactKind` (Java) + `ai-hub-artifact.graphqls` (GraphQL enum)** — append
   `SKILL_REFERENCED` at the end of each. Keeps every existing ordinal stable
   (`EnumOrdinalStabilityTest`).
3. **New `OpenSkillTabToolCallback`** — mirror `OpenDataTableTabToolCallback`: signaling-only
   `ToolCallback`, tool `openSkillTab`, input `{skillId, name}`, output
   `{opened, skillId, name}`; validates blank `skillId`/`name` → `toolError`; records the skill
   server-side via the dedup-aware recorder method from #4. Constructor takes
   `@Nullable AiHubTaskArtifactRecorder`.
4. **Dedup-aware reference recorder** — add
   `AiHubTaskArtifactRecorder.recordReference(String threadId, @Nullable Long userId, String
   artifactKind, String artifactId, String artifactName)` and implement it in the service
   (`recordReferenceByThread`): resolve the task by `(threadId, userId)`, then **dedup** on
   `(task, kind, artifactId)` (return the existing row if present, else save). This mirrors the
   GraphQL `recordReference` dedup but is keyed by `threadId` (what tool callbacks have) instead of
   `taskId`. Used by the skill/data-table/KB open tools.
5. **`OpenDataTableTabToolCallback` + `OpenKnowledgeBaseTabToolCallback`** — add a
   `@Nullable AiHubTaskArtifactRecorder` constructor param; on successful open, record
   `DATA_TABLE_REFERENCED` / `KB_REFERENCED` via the new dedup-aware method (server robustness layer).
6. **`AiHubConfiguration`** — register `OpenSkillTabToolCallback`, and pass the recorder to the
   skill/data-table/KB open tools at the **recorder-enabled** registration site (the one using
   `OpenWorkflowTabToolCallback(aiHubTaskArtifactRecorder)`). Leave the **null-recorder** ASK-mode
   site passing `null` (its comment already documents that ASK mode relies on the client hook). Apply
   the same to `DataAnalystConfiguration` if it constructs those tools.
7. **`prompt_ai_hub_ask.txt` + `prompt_ai_hub_build.txt`** — add an `openSkillTab({skillId, name})`
   line next to the other `open*Tab` entries.

## Client changes (`client/src/pages/automation/ai-hub`)

8. **GraphQL codegen** — after adding `SKILL_REFERENCED` to the schema, regenerate so
   `AiHubTaskArtifactKind.SkillReferenced` exists in `graphql.ts`.
9. **`useAiHubTabsStore`** — add `{id, kind: 'skill', skillId, name}` to `AiHubTabType` and an
   `openSkillTab(skillId, name)` action that dedups by `skillId` (mirror `openDataTableTab`).
10. **`AiSkillDetail`** — accept an optional `skillId` prop (fall back to the route param) so it can
    be embedded read-only in the panel.
11. **`AiHubResourcePanel`** — render `kind === 'skill'` via `<AiSkillDetail skillId={...} />`.
12. **`AiHubRuntimeProvider`** — add an `else if (toolCallName === 'openSkillTab')` branch that
    validates the result (`validateOpenSkillTabResult`, mirroring `validateOpenDataTableTabResult`)
    and calls `openSkillTab(...)`.
13. **`useRecordReferencedArtifacts`** — add `skill: AiHubTaskArtifactKind.SkillReferenced` to
    `KIND_TO_ARTIFACT_KIND` and a `case 'skill'` in `resolveArtifactKey` returning
    `{artifactId: tab.skillId, kind: KIND_TO_ARTIFACT_KIND.skill}`.
14. **`AiHubTasksSidebar`** — render and reopen `SKILL_REFERENCED` (icon/label + `openSkillTab`),
    mirroring the existing `DATA_TABLE_REFERENCED` handling.
15. **`useSwitchTask`** — replay `openSkillTab` for `SKILL_REFERENCED` artifacts on task switch,
    mirroring the data-table/KB replay.

## Data flow (open a skill)

1. Agent calls `openSkillTab({skillId, name})`.
2. `OpenSkillTabToolCallback` records `SKILL_REFERENCED` server-side (dedup-aware) and returns
   `{opened: true, skillId, name}`.
3. `AiHubRuntimeProvider` intercepts the result and calls `openSkillTab(skillId, name)`.
4. `useRecordReferencedArtifacts` also records the skill via the GraphQL mutation (dedup collapses
   with the server row); `AiHubResourcePanel` renders the skill tab via `AiSkillDetail`.
5. The artifact appears in `AiHubTasksSidebar`; clicking it replays `openSkillTab`.

## Error handling

- `OpenSkillTabToolCallback` returns a `toolError` for blank `skillId`/`name` (mirrors data-table).
  Artifact-record failures are logged and swallowed — the open signal must still succeed.
- The client `surfaceTabOpenFailure` path already handles unparseable / `opened: false` results.

## Testing

- **`EnumOrdinalStabilityTest`** — pin the new `SKILL_REFERENCED` ordinal.
- **Server** — `OpenSkillTabToolCallbackTest` (valid open returns `opened: true` + records; blank
  inputs → `toolError`); a service test for the new dedup-aware `recordReference` (second call with
  the same `(thread, kind, artifactId)` does not add a row); extend/confirm data-table & KB tool
  tests now record via the dedup-aware method.
- **Client** — `useAiHubTabsStore` `openSkillTab` (open + dedup); `useRecordReferencedArtifacts`
  records `SKILL_REFERENCED` for a skill tab; `AiHubTasksSidebar` `SKILL_REFERENCED` render + reopen.

## Rollout / compatibility

- Append-only enum changes (Java + GraphQL) keep existing `ai_hub_task_artifact.kind` ordinals and
  GraphQL values stable.
- Re-enabling delete is additive.
- The dedup-aware server layer collapses with the client hook's row, so DT/KB/skill never stack
  duplicate artifacts.
