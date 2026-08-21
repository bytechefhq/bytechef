# AI Hub Workflow Tab → Project Editor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the AI Hub resource-panel workflow tab behave like the Project editor: project-scoped tabs titled "Project V{n}", an in-tab workflow selector, and Publish/Deploy buttons.

**Architecture:** A workflow tab becomes project-scoped by changing the tabs-store dedup key from `workflowId` to `projectId`. The tab-strip title is rendered from a small project-query component. The existing embedded `ProjectHeader` gains opt-in, default-off props to surface `WorkflowSelect` (left) and `PublishPopover`+`DeployButton` (right), with an `onWorkflowChange` override so the AI Hub re-points the tab instead of navigating the route. `EmbeddableWorkflowEditor` threads those props through, and `AiHubWorkflowViewer` wires them.

**Tech Stack:** React 19 + TypeScript, Zustand, TanStack Query, Vitest + Testing Library, Tailwind.

**Spec:** `docs/superpowers/specs/2026-06-01-ai-hub-workflow-tab-project-editor-design.md`

**Conventions:** All client commands run from `client/`. Run a single test file with `npx vitest run <path>`. ESLint enforces alphabetical object-key order (`sort-keys`) and alphabetical named-import order. Interface names end in `I` or `Props`. Commit messages: `hub - <description>` (this is AI Hub work).

---

### Task 1: Make workflow tabs project-scoped (store dedup by projectId)

Today `openWorkflowTab` dedups by `workflowId`, so opening a second workflow from the same project creates a second tab. For project-scoped tabs we dedup by `projectId` and, on a match, re-point the existing tab's selected workflow.

**Files:**
- Modify: `client/src/pages/automation/ai-hub/stores/useAiHubTabsStore.ts` (the `openWorkflowTab` action, ~lines 187–226)
- Test: `client/src/pages/automation/ai-hub/stores/tests/useAiHubTabsStore.test.ts`

- [ ] **Step 1: Write the failing tests**

Append this `describe` block to `client/src/pages/automation/ai-hub/stores/tests/useAiHubTabsStore.test.ts` (keep existing imports; `aiHubTabsStore` is already imported there — if not, add `import {aiHubTabsStore} from '@/pages/automation/ai-hub/stores/useAiHubTabsStore';`):

```ts
describe('openWorkflowTab project-scoped dedup', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({
            activeTabId: undefined,
            activeTaskId: undefined,
            openTabs: [],
            rightPanelOpen: false,
            snapshotsByTaskId: {},
            tasksSidebarCollapsed: true,
        });
    });

    it('opens a single tab per project and re-points it when another workflow of the same project opens', () => {
        const store = aiHubTabsStore.getState();

        store.openWorkflowTab('wf-a', 'project-1', 11, 'Workflow A');
        store.openWorkflowTab('wf-b', 'project-1', 12, 'Workflow B');

        const workflowTabs = aiHubTabsStore.getState().openTabs.filter((tab) => tab.kind === 'workflow');

        expect(workflowTabs).toHaveLength(1);

        const tab = workflowTabs[0]!;

        if (tab.kind === 'workflow') {
            expect(tab.projectId).toBe('project-1');
            expect(tab.workflowId).toBe('wf-b');
            expect(tab.projectWorkflowId).toBe(12);
            expect(tab.name).toBe('Workflow B');
        }
    });

    it('opens separate tabs for workflows from different projects', () => {
        const store = aiHubTabsStore.getState();

        store.openWorkflowTab('wf-a', 'project-1', 11, 'Workflow A');
        store.openWorkflowTab('wf-c', 'project-2', 21, 'Workflow C');

        const workflowTabs = aiHubTabsStore.getState().openTabs.filter((tab) => tab.kind === 'workflow');

        expect(workflowTabs).toHaveLength(2);
    });
});
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `npx vitest run src/pages/automation/ai-hub/stores/tests/useAiHubTabsStore.test.ts -t "project-scoped dedup"`
Expected: FAIL — the first test sees 2 workflow tabs (dedup is by `workflowId`, so `wf-a` and `wf-b` both create tabs).

- [ ] **Step 3: Change the dedup key to projectId and re-point on match**

In `client/src/pages/automation/ai-hub/stores/useAiHubTabsStore.ts`, replace the body of `openWorkflowTab` (the `set((state) => {...})` block, ~lines 190–223) with:

```ts
                    set((state) => {
                        const existing = state.openTabs.find(
                            (tab): tab is Extract<AiHubTabType, {kind: 'workflow'}> =>
                                tab.kind === 'workflow' && tab.projectId === projectId
                        );

                        if (existing) {
                            tabIdToReturn = existing.id;

                            // A workflow tab is project-scoped: opening another workflow from the same
                            // project (sidebar/composer pick or the in-tab selector) re-points THIS tab's
                            // selected workflow instead of spawning a duplicate. projectId/tab id stay put.
                            return {
                                ...state,
                                activeTabId: existing.id,
                                openTabs: state.openTabs.map((tab) =>
                                    tab.id === existing.id && tab.kind === 'workflow'
                                        ? {...tab, name, projectWorkflowId, workflowId}
                                        : tab
                                ),
                                rightPanelOpen: true,
                            };
                        }

                        const newTab: AiHubTabType = {
                            id: getRandomId(),
                            kind: 'workflow',
                            name,
                            projectId,
                            projectWorkflowId,
                            workflowId,
                        };

                        tabIdToReturn = newTab.id;

                        return {
                            ...state,
                            activeTabId: newTab.id,
                            openTabs: [...state.openTabs, newTab],
                            rightPanelOpen: true,
                        };
                    });
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `npx vitest run src/pages/automation/ai-hub/stores/tests/useAiHubTabsStore.test.ts`
Expected: PASS (new dedup tests + all pre-existing store tests).

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/automation/ai-hub/stores/useAiHubTabsStore.ts client/src/pages/automation/ai-hub/stores/tests/useAiHubTabsStore.test.ts
git commit -m "hub - Make AI Hub workflow tabs project-scoped (dedup by projectId)"
```

---

### Task 2: Tab-strip title shows "Project V{n}"

The tab strip renders `tab.name`. For workflow tabs we render a small component that resolves the project name + version from `projectId`.

**Files:**
- Create: `client/src/pages/automation/ai-hub/WorkflowTabLabel.tsx`
- Create: `client/src/pages/automation/ai-hub/tests/WorkflowTabLabel.test.tsx`
- Modify: `client/src/pages/automation/ai-hub/AiHubResourcePanel.tsx` (tab-strip button, ~line 105)

- [ ] **Step 1: Write the failing test**

Create `client/src/pages/automation/ai-hub/tests/WorkflowTabLabel.test.tsx`:

```tsx
import {render, screen} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import WorkflowTabLabel from '../WorkflowTabLabel';

const useGetProjectQueryMock = vi.fn();

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetProjectQuery: (...args: unknown[]) => useGetProjectQueryMock(...args),
}));

describe('WorkflowTabLabel', () => {
    afterEach(() => {
        vi.clearAllMocks();
    });

    it('renders the project name with version once the project query resolves', () => {
        useGetProjectQueryMock.mockReturnValue({data: {lastProjectVersion: 2, name: 'AI Agent 2'}});

        render(<WorkflowTabLabel fallbackName="agent1" projectId="7" />);

        expect(screen.getByText('AI Agent 2 V2')).toBeInTheDocument();
    });

    it('falls back to the workflow name while the project is loading', () => {
        useGetProjectQueryMock.mockReturnValue({data: undefined});

        render(<WorkflowTabLabel fallbackName="agent1" projectId="7" />);

        expect(screen.getByText('agent1')).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `npx vitest run src/pages/automation/ai-hub/tests/WorkflowTabLabel.test.tsx`
Expected: FAIL — `WorkflowTabLabel` module does not exist.

- [ ] **Step 3: Create the component**

Create `client/src/pages/automation/ai-hub/WorkflowTabLabel.tsx`:

```tsx
import {useGetProjectQuery} from '@/shared/queries/automation/projects.queries';

interface WorkflowTabLabelProps {
    fallbackName: string;
    projectId: string;
}

/**
 * Tab-strip label for a workflow (project-scoped) tab. Shows "<projectName> V<version>" resolved live
 * from the project, falling back to the selected workflow's name while the project query is loading or
 * if it errors. The query is react-query-cached so multiple tabs of the same project share one fetch.
 */
const WorkflowTabLabel = ({fallbackName, projectId}: WorkflowTabLabelProps) => {
    const {data: project} = useGetProjectQuery(Number(projectId), undefined, Number(projectId) > 0);

    if (!project) {
        return <>{fallbackName}</>;
    }

    const version = project.lastProjectVersion != null ? ` V${project.lastProjectVersion}` : '';

    return <>{`${project.name}${version}`}</>;
};

export default WorkflowTabLabel;
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `npx vitest run src/pages/automation/ai-hub/tests/WorkflowTabLabel.test.tsx`
Expected: PASS.

- [ ] **Step 5: Wire it into the tab strip**

In `client/src/pages/automation/ai-hub/AiHubResourcePanel.tsx`, add the import near the other imports (keep named-import order):

```tsx
import WorkflowTabLabel from '@/pages/automation/ai-hub/WorkflowTabLabel';
```

Then replace the tab button label (the `{tab.name}` inside the `<button …title={tab.name}…>` at ~line 105) with a workflow-aware label:

```tsx
                                    <button
                                        className="max-w-40 truncate"
                                        onClick={() => setActiveTab(tab.id)}
                                        title={tab.name}
                                        type="button"
                                    >
                                        {tab.kind === 'workflow' ? (
                                            <WorkflowTabLabel fallbackName={tab.name} projectId={tab.projectId} />
                                        ) : (
                                            tab.name
                                        )}
                                    </button>
```

- [ ] **Step 6: Typecheck + run the panel's existing tests**

Run: `npx vitest run src/pages/automation/ai-hub/tests/AiHubPanel.test.tsx && npx tsc --noEmit`
Expected: PASS / no type errors. (If `tsc --noEmit` is slow, `npm run typecheck` is equivalent.)

- [ ] **Step 7: Commit**

```bash
git add client/src/pages/automation/ai-hub/WorkflowTabLabel.tsx client/src/pages/automation/ai-hub/tests/WorkflowTabLabel.test.tsx client/src/pages/automation/ai-hub/AiHubResourcePanel.tsx
git commit -m "hub - Show project name + version as AI Hub workflow tab title"
```

---

### Task 3: Extend ProjectHeader with opt-in selector + Publish/Deploy + onWorkflowChange

`ProjectHeader` hides the workflow selector and Publish/Deploy when `embedded`. Add three opt-in props (all default to current behavior) so the AI Hub embedded editor can surface them with its own selector wiring. This is presentational glue over already-tested components (`WorkflowSelect`, `PublishPopover`, `DeployButton`); it is covered by typecheck + a manual visual step at the end of the plan rather than a heavy full-render test (ProjectHeader depends on ~6 stores/hooks, and `WorkflowSelect`/`PublishPopover` already have their own unit tests).

**Files:**
- Modify: `client/src/pages/automation/project/components/project-header/ProjectHeader.tsx`

- [ ] **Step 1: Add the new optional props to the interface**

In `client/src/pages/automation/project/components/project-header/ProjectHeader.tsx`, extend `ProjectHeaderProps` (keep keys alphabetical):

```tsx
interface ProjectHeaderProps {
    bottomResizablePanelRef: RefObject<PanelImperativeHandle | null>;
    chatTrigger?: boolean;
    embedded?: boolean;
    onWorkflowChange?: (projectWorkflowId: number) => void;
    projectId: number;
    projectWorkflowId: number;
    runDisabled: boolean;
    showPublishDeploy?: boolean;
    showWorkflowSelect?: boolean;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}
```

And add them to the destructure (keep alphabetical):

```tsx
const ProjectHeader = ({
    bottomResizablePanelRef,
    chatTrigger,
    embedded,
    onWorkflowChange,
    projectId,
    projectWorkflowId,
    runDisabled,
    showPublishDeploy,
    showWorkflowSelect,
    updateWorkflowMutation,
}: ProjectHeaderProps) => {
```

- [ ] **Step 2: Import WorkflowSelect**

Add to the imports (keep import order; it sits with the other `project-header/components/*` imports):

```tsx
import WorkflowSelect from '@/pages/automation/project/components/project-header/components/WorkflowSelect';
```

- [ ] **Step 3: Render the selector on the left when requested**

In the left `<div className="flex items-center">` (currently only the `!embedded` breadcrumb block), add an embedded-selector branch AFTER the existing `{!embedded && (...)}` block, still inside the left div:

```tsx
                {embedded && showWorkflowSelect && projectWorkflows && (
                    <WorkflowSelect
                        currentWorkflowLabel={workflow?.label}
                        onValueChange={onWorkflowChange ?? handleProjectWorkflowValueChange}
                        projectId={projectId}
                        projectWorkflowId={projectWorkflowId}
                        projectWorkflows={projectWorkflows}
                    />
                )}
```

- [ ] **Step 4: Render Publish/Deploy on the right when requested**

In the right `<div className="flex items-center">`, after the existing `{!embedded && (<ButtonGroup>…</ButtonGroup>)}` block, add:

```tsx
                {embedded && showPublishDeploy && (
                    <ButtonGroup>
                        <PublishPopover
                            isPending={publishProjectMutationIsPending}
                            onPublishProjectSubmit={handlePublishProjectSubmit}
                        />

                        <DeployButton project={project} />
                    </ButtonGroup>
                )}
```

- [ ] **Step 5: Typecheck**

Run: `npx tsc --noEmit`
Expected: no type errors. (`workflow?.label` is safe — `workflow` may be the empty initial object.)

- [ ] **Step 6: Run existing project-header tests (regression guard)**

Run: `npx vitest run src/pages/automation/project/components/project-header`
Expected: PASS — the new branches are gated on the new flags (default `undefined`/falsy), so existing non-embedded and embedded callers are unchanged.

- [ ] **Step 7: Commit**

```bash
git add client/src/pages/automation/project/components/project-header/ProjectHeader.tsx
git commit -m "hub - Add opt-in workflow selector + Publish/Deploy to embedded ProjectHeader"
```

---

### Task 4: Thread the new props through EmbeddableWorkflowEditor

`EmbeddableWorkflowEditor` wraps `ProjectHeader embedded`. Forward the three new props so callers (the AI Hub) can opt in.

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/EmbeddableWorkflowEditor.tsx`

- [ ] **Step 1: Extend the inner and outer prop interfaces**

Edit `EmbeddableWorkflowEditorInnerPropsI` (~line 41) and `EmbeddableWorkflowEditorPropsI` (~line 241) to both read (keys alphabetical):

```tsx
interface EmbeddableWorkflowEditorInnerPropsI {
    onWorkflowChange?: (projectWorkflowId: number) => void;
    projectId: number;
    projectWorkflowId: number;
    showPublishDeploy?: boolean;
    showWorkflowSelect?: boolean;
}
```

```tsx
export interface EmbeddableWorkflowEditorPropsI {
    onWorkflowChange?: (projectWorkflowId: number) => void;
    projectId: number;
    projectWorkflowId: number;
    showPublishDeploy?: boolean;
    showWorkflowSelect?: boolean;
}
```

- [ ] **Step 2: Destructure the new props in the inner component**

Change the inner signature (~line 51):

```tsx
const EmbeddableWorkflowEditorInner = ({
    onWorkflowChange,
    projectId,
    projectWorkflowId,
    showPublishDeploy,
    showWorkflowSelect,
}: EmbeddableWorkflowEditorInnerPropsI) => {
```

- [ ] **Step 3: Pass them to ProjectHeader**

Update the `<ProjectHeader …/>` usage (~line 199) to add the three props (keep prop order alphabetical):

```tsx
                    <ProjectHeader
                        bottomResizablePanelRef={bottomResizablePanelRef}
                        chatTrigger={chatTrigger}
                        embedded
                        onWorkflowChange={onWorkflowChange}
                        projectId={projectId}
                        projectWorkflowId={projectWorkflowId}
                        runDisabled={runDisabled}
                        showPublishDeploy={showPublishDeploy}
                        showWorkflowSelect={showWorkflowSelect}
                        updateWorkflowMutation={updateWorkflowEditorMutation}
                    />
```

- [ ] **Step 4: Forward props through the outer wrapper**

Update the default-exported wrapper (~line 262):

```tsx
const EmbeddableWorkflowEditor = ({
    onWorkflowChange,
    projectId,
    projectWorkflowId,
    showPublishDeploy,
    showWorkflowSelect,
}: EmbeddableWorkflowEditorPropsI) => (
    <WorkflowDataStoreProvider>
        <EmbeddableWorkflowEditorInner
            onWorkflowChange={onWorkflowChange}
            projectId={projectId}
            projectWorkflowId={projectWorkflowId}
            showPublishDeploy={showPublishDeploy}
            showWorkflowSelect={showWorkflowSelect}
        />
    </WorkflowDataStoreProvider>
);
```

- [ ] **Step 5: Typecheck**

Run: `npx tsc --noEmit`
Expected: no type errors. Existing callers that pass only `projectId`/`projectWorkflowId` still compile (new props optional).

- [ ] **Step 6: Commit**

```bash
git add client/src/pages/platform/workflow-editor/EmbeddableWorkflowEditor.tsx
git commit -m "hub - Forward selector/Publish-Deploy props through EmbeddableWorkflowEditor"
```

---

### Task 5: Wire the AI Hub workflow viewer (selector re-points the tab)

`AiHubWorkflowViewer` opts into the selector + Publish/Deploy and provides `onWorkflowChange`, which resolves the chosen `projectWorkflowId` to its workflow `id`+`label` and re-points the project-scoped tab via `openWorkflowTab`. The resolution is extracted as a pure helper so it can be unit-tested without rendering the editor.

**Files:**
- Modify: `client/src/pages/automation/ai-hub/AiHubWorkflowViewer.tsx`
- Test: `client/src/pages/automation/ai-hub/tests/AiHubWorkflowViewer.test.tsx`

- [ ] **Step 1: Write the failing test for the resolver helper**

Create `client/src/pages/automation/ai-hub/tests/AiHubWorkflowViewer.test.tsx`:

```tsx
import {describe, expect, it} from 'vitest';

import {resolveWorkflowTabSelection} from '../AiHubWorkflowViewer';

describe('resolveWorkflowTabSelection', () => {
    const workflows = [
        {id: 'wf-a', label: 'Workflow A', projectWorkflowId: 11},
        {id: 'wf-b', label: 'Workflow B', projectWorkflowId: 12},
    ];

    it('resolves the openWorkflowTab args for the chosen projectWorkflowId', () => {
        expect(resolveWorkflowTabSelection(workflows, 'project-1', 12)).toEqual({
            name: 'Workflow B',
            projectId: 'project-1',
            projectWorkflowId: 12,
            workflowId: 'wf-b',
        });
    });

    it('returns null when no workflow matches', () => {
        expect(resolveWorkflowTabSelection(workflows, 'project-1', 99)).toBeNull();
    });

    it('returns null when the workflow list is undefined', () => {
        expect(resolveWorkflowTabSelection(undefined, 'project-1', 12)).toBeNull();
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `npx vitest run src/pages/automation/ai-hub/tests/AiHubWorkflowViewer.test.tsx`
Expected: FAIL — `resolveWorkflowTabSelection` is not exported.

- [ ] **Step 3: Rewrite AiHubWorkflowViewer with the helper + wiring**

Replace the entire contents of `client/src/pages/automation/ai-hub/AiHubWorkflowViewer.tsx` with:

```tsx
import {aiHubTabsStore} from '@/pages/automation/ai-hub/stores/useAiHubTabsStore';
import EmbeddableWorkflowEditor from '@/pages/platform/workflow-editor/EmbeddableWorkflowEditor';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {useGetProjectWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';

interface AiHubWorkflowViewerProps {
    name: string;
    projectId: string;
    projectWorkflowId: number;
}

interface WorkflowTabSelectionI {
    name: string;
    projectId: string;
    projectWorkflowId: number;
    workflowId: string;
}

/**
 * Resolve the `openWorkflowTab` arguments for a workflow the user picked in the in-tab selector. The
 * selector only yields a `projectWorkflowId`; we look up the matching workflow to recover its UUID `id`
 * and `label`. Returns null when the list is missing or the id isn't found, so callers can no-op safely.
 */
export function resolveWorkflowTabSelection(
    projectWorkflows: Workflow[] | undefined,
    projectId: string,
    projectWorkflowId: number
): WorkflowTabSelectionI | null {
    const match = projectWorkflows?.find((workflow) => workflow.projectWorkflowId === projectWorkflowId);

    if (!match) {
        return null;
    }

    return {
        name: match.label ?? '',
        projectId,
        projectWorkflowId,
        workflowId: match.id ?? '',
    };
}

const AiHubWorkflowViewer = ({projectId, projectWorkflowId}: AiHubWorkflowViewerProps) => {
    const {data: projectWorkflows} = useGetProjectWorkflowsQuery(Number(projectId), Number(projectId) > 0);

    const handleWorkflowChange = (nextProjectWorkflowId: number) => {
        const selection = resolveWorkflowTabSelection(projectWorkflows, projectId, nextProjectWorkflowId);

        if (!selection) {
            return;
        }

        // Tab is project-scoped: openWorkflowTab dedups by projectId and re-points this tab's workflow.
        aiHubTabsStore
            .getState()
            .openWorkflowTab(selection.workflowId, selection.projectId, selection.projectWorkflowId, selection.name);
    };

    return (
        <div className="flex size-full flex-col">
            {projectWorkflowId > 0 ? (
                <EmbeddableWorkflowEditor
                    onWorkflowChange={handleWorkflowChange}
                    projectId={+projectId}
                    projectWorkflowId={projectWorkflowId}
                    showPublishDeploy
                    showWorkflowSelect
                />
            ) : (
                <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
                    Workflow reference unavailable.
                </div>
            )}
        </div>
    );
};

export default AiHubWorkflowViewer;
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `npx vitest run src/pages/automation/ai-hub/tests/AiHubWorkflowViewer.test.tsx`
Expected: PASS.

- [ ] **Step 5: Typecheck**

Run: `npx tsc --noEmit`
Expected: no type errors. (`AiHubResourcePanel` still passes `name`/`projectId`/`projectWorkflowId` to the viewer; the `name` prop remains in the interface for compatibility even though the viewer no longer renders it directly.)

- [ ] **Step 6: Commit**

```bash
git add client/src/pages/automation/ai-hub/AiHubWorkflowViewer.tsx client/src/pages/automation/ai-hub/tests/AiHubWorkflowViewer.test.tsx
git commit -m "hub - Wire in-tab workflow selector + Publish/Deploy in AiHubWorkflowViewer"
```

---

### Task 6: Full verification + manual visual check

**Files:** none (verification only)

- [ ] **Step 1: Run the full client check**

Run: `npm run check`
Expected: lint + typecheck + all tests PASS.

- [ ] **Step 2: Manual visual verification (covers Task 3's presentational glue)**

Start the client (`npm run dev`) with the backend running, open the AI Hub, open a workflow in the resource panel, and confirm:
- The tab-strip title reads `"<Project> V<n>"` (not the workflow name).
- The in-tab header shows the **workflow selector** on the left; switching it re-points the same tab (no new tab) and updates the canvas + selector, while the tab title (project) stays unchanged.
- Opening a different workflow from the **same** project via the composer/sidebar focuses the existing tab; a workflow from a **different** project opens a new tab.
- The in-tab header shows **Publish** and **Deploy** on the right; Publish opens the description popover and publishes; Deploy opens the deployment dialog.

- [ ] **Step 3: Final commit (if the visual check required any tweak)**

```bash
git add -A
git commit -m "hub - Finalize AI Hub workflow tab project-editor header"
```

---

## Self-review notes

- **Spec coverage:** Tab=project (Task 1) ✓; tab title project+version (Task 2) ✓; in-tab workflow selector left (Tasks 3–5) ✓; Publish/Deploy right (Tasks 3–5) ✓; `onWorkflowChange` re-points tab, no route nav (Tasks 3, 5) ✓; defaults keep the real Project page unchanged (Task 3 gating, Task 6 regression run) ✓.
- **Type consistency:** `openWorkflowTab(workflowId, projectId, projectWorkflowId, name)` used identically across Tasks 1 and 5; `resolveWorkflowTabSelection` returns `{name, projectId, projectWorkflowId, workflowId}` consumed verbatim in Task 5; the three new props (`onWorkflowChange`, `showPublishDeploy`, `showWorkflowSelect`) have matching names in Tasks 3 and 4.
- **No placeholders:** every code step contains complete code; verification steps give exact commands + expected outcomes.
