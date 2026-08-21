# AI Hub Artifacts Card Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the AI Hub task-artifact list out of the tasks sidebar into a floating "Artifacts" card pinned to the top-right of the chat pane, with an "Artifacts" branch in the resource panel's `+` menu as its counterpart while the panel is open.

**Architecture:** The ~350 lines of artifact logic currently inside the 1470-line `AiHubTasksSidebar.tsx` are extracted into a new `ai-hub/artifacts/` module (Task 1), a task-scoped row component is built on top of it (Task 2), the card is built and wired into `AiHubPanel` (Task 3), the sidebar's artifact affordances are deleted (Task 4), and the `+` menu gains an artifacts branch (Task 5). The card is built before the sidebar is stripped so there is never a commit without the feature.

**Tech Stack:** React 19 + TypeScript 5.9, Zustand (with `persist`), TanStack Query, TailwindCSS 4, Vitest 4 + `@testing-library/react`, lucide-react icons.

**Spec:** `docs/superpowers/specs/2026-08-07-ai-hub-artifacts-card-design.md`

## Global Constraints

- Client-only change. No server, GraphQL schema, or `ai_hub_task_artifact` changes.
- All paths below are relative to `client/`. Run every command from `client/`.
- ESLint `sort-keys`: object literal keys must be in ascending alphabetical order. `--fix` does **not** fix this; fix by hand.
- Named imports inside `{}` must be sorted alphabetically (`bytechef/sort-import-destructures`), `type` imports sorting by their name.
- Lucide icons are imported with the `Icon` suffix (`PackageIcon`, not `Package`).
- Interface names must end with `I` or `Props`.
- Use `twMerge` from `tailwind-merge` for conditional classes. Never `cn()`.
- Hook order inside a component: `useState` → `useRef` → store hooks → other custom hooks → derived values / `useMemo` / `useCallback` → `useEffect` → `return`.
- No short or cryptic variable names, including arrow-function parameters (`(artifact) =>`, not `(a) =>`).
- Commit messages for client changes: `732 client - <description>`.
- Never `git commit --amend` on this branch — the user commits in parallel. Always fresh commits.
- Full verification command: `npm run check` (lint + typecheck + tests). Single test file: `npx vitest run <path>`.

---

### Task 1: Extract the artifact module out of the sidebar

Pure move, no behavior change. Everything keeps working exactly as it does today; only the import graph changes. `getArtifactIcon` returns JSX so it needs a `.tsx` file — hence two files rather than the single one the spec sketched.

**Files:**
- Create: `src/ee/pages/automation/ai-hub/artifacts/artifactOpen.ts`
- Create: `src/ee/pages/automation/ai-hub/artifacts/artifactIcons.tsx`
- Create: `src/ee/pages/automation/ai-hub/artifacts/tests/artifactOpen.test.ts`
- Modify: `src/ee/pages/automation/ai-hub/tasks/AiHubTasksSidebar.tsx` (delete `:97-408` region helpers, add imports)
- Modify: `src/ee/pages/automation/ai-hub/tasks/tests/AiHubTasksSidebar.test.tsx` (remove the two migrated `describe` blocks)

**Interfaces:**
- Produces:
  - `artifactOpen.ts`: `handleArtifactQuickOpen(artifact: AiHubTaskArtifactI): Promise<void>`, `openArtifactInTask(artifact: AiHubTaskArtifactI, task: AiHubTaskI, currentTaskId: number | undefined, switchTask: (task: AiHubTaskI) => Promise<boolean>): Promise<void>`, `isArtifactClickable(artifact: AiHubTaskArtifactI): boolean`, `isArtifactRemovable(artifact: AiHubTaskArtifactI): boolean`
  - `artifactIcons.tsx`: `getArtifactIcon(kind: AiHubArtifactKindType): ReactElement`
  - `parseMetadataJson` and `openCodeWorkflowArtifact` stay module-private (not exported).

- [ ] **Step 1: Create `artifacts/artifactOpen.ts`**

Move — do not retype — these from `AiHubTasksSidebar.tsx` into the new file, preserving every comment verbatim:

| Symbol | Current location |
|---|---|
| `parseMetadataJson` | `:151-171` (keep `function`, no `export`) |
| the quick-open doc comment block | `:173-198` |
| `openCodeWorkflowArtifact` | `:287-312` (no `export`) |
| `handleArtifactQuickOpen` | `:314-410` (`export`) |
| `openArtifactInTask` | `:413-447` (`export`) |
| `isArtifactRemovable` | `:449-462` (`export`) |
| `isArtifactClickable` | `:464-531` (`export`) |

Change one thing inside `parseMetadataJson`: the warning prefix, since the file it lives in changed.

```ts
console.warn('[artifactOpen] Failed to parse artifact metadataJson; quick-open will be disabled', {
    parseError,
    raw: metadataJson,
});
```

File header imports:

```ts
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {AiHubTaskArtifactI, AiHubTaskI} from '@/ee/pages/automation/ai-hub/tasks/api/tasks.api';
import {ProjectApi} from '@/shared/middleware/automation/configuration';
import {toast} from 'sonner';
```

Note `isArtifactClickable` calls `parseMetadataJson`, and `handleArtifactQuickOpen` calls both `parseMetadataJson` and `openCodeWorkflowArtifact` — all four now live in this one file, so no cross-file wiring is needed.

- [ ] **Step 2: Create `artifacts/artifactIcons.tsx`**

Move `getArtifactIcon` from `AiHubTasksSidebar.tsx:97-142` verbatim, adding `export`:

```tsx
import {AiHubArtifactKindType} from '@/ee/pages/automation/ai-hub/tasks/api/tasks.api';
import {
    BlocksIcon,
    BrainIcon,
    CodeIcon,
    DatabaseIcon,
    FileTextIcon,
    HexagonIcon,
    ImageIcon,
    PlayIcon,
    WorkflowIcon,
    WrenchIcon,
} from 'lucide-react';

export function getArtifactIcon(kind: AiHubArtifactKindType) {
    // ...body moved verbatim from AiHubTasksSidebar.tsx:97-142
}
```

- [ ] **Step 3: Point the sidebar at the new module**

In `AiHubTasksSidebar.tsx`, delete the seven moved symbols and add these two imports (they sort after the `@/components/...` group, alphabetically by path — `artifacts/` sorts before `runtime-providers/`):

```ts
import {getArtifactIcon} from '@/ee/pages/automation/ai-hub/artifacts/artifactIcons';
import {
    handleArtifactQuickOpen,
    isArtifactClickable,
    isArtifactRemovable,
    openArtifactInTask,
} from '@/ee/pages/automation/ai-hub/artifacts/artifactOpen';
```

`handleArtifactQuickOpen` is re-exported nowhere — the sidebar's own test imported it, and Step 5 moves that test. `ArtifactRow` / `ArtifactList` stay in the sidebar for now; Task 4 deletes them.

Do **not** delete lucide imports by eye. `HexagonIcon` is also used by the Context → Skills link (`:1349`), and `LinkIcon` by Connectors. Run `npm run lint` and remove only what it reports as unused.

- [ ] **Step 4: Create `artifacts/tests/artifactOpen.test.ts` from the migrated blocks**

Move, verbatim, from `src/ee/pages/automation/ai-hub/tasks/tests/AiHubTasksSidebar.test.tsx`:
- the `vi.hoisted` block (`:16-19`) and both `vi.mock` calls (`:21-38`),
- `buildTask` (`:40-58`) and `buildFileArtifact` (`:60-72`),
- `describe('handleArtifactQuickOpen')` (`:74-242`),
- `describe('openArtifactInTask')` (`:244-320`).

Header of the new file:

```ts
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubTaskArtifactI, AiHubTaskI} from '../../tasks/api/tasks.api';
import {handleArtifactQuickOpen, openArtifactInTask} from '../artifactOpen';
```

- [ ] **Step 5: Strip the migrated blocks from the sidebar test**

In `AiHubTasksSidebar.test.tsx`, delete the two `describe` blocks, `buildTask`, `buildFileArtifact`, the `vi.hoisted` block, both `vi.mock` calls, and drop `handleArtifactQuickOpen` / `openArtifactInTask` from the `../AiHubTasksSidebar` import list. Keep `cancelTaskRunIfStreaming`, `getTasksPage`, `reconcileProbedTaskActivity`, and `TASKS_PAGE_SIZE`.

If `cancelTaskRunIfStreaming`'s describe uses `buildTask`, keep `buildTask` in this file too — duplicating a 15-line fixture across two test files is correct here; the alternative is a shared fixture module for one helper.

- [ ] **Step 6: Run both test files**

```bash
npx vitest run src/ee/pages/automation/ai-hub/artifacts/tests/artifactOpen.test.ts src/ee/pages/automation/ai-hub/tasks/tests/AiHubTasksSidebar.test.tsx
```

Expected: PASS. All cases are moves — a failure means something was retyped rather than moved.

- [ ] **Step 7: Full check**

```bash
npm run check
```

Expected: PASS. This is the gate that catches an unused import left behind in the sidebar.

- [ ] **Step 8: Commit**

```bash
git add src/ee/pages/automation/ai-hub/artifacts src/ee/pages/automation/ai-hub/tasks/AiHubTasksSidebar.tsx src/ee/pages/automation/ai-hub/tasks/tests/AiHubTasksSidebar.test.tsx
git commit -m "732 client - Extract AI Hub artifact open/icon helpers out of the tasks sidebar"
```

---

### Task 2: The task-scoped artifact row

A row that opens its artifact against the **active** task, with no task-switching. It is what the card renders.

**Files:**
- Create: `src/ee/pages/automation/ai-hub/artifacts/AiHubArtifactRow.tsx`
- Test: `src/ee/pages/automation/ai-hub/artifacts/tests/AiHubArtifactRow.test.tsx`

**Interfaces:**
- Consumes: `handleArtifactQuickOpen`, `isArtifactClickable`, `isArtifactRemovable` from Task 1's `artifactOpen.ts`; `getArtifactIcon` from `artifactIcons.tsx`.
- Produces: default export `AiHubArtifactRow`, props `{artifact: AiHubTaskArtifactI; workspaceId: number}`.

- [ ] **Step 1: Write the failing test**

Create `src/ee/pages/automation/ai-hub/artifacts/tests/AiHubArtifactRow.test.tsx`:

```tsx
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubTaskArtifactI} from '../../tasks/api/tasks.api';
import AiHubArtifactRow from '../AiHubArtifactRow';

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteAiHubTaskArtifactMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

function buildArtifact(overrides: Partial<AiHubTaskArtifactI> = {}): AiHubTaskArtifactI {
    return {
        artifactId: 'file-1',
        artifactName: 'report.csv',
        createdAt: new Date().toISOString(),
        id: 1,
        kind: 'FILE_CREATED',
        metadataJson: null,
        status: 'APPLIED',
        taskId: 1,
        ...overrides,
    };
}

describe('AiHubArtifactRow', () => {
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

    it('opens the artifact in the resource panel on click', () => {
        render(<AiHubArtifactRow artifact={buildArtifact()} workspaceId={1} />);

        fireEvent.click(screen.getByRole('button', {name: /report.csv/}));

        const state = aiHubTabsStore.getState();

        expect(state.openTabs).toHaveLength(1);
        expect(state.openTabs[0]!.kind).toBe('file');
        expect(state.rightPanelOpen).toBe(true);
    });

    it('shows a remove affordance only for user-attached reference kinds', () => {
        const {rerender} = render(<AiHubArtifactRow artifact={buildArtifact()} workspaceId={1} />);

        expect(screen.queryByRole('button', {name: 'Remove report.csv'})).toBeNull();

        rerender(<AiHubArtifactRow artifact={buildArtifact({kind: 'FILE_REFERENCED'})} workspaceId={1} />);

        expect(screen.getByRole('button', {name: 'Remove report.csv'})).toBeInTheDocument();
    });

    it('renders a non-clickable artifact as static text', () => {
        // WORKFLOW_CREATED without a projectId in metadata has nowhere to route to.
        render(<AiHubArtifactRow artifact={buildArtifact({kind: 'WORKFLOW_CREATED'})} workspaceId={1} />);

        expect(screen.queryByRole('button')).toBeNull();
        expect(screen.getByText('report.csv')).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
npx vitest run src/ee/pages/automation/ai-hub/artifacts/tests/AiHubArtifactRow.test.tsx
```

Expected: FAIL — cannot resolve `../AiHubArtifactRow`.

- [ ] **Step 3: Write the component**

Create `src/ee/pages/automation/ai-hub/artifacts/AiHubArtifactRow.tsx`. This is `AiHubTasksSidebar.tsx:540-619`'s `ArtifactRow` with the `task` prop, the `currentTaskId` store read, the `useSwitchTask` call, and the `openArtifactInTask` indirection removed — the card only ever shows the active task's artifacts, so there is nothing to switch to.

```tsx
import {getArtifactIcon} from '@/ee/pages/automation/ai-hub/artifacts/artifactIcons';
import {
    handleArtifactQuickOpen,
    isArtifactClickable,
    isArtifactRemovable,
} from '@/ee/pages/automation/ai-hub/artifacts/artifactOpen';
import {AiHubTaskArtifactI} from '@/ee/pages/automation/ai-hub/tasks/api/tasks.api';
import {useDeleteAiHubTaskArtifactMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {type MouseEvent} from 'react';
import {toast} from 'sonner';

interface AiHubArtifactRowPropsI {
    artifact: AiHubTaskArtifactI;
    workspaceId: number;
}

const AiHubArtifactRow = ({artifact, workspaceId}: AiHubArtifactRowPropsI) => {
    const queryClient = useQueryClient();

    const clickable = isArtifactClickable(artifact);
    const removable = isArtifactRemovable(artifact);

    const deleteMutation = useDeleteAiHubTaskArtifactMutation({
        onError: (error) => {
            const message = error instanceof Error ? error.message : String(error);

            toast.error(`Failed to remove ${artifact.artifactName}: ${message}`);
        },
        // The artifact list query is keyed by ['aiHubTasks', 'artifacts', taskId, workspaceId] (see
        // AiHubTasksKeys.artifacts) — invalidate by that prefix so the row disappears without needing the
        // exact key shape react-query built it under. Matching on both segments avoids needlessly
        // refetching the task list/messages, which share the 'aiHubTasks' root. Mirrors
        // useRecordReferencedArtifacts' invalidation strategy.
        onSuccess: () => {
            queryClient.invalidateQueries({
                predicate: (query) => {
                    const key = query.queryKey;

                    if (!Array.isArray(key) || key.length < 2) {
                        return false;
                    }

                    return key[0] === 'aiHubTasks' && key[1] === 'artifacts';
                },
            });
        },
    });

    const handleRemove = (event: MouseEvent<HTMLButtonElement>) => {
        // The remove button sits inside the quick-open button when the row is clickable; stop the click
        // from bubbling so removing doesn't also open the artifact.
        event.stopPropagation();

        deleteMutation.mutate({
            input: {
                artifactId: String(artifact.id),
                workspaceId: String(workspaceId),
            },
        });
    };

    const content = (
        <div className="flex min-w-0 flex-1 items-center gap-1.5">
            {getArtifactIcon(artifact.kind)}

            <span className="min-w-0 flex-1 truncate text-xs text-foreground">{artifact.artifactName}</span>

            {removable && (
                <button
                    aria-label={`Remove ${artifact.artifactName}`}
                    className="shrink-0 rounded p-0.5 text-muted-foreground opacity-0 group-hover:opacity-100 hover:bg-muted hover:text-foreground"
                    disabled={deleteMutation.isPending}
                    onClick={handleRemove}
                    type="button"
                >
                    <XIcon className="size-3" />
                </button>
            )}
        </div>
    );

    if (clickable) {
        return (
            <button
                className="group flex w-full items-center gap-1.5 rounded px-1.5 py-1 text-left hover:bg-accent"
                onClick={() => void handleArtifactQuickOpen(artifact)}
                type="button"
            >
                {content}
            </button>
        );
    }

    return <div className="group flex items-center gap-1.5 px-1.5 py-1">{content}</div>;
};

export default AiHubArtifactRow;
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
npx vitest run src/ee/pages/automation/ai-hub/artifacts/tests/AiHubArtifactRow.test.tsx
```

Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add src/ee/pages/automation/ai-hub/artifacts
git commit -m "732 client - Add task-scoped AI Hub artifact row component"
```

---

### Task 3: The floating Artifacts card

**Files:**
- Create: `src/ee/pages/automation/ai-hub/artifacts/AiHubArtifactsCard.tsx`
- Modify: `src/ee/pages/automation/ai-hub/stores/useAiHubSettingsStore.ts`
- Modify: `src/ee/pages/automation/ai-hub/AiHubPanel.tsx:90` (mount the card in the panel's relative root)
- Test: `src/ee/pages/automation/ai-hub/artifacts/tests/AiHubArtifactsCard.test.tsx`

**Interfaces:**
- Consumes: `AiHubArtifactRow` (Task 2); `useAiHubTaskArtifactsQuery(taskId: number | undefined, workspaceId: number, enabled?: boolean)` and `useAiHubTasksQuery(workspaceId, environmentId, status)` from `tasks/hooks/useTasks`.
- Produces: default export `AiHubArtifactsCard` (no props); `useAiHubSettingsStore` gains `artifactsCardCollapsed: boolean` and `setArtifactsCardCollapsed: (collapsed: boolean) => void`.

- [ ] **Step 1: Add the collapse flag to the settings store**

Modify `src/ee/pages/automation/ai-hub/stores/useAiHubSettingsStore.ts`. Keys stay alphabetical in both the interface and the creator object:

```ts
interface AiHubSettingsStateI {
    artifactsCardCollapsed: boolean;
    setArtifactsCardCollapsed: (artifactsCardCollapsed: boolean) => void;
    setShowToolCalls: (showToolCalls: boolean) => void;
    showToolCalls: boolean;
}
```

and inside `persist((set) => ({ ... }))`:

```ts
                artifactsCardCollapsed: false,

                setArtifactsCardCollapsed: (artifactsCardCollapsed) => set({artifactsCardCollapsed}),

                setShowToolCalls: (showToolCalls) => set({showToolCalls}),

                showToolCalls: false,
```

Extend the store's doc comment with a sentence: the Artifacts card overlays the transcript, so its collapsed state is persisted globally rather than per task — a user who finds it intrusive collapses it once.

- [ ] **Step 2: Write the failing test**

Create `src/ee/pages/automation/ai-hub/artifacts/tests/AiHubArtifactsCard.test.tsx`:

```tsx
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {aiHubTasksStore} from '@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiHubArtifactsCard from '../AiHubArtifactsCard';

const {mockTaskKind, mockUseAiHubTaskArtifactsQuery} = vi.hoisted(() => ({
    mockTaskKind: {current: 'STANDARD'},
    mockUseAiHubTaskArtifactsQuery: vi.fn(),
}));

vi.mock('@/ee/pages/automation/ai-hub/tasks/hooks/useTasks', () => ({
    useAiHubTaskArtifactsQuery: (...args: unknown[]) => mockUseAiHubTaskArtifactsQuery(...args),
    useAiHubTasksQuery: () => ({data: [{id: 7, kind: mockTaskKind.current}]}),
}));

vi.mock('@/ee/pages/automation/ai-hub/artifacts/AiHubArtifactRow', () => ({
    default: ({artifact}: {artifact: {artifactName: string}}) => <div>{artifact.artifactName}</div>,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

describe('AiHubArtifactsCard', () => {
    beforeEach(() => {
        mockTaskKind.current = 'STANDARD';

        mockUseAiHubTaskArtifactsQuery.mockReturnValue({
            data: [{artifactName: 'report.csv', id: 1}],
        });

        aiHubTasksStore.setState({currentTaskId: 7});
        aiHubTabsStore.setState({rightPanelOpen: false});
    });

    it('renders the artifacts of the active task', () => {
        render(<AiHubArtifactsCard />);

        expect(screen.getByText('Artifacts')).toBeInTheDocument();
        expect(screen.getByText('report.csv')).toBeInTheDocument();
    });

    it('renders nothing while the resource panel is open', () => {
        aiHubTabsStore.setState({rightPanelOpen: true});

        const {container} = render(<AiHubArtifactsCard />);

        expect(container).toBeEmptyDOMElement();
    });

    it('renders nothing when the task has no artifacts', () => {
        mockUseAiHubTaskArtifactsQuery.mockReturnValue({data: []});

        const {container} = render(<AiHubArtifactsCard />);

        expect(container).toBeEmptyDOMElement();
    });

    it('renders nothing for a workflow chat, which never produces artifacts', () => {
        mockTaskKind.current = 'WORKFLOW_CHAT';

        const {container} = render(<AiHubArtifactsCard />);

        expect(container).toBeEmptyDOMElement();
    });

    it('renders nothing on the home view, where no task is active', () => {
        aiHubTasksStore.setState({currentTaskId: undefined});

        const {container} = render(<AiHubArtifactsCard />);

        expect(container).toBeEmptyDOMElement();
    });
});
```

- [ ] **Step 3: Run the test to verify it fails**

```bash
npx vitest run src/ee/pages/automation/ai-hub/artifacts/tests/AiHubArtifactsCard.test.tsx
```

Expected: FAIL — cannot resolve `../AiHubArtifactsCard`.

- [ ] **Step 4: Write the card**

Create `src/ee/pages/automation/ai-hub/artifacts/AiHubArtifactsCard.tsx`:

```tsx
import AiHubArtifactRow from '@/ee/pages/automation/ai-hub/artifacts/AiHubArtifactRow';
import useAiHubSettingsStore from '@/ee/pages/automation/ai-hub/stores/useAiHubSettingsStore';
import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useAiHubTaskArtifactsQuery, useAiHubTasksQuery} from '@/ee/pages/automation/ai-hub/tasks/hooks/useTasks';
import {useAiHubTasksStore} from '@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ChevronDownIcon, ChevronUpIcon} from 'lucide-react';

/**
 * Floating list of the ACTIVE task's artifacts, pinned to the top-right of the chat pane.
 *
 * <p>Hidden entirely whenever the resource panel is open: the panel's tab strip already lists what has
 * been opened, and the chat pane narrows to ~38% in that layout, which leaves no room for a 256px overlay.
 * Clicking a row opens the artifact as a tab — and every {@code aiHubTabsStore.open*Tab} setter also sets
 * {@code rightPanelOpen: true}, so the click that opens the panel is the same click that hides this card.</p>
 *
 * <p>Renders nothing when the task has no artifacts. A floating "No artifacts yet" card would be pure
 * noise over the transcript; that message lives in the resource panel's + menu instead, which the user
 * opens deliberately.</p>
 */
const AiHubArtifactsCard = () => {
    const artifactsCardCollapsed = useAiHubSettingsStore((state) => state.artifactsCardCollapsed);
    const setArtifactsCardCollapsed = useAiHubSettingsStore((state) => state.setArtifactsCardCollapsed);

    const rightPanelOpen = useAiHubTabsStore((state) => state.rightPanelOpen);
    const currentTaskId = useAiHubTasksStore((state) => state.currentTaskId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    // Cache lookup, not a fetch — the sidebar already loaded this list under the same query key.
    const {data: tasks} = useAiHubTasksQuery(currentWorkspaceId, currentEnvironmentId, 'ACTIVE');

    const currentTask = tasks?.find((task) => task.id === currentTaskId);
    // Workflow chats route through a webhook rather than the LLM agent, so they never accrue artifacts.
    const isWorkflowChat = currentTask?.kind === 'WORKFLOW_CHAT';

    const enabled = currentTaskId != null && currentWorkspaceId != null && !rightPanelOpen && !isWorkflowChat;

    const {data: artifacts} = useAiHubTaskArtifactsQuery(currentTaskId, currentWorkspaceId ?? 0, enabled);

    if (!enabled || !artifacts || artifacts.length === 0) {
        return null;
    }

    return (
        // Island styling mirrors AiHubResourcePanel's card (rounded-xl + border + shadow on
        // surface-neutral-primary) so the two right-hand surfaces read as the same material.
        <div className="absolute top-14 right-3 z-10 w-64 overflow-hidden rounded-xl border bg-surface-neutral-primary shadow-sm">
            <button
                aria-expanded={!artifactsCardCollapsed}
                className="flex w-full items-center gap-2 px-3 py-2 hover:bg-accent"
                onClick={() => setArtifactsCardCollapsed(!artifactsCardCollapsed)}
                type="button"
            >
                <span className="flex-1 text-left text-xs font-medium text-content-neutral-secondary">Artifacts</span>

                <span className="rounded bg-muted px-1 py-0.5 text-xs font-medium text-muted-foreground">
                    {artifacts.length}
                </span>

                {artifactsCardCollapsed ? (
                    <ChevronDownIcon className="size-3.5 text-muted-foreground" />
                ) : (
                    <ChevronUpIcon className="size-3.5 text-muted-foreground" />
                )}
            </button>

            {!artifactsCardCollapsed && (
                <div className="flex max-h-64 flex-col overflow-y-auto px-1.5 pb-1.5">
                    {artifacts.map((artifact) => (
                        <AiHubArtifactRow artifact={artifact} key={artifact.id} workspaceId={currentWorkspaceId} />
                    ))}
                </div>
            )}
        </div>
    );
};

export default AiHubArtifactsCard;
```

- [ ] **Step 5: Run the test to verify it passes**

```bash
npx vitest run src/ee/pages/automation/ai-hub/artifacts/tests/AiHubArtifactsCard.test.tsx
```

Expected: PASS (5 tests).

- [ ] **Step 6: Mount the card in the chat panel**

In `AiHubPanel.tsx`, add the import (sorts before `AiHubChatComposer` — `artifacts/` precedes `composer/`):

```ts
import AiHubArtifactsCard from '@/ee/pages/automation/ai-hub/artifacts/AiHubArtifactsCard';
```

and render it as the first child of the root element at `:90`, which is already `relative`:

```tsx
        <div className="relative flex size-full min-h-[50vh] flex-col overflow-x-hidden">
            {/* Floating artifacts list, absolutely positioned against this relative root. Self-hiding —
             * see AiHubArtifactsCard for the four conditions under which it renders nothing at all. */}

            <AiHubArtifactsCard />

            {/* ...existing header... */}
```

- [ ] **Step 7: Verify the panel still renders**

```bash
npx vitest run src/ee/pages/automation/ai-hub/tests/AiHubPanel.test.tsx
```

Expected: PASS. `AiHubPanel.test.tsx` already mocks `tasks/hooks/useTasks` with only `useAiHubTasksQuery` — the card also calls `useAiHubTaskArtifactsQuery` from that module, so add it to the existing mock at `:17-19`:

```tsx
vi.mock('@/ee/pages/automation/ai-hub/tasks/hooks/useTasks', () => ({
    useAiHubTaskArtifactsQuery: () => ({data: []}),
    useAiHubTasksQuery: () => ({data: [], isLoading: false}),
}));
```

- [ ] **Step 8: Full check**

```bash
npm run check
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add src/ee/pages/automation/ai-hub
git commit -m "732 client - Add floating Artifacts card to the AI Hub chat panel"
```

---

### Task 4: Strip the artifact affordances from the tasks sidebar

The sidebar becomes a flat list of task rows. `openArtifactInTask` goes with it — the card is scoped to the active task, so nothing switches tasks to open a resource any more.

**Files:**
- Modify: `src/ee/pages/automation/ai-hub/tasks/AiHubTasksSidebar.tsx`
- Modify: `src/ee/pages/automation/ai-hub/artifacts/artifactOpen.ts` (drop `openArtifactInTask`)
- Modify: `src/ee/pages/automation/ai-hub/artifacts/tests/artifactOpen.test.ts` (drop its describe block)

**Interfaces:**
- Consumes: nothing new.
- Produces: `TaskItemProps` loses `isExpanded` and `onToggleExpand`. No other exported signature changes.

- [ ] **Step 1: Delete the sidebar's artifact components**

In `AiHubTasksSidebar.tsx`, delete:
- `ArtifactRowProps` + `ArtifactRow` (`:534-619`),
- `ArtifactListProps` + `ArtifactList` (`:623-656`).

- [ ] **Step 2: Flatten `TaskItem`**

Still in `AiHubTasksSidebar.tsx`:
- `TaskItemProps`: remove `isExpanded: boolean;` (`:661`) and `onToggleExpand: () => void;` (`:666`).
- Destructuring (`:672-685`): remove `isExpanded` and `onToggleExpand`.
- Remove `showArtifacts` (`:686`) and its two-line comment.
- Remove the `artifactsEnabled` / `useAiHubTaskArtifactsQuery` / `artifactCount` block (`:707-719`) including the "Artifact count badge" doc comment — it documents a deferral that no longer exists.
- Remove the count badge JSX (`:862-866`).
- Remove the expand-chevron `<button>` (`:878-893`).
- Remove the trailing `{isExpanded && showArtifacts && <ArtifactList task={task} workspaceId={workspaceId} />}` (`:936`).

- [ ] **Step 3: Remove the expansion state from the list**

- Delete `const [expandedTaskIds, setExpandedTaskIds] = useState<Set<number>>(new Set());` (`:951`).
- Delete `handleToggleExpand` (`:1178-1186`).
- Delete the `isExpanded={expandedTaskIds.has(task.id)}` (`:1416`) and `onToggleExpand={() => handleToggleExpand(task.id)}` (`:1422`) props at the `<TaskItem>` call site.

Also update the sidebar comment at `:1336` — it refers to "Artifact History" as a sibling top-level menu item, which does not exist. Reduce that clause to "(alongside Personal Agents and Workflow Chats)".

- [ ] **Step 4: Delete `openArtifactInTask`**

In `artifacts/artifactOpen.ts`, delete `openArtifactInTask` and its doc comment. Its only consumer was the sidebar row deleted in Step 1. `AiHubTaskI` becomes an unused import there — remove it. In `artifacts/tests/artifactOpen.test.ts`, delete `describe('openArtifactInTask')` and the `buildTask` / `buildFileArtifact` helpers if nothing else in that file uses them.

- [ ] **Step 5: Prune imports**

```bash
npm run lint
```

Remove only what lint reports. Expected removals in the sidebar: `getArtifactIcon`, `handleArtifactQuickOpen`, `isArtifactClickable`, `isArtifactRemovable`, `openArtifactInTask`, `useAiHubTaskArtifactsQuery`, `useDeleteAiHubTaskArtifactMutation`, `AiHubArtifactKindType`, `AiHubTaskArtifactI`, `XIcon`, `MouseEvent`. **Verify each against the file before deleting** — `ChevronDownIcon` / `ChevronRightIcon` are also used by the Context collapsible, `HexagonIcon` by the Skills link, and `useQueryClient` / `toast` by the task delete flow.

- [ ] **Step 6: Run the sidebar and artifact tests**

```bash
npx vitest run src/ee/pages/automation/ai-hub/tasks src/ee/pages/automation/ai-hub/artifacts
```

Expected: PASS.

- [ ] **Step 7: Full check**

```bash
npm run check
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/ee/pages/automation/ai-hub
git commit -m "732 client - Flatten AI Hub sidebar task rows, dropping the nested artifact list"
```

---

### Task 5: Artifacts branch in the resource panel's + menu

The card hides while the resource panel is open, so this branch is how the list stays reachable in that layout.

**Files:**
- Modify: `src/ee/pages/automation/ai-hub/AiHubFilePicker.tsx`
- Test: `src/ee/pages/automation/ai-hub/tests/AiHubFilePicker.test.tsx` (new file — the picker has no test today)

**Interfaces:**
- Consumes: `handleArtifactQuickOpen`, `isArtifactClickable` (Task 1), `getArtifactIcon` (Task 1), `useAiHubTaskArtifactsQuery` (existing).
- Produces: nothing new exported.

- [ ] **Step 1: Write the failing test**

Create `src/ee/pages/automation/ai-hub/tests/AiHubFilePicker.test.tsx`:

```tsx
import AiHubFilePicker from '@/ee/pages/automation/ai-hub/AiHubFilePicker';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {aiHubTasksStore} from '@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {mockUseAiHubTaskArtifactsQuery} = vi.hoisted(() => ({
    mockUseAiHubTaskArtifactsQuery: vi.fn(),
}));

vi.mock('@/ee/pages/automation/ai-hub/tasks/hooks/useTasks', () => ({
    useAiHubTaskArtifactsQuery: (...args: unknown[]) => mockUseAiHubTaskArtifactsQuery(...args),
}));

// The picker's other branches fan out into project/workflow/file/data-table/knowledge-base queries that
// are irrelevant to the artifacts branch; stub them flat so the test exercises one branch only.
vi.mock('@/shared/middleware/graphql', () => ({
    useDataTablesQuery: () => ({data: undefined}),
    useGetAssetFilesQuery: () => ({data: undefined}),
    useKnowledgeBasesQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetWorkspaceProjectsQuery: () => ({data: []}),
}));

vi.mock('@/shared/queries/automation/workflowExecutions.queries', () => ({
    useInfiniteWorkspaceProjectWorkflowExecutionsQuery: () => ({
        data: undefined,
        fetchNextPage: vi.fn(),
        hasNextPage: false,
        isFetchingNextPage: false,
    }),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
};

describe('AiHubFilePicker artifacts branch', () => {
    beforeEach(() => {
        aiHubTasksStore.setState({currentTaskId: 7});

        aiHubTabsStore.setState({
            activeTabId: undefined,
            activeTaskId: undefined,
            openTabs: [],
            rightPanelOpen: false,
            snapshotsByTaskId: {},
            tasksSidebarCollapsed: true,
        });

        mockUseAiHubTaskArtifactsQuery.mockReturnValue({
            data: [
                {
                    artifactId: 'file-1',
                    artifactName: 'report.csv',
                    createdAt: new Date().toISOString(),
                    id: 1,
                    kind: 'FILE_CREATED',
                    metadataJson: null,
                    status: 'APPLIED',
                    taskId: 7,
                },
            ],
        });
    });

    it('drills into Artifacts and opens the picked artifact as a tab', () => {
        wrap(<AiHubFilePicker />);

        fireEvent.click(screen.getByRole('button', {name: 'Add resource'}));
        fireEvent.click(screen.getByText('Artifacts'));
        fireEvent.click(screen.getByText('report.csv'));

        const state = aiHubTabsStore.getState();

        expect(state.openTabs).toHaveLength(1);
        expect(state.openTabs[0]!.kind).toBe('file');
    });

    it('shows an empty state when the task has no artifacts', () => {
        mockUseAiHubTaskArtifactsQuery.mockReturnValue({data: []});

        wrap(<AiHubFilePicker />);

        fireEvent.click(screen.getByRole('button', {name: 'Add resource'}));
        fireEvent.click(screen.getByText('Artifacts'));

        expect(screen.getByText('No artifacts yet.')).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
npx vitest run src/ee/pages/automation/ai-hub/tests/AiHubFilePicker.test.tsx
```

Expected: FAIL — `Unable to find an element with the text: Artifacts`.

- [ ] **Step 3: Extend the menu path type**

In `AiHubFilePicker.tsx:92`:

```ts
type MenuPathType =
    | []
    | ['workflows']
    | ['files']
    | ['dataTables']
    | ['knowledgeBases']
    | ['workflowExecutions']
    | ['artifacts'];
```

- [ ] **Step 4: Add the data and the select handler**

Add imports (alphabetical within their groups; `PackageIcon` sorts between `HistoryIcon` and `PlusIcon`):

```ts
import {getArtifactIcon} from '@/ee/pages/automation/ai-hub/artifacts/artifactIcons';
import {handleArtifactQuickOpen, isArtifactClickable} from '@/ee/pages/automation/ai-hub/artifacts/artifactOpen';
import {AiHubTaskArtifactI} from '@/ee/pages/automation/ai-hub/tasks/api/tasks.api';
import {useAiHubTaskArtifactsQuery} from '@/ee/pages/automation/ai-hub/tasks/hooks/useTasks';
import {useAiHubTasksStore} from '@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore';
```

Inside the component, next to the other store reads (after `environmentId` at `:103`):

```ts
    const currentTaskId = useAiHubTasksStore((state) => state.currentTaskId);
```

next to the other queries:

```ts
    // Warm cache hit — the Artifacts card issues this same query under the same key. Non-clickable
    // artifacts (a WORKFLOW_CREATED with no projectId in metadata, say) have nowhere to route to, so they
    // are filtered out rather than rendered as dead menu entries.
    const {data: taskArtifacts} = useAiHubTaskArtifactsQuery(
        currentTaskId,
        currentWorkspaceId ?? 0,
        Boolean(currentWorkspaceId)
    );
```

as a derived value:

```ts
    const clickableArtifacts = useMemo(() => (taskArtifacts ?? []).filter(isArtifactClickable), [taskArtifacts]);
```

and next to the other `handleSelect*` functions (after `handleSelectWorkflowExecution` at `:245`):

```ts
    const handleSelectArtifact = (artifact: AiHubTaskArtifactI) => {
        void handleArtifactQuickOpen(artifact);
        closeAndReset();
    };
```

- [ ] **Step 5: Add the root item and the branch**

Append to the root `CommandGroup`, after the `root-workflow-executions` item (`:463-472`):

```tsx
                                <CommandItem onSelect={() => setMenuPath(['artifacts'])} value="root-artifacts">
                                    <PackageIcon className="mr-2 size-3.5" />

                                    <span className="flex-1">Artifacts</span>

                                    <ChevronRightIcon className="size-3.5 text-muted-foreground" />
                                </CommandItem>
```

and add the branch body alongside the other `menuPath[0] === …` blocks:

```tsx
                                {menuPath[0] === 'artifacts' && (
                                    <CommandGroup heading="Artifacts">
                                        {clickableArtifacts.length === 0 && (
                                            <CommandEmpty>No artifacts yet.</CommandEmpty>
                                        )}

                                        {clickableArtifacts.map((artifact) => (
                                            <CommandItem
                                                key={`artifact-${artifact.id}`}
                                                onSelect={() => handleSelectArtifact(artifact)}
                                                value={`artifact-${artifact.id}-${artifact.artifactName}`}
                                            >
                                                {getArtifactIcon(artifact.kind)}

                                                <span className="ml-2 truncate">{artifact.artifactName}</span>
                                            </CommandItem>
                                        ))}
                                    </CommandGroup>
                                )}
```

Artifacts are deliberately **not** added to the `debouncedSearch` results view (`:285-423`). An artifact for a file is that file — listing them there would show every matching file twice under one search term. No `artifactsShowCount` cap either: the list is one task's artifacts, not a workspace-wide table.

- [ ] **Step 6: Run the test to verify it passes**

```bash
npx vitest run src/ee/pages/automation/ai-hub/tests/AiHubFilePicker.test.tsx
```

Expected: PASS (2 tests).

- [ ] **Step 7: Full check**

```bash
npm run check
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/ee/pages/automation/ai-hub
git commit -m "732 client - Add Artifacts branch to the AI Hub resource panel picker"
```

---

## Manual verification

After Task 5, run the app (`npm run dev`, server on :9555) and check in `/automation/ai-hub`:

1. Open a task that has artifacts with the resource panel closed → the card floats top-right of the chat, showing the count.
2. Click an artifact → it opens as a tab in the resource panel, and the card disappears in the same motion.
3. Close the resource panel → the card comes back.
4. With the panel open, `+` → **Artifacts** → the same list; picking one opens it as a tab.
5. Collapse the card via its chevron, switch tasks, reload → it stays collapsed (persisted globally).
6. Open a workflow chat → no card, in either state.
7. Sidebar task rows show no chevron, no count badge, and no nested list.
