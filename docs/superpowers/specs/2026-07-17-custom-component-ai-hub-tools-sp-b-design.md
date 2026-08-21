# Custom Component AI Hub Tools + Open-in-Panel (SP-B) — Design

**Date:** 2026-07-17
**Status:** Approved (design)
**Sub-project:** SP-B of the Custom Components initiative (SP-A = settings source editing/create-empty, done; SP-C = copilot build subagent, later).
**Area:** EE AI Hub (`server/ee/libs/ai/ai-hub`), EE `automation-ai-tool`, AI Hub client (`client/src/pages/automation/ai-hub`), `platform-custom-component` (facade already built in SP-A).

## Problem

SP-A gave the settings UI (detail route + Monaco editor + create-empty) and the facade methods
(`getCustomComponentSource`, `updateCustomComponentSource`, `createEmptyCustomComponent`, `delete`,
`getCustomComponents`). SP-B exposes those to the **AI Hub agent** as tools and lets the agent open a
custom component in the AI Hub resource panel — mirroring the skills feature (`SkillsTools`,
`openSkillTab`, `SKILL_REFERENCED`, panel rendering, artifact recording).

## Scope decisions (from brainstorming)

- **Editable panel** — the AI Hub panel renders the **editable** `CustomComponentDetail` (Monaco +
  Save/compile-gate), not a read-only variant. Simpler than skills (no read-only embedded component
  needed) — reuse SP-A's detail with an id prop.
- **Create = create-empty + update-source** — the create tool calls SP-A's
  `createEmptyCustomComponent(name, language)`; the agent then calls the update-source tool to fill it
  in. No new facade method; each write is compile-gated by SP-A.
- **Tools on the main AI Hub agent** — `CustomComponentTools` are registered directly on the AI Hub
  agent's tool set (like `ProjectTools`/`DataTableTools`), not behind a subagent. (SP-C adds a
  dedicated build subagent later; SP-B is the direct tools + panel.)

## Non-goals (this sub-project)

- The copilot build subagent (SP-C).
- Multi-file components / engine changes (never — single-file per SP-A).
- Python/Ruby create-empty (SP-A ships JS create only; the agent can still edit existing non-Java).

## Server design

1. **`AiHubTaskArtifactKind` (Java) + `ai-hub-artifact.graphqls` + the wire-format snapshot test** —
   append `CUSTOM_COMPONENT_REFERENCED` (append-only, ordinal-stable; the snapshot test + the client
   `AiHubArtifactKindType` literal must both list it, as the skills work established).
2. **`CustomComponentTools`** — new `@Component` in EE
   `server/ee/libs/automation/automation-ai/automation-ai-tool` (it wraps the EE
   `CustomComponentFacade`; add the dep on `platform-custom-component-configuration-api`). `@Tool`
   methods mirroring `SkillsTools`, each wrapping a facade call in try/catch → `ExecutionException`
   with a `CustomComponentToolErrorType`:
   - `createCustomComponent(name, language)` → `createEmptyCustomComponent`; returns the created id/name.
   - `updateCustomComponentSource(id, content)` → compile-gated update; returns a confirmation.
   - `deleteCustomComponent(id)` → `delete`; returns a confirmation message (like `deleteAiSkill`).
   - `getCustomComponentSource(id)` → source text.
   - `listCustomComponents()` → the component list (id/name/title/language).
3. **`OpenCustomComponentTabToolCallback`** (EE ai-hub-service) — mirror `OpenSkillTabToolCallback`:
   tool `openCustomComponentTab`, input `{customComponentId, name}`, output
   `{opened, customComponentId, name}`; records `CUSTOM_COMPONENT_REFERENCED` via the existing
   dedup-aware `AiHubTaskArtifactRecorder.recordReference` (built in the skills work).
4. **`AiHubConfiguration`** — register `CustomComponentTools` on the AI Hub agent tool set (the same
   way the other CRUD tool `@Component`s are contributed) and `OpenCustomComponentTabToolCallback` at
   both sites (recorder@BUILD, null@ASK). Document `openCustomComponentTab({customComponentId, name})`
   in `prompt_ai_hub_ask.txt` + `prompt_ai_hub_build.txt`.

## Client design (`client/src/pages/automation/ai-hub`)

5. **GraphQL codegen** — regenerate so `AiHubTaskArtifactKind.CustomComponentReferenced` exists; add
   `CUSTOM_COMPONENT_REFERENCED` to the hand-maintained `AiHubArtifactKindType` in `tasks.api.ts`.
6. **`useAiHubTabsStore`** — add `{id, kind: 'customComponent', name, customComponentId}` to
   `AiHubTabType` + `openCustomComponentTab(customComponentId, name)` (dedup by id, mirror
   `openSkillTab`); add the exhaustiveness case in `AiHubRuntimeProvider.getTabGenericId`.
7. **`CustomComponentDetail`** (SP-A) — accept an optional `customComponentId?: string` prop (fall
   back to the route param), so it can be embedded in the panel **editably** (component-split like
   `AiSkillDetail`, or a simple prop if clean).
8. **`AiHubResourcePanel`** — render `kind === 'customComponent'` →
   `<CustomComponentDetail customComponentId={activeTab.customComponentId} />`.
9. **`AiHubRuntimeProvider`** — `else if (toolCallName === 'openCustomComponentTab')` branch +
   `validateOpenCustomComponentTabResult` (mirror the openSkillTab branch/validator).
10. **`useRecordReferencedArtifacts`** — `customComponent: AiHubTaskArtifactKind.CustomComponentReferenced`
    in `KIND_TO_ARTIFACT_KIND` (typed by `AiHubTabType['kind']`, per the skills fix) + a
    `case 'customComponent'` in `resolveArtifactKey`.
11. **`AiHubTasksSidebar`** — render + reopen `CUSTOM_COMPONENT_REFERENCED` (icon/label +
    `isArtifactClickable` + quick-open `openCustomComponentTab`); include it in `isArtifactRemovable`
    for parity (the skills final review made removability in-scope).
12. **`useSwitchTask`** — replay `openCustomComponentTab` for `CUSTOM_COMPONENT_REFERENCED` artifacts.

## Data flow (open a custom component)

1. Agent calls `openCustomComponentTab({customComponentId, name})`.
2. `OpenCustomComponentTabToolCallback` records `CUSTOM_COMPONENT_REFERENCED` (dedup-aware) and returns
   `{opened, customComponentId, name}`.
3. `AiHubRuntimeProvider` intercepts → `openCustomComponentTab(...)`.
4. `useRecordReferencedArtifacts` also records it (dedup collapses); `AiHubResourcePanel` renders the
   editable `CustomComponentDetail`.
5. The artifact appears in the sidebar; clicking replays `openCustomComponentTab`.

## Error handling

- Tool failures (bad source on update, duplicate/invalid name on create, Java-source edit) surface as
  the SP-A facade exceptions, wrapped by `CustomComponentTools` into tool errors the agent reads.
- `OpenCustomComponentTabToolCallback` returns `toolError` for blank id/name; artifact-record failures
  logged+swallowed. Client `surfaceTabOpenFailure` handles unparseable/`opened:false`.

## Testing

- Server: `CustomComponentToolsTest` (each tool wraps the facade + error path); `OpenCustomComponentTabToolCallbackTest`
  (opened result + blank-input error + records `CUSTOM_COMPONENT_REFERENCED`); `EnumOrdinalStabilityTest`
  + wire-format test pin the new kind.
- Client: `useAiHubTabsStore` `openCustomComponentTab`; `useRecordReferencedArtifacts` records the kind;
  `AiHubTasksSidebar` render/reopen; `CustomComponentDetail` renders with the id prop.

## Rollout / compatibility

- Append-only enum changes (Java + GraphQL) keep ordinals/values stable.
- Reuses the dedup-aware `recordReference` from the skills work — DT/KB/skill/custom-component all
  dual-record consistently.
- All additive; SP-A settings behavior untouched.
