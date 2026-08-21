# AI Hub — Full Workflow Editor & Workflow Execution Tabs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the AI Hub right-panel workflow tab host the full workflow editor (with Run), and add a new `workflowExecution` tab kind so workflow executions open as right-panel tabs instead of a new browser tab.

**Architecture:** Two independent phases. Phase 1 adds a `workflowExecution` tab kind end-to-end (store → viewer → `renderTabBody` → artifact-list reroute), reusing the workflow-execution sheet's body via a new shared `WorkflowExecutionDetail` component. Phase 2 restructures `EmbeddableWorkflowEditor` to mirror the full project page's right column (`ProjectHeader` + editor + in-tab test-output panel). Phases share no code and can ship separately.

**Tech Stack:** React 19 + TypeScript, Zustand stores, TanStack Query, Vitest + Testing Library, Tailwind.

**Spec:** `docs/superpowers/specs/2026-05-22-ai-hub-workflow-execution-tabs-design.md`

**Commit convention:** client changes → `520 client - <description>`. Stage only files you touched; the working tree has unrelated `automation-workflows` changes — never stage those. End commit messages with the `Co-Authored-By` trailer.

---

## File Structure

**Phase 1 — execution tabs**
- `client/src/pages/automation/ai-hub/stores/useAiHubTabsStore.ts` — *modify*: new `workflowExecution` tab variant + `openWorkflowExecutionTab`.
- `client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/hooks/useWorkflowExecutionDetail.ts` — *create*: detail orchestration hook, parameterized by execution id.
- `client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionDetail.tsx` — *create*: shared execution-detail body (50/50 split).
- `client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet.tsx` — *modify*: render `WorkflowExecutionDetail`.
- `client/src/pages/automation/ai-hub/AiHubWorkflowExecutionViewer.tsx` — *create*: the `workflowExecution` tab body.
- `client/src/pages/automation/ai-hub/AiHubResourcePanel.tsx` — *modify*: `renderTabBody` branch.
- `client/src/pages/automation/ai-hub/tasks/AiHubTasksSidebar.tsx` — *modify*: reroute `WORKFLOW_EXECUTION_STARTED` quick-open.

**Phase 2 — full editor**
- `client/src/pages/platform/workflow-editor/EmbeddableWorkflowEditor.tsx` — *modify*: `ProjectHeader` + test-output panel; `showWorkflowInputs`.
- `client/src/pages/automation/ai-hub/AiHubWorkflowViewer.tsx` — *modify*: drop bespoke header.

---

# Phase 1 — Workflow Execution Tabs

## Task 1: Add the `workflowExecution` tab kind to the tabs store

**Files:**
- Modify: `client/src/pages/automation/ai-hub/stores/useAiHubTabsStore.ts`
- Test: `client/src/pages/automation/ai-hub/stores/tests/useAiHubTabsStore.test.ts`

- [ ] **Step 1: Write the failing tests**

Add to `useAiHubTabsStore.test.ts`, before the final closing `});` of the `describe('useAiHubTabsStore', …)` block:

```ts
    describe('openWorkflowExecutionTab', () => {
        it('opens a workflowExecution tab with the correct fields and opens the right panel', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            let tabId = '';

            act(() => {
                tabId = result.current.openWorkflowExecutionTab(501, 'Run #501');
            });

            expect(result.current.openTabs).toHaveLength(1);

            const tab = result.current.openTabs[0]!;

            expect(tab.kind).toBe('workflowExecution');
            expect(tab.id).toBe(tabId);
            expect(tab.name).toBe('Run #501');

            if (tab.kind === 'workflowExecution') {
                expect(tab.workflowExecutionId).toBe(501);
            }

            expect(result.current.activeTabId).toBe(tabId);
            expect(result.current.rightPanelOpen).toBe(true);
        });

        it('focuses an existing tab when the same workflowExecutionId is opened again', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            let firstTabId = '';

            act(() => {
                firstTabId = result.current.openWorkflowExecutionTab(501, 'Run #501');
                result.current.openWorkflowExecutionTab(502, 'Run #502');
            });

            expect(result.current.openTabs).toHaveLength(2);
            expect(result.current.activeTabId).not.toBe(firstTabId);

            act(() => {
                result.current.openWorkflowExecutionTab(501, 'Run #501');
            });

            expect(result.current.openTabs).toHaveLength(2);
            expect(result.current.activeTabId).toBe(firstTabId);
        });
    });
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `cd client && npx vitest run src/pages/automation/ai-hub/stores/tests/useAiHubTabsStore.test.ts`
Expected: FAIL — `openWorkflowExecutionTab` does not exist on the store.

- [ ] **Step 3: Add the tab variant to `AiHubTabType`**

In `useAiHubTabsStore.ts`, extend the `AiHubTabType` union (after the `dataTable` line, before `knowledgeBase`):

```ts
    | {id: string; kind: 'dataTable'; dataTableId: string; name: string}
    | {id: string; kind: 'workflowExecution'; workflowExecutionId: number; name: string}
    | {id: string; kind: 'knowledgeBase'; knowledgeBaseId: string; name: string};
```

- [ ] **Step 4: Add `openWorkflowExecutionTab` to the `AiHubTabsStateI` interface**

In the `AiHubTabsStateI` interface, after `openKnowledgeBaseTab`:

```ts
    openKnowledgeBaseTab: (knowledgeBaseId: string, name: string) => string;
    openWorkflowExecutionTab: (workflowExecutionId: number, name: string) => string;
```

- [ ] **Step 5: Implement `openWorkflowExecutionTab`**

In the store object, after the `openWorkflowTab` implementation (mirrors it; note `workflowExecutionId` is a `number`):

```ts
                openWorkflowExecutionTab: (workflowExecutionId, name) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const existing = state.openTabs.find(
                            (tab): tab is Extract<AiHubTabType, {kind: 'workflowExecution'}> =>
                                tab.kind === 'workflowExecution' &&
                                tab.workflowExecutionId === workflowExecutionId
                        );

                        if (existing) {
                            tabIdToReturn = existing.id;

                            return {
                                ...state,
                                activeTabId: existing.id,
                                rightPanelOpen: true,
                            };
                        }

                        const newTab: AiHubTabType = {
                            id: getRandomId(),
                            kind: 'workflowExecution',
                            name,
                            workflowExecutionId,
                        };

                        tabIdToReturn = newTab.id;

                        return {
                            ...state,
                            activeTabId: newTab.id,
                            openTabs: [...state.openTabs, newTab],
                            rightPanelOpen: true,
                        };
                    });

                    return tabIdToReturn;
                },
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `cd client && npx vitest run src/pages/automation/ai-hub/stores/tests/useAiHubTabsStore.test.ts`
Expected: PASS — all tests including the two new ones.

- [ ] **Step 7: Verify `useRecordReferencedArtifacts` still typechecks**

The new tab kind flows into `useRecordReferencedArtifacts` (`tasks/hooks/useRecordReferencedArtifacts.ts`). Its `KIND_TO_ARTIFACT_KIND` is a `Partial<Record<…>>`, so a kind with no entry is skipped — the new kind needs **no** entry (executions are already recorded as `WORKFLOW_EXECUTION_STARTED`).

Run: `cd client && npm run typecheck`
Expected: PASS. If `useRecordReferencedArtifacts` errors on a non-exhaustive `kind`, add an early `return` for `tab.kind === 'workflowExecution'` in its tab loop.

- [ ] **Step 8: Commit**

```bash
git add client/src/pages/automation/ai-hub/stores/useAiHubTabsStore.ts client/src/pages/automation/ai-hub/stores/tests/useAiHubTabsStore.test.ts
git commit -m "$(printf '520 client - Add workflowExecution tab kind to AI Hub tabs store\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

---

## Task 2: Extract `useWorkflowExecutionDetail` from `useWorkflowExecutionSheet`

The execution-detail orchestration (data fetch, polling, `activeTab`/`selectedItem`/`dialogOpen` state, `deepestFailedExecution`, `handleTaskClick`, the job-change effect) currently lives in `useWorkflowExecutionSheet` and reads the execution id from `useWorkflowExecutionSheetStore`. Extract it into a hook parameterized by `workflowExecutionId` so both the sheet and the AI Hub tab can use it.

**Files:**
- Create: `client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/hooks/useWorkflowExecutionDetail.ts`

- [ ] **Step 1: Create `useWorkflowExecutionDetail`**

This is `useWorkflowExecutionSheet` minus the store reads, copilot, and sheet open/close. It takes `workflowExecutionId` + `enabled` as parameters. Full file:

```ts
import {getWorkflowStatusType} from '@/shared/components/workflow-executions/util/workflowExecution-utils';
import {JobStatusEnum, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {useGetProjectWorkflowExecutionQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {TabValueType} from '@/shared/types';
import getDeepestFailedExecution from '@/shared/util/getDeepestFailedExecution';
import {useEffect, useMemo, useRef, useState} from 'react';

const POLLING_INTERVAL_MS = 2000;

/**
 * Execution-detail orchestration shared by the workflow-execution slide-over sheet and the AI Hub
 * `workflowExecution` resource tab. Parameterized by `workflowExecutionId` (the sheet's store-driven
 * version used to read the id from `useWorkflowExecutionSheetStore`; the tab passes a per-tab id).
 */
const useWorkflowExecutionDetail = (workflowExecutionId: number, enabled: boolean) => {
    const [activeTab, setActiveTab] = useState<TabValueType>('output');
    const [dialogOpen, setDialogOpen] = useState(false);
    const [selectedItem, setSelectedItem] = useState<TaskExecution | TriggerExecution | undefined>(undefined);

    const jobIdRef = useRef<string | undefined>(undefined);

    const {data: workflowExecution, isLoading: workflowExecutionLoading} = useGetProjectWorkflowExecutionQuery(
        {id: workflowExecutionId},
        enabled
    );

    const isWorkflowRunning = useMemo(() => {
        if (!workflowExecution?.job) {
            return false;
        }

        return getWorkflowStatusType(workflowExecution.job, workflowExecution.triggerExecution) === 'running';
    }, [workflowExecution]);

    useGetProjectWorkflowExecutionQuery({id: workflowExecutionId}, enabled && isWorkflowRunning, POLLING_INTERVAL_MS);

    const job = workflowExecution?.job;
    const triggerExecution = workflowExecution?.triggerExecution;

    const taskExecutions = useMemo(() => job?.taskExecutions || [], [job?.taskExecutions]);

    const deepestFailedExecution = useMemo(() => {
        if (triggerExecution) {
            const result = getDeepestFailedExecution({
                currentPath: [],
                execution: triggerExecution,
                isTriggerExecution: true,
            });

            if (result) {
                return result;
            }
        }

        for (const taskExecution of taskExecutions) {
            const result = getDeepestFailedExecution({currentPath: [], execution: taskExecution});

            if (result) {
                return result;
            }
        }

        return null;
    }, [taskExecutions, triggerExecution]);

    const jobFailedWithNoExecutions = !job?.taskExecutions?.length && job?.status === JobStatusEnum.Failed;

    const jobFailureError = job?.error ?? {
        message: 'Workflow execution failed before any executions were created.',
        stackTrace: [],
    };

    const isTriggerExecution = selectedItem?.id === triggerExecution?.id;

    const handleTaskClick = (taskExecution: TaskExecution | TriggerExecution) => {
        setActiveTab(taskExecution.error ? 'error' : 'output');

        setSelectedItem(taskExecution);
    };

    useEffect(() => {
        if (!job?.id || job.id === jobIdRef.current) {
            return;
        }

        jobIdRef.current = job.id;

        const hasNoTaskExecutions = !job.taskExecutions || job.taskExecutions.length === 0;
        const jobFailed = hasNoTaskExecutions && job.status === JobStatusEnum.Failed;
        const newActiveTab = jobFailed || deepestFailedExecution?.execution.error ? 'error' : 'output';

        setActiveTab(newActiveTab);

        setSelectedItem(deepestFailedExecution?.execution || triggerExecution || job.taskExecutions?.[0] || undefined);
    }, [deepestFailedExecution, job, triggerExecution]);

    return {
        activeTab,
        deepestFailedExecution,
        dialogOpen,
        handleTaskClick,
        isTriggerExecution,
        jobFailedWithNoExecutions,
        jobFailureError,
        selectedItem,
        setActiveTab,
        setDialogOpen,
        taskExecutions,
        workflowExecution,
        workflowExecutionLoading,
    };
};

export default useWorkflowExecutionDetail;
```

- [ ] **Step 2: Verify it typechecks**

Run: `cd client && npm run typecheck`
Expected: PASS. (No test yet — Task 3's component test exercises it.)

- [ ] **Step 3: Commit**

```bash
git add client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/hooks/useWorkflowExecutionDetail.ts
git commit -m "$(printf '520 client - Extract useWorkflowExecutionDetail orchestration hook\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

---

## Task 3: Create the shared `WorkflowExecutionDetail` component

**Files:**
- Create: `client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionDetail.tsx`
- Test: `client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/tests/WorkflowExecutionDetail.test.tsx`

- [ ] **Step 1: Write the failing test**

```tsx
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {executionQueryMock} = vi.hoisted(() => ({executionQueryMock: vi.fn()}));

vi.mock('@/shared/queries/automation/workflowExecutions.queries', () => ({
    useGetProjectWorkflowExecutionQuery: executionQueryMock,
}));

const WorkflowExecutionDetail = (await import('../WorkflowExecutionDetail')).default;

describe('WorkflowExecutionDetail', () => {
    beforeEach(() => {
        executionQueryMock.mockReset();
    });

    it('shows a loading state while the execution is loading', () => {
        executionQueryMock.mockReturnValue({data: undefined, isLoading: true});

        render(<WorkflowExecutionDetail workflowExecutionId={501} />);

        expect(screen.getByTestId('workflow-execution-detail-loading')).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd client && npx vitest run src/pages/automation/workflow-executions/components/workflow-execution-sheet/tests/WorkflowExecutionDetail.test.tsx`
Expected: FAIL — `WorkflowExecutionDetail` module not found.

- [ ] **Step 3: Create `WorkflowExecutionDetail`**

This lifts the body of `WorkflowExecutionSheet.tsx` (lines 94–144 — the loading branch + the `ResizablePanelGroup`). Full file:

```tsx
import LoadingIcon from '@/components/LoadingIcon';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';

import WorkflowExecutionSheetContent from './WorkflowExecutionSheetContent';
import WorkflowExecutionSheetWorkflowPanel from './WorkflowExecutionSheetWorkflowPanel';
import useWorkflowExecutionDetail from './hooks/useWorkflowExecutionDetail';

interface WorkflowExecutionDetailProps {
    enabled?: boolean;
    workflowExecutionId: number;
}

/**
 * Shared workflow-execution detail body — the job/trigger/task accordion + output panel on the left,
 * the read-only workflow canvas on the right. Rendered by both the workflow-execution slide-over sheet
 * (`WorkflowExecutionSheet`) and the AI Hub `workflowExecution` resource tab
 * (`AiHubWorkflowExecutionViewer`). No `Sheet` chrome — the caller owns the wrapper.
 */
const WorkflowExecutionDetail = ({enabled = true, workflowExecutionId}: WorkflowExecutionDetailProps) => {
    const {
        activeTab,
        deepestFailedExecution,
        dialogOpen,
        handleTaskClick,
        isTriggerExecution,
        jobFailedWithNoExecutions,
        jobFailureError,
        selectedItem,
        setActiveTab,
        setDialogOpen,
        taskExecutions,
        workflowExecution,
        workflowExecutionLoading,
    } = useWorkflowExecutionDetail(workflowExecutionId, enabled);

    if (workflowExecutionLoading) {
        return (
            <div className="flex size-full items-center justify-center" data-testid="workflow-execution-detail-loading">
                <LoadingIcon className="size-6" />
            </div>
        );
    }

    return (
        <div className="flex min-h-0 flex-1 p-3">
            <ResizablePanelGroup className="h-full" orientation="horizontal">
                <ResizablePanel
                    className="flex min-h-0 w-1/2 flex-col overflow-hidden rounded-md bg-surface-neutral-primary"
                    defaultSize={50}
                >
                    {workflowExecution?.job && (
                        <WorkflowExecutionSheetContent
                            activeTab={activeTab}
                            deepestFailedExecution={deepestFailedExecution}
                            dialogOpen={dialogOpen}
                            handleTaskClick={handleTaskClick}
                            isTriggerExecution={isTriggerExecution}
                            job={workflowExecution.job}
                            jobFailedWithNoExecutions={jobFailedWithNoExecutions}
                            jobFailureError={jobFailureError}
                            selectedItem={selectedItem}
                            setActiveTab={setActiveTab}
                            setDialogOpen={setDialogOpen}
                            taskExecutions={taskExecutions}
                            triggerExecution={workflowExecution?.triggerExecution}
                        />
                    )}
                </ResizablePanel>

                <ResizableHandle className="mx-2.5" withHandle />

                <ResizablePanel className="flex min-h-0 w-1/2 flex-col overflow-hidden" defaultSize={50}>
                    {workflowExecution && (
                        <WorkflowReadOnlyProvider
                            value={{useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery}}
                        >
                            <WorkflowExecutionSheetWorkflowPanel workflowExecution={workflowExecution} />
                        </WorkflowReadOnlyProvider>
                    )}
                </ResizablePanel>
            </ResizablePanelGroup>
        </div>
    );
};

export default WorkflowExecutionDetail;
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd client && npx vitest run src/pages/automation/workflow-executions/components/workflow-execution-sheet/tests/WorkflowExecutionDetail.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionDetail.tsx client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/tests/WorkflowExecutionDetail.test.tsx
git commit -m "$(printf '520 client - Add shared WorkflowExecutionDetail component\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

---

## Task 4: Render `WorkflowExecutionDetail` inside `WorkflowExecutionSheet`

Replace the inline body in the sheet with the shared component, so the sheet and the tab stay in sync.

**Files:**
- Modify: `client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet.tsx`

- [ ] **Step 1: Replace the sheet body**

In `WorkflowExecutionSheet.tsx`, replace the `workflowExecutionLoading ? (…) : (…)` block (the loading icon + the `ResizablePanelGroup`, lines ~94–144) with:

```tsx
                    <WorkflowExecutionDetail
                        enabled={workflowExecutionSheetOpen}
                        workflowExecutionId={workflowExecutionId}
                    />
```

`workflowExecutionId` and `workflowExecutionSheetOpen` already come from `useWorkflowExecutionSheet()`. Add `workflowExecutionId` to that hook's destructure if not already there (it returns `workflowExecutionSheetOpen` and `workflowExecution`; expose `workflowExecutionId` too — it is already read from the store inside the hook, add it to the hook's return object).

- [ ] **Step 2: Clean up now-unused imports**

Remove imports left unused in `WorkflowExecutionSheet.tsx` after the swap: `LoadingIcon`, `ResizableHandle`, `ResizablePanel`, `ResizablePanelGroup`, `WorkflowReadOnlyProvider`, `useGetComponentDefinitionsQuery`, `WorkflowExecutionSheetContent`, `WorkflowExecutionSheetWorkflowPanel`. Add `import WorkflowExecutionDetail from './WorkflowExecutionDetail';`. Keep imports still used by the header (`Skeleton`, `Button`, `Tooltip*`, `WorkflowIcon`, `SparklesIcon`, etc.).

- [ ] **Step 3: Verify**

Run: `cd client && npm run typecheck && npx eslint src/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet.tsx`
Expected: PASS, no lint errors (no unused imports).

- [ ] **Step 4: Run the existing workflow-executions tests**

Run: `cd client && npx vitest run src/pages/automation/workflow-executions`
Expected: PASS — the sheet still renders.

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet.tsx client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/hooks/useWorkflowExecutionSheet.ts
git commit -m "$(printf '520 client - Render WorkflowExecutionDetail inside the execution sheet\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

---

## Task 5: Create `AiHubWorkflowExecutionViewer`

**Files:**
- Create: `client/src/pages/automation/ai-hub/AiHubWorkflowExecutionViewer.tsx`
- Test: `client/src/pages/automation/ai-hub/tests/AiHubWorkflowExecutionViewer.test.tsx`

- [ ] **Step 1: Write the failing test**

```tsx
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {detailMock} = vi.hoisted(() => ({detailMock: vi.fn()}));

vi.mock(
    '@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionDetail',
    () => ({default: detailMock})
);

const AiHubWorkflowExecutionViewer = (await import('../AiHubWorkflowExecutionViewer')).default;

describe('AiHubWorkflowExecutionViewer', () => {
    beforeEach(() => {
        detailMock.mockReset();
        detailMock.mockImplementation(({workflowExecutionId}: {workflowExecutionId: number}) => (
            <div data-testid="detail">{workflowExecutionId}</div>
        ));
    });

    it('renders the execution detail for the given execution id', () => {
        render(<AiHubWorkflowExecutionViewer name="Run #501" workflowExecutionId={501} />);

        expect(screen.getByTestId('detail')).toHaveTextContent('501');
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd client && npx vitest run src/pages/automation/ai-hub/tests/AiHubWorkflowExecutionViewer.test.tsx`
Expected: FAIL — module not found.

- [ ] **Step 3: Create `AiHubWorkflowExecutionViewer`**

Mirrors `AiHubWorkflowViewer`'s shape (thin name header + body). Full file:

```tsx
import WorkflowExecutionDetail from '@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionDetail';

interface AiHubWorkflowExecutionViewerProps {
    name: string;
    workflowExecutionId: number;
}

/**
 * Body of a `workflowExecution` resource tab. Renders the shared {@link WorkflowExecutionDetail}
 * (job / trigger / task accordion + read-only canvas) — the same content as the standalone
 * workflow-execution slide-over sheet, minus the Sheet chrome.
 */
const AiHubWorkflowExecutionViewer = ({name, workflowExecutionId}: AiHubWorkflowExecutionViewerProps) => (
    <div className="flex size-full flex-col">
        <header className="flex shrink-0 items-center gap-2 border-b border-stroke-neutral-secondary px-4 py-3">
            <span className="truncate text-sm font-semibold text-content-neutral-primary">{name}</span>
        </header>

        <div className="min-h-0 flex-1">
            <WorkflowExecutionDetail workflowExecutionId={workflowExecutionId} />
        </div>
    </div>
);

export default AiHubWorkflowExecutionViewer;
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd client && npx vitest run src/pages/automation/ai-hub/tests/AiHubWorkflowExecutionViewer.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/automation/ai-hub/AiHubWorkflowExecutionViewer.tsx client/src/pages/automation/ai-hub/tests/AiHubWorkflowExecutionViewer.test.tsx
git commit -m "$(printf '520 client - Add AiHubWorkflowExecutionViewer tab body\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

---

## Task 6: Wire the `workflowExecution` branch into `renderTabBody`

**Files:**
- Modify: `client/src/pages/automation/ai-hub/AiHubResourcePanel.tsx`

- [ ] **Step 1: Add the import and the dispatch branch**

In `AiHubResourcePanel.tsx`, add the import (alphabetical order in the import group):

```tsx
import AiHubWorkflowExecutionViewer from '@/pages/automation/ai-hub/AiHubWorkflowExecutionViewer';
```

In `renderTabBody`, add the branch after the `workflow` branch:

```tsx
    if (tab.kind === 'workflowExecution') {
        return <AiHubWorkflowExecutionViewer name={tab.name} workflowExecutionId={tab.workflowExecutionId} />;
    }
```

- [ ] **Step 2: Verify**

Run: `cd client && npm run typecheck && npx eslint src/pages/automation/ai-hub/AiHubResourcePanel.tsx`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add client/src/pages/automation/ai-hub/AiHubResourcePanel.tsx
git commit -m "$(printf '520 client - Render workflowExecution tabs in the AI Hub resource panel\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

---

## Task 7: Reroute the artifact-list quick-open to a tab

**Files:**
- Modify: `client/src/pages/automation/ai-hub/tasks/AiHubTasksSidebar.tsx`

- [ ] **Step 1: Replace the `WORKFLOW_EXECUTION_STARTED` branch**

In `AiHubTasksSidebar.tsx`, `handleArtifactQuickOpen`, replace:

```ts
    if (artifact.kind === 'WORKFLOW_EXECUTION_STARTED') {
        window.open(`/automation/executions/${artifact.artifactId}`, '_blank');

        return;
    }
```

with:

```ts
    if (artifact.kind === 'WORKFLOW_EXECUTION_STARTED') {
        // Opens the execution as a right-panel tab instead of a new browser tab. artifactId is the
        // workflow execution id; openWorkflowExecutionTab takes a number.
        aiHubTabsStore.getState().openWorkflowExecutionTab(Number(artifact.artifactId), artifact.artifactName);

        return;
    }
```

`aiHubTabsStore` is already imported in this file. `isArtifactClickable` already returns `true` for `WORKFLOW_EXECUTION_STARTED` — no change needed there.

- [ ] **Step 2: Verify**

Run: `cd client && npm run typecheck && npx eslint src/pages/automation/ai-hub/tasks/AiHubTasksSidebar.tsx`
Expected: PASS.

- [ ] **Step 3: Run the AI Hub test suite**

Run: `cd client && npx vitest run src/pages/automation/ai-hub`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add client/src/pages/automation/ai-hub/tasks/AiHubTasksSidebar.tsx
git commit -m "$(printf '520 client - Open workflow executions as AI Hub tabs from the artifact list\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

**Phase 1 is now shippable** — workflow executions open as right-panel tabs.

---

# Phase 2 — Full Editor in the Workflow Tab

## Task 8: Restructure `EmbeddableWorkflowEditor` to embed `ProjectHeader` + test output

The workflow tab today renders a slim editor. Restructure `EmbeddableWorkflowEditorInner` to mirror the right-hand column of the full project page (`client/src/pages/automation/project/Project.tsx`, lines 81–140) — minus the project left sidebar. This delivers Run, publish/deploy, and an in-tab test-output panel.

**Reference file:** `Project.tsx` — copy its structure for the `<div className="flex w-full flex-col">` block (lines 81–140), adapting as below.

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/EmbeddableWorkflowEditor.tsx`

- [ ] **Step 1: Add the new imports**

Add to `EmbeddableWorkflowEditor.tsx`:

```tsx
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import ProjectHeader from '@/pages/automation/project/components/project-header/ProjectHeader';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import WorkflowTestRunLeaveDialog from '@/shared/components/WorkflowTestRunLeaveDialog';
import {useWorkflowTestRunGuard} from '@/shared/hooks/useWorkflowTestRunGuard';
import {useRef} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
```

- [ ] **Step 2: Add the header/test-output wiring inside `EmbeddableWorkflowEditorInner`**

Inside `EmbeddableWorkflowEditorInner`, after the existing `const {runDisabled} = useRun();`, add:

```tsx
    const bottomResizablePanelRef = useRef<PanelImperativeHandle>(null);

    const {cancelLeave, confirmLeave, showLeaveDialog, workflowIsRunning, workflowTestExecution} =
        useWorkflowTestRunGuard(workflow.id, currentEnvironmentId);

    const handleTestOutputCloseClick = () => bottomResizablePanelRef.current?.resize(0);

    const chatTrigger =
        workflow.triggers != null &&
        workflow.triggers.findIndex((trigger) => trigger.type.includes('chat/')) !== -1;
```

`workflow` and `currentEnvironmentId` are already read at the top of `EmbeddableWorkflowEditorInner`.

- [ ] **Step 3: Replace the editor render block**

In `EmbeddableWorkflowEditorInner`, the current return wraps `<WorkflowEditorLayout … />` in `<WorkflowEditorProvider>`. Replace the `WorkflowEditorProvider` child (the `<WorkflowEditorLayout … />`) so the provider now wraps a header + a vertical resizable group. Inside `<WorkflowEditorProvider value={{…}}>`:

```tsx
            <div className="flex size-full flex-col">
                <ProjectHeader
                    bottomResizablePanelRef={bottomResizablePanelRef}
                    chatTrigger={chatTrigger}
                    projectId={projectId}
                    projectWorkflowId={projectWorkflowId}
                    runDisabled={runDisabled}
                    updateWorkflowMutation={updateWorkflowEditorMutation}
                />

                <ResizablePanelGroup className="flex-1 bg-surface-main" orientation="vertical">
                    <ResizablePanel className="relative flex" defaultSize={650}>
                        <WorkflowEditorLayout
                            leftSidebarOpen={false}
                            runDisabled={runDisabled}
                            showCopilot={false}
                            showWorkflowInputs={true}
                            workflowReferenceId={projectWorkflowId}
                        />
                    </ResizablePanel>

                    <ResizableHandle className="bg-muted" />

                    <ResizablePanel className="bg-background" defaultSize={0} panelRef={bottomResizablePanelRef}>
                        {workflowTestExecution && (
                            <WorkflowExecutionsTestOutput
                                onCloseClick={handleTestOutputCloseClick}
                                workflowIsRunning={workflowIsRunning}
                                workflowTestExecution={workflowTestExecution}
                            />
                        )}
                    </ResizablePanel>
                </ResizablePanelGroup>
            </div>
```

Note the changes vs. today: `showWorkflowInputs={true}` (was `false`); `leftSidebarOpen={false}` and `showCopilot={false}` stay. `updateWorkflowEditorMutation` is the mutation `EmbeddableWorkflowEditorInner` already builds.

- [ ] **Step 4: Render the leave-guard dialog**

The outer return of `EmbeddableWorkflowEditorInner` currently is just `<WorkflowEditorProvider>…</WorkflowEditorProvider>`. Wrap it so the leave dialog renders alongside:

```tsx
        <>
            <WorkflowTestRunLeaveDialog onCancel={cancelLeave} onConfirm={confirmLeave} open={showLeaveDialog} />

            <WorkflowEditorProvider value={{…}}>
                {/* the div from Step 3 */}
            </WorkflowEditorProvider>
        </>
```

- [ ] **Step 5: Update the docstring**

The `EmbeddableWorkflowEditor` JSDoc lists "No page header chrome", "Run is disabled", "Workflow inputs sheet is hidden" — these are now false. Update the "Differences from the full Project.tsx editor" list to: "No project left sidebar (the project/workflow tree)" and "No Copilot panel (the AI Hub chat is the copilot)". Remove the run-disabled and inputs-hidden bullets.

- [ ] **Step 6: Verify**

Run: `cd client && npm run typecheck && npx eslint src/pages/platform/workflow-editor/EmbeddableWorkflowEditor.tsx`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add client/src/pages/platform/workflow-editor/EmbeddableWorkflowEditor.tsx
git commit -m "$(printf '520 client - Embed the full project editor (header, Run, test output) in the AI Hub workflow tab\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

---

## Task 9: Drop the bespoke header from `AiHubWorkflowViewer`

The workflow tab's `AiHubWorkflowViewer` has its own header (workflow name + "Open in editor"). `ProjectHeader` (Task 8) now provides the header, so this becomes a thin wrapper — or collapses entirely.

**Files:**
- Modify: `client/src/pages/automation/ai-hub/AiHubWorkflowViewer.tsx`
- Modify: `client/src/pages/automation/ai-hub/AiHubResourcePanel.tsx` (only if `AiHubWorkflowViewer` is removed)

- [ ] **Step 1: Reduce `AiHubWorkflowViewer` to the editor only**

Replace the body of `AiHubWorkflowViewer.tsx` — drop the `<header>` (name + "Open in editor" `Link`) and the `editorHref`. The component becomes:

```tsx
import EmbeddableWorkflowEditor from '@/pages/platform/workflow-editor/EmbeddableWorkflowEditor';

interface AiHubWorkflowViewerProps {
    name: string;
    projectId: string;
    projectWorkflowId: number;
}

const AiHubWorkflowViewer = ({projectId, projectWorkflowId}: AiHubWorkflowViewerProps) => (
    <div className="flex size-full flex-col">
        {projectWorkflowId > 0 ? (
            <EmbeddableWorkflowEditor projectId={+projectId} projectWorkflowId={projectWorkflowId} />
        ) : (
            <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
                Workflow reference unavailable.
            </div>
        )}
    </div>
);

export default AiHubWorkflowViewer;
```

`name` stays in the props interface (the `renderTabBody` call site passes it; the tab strip already shows the name). The `Link` / `ExternalLinkIcon` imports are removed.

- [ ] **Step 2: Verify**

Run: `cd client && npm run typecheck && npx eslint src/pages/automation/ai-hub/AiHubWorkflowViewer.tsx`
Expected: PASS — no unused `name` (it is still a declared prop; if eslint flags it unused, prefix the destructure is not allowed — instead keep it referenced by leaving it in the interface only and destructuring just `projectId, projectWorkflowId`, which is what the code above does; `name` stays in the interface for the caller's type).

If eslint flags `name` as an unused interface member, that is acceptable (interface members are not flagged as unused). If it flags the call site, no change is needed there.

- [ ] **Step 3: Run the AI Hub suite**

Run: `cd client && npx vitest run src/pages/automation/ai-hub`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add client/src/pages/automation/ai-hub/AiHubWorkflowViewer.tsx
git commit -m "$(printf '520 client - Drop the bespoke workflow-tab header in favor of the project header\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

---

## Task 10: Full verification

- [ ] **Step 1: Run the full client check**

Run: `cd client && npm run check`
Expected: lint + typecheck + all tests PASS.

- [ ] **Step 2: Manual smoke (optional but recommended)**

In the AI Hub: open a workflow tab → confirm the project header with a Run button renders, and Running surfaces an in-tab test-output panel. Click a `WORKFLOW_EXECUTION_STARTED` row in a task's artifact list → confirm it opens as a right-panel execution tab (job/trigger/task accordion + read-only canvas), not a new browser tab.

---

## Self-Review notes (for the executor)

- **Spec coverage:** Part A → Tasks 8–9. Part B → Tasks 1–6. Part C → Task 7. Workflow tab header (#1) → Task 9. Artifact recording (no change) → Task 1 Step 7.
- **Type consistency:** `openWorkflowExecutionTab(workflowExecutionId: number, name: string)` — id is a `number` everywhere (tab variant, store action, viewer prop, `Number(artifact.artifactId)` at the call site). `WorkflowExecutionDetail` prop is `workflowExecutionId: number`, `enabled?: boolean`.
- **Known risk — `ProjectHeader` fit:** `ProjectHeader` is built for full-page width (breadcrumb, publish, deploy, settings, left-sidebar toggle). At the ~60 % AI Hub panel width it may crowd. If it does, a follow-up compact pass (hide `LeftSidebarButton`, condense `ProjectBreadcrumb`) is acceptable — out of scope for these tasks.
- **Known risk — `useProject` is not reused:** `EmbeddableWorkflowEditor` deliberately does not call `useProject` (it reads route params). It builds its own mutations + refs, as it already did.
