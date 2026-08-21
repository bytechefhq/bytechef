# AI Hub Workflow Tab → Project Editor — Design

Date: 2026-06-01
Status: Approved (design)
Scope: Client only (`client/`)

## Problem

When a workflow opens in the AI Hub resource panel (the right-side "island"), the tab is a bare
canvas: the tab-strip title shows just the workflow name (e.g. `agent1`), and the embedded editor
header hides the workflow selector, Publish, and Deploy. The user wants the workflow tab to read like
the real Project editor (`Project.tsx`):

1. The **tab-strip title** shows the **project name + version** (e.g. `AI Agent 2 V2`).
2. The in-tab header **left** side has a **workflow selector** showing the currently selected
   workflow (like `Project.tsx`'s `ProjectBreadcrumb` / `WorkflowSelect`).
3. The in-tab header **right** side has **Publish** and **Deploy** buttons (like `Project.tsx`).

## Key insight

Almost all of this already exists. `EmbeddableWorkflowEditor`
(`client/src/pages/platform/workflow-editor/EmbeddableWorkflowEditor.tsx`) renders
`ProjectHeader` with `embedded={true}`, which deliberately hides `ProjectBreadcrumb`
(`ProjectTitle` + `WorkflowSelect`), `PublishPopover`, and `DeployButton`. Those components are fully
built and wired (queries + mutations + dialogs). The work is to (a) make the AI Hub workflow tab
project-scoped, (b) surface those existing header pieces in embedded mode with AI-Hub-appropriate
wiring, and (c) render the tab-strip title from the project.

## Decisions (locked)

- **Tab = project.** A workflow tab represents a project. Opening another workflow from the same
  project focuses the existing tab and re-points its selected workflow, instead of opening a duplicate
  tab. (Mirrors `Project.tsx`, where the page is the project and the selector picks the workflow.)
- **One header row.** Extend the existing embedded `ProjectHeader` to optionally show the selector +
  Publish/Deploy, rather than stacking a second AI-Hub-specific header above the canvas. Defaults stay
  backward-compatible so the real Project page is unaffected.

## Architecture

### 1. Tab becomes project-scoped (`useAiHubTabsStore.ts`)

File: `client/src/pages/automation/ai-hub/stores/useAiHubTabsStore.ts`

- The `AiHubTabType` `'workflow'` variant keeps its fields
  (`id`, `kind`, `workflowId`, `projectId`, `projectWorkflowId`, `name`). `name` continues to hold the
  **selected workflow's** label (used as a fallback while the project query loads).
- Change `openWorkflowTab`'s **dedup key from `workflowId` to `projectId`**:
  - If a `'workflow'` tab with the same `projectId` already exists: activate it, set
    `rightPanelOpen: true`, and **update** that tab's `workflowId`, `projectWorkflowId`, and `name` to
    the newly chosen workflow. (This is what makes the in-tab selector and "open another workflow from
    the same project" both re-point the same tab.)
  - Otherwise: create a new tab as today.
- `openWorkflowTab`'s signature is unchanged: `(workflowId, projectId, projectWorkflowId, name)`.

### 2. Tab-strip title = "Project V{n}" (`AiHubResourcePanel.tsx`)

File: `client/src/pages/automation/ai-hub/AiHubResourcePanel.tsx` (tab-strip map, ~line 87–106 renders
`{tab.name}`).

- Introduce a small `WorkflowTabLabel` component used **only for `kind === 'workflow'`** tabs:
  - Props: `projectId: string`, `fallbackName: string`.
  - Uses `useGetProjectQuery(Number(projectId))` to read `project.name` and
    `project.lastProjectVersion`.
  - Renders `${project.name} V${project.lastProjectVersion}` once loaded; otherwise `fallbackName`
    (the current `tab.name`). The query is react-query-cached, so multiple same-project tabs share one
    fetch.
- Non-workflow tabs keep rendering `tab.name` verbatim.

### 3. In-tab header surfaces selector + Publish/Deploy (one row)

Files:
- `client/src/pages/automation/project/components/project-header/ProjectHeader.tsx`
- `client/src/pages/automation/project/components/project-header/hooks/useProjectHeader.ts`
- `client/src/pages/platform/workflow-editor/EmbeddableWorkflowEditor.tsx`
- `client/src/pages/automation/ai-hub/AiHubWorkflowViewer.tsx`

`ProjectHeader` currently gates `ProjectBreadcrumb` / `SettingsMenu` / `PublishPopover` /
`DeployButton` behind `!embedded`. Add **opt-in props** (all default to current behavior so the real
Project page is untouched):

- `showWorkflowSelect?: boolean` — when true (even if `embedded`), render `WorkflowSelect` on the left
  (the `ProjectTitle` part of the breadcrumb stays hidden in embedded mode since the project+version is
  already in the tab-strip title; only the selector shows).
- `showPublishDeploy?: boolean` — when true (even if `embedded`), render `PublishPopover` +
  `DeployButton` on the right.
- `onWorkflowChange?: (projectWorkflowId: number) => void` — when provided, `WorkflowSelect`'s
  `onValueChange` calls this **instead of** the route-navigating `handleProjectWorkflowValueChange`.

`useProjectHeader` already fetches `project` (`useGetProjectQuery`) and `projectWorkflows`
(`useGetProjectWorkflowsQuery`) and exposes `handlePublishProjectSubmit` +
`publishProjectMutationIsPending`; `DeployButton` self-fetches deployments. So Publish/Deploy reuse the
existing wiring with **no new mutations**.

`EmbeddableWorkflowEditor` gains matching pass-through props
(`showWorkflowSelect`, `showPublishDeploy`, `onWorkflowChange`) forwarded to its inner `ProjectHeader`.

`AiHubWorkflowViewer` (`client/src/pages/automation/ai-hub/AiHubWorkflowViewer.tsx`) renders
`EmbeddableWorkflowEditor` with:
- `showWorkflowSelect={true}`, `showPublishDeploy={true}`.
- `onWorkflowChange={(projectWorkflowId) => …}` — resolves the chosen workflow's `id` + `label` from
  the project's workflow list and calls
  `aiHubTabsStore.getState().openWorkflowTab(workflowId, projectId, projectWorkflowId, label)`. Because
  the tab now dedups by `projectId`, this re-points the current tab in place (Section 1).

### Selector onChange data flow

`WorkflowSelect` options carry `workflow.projectWorkflowId` (value) and `workflow.label` (display).
`onWorkflowChange(projectWorkflowId)` looks up the matching `Workflow` in the already-loaded
`projectWorkflows` list to get its `id` (UUID) and `label`, then calls `openWorkflowTab(...)`. No route
navigation occurs in the AI Hub.

## Components / boundaries

- `WorkflowTabLabel` (new, AI Hub) — pure: given a `projectId`, renders project+version; depends only
  on `useGetProjectQuery`.
- `ProjectHeader` (extended) — new optional props are additive; existing callers behave identically.
- `AiHubWorkflowViewer` (extended) — owns the AI-Hub-specific `onWorkflowChange` wiring; depends on the
  tabs store and the project-workflows list.

## Error handling

- `WorkflowTabLabel`: while `useGetProjectQuery` is loading or errors, fall back to `fallbackName`
  (never blank). A deleted project → query error → fallback name; the tab still closes normally.
- Publish/Deploy reuse `Project.tsx`'s existing error/toast handling unchanged.
- Selecting a workflow whose lookup fails (not in the loaded list) is a no-op (no tab re-point).

## Testing

- `useAiHubTabsStore` unit tests: opening two different workflows from the **same** project yields
  **one** tab whose `workflowId`/`projectWorkflowId`/`name` reflect the most recently opened workflow;
  opening workflows from **different** projects yields separate tabs. Existing snapshot/restore tests
  keyed on tab identity still pass.
- `WorkflowTabLabel`: renders `Project V{n}` when the project query resolves; renders `fallbackName`
  while loading / on error.
- `ProjectHeader`: with `showWorkflowSelect` / `showPublishDeploy` set under `embedded`, the selector +
  Publish/Deploy render; with the flags unset (default), embedded mode hides them exactly as today
  (guards the real Project page from regressions).
- `AiHubWorkflowViewer`: `onWorkflowChange` resolves the workflow and calls `openWorkflowTab` with the
  right `(workflowId, projectId, projectWorkflowId, label)`.

## Out of scope

- Resource-picker loading/grouping/subtitle changes (separate spec:
  `2026-06-01-ai-hub-resource-picker-improvements-design.md`).
- Any backend change. Entirely client-side.
