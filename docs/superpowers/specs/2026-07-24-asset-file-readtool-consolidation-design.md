# Prep B — Asset-file + read-tool consolidation into automation-ai-tool

**Date:** 2026-07-24
**Status:** Design — approved for planning
**Author:** Ivica Cardic

## Context

Second of the three sequenced pieces (Prep A relocated the management manager subagents; this
consolidates the viewer-backing read tools + the asset-file family; the Feature adds the four
read-only MCP App viewers). The goal is to make `automation-ai-tool` the home for automation tools
that sit on automation/platform infra rather than AI-Hub chat state, so the Feature can surface
them on the management MCP server.

## Goal

- Move the **asset-file tool family** (6 tools) out of `ai-hub-service` into **CE
  `automation-ai-tool`**: `GetAssetFileContentToolCallback`, `ListAssetFilesToolCallback`,
  `CloneAssetFileToolCallback`, `CreateAssetFileToolCallback`, `CreateBinaryAssetFileToolCallback`,
  `UpdateAssetFileContentToolCallback` (all sit on the CE `AssetFileFacade`).
- Consolidate the **read tools**: `getCodeWorkflowSource` / `getCustomComponentSource` already live
  in EE `automation-ai-tool`; `queryDataTable` is deduplicated (the `ai-hub-service` copy removed in
  favour of the `automation-ai-tool` copy); `getAssetFileContent` lands in CE `automation-ai-tool`
  with the family.
- Extract a **CE artifact-recorder SPI** so the three artifact-recording write tools decouple from
  the EE recorder while landing in CE.

## Non-goals

- No viewer / MCP-server / `structuredContent` work (the Feature).
- No change to the ~30 other `ai-hub` tools that use `AiHubToolInvocationContext` / the recorder —
  they stay EE and keep using the EE types.
- No behaviour change to what any tool does.

## Coupling analysis (why this is bigger than Prep A)

All six asset tools read `AiHubToolInvocationContext` (EE) using the **full** field set
(workspaceId, userId, threadId, environmentId via `resolveEnvironmentOrDefault`), not just the
workspace/environment pair the managers needed. The three write tools additionally depend on
`AiHubTaskArtifactRecorder` (EE) — but that is already a **minimal interface** ("record task
artifacts without coupling to ai-hub internals"); the write tools only call its `record(...)`
overloads. So the decouple is a straight interface promotion, not a rewrite.

## Design

### 1. CE full context accessor

Generalize the Prep A `ManagerToolInvocationContext` into
`com.bytechef.automation.ai.tool.AutomationToolInvocationContext` — a CE record mirroring the full
field set of the EE `AiHubToolInvocationContext` (`workspaceId, userId, sourceOrdinal,
lastUserPrompt, environmentId, threadId`) plus `fromToolContext(ToolContext)` and
`resolveEnvironmentOrDefault(...)`, reading the same key strings (the documented lockstep contract).
The managers (Prep A) switch from `ManagerToolInvocationContext` to the two fields they use on the
generalized type; `ManagerToolInvocationContext` is deleted. The EE `AiHubToolInvocationContext`
stays for the ai-hub tools that remain.

### 2. CE artifact-recorder SPI

The recorder populates the **AI Hub task's artifacts sidebar** (`ai_hub_task_artifact`, keyed by the
AG-UI `threadId`). It is therefore an AI-Hub-chat concept with a single implementation; on any
surface without an AI-Hub task (the management MCP server, etc.) the recorder is absent and the
`@Nullable` skip path fires — artifact recording is a no-op off AI Hub by design. Promoting the
interface to CE is thus a thin contract extraction, not a capability move.

Promote the `AiHubTaskArtifactRecorder` interface to CE `automation-ai-tool` as
`com.bytechef.automation.ai.tool.ToolArtifactRecorder` (String/Long/@Nullable only — CE-clean),
keeping the same method set (`record` ×2, `recordReference`, `recordWorkflowReference`). The three
CE write tools depend on `@Nullable ToolArtifactRecorder` (null → skip, the existing OpenResourceTab
pattern). The EE `AiHubTaskArtifactRecorder` becomes `extends ToolArtifactRecorder` (or is replaced
by the CE type where the ai-hub impl is wired), so the ai-hub recorder bean satisfies the CE
interface when the tools are constructed in ai-hub configs. The remaining EE ai-hub tools that use
the recorder keep the EE type (which now extends the CE one), so they are unaffected.

### 3. Asset-file tool move

`git mv` the six tools to CE `automation-ai-tool` (`com.bytechef.automation.ai.tool`), Apache
header, drop `@version ee`, swap `AiHubToolInvocationContext` → `AutomationToolInvocationContext`
and (write tools) `AiHubTaskArtifactRecorder` → `ToolArtifactRecorder`. Add CE build deps:
`automation-assetfile` API. Their construction sites in `ai-hub` configs re-import the CE tool
classes and pass the recorder bean (which now implements the CE interface).

### 4. queryDataTable dedup

The `ai-hub-service` `QueryDataTableToolCallback` and the `automation-ai-tool` (EE)
`…datatable.QueryDataTableToolCallback` differ; the facade (`WorkspaceDataTableFacade`) and services
(`platform.data.table`) are CE. Keep ONE canonical copy in `automation-ai-tool`, delete the
`ai-hub-service` duplicate, and repoint the ai-hub data_table subagent construction at the retained
copy. (Whether the retained copy is CE- or EE-packaged is settled during planning by its facade
edition — CE if clean.)

### 5. getCodeWorkflowSource / getCustomComponentSource

Already in EE `automation-ai-tool` (their facades are EE). No move; listed here only to confirm the
four viewer-backing read tools all resolve to `automation-ai-tool` after Prep B.

## Testing

- Relocate each asset-file tool's existing test alongside it (CE), repackaged; the write-tool tests
  exercise the `@Nullable` recorder both present and absent.
- A CE `AutomationToolInvocationContext` test (full field rehydration + `resolveEnvironmentOrDefault`
  default).
- The EE recorder-still-satisfies-CE-interface path is covered by ai-hub's existing recorder tests
  compiling against the CE type.
- Dedup: the retained `queryDataTable` test stays; the removed duplicate's unique assertions (if
  any) fold into it.

## Risks

- **Recorder interface drift.** Promoting the interface must preserve every method signature the EE
  impl and all EE callers use. Mitigation: make the EE `AiHubTaskArtifactRecorder` extend the CE
  `ToolArtifactRecorder` with no method changes, so EE callers are untouched.
- **Context field parity.** `AutomationToolInvocationContext` must expose every field the asset
  tools read; a missing accessor is a compile error caught immediately.
- **queryDataTable behavioural divergence.** The two copies differ — planning must diff them and
  keep the superset behaviour, not silently drop a branch.

## Rollout

Single PR, no data migration, no flag. Transparent at runtime: the same tools run with the same
facades, recorder, and context; only their module/package and the recorder/context type names
change.
