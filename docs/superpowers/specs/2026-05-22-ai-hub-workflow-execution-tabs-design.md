# AI Hub — Full Workflow Editor & Workflow Execution Tabs

**Date:** 2026-05-22
**Status:** Draft for review
**Scope:** AI Hub right-panel resource tabs (`client/src/pages/automation/ai-hub/`)

## Summary

Two changes to the AI Hub right-panel resource tabs:

1. The **workflow** tab hosts the *full* workflow editor — the project header (Run,
   workflow actions, publish/deploy), workflow inputs, component palette, code editor,
   and an in-tab test-output panel — instead of today's slimmed embed, and drops the
   now-redundant "Open in editor" link.
2. A new **workflow execution** tab kind shows a workflow execution's run detail inside
   the right panel. Workflow-execution artifacts that today open in a new browser tab
   from the Tasks-sidebar artifact list instead open as a right-panel tab — which also
   covers agent-run executions, since those surface as artifacts.

This supersedes the earlier standalone "#1 — relocate the `⊞`/`</>` toolbar" idea: a
full editor tab redefines that header, so #1 is folded in here (see *Workflow tab header*).

## Background

The AI Hub right panel (`AiHubResourcePanel`) shows resource tabs. `AiHubTabType`
(`stores/useAiHubTabsStore.ts`) currently has four kinds — `file`, `workflow`,
`dataTable`, `knowledgeBase`. `renderTabBody` in `AiHubResourcePanel.tsx` dispatches on
`tab.kind`.

- The `workflow` tab renders `AiHubWorkflowViewer`, which has a header (workflow name +
  an "Open in editor" link that opens the full editor in a **new browser tab**) and
  embeds `EmbeddableWorkflowEditor`.
- `EmbeddableWorkflowEditor` mounts the shared `WorkflowEditorLayout` with
  `leftSidebarOpen={false}`, `showCopilot={false}`, `showWorkflowInputs={false}` — a
  deliberately slim editor.
- Workflow executions have **no** tab kind. The Tasks-sidebar artifact list opens
  `WORKFLOW_EXECUTION_STARTED` rows via `window.open('/automation/executions/<id>',
  '_blank')`. The canonical execution-detail UI is `WorkflowExecutionSheet`, a slide-over
  on the Workflow Executions page, driven by `useWorkflowExecutionSheetStore`.

## Design

### Part A — Full editor in the workflow tab

The workflow tab renders the **project header** (`ProjectHeader`) above the editor, plus
the collapsible bottom test-output panel — mirroring the right-hand column of the full
project page (`Project.tsx`). This is what delivers **Run**: `ProjectHeader` owns the
Run / Stop controls, the output-panel toggle, workflow actions, publish and deploy.

`EmbeddableWorkflowEditor` is restructured to match `Project.tsx`'s right column:

- `ProjectHeader` at the top — fed `projectId`, `projectWorkflowId`, `runDisabled` (from
  `useRun()`), `updateWorkflowMutation` (the editor mutation it already builds),
  `chatTrigger` (derived from `workflow.triggers`), and a `bottomResizablePanelRef`.
- A vertical `ResizablePanelGroup`: `WorkflowEditorLayout` on top, and
  `WorkflowExecutionsTestOutput` in the collapsible bottom panel driven by
  `bottomResizablePanelRef` (so a Run's output surfaces in-tab).
- `WorkflowEditorLayout` gets `showWorkflowInputs={true}`. `leftSidebarOpen` stays
  `false` — that sidebar is the project/workflow tree, out of place in a single-workflow
  tab. `showCopilot` stays `false` — the AI Hub chat *is* the copilot.

`ProjectHeader` is built for full-page width and carries project-lifecycle controls
(breadcrumb, publish, deploy, settings, left-sidebar toggle). It is embedded **as-is**;
if it crowds the ~60 % panel width, a compact pass (e.g. hiding the left-sidebar toggle,
condensing the breadcrumb) is an implementation refinement, not a separate design.

The editor's floating `⊞`/`</>` toolbar (`WorkflowRightSidebar`) stays as the editor's
standard chrome.

### Part B — Workflow execution tab

New `AiHubTabType` variant:

```ts
{id: string; kind: 'workflowExecution'; workflowExecutionId: number; name: string}
```

New store action `openWorkflowExecutionTab(workflowExecutionId, name)` mirroring
`openWorkflowTab`: find an existing tab with the same `workflowExecutionId` → focus it,
otherwise append; sets `rightPanelOpen: true`. Covered by the per-task snapshot/persist
logic like the other kinds.

New component `AiHubWorkflowExecutionViewer`, rendered by `renderTabBody` for
`kind: 'workflowExecution'`. It reuses the execution sheet's two inner panels —
`WorkflowExecutionSheetContent` (job / trigger / task-execution accordion + output/error
tabs) and `WorkflowExecutionSheetWorkflowPanel` (read-only workflow canvas) — *without*
the `Sheet` wrapper.

`WorkflowExecutionSheetWorkflowPanel` takes just `{workflowExecution}` and renders
standalone. `WorkflowExecutionSheetContent` is heavier — it needs ~13 orchestrated props
(`activeTab`, `selectedItem`, `dialogOpen`, `deepestFailedExecution`, `handleTaskClick`,
…) produced by the `useWorkflowExecutionSheet` hook. The current hook reads its execution
id from `useWorkflowExecutionSheetStore`; the viewer therefore extracts the sheet's body
into a shared `WorkflowExecutionDetail` component that takes `workflowExecutionId` as a
prop. `WorkflowExecutionSheet` (the existing slide-over) and `AiHubWorkflowExecutionViewer`
(the new tab) both render `WorkflowExecutionDetail`. The plan pins the exact refactor.

Layout: **side-by-side**, the same 50/50 split the sheet uses — content panel left,
read-only workflow canvas right.

### Part C — Rerouting the artifact-list entry point

`AiHubTasksSidebar.handleArtifactQuickOpen`'s `WORKFLOW_EXECUTION_STARTED` branch calls
`aiHubTabsStore.getState().openWorkflowExecutionTab(Number(artifact.artifactId),
artifact.artifactName)` instead of `window.open('/automation/executions/<id>', '_blank')`.
`artifactId` is the execution id; `isArtifactClickable` already returns `true` for this
kind.

This also covers the **chat** path: an agent workflow run records a
`WORKFLOW_EXECUTION_STARTED` artifact, which appears in the Tasks-sidebar artifact list —
so rerouting that one quick-open handler makes agent-run executions open as tabs too. A
literal "View execution" control inside the chat bubble is **not** added: the
`runChatWorkflow` tool result carries only progressive output text, no execution id, so
an in-bubble control would need a server change to surface the id (out of scope).

### Workflow tab header (resolves earlier "#1")

`AiHubWorkflowViewer`'s bespoke header (workflow name + "Open in editor" link) is
**replaced** by `ProjectHeader` (Part A). "Open in editor" is removed — the tab *is* the
full editor. The earlier standalone "#1 — move the `⊞`/`</>` toolbar into the tab strip"
is dropped: in a full-editor tab the editor's own floating toolbar is correct as-is.

### Artifact recording

`useRecordReferencedArtifacts` is **not** extended for the new kind. Executions are
already captured server-side as `WORKFLOW_EXECUTION_STARTED` artifacts; opening one as a
tab needs no second artifact record. The hook's `KIND_TO_ARTIFACT_KIND` map simply has
no `workflowExecution` entry, so the new tab kind is skipped — no server change needed.

## Files

**Modify**

- `stores/useAiHubTabsStore.ts` — `workflowExecution` variant, `openWorkflowExecutionTab`,
  snapshot/persist coverage.
- `AiHubResourcePanel.tsx` — `renderTabBody` branch for `workflowExecution`.
- `AiHubWorkflowViewer.tsx` — drop the bespoke header (now `ProjectHeader`); may
  collapse into `EmbeddableWorkflowEditor` if no wrapper logic remains.
- `EmbeddableWorkflowEditor.tsx` — restructure to mirror `Project.tsx`'s right column:
  `ProjectHeader` + vertical `ResizablePanelGroup` (`WorkflowEditorLayout` +
  `WorkflowExecutionsTestOutput`); `showWorkflowInputs={true}`.
- `tasks/AiHubTasksSidebar.tsx` — reroute the `WORKFLOW_EXECUTION_STARTED` quick-open to
  `openWorkflowExecutionTab`.
- `workflow-executions/.../WorkflowExecutionSheet.tsx` (+ `useWorkflowExecutionSheet`) —
  extract the sheet body into `WorkflowExecutionDetail`, parameterized by
  `workflowExecutionId`; the sheet renders it.

**Create**

- `AiHubWorkflowExecutionViewer.tsx` — the `workflowExecution` tab body.
- `workflow-executions/.../WorkflowExecutionDetail.tsx` — shared execution-detail body
  (used by both the sheet and the AI Hub tab).

## Testing

- `useAiHubTabsStore` tests: `openWorkflowExecutionTab` opens a new tab / focuses an
  existing one, sets `rightPanelOpen`, and snapshots per task.
- `AiHubWorkflowExecutionViewer` test: renders execution content for a given id
  (GraphQL queries mocked).
- `AiHubTasksSidebar` quick-open test: a `WORKFLOW_EXECUTION_STARTED` artifact opens an
  execution tab and does not call `window.open`.

## Resolved decisions

1. **Run** — enabled. The workflow tab gets the full `ProjectHeader`, which carries the
   Run / Stop controls (Part A).
2. **Execution viewer layout** — side-by-side, the 50/50 split the sheet uses (Part B).

## Implementation notes

- **Execution viewer reuse** — `WorkflowExecutionSheetContent` needs ~13 orchestrated
  props from `useWorkflowExecutionSheet`, which today reads `useWorkflowExecutionSheetStore`.
  The plan extracts a shared `WorkflowExecutionDetail` parameterized by id, so the sheet
  and the AI Hub tab share one body.
- **`ProjectHeader` fit** — embedded as-is; a compact pass is a refinement if it crowds
  the panel (Part A).

## Out of scope

- Changes to the standalone Workflow Executions page or its `WorkflowExecutionSheet`.
- Any server-side change (new artifact kinds, new GraphQL operations).
