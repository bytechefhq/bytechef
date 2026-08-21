# AI Hub artifacts card — design

**Date:** 2026-08-07
**Status:** Approved, not yet implemented
**Scope:** Client only (`client/src/ee/pages/automation/ai-hub/`). No server, GraphQL, or schema changes.

## Problem

A task's artifacts render as a collapsible child list under each task row in the AI Hub tasks sidebar
(`AiHubTasksSidebar.tsx`, `ArtifactList` at `:628`). That placement has three costs:

1. It nests a second level of rows inside a list whose job is task navigation, so the sidebar reads as
   cluttered even when a task has no artifacts (`No artifacts yet` renders under an expanded row).
2. Artifacts are invisible until the user expands a row. The count badge is deliberately deferred behind
   `isExpanded` (`:707-716`) to avoid firing N queries for N visible rows, so a task's output count cannot
   be seen at a glance — the badge only appears after a first expand.
3. The artifacts of the task the user is actually looking at get no more prominence than any other task's.

Artifacts belong next to the conversation that produced them, not next to the list of conversations.

## Solution

Move the list to a floating **Artifacts** card pinned to the top-right of the chat pane, and add an
**Artifacts** branch to the resource panel's `+` menu as the card's counterpart while the panel is open.
The sidebar becomes a flat list of tasks.

Clicking an artifact opens it in the right resource panel. That behavior already exists — the 18-kind
dispatcher `handleArtifactQuickOpen` (`:314`) maps every artifact kind onto an
`aiHubTabsStore.open*Tab(...)` call, and each of those setters already sets `rightPanelOpen: true`. Only
the trigger surface changes.

## Architecture

### New module: `ai-hub/artifacts/`

The artifact logic (~350 lines) is extracted out of the 1470-line `AiHubTasksSidebar.tsx`, because the card
and the `+` branch both need it and neither should import from a sidebar component.

| File | Contents |
|---|---|
| `artifacts/artifactOpen.ts` | `handleArtifactQuickOpen`, `openCodeWorkflowArtifact`, `parseMetadataJson`, `isArtifactClickable`, `isArtifactRemovable`, `getArtifactIcon` |
| `artifacts/AiHubArtifactRow.tsx` | icon + truncated name + click handler + delete mutation and its query invalidation |
| `artifacts/AiHubArtifactsCard.tsx` | the floating card |
| `artifacts/tests/artifactOpen.test.ts` | the migrated `describe('handleArtifactQuickOpen')` block |
| `artifacts/tests/AiHubArtifactsCard.test.tsx` | new — render conditions and click-opens-tab |

All per-kind knowledge (which kinds are clickable, which are removable, which icon, how each maps to a tab)
lives in `artifactOpen.ts`, in one place. Outside the sidebar and its own test file there are currently zero
consumers of any of these symbols, so the lift is mechanical.

`openArtifactInTask` (`:430`) is **deleted**. Its switch-task-then-open sequence existed only because the
sidebar could display a non-active task's artifacts; with the card scoped to the active task there is no
caller. Its three tests go with it.

Rejected alternatives: leaving the helpers in `AiHubTasksSidebar.tsx` and importing from it (makes a
component file a de-facto utility module), and hand-rolling a second dispatcher in the card (duplicates an
18-branch mapping that would drift).

### The card

`AiHubArtifactsCard` renders inside `AiHubPanel`'s already-`relative` root as an absolutely-positioned
island — `absolute right-3 top-14 z-10 w-64`, styled to match the resource panel's card
(`rounded-xl border bg-surface-neutral-primary shadow-sm`).

**Render conditions.** All four must hold:

- a task is active,
- `!rightPanelOpen`,
- the task kind is not `WORKFLOW_CHAT` (webhook-routed; never produces artifacts),
- the artifact list is non-empty.

The empty case renders nothing at all. A floating "No artifacts yet" card is noise; that message survives
only in the `+` menu, which the user opens deliberately.

**Structure.** A header row (`Artifacts`, count, collapse chevron) over a `max-h-64 overflow-y-auto` list of
`AiHubArtifactRow`s. Rows keep today's affordances: the per-kind icon, the truncated name, and the remove
`X` on the seven `*_REFERENCED` kinds (`isArtifactRemovable`).

**Collapse.** One persisted boolean in `useAiHubSettingsStore` (global, not per-task). Collapsed, the card
is a small `Artifacts · 3` pill. A card that overlays the transcript must be dismissible without being
forgotten between tasks.

**Contents.** Every artifact kind, agent-produced and user-attached alike — a straight relocation of today's
list under the title "Artifacts". No filtering concept is introduced, and nothing the sidebar showed becomes
unreachable.

**Behavior when the resource panel opens.** The card disappears. The panel's tab strip is already the list
of what has been opened, and the chat pane narrows to ~38% when the panel is open, which leaves no room for
a 256px overlay. Clicking a row therefore opens the panel and hides the card in one motion — the
interaction closes on itself.

**Query cost.** The card issues exactly one `useAiHubTaskArtifactsQuery`, for the active task. This is
strictly cheaper than the sidebar's N-rows-N-queries risk, so the `isExpanded` deferral and its
"badge only appears after first expand" wart both disappear.

### The `+` menu branch

`AiHubFilePicker` models its menu as a `MenuPathType` union with drill-in branches (`:92`). Artifacts is one
more arm:

- `MenuPathType` gains `['artifacts']`.
- A root `CommandItem` — "Artifacts", `PackageIcon` — placed last, after Workflow Executions.
- The branch reads `useAiHubTaskArtifactsQuery(currentTaskId, workspaceId)` (a warm cache hit; same query
  the card uses) and renders one `CommandItem` per artifact, guarded by `isArtifactClickable`. Selecting one
  calls `handleArtifactQuickOpen` and closes the popover.
- Empty state: `CommandEmpty` with "No artifacts yet".

Scope is the **current task's** artifacts, matching the card. The two surfaces are one list.

Artifacts are deliberately **excluded from the picker's cross-kind search view** (`:285-423`, the flat
results shown once a search term is typed). An artifact for a file is that file; including them would list
every matching file twice.

### Deletions in `AiHubTasksSidebar.tsx`

- Helpers: `getArtifactIcon`, `parseMetadataJson`, `handleArtifactQuickOpen`, `openCodeWorkflowArtifact`,
  `openArtifactInTask`, `isArtifactRemovable`, `isArtifactClickable`.
- Components: `ArtifactRow`, `ArtifactList`.
- In `TaskItem`: the `isExpanded` / `onToggleExpand` props, `showArtifacts`, the deferred
  `useAiHubTaskArtifactsQuery` + `artifactCount`, the count badge (`:862`), the expand chevron (`:878`), and
  the trailing `{isExpanded && showArtifacts && <ArtifactList …/>}` (`:936`).
- In the parent list: `expandedTaskIds` state (`:951`), `handleToggleExpand` (`:1178`), and the two props
  passed at `:1416` / `:1422`.
- Imports pruned to whatever the linter still requires (`ChevronLeftIcon` stays — the archive toggle uses
  it; `ChevronRightIcon` stays if the Context collapsible still needs it).

## Decisions

- **No `+` on the card.** ChatGPT's Outputs card has one. Here the composer's `+` already attaches
  resources, and `useRecordReferencedArtifacts` mirrors every resulting tab back into the artifact list, so
  a card-level `+` would be a third route to one outcome.
- **Cross-task artifact access is dropped.** The sidebar could open an artifact belonging to a
  non-active task (`openArtifactInTask` switched tasks first). Artifacts are now reachable by opening their
  task. The workspace-wide paged `aiHubTaskArtifacts` query remains unused by this feature; a cross-task
  history surface, if wanted, is separate work.
- **Card title is "Artifacts", not "Outputs".** The list holds both agent-produced and user-attached rows;
  "Outputs" would misdescribe half of them.

## Testing

- `artifacts/tests/artifactOpen.test.ts` — the migrated kind-dispatch cases (workflow execution, skill,
  custom component, code workflow), unchanged. They test an exported function and never touched the sidebar
  component.
- `artifacts/tests/AiHubArtifactsCard.test.tsx` — the four render conditions (hidden when the panel is open,
  hidden when empty, hidden for `WORKFLOW_CHAT`, shown otherwise) and that a row click opens a tab.
- `AiHubTasksSidebar.test.tsx` — the two artifact `describe` blocks removed; remaining task-row cases must
  still pass against the flattened row.
- Existing `AiHubPanel.test.tsx` may need the new artifacts query mocked.
- Verification: `cd client && npm run check` (lint + typecheck + tests).

## Out of scope

- Server, GraphQL schema, and `ai_hub_task_artifact` semantics are untouched.
- The composer's `ResourcePickerMenu` (a separate, richer picker) is untouched.
- Cross-task / workspace-wide artifact history.
