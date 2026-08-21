# Custom Component AI Hub Tools + Open-in-Panel (SP-B) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Give the AI Hub agent tools to create/update/delete/read custom components and open one (editable) in the AI Hub resource panel — mirroring the shipped skills feature.

**Architecture:** `CustomComponentTools` (EE) wraps SP-A's `CustomComponentFacade`; `OpenCustomComponentTabToolCallback` mirrors `OpenSkillTabToolCallback` + records a new `CUSTOM_COMPONENT_REFERENCED` artifact via the existing dedup-aware `recordReference`; the client tab/panel/hook/sidebar/replay wiring mirrors `openSkillTab`; the panel renders SP-A's editable `CustomComponentDetail` via an id prop.

**Tech Stack:** Java 25 / Spring Boot 4, Spring AI tools, GraphQL, React 19 / TS / Zustand, Vitest, JUnit 5.

## Global Constraints

- EE files use the Enterprise license header + `@version ee`. Enum values append-only (ordinal-stable; pinned by `EnumOrdinalStabilityTest` + `AiHubTaskArtifactKindWireFormatTest`, which ALSO requires the client `AiHubArtifactKindType` literal in `client/src/pages/automation/ai-hub/tasks/api/tasks.api.ts`).
- The dedup-aware `AiHubTaskArtifactRecorder.recordReference(threadId, userId, kind, artifactId, name)` already exists (skills work) — reuse it; do NOT re-add dedup.
- Client: interface names `I`/`Props`; sort-keys ascending; named imports sorted; icons `*Icon`; twMerge; `KIND_TO_ARTIFACT_KIND` is typed `Partial<Record<AiHubTabType['kind'], ...>>` (skills fix) — do NOT widen `ReferencedResourceKindType`.
- Run `./gradlew spotlessApply` + touched-module `check` (NOT just `test`) before server commits; `cd client && npm run check` before client commits.
- Commit prefix: server `732 <desc>`, client `732 client - <desc>`.
- Reference implementations to mirror (all shipped): `OpenSkillTabToolCallback`, `SkillsTools`/`SkillToolErrorType`, `AiHubConfiguration` skill registrations, `useAiHubTabsStore.openSkillTab`, `AiHubRuntimeProvider` openSkillTab branch + `validateOpenSkillTabResult`, `useRecordReferencedArtifacts` skill mapping, `AiHubTasksSidebar` SKILL_REFERENCED, `useSwitchTask` openSkillTab replay, `AiSkillDetail`'s `skillId`-prop embedding.
- Spec: `docs/superpowers/specs/2026-07-17-custom-component-ai-hub-tools-sp-b-design.md`.

---

## Task 1: `CUSTOM_COMPONENT_REFERENCED` artifact kind (Java + GraphQL + wire-format + client literal)

**Files:** `AiHubTaskArtifactKind.java`; `ai-hub-artifact.graphqls`; `AiHubTaskArtifactKindWireFormatTest.java`; `EnumOrdinalStabilityTest`; `client/src/pages/automation/ai-hub/tasks/api/tasks.api.ts`.

- [ ] Mirror the skills `SKILL_REFERENCED` addition exactly (that commit is the template): append `CUSTOM_COMPONENT_REFERENCED` last in the Java enum + GraphQL enum; add to `EXPECTED_WIRE_NAMES` (alphabetical) in the wire-format test; bump the ordinal-stability test; add `'CUSTOM_COMPONENT_REFERENCED'` to `AiHubArtifactKindType` in `tasks.api.ts`. Run `:...:ai-hub-api:test` + `cd client && npm run typecheck`. Commit server + client separately.

---

## Task 2: `CustomComponentTools` + `ReadCustomComponentTools` (EE) + error type

**Files:** new `CustomComponentTools.java`, `ReadCustomComponentTools.java`, `CustomComponentToolErrorType.java` in `server/ee/libs/automation/automation-ai/automation-ai-tool/...`; add `platform-custom-component-configuration-api` dep to that module's `build.gradle.kts`.
Test: `CustomComponentToolsTest`.

**Interfaces:** Produces the two `@Component` tool beans (consumed by Task 3 registration).

- [ ] Mirror `SkillsTools`/`ReadSkillsTools`/`SkillToolErrorType`. `CustomComponentToolErrorType` extends `AbstractErrorType` with values (start at 100): `CREATE`, `UPDATE_SOURCE`, `DELETE`, `GET_SOURCE`, `LIST`.
- [ ] `CustomComponentTools` (`@Component`, injects `CustomComponentFacade`) `@Tool` methods, each try/catch→`ExecutionException(msg, e, CustomComponentToolErrorType.X)`:
  - `createCustomComponent(@ToolParam String name, @ToolParam CustomComponent.Language language)` → `facade.createEmptyCustomComponent(name, language)`; return `"Created custom component " + result.getId() + " (" + name + ")."` (language limited to JAVASCRIPT by the facade; document that in the @Tool description).
  - `updateCustomComponentSource(@ToolParam long id, @ToolParam String content)` → `facade.updateCustomComponentSource(id, content)`; return `"Updated custom component " + id + "."`.
  - `deleteCustomComponent(@ToolParam long id)` → `facade.delete(id)`; return `"Deleted custom component " + id + "."`.
- [ ] `ReadCustomComponentTools` (`@Component`, injects `CustomComponentFacade`) read-only `@Tool` methods: `getCustomComponentSource(long id)` → source text; `listCustomComponents()` → `facade.getCustomComponents()` (return the list; the agent reads id/name/title/language).
- [ ] Tests: each tool verifies the facade call + returns the confirmation/text; a facade-throws case surfaces as `ExecutionException`. Match `SkillsToolsTest` style. Run the module `check`. Commit.

---

## Task 3: `OpenCustomComponentTabToolCallback` + register tools/callback in config + prompts

**Files:** new `OpenCustomComponentTabToolCallback.java` (ai-hub-service, mirror `OpenSkillTabToolCallback`); `AiHubConfiguration.java`; `DataAnalystConfiguration.java` if relevant; `prompt_ai_hub_ask.txt` + `prompt_ai_hub_build.txt`. Test: `OpenCustomComponentTabToolCallbackTest`.

- [ ] `OpenCustomComponentTabToolCallback` — clone `OpenSkillTabToolCallback`: tool `openCustomComponentTab`, input record `{customComponentId, name}`, output `{opened, customComponentId, name}`, records `"CUSTOM_COMPONENT_REFERENCED"` via `recordReference`. Ctor `@Nullable AiHubTaskArtifactRecorder` + `@SuppressFBWarnings("EI_EXPOSE_REP2")`. Test mirrors `OpenSkillTabToolCallbackTest` (opened result, blank-id error, record-path via mock recorder).
- [ ] `AiHubConfiguration`: register `OpenCustomComponentTabToolCallback(aiHubTaskArtifactRecorder)` at the BUILD site + `(null)` at the ASK site (like openSkillTab). Add `CustomComponentTools` to the BUILD `AiHubGlobalToolCatalog` toolObjects list (the bean at ~L665 that does `ToolCallbacks.from(toolObjects)` with ProjectTools/ComponentTools/etc. — add `CustomComponentTools customComponentTools` as a param and include it) and `ReadCustomComponentTools` to the ASK catalog (~L659 with the `Read*` tools). Add the sorted imports.
- [ ] Prompts: add `- openCustomComponentTab({customComponentId, name}) — show a custom component in the resource panel.` next to the other `open*Tab` entries.
- [ ] Build the module (`compileJava`) + run the module `test`; commit.

---

## Task 4: Client GraphQL codegen

- [ ] `cd client && npx graphql-codegen`; verify `AiHubTaskArtifactKind.CustomComponentReferenced = 'CUSTOM_COMPONENT_REFERENCED'` present; `npm run typecheck`; commit only the generated file(s). (Task 1's schema change drives this.)

---

## Task 5: Tab store — `customComponent` kind + `openCustomComponentTab`

**Files:** `useAiHubTabsStore.ts` (+ test); `AiHubRuntimeProvider.tsx` (`getTabGenericId` exhaustiveness case only).

- [ ] Mirror `openSkillTab`: add `{id, kind: 'customComponent', name, customComponentId}` to `AiHubTabType`; `openCustomComponentTab(customComponentId, name) => string` (dedup by `customComponentId`). Add the `getTabGenericId` case returning `tab.customComponentId`. Test open+dedup. `npm run check`; commit.

---

## Task 6: `CustomComponentDetail` accepts a `customComponentId` prop (editable embed)

**Files:** SP-A's `CustomComponentDetail.tsx` (+ test).

- [ ] Read the current component (it reads `:id` via `useParams`). Add an optional `customComponentId?: string` prop; use it instead of the route param when provided (`const id = customComponentIdProp ?? params.id`). Keep it EDITABLE (Save/Monaco) in both modes — no read-only variant. If route-only chrome (a back button) exists, guard it behind `!customComponentIdProp`. Test: renders + fetches by the prop id without a route param. `npm run check`; commit.

---

## Task 7: Panel renders the custom-component tab

**Files:** `AiHubResourcePanel.tsx`.

- [ ] Add `if (tab.kind === 'customComponent') return <CustomComponentDetail customComponentId={tab.customComponentId} />;` (sorted import from `@/ee/pages/settings/platform/custom-components/CustomComponentDetail`). `npm run check`; commit.

---

## Task 8: Runtime provider `openCustomComponentTab` branch + validator

**Files:** `AiHubRuntimeProvider.tsx`.

- [ ] Mirror the `openSkillTab` branch/validator: add `validateOpenCustomComponentTabResult` ({opened, customComponentId, name}) local const + `else if (toolCallName === 'openCustomComponentTab')` branch calling `openCustomComponentTab(parsed.customComponentId, parsed.name)`. Do NOT touch `getTabGenericId` (Task 5 owns it). `npm run check`; commit.

---

## Task 9: Record artifacts in the hook

**Files:** `useRecordReferencedArtifacts.ts`.

- [ ] Add `customComponent: AiHubTaskArtifactKind.CustomComponentReferenced` to `KIND_TO_ARTIFACT_KIND` and a `case 'customComponent': return {artifactId: tab.customComponentId, kind: KIND_TO_ARTIFACT_KIND.customComponent};` in `resolveArtifactKey`. Update the header comment (now six kinds). `npm run check`; commit.

---

## Task 10: Sidebar render + reopen + removable

**Files:** `AiHubTasksSidebar.tsx` (+ test).

- [ ] Mirror the `SKILL_REFERENCED` handling in all THREE places skills touched (icon/label; `isArtifactClickable`; quick-open → `openCustomComponentTab(artifact.artifactId, artifact.artifactName)`) PLUS add `CUSTOM_COMPONENT_REFERENCED` to `isArtifactRemovable` (parity, per the skills final review). Pick a sensible Lucide `*Icon` (e.g. `Blocks`/`PackageIcon` — check what the custom-components settings nav uses). Test: render + click→`openCustomComponentTab`. `npm run check`; commit.

---

## Task 11: `useSwitchTask` replay

**Files:** `useSwitchTask.ts`.

- [ ] Add a `case 'CUSTOM_COMPONENT_REFERENCED'` replay returning `{args: {customComponentId: artifact.artifactId, name: artifact.artifactName}, toolName: 'openCustomComponentTab'}` (match the sibling shape + sort-keys). `npm run check`; commit.

---

## Task 12: Full-stack verification

- [ ] Server: `check` on `ai-hub-api`, `ai-hub-service`, `ai-hub-graphql`, and EE `automation-ai-tool` modules → BUILD SUCCESSFUL.
- [ ] Client: `npm run check` → green.
- [ ] (optional smoke) agent creates → opens (editable panel) → edits+saves → deletes a custom component; artifact appears once in the sidebar and reopens.

---

## Self-review notes (coverage vs spec)
- Spec §Server 1 (artifact kind) → Task 1; 2 (tools) → Task 2; 3 (open callback) → Task 3; 4 (register+prompts) → Task 3. §Client 5 (codegen) → Task 4; 6 (store) → Task 5; 7 (detail prop) → Task 6; 8 (panel) → Task 7; 9 (runtime) → Task 8; 10 (hook) → Task 9; 11 (sidebar) → Task 10; 12 (switch) → Task 11. §Testing folded per task + Task 12.
