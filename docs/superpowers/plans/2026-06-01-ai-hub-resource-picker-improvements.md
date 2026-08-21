# AI Hub Resource Picker Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the AI Hub resource pickers so all workflows load and group by project, workflow executions page in from the server via "Show more", and execution rows show their subtitle beneath the name.

**Architecture:** Two pickers share the same patterns: `AiHubFilePicker` (the resource-panel `+` menu, the one in the user's screenshots) and `ResourcePickerMenu` (the composer `@`/`+` menu). `ResourcePickerMenu` already groups workflows via a project drilldown; `AiHubFilePicker` shows them flat. A pure `groupWorkflowsByProject` helper is extracted and shared. Executions move from a single first-page fetch to an accumulating `useInfiniteQuery` whose "Show more" calls `fetchNextPage`. Execution row layout becomes a stacked name-over-subtitle column.

**Tech Stack:** React 19 + TypeScript, TanStack Query (`useInfiniteQuery`), Zustand, Vitest, Tailwind.

**Spec:** `docs/superpowers/specs/2026-06-01-ai-hub-resource-picker-improvements-design.md`

**Conventions:** Run all client commands from `client/`. ESLint enforces alphabetical object-key order (`sort-keys`, though several of these files carry a top-of-file `/* eslint-disable sort-keys */`), alphabetical named-import order, and alphabetical JSX prop order. Interface names end in `I`/`Props`. Commit messages: `hub - <description>`.

**Concurrency note:** Another worker may be committing to branch `0_732` simultaneously. Always `git add` EXPLICIT paths (never `git add -A`); after committing, `git status --short` and leave any unrelated modified files alone. Ignore known pre-existing type errors in `functionSuggestion*` files.

---

### Task 1: Execution rows show subtitle beneath the name

The execution row renders `name` and `project · STATUS` inline on one row (subtitle to the right). Stack them: name on top, muted subtitle beneath. There are **4 identical occurrences**: `ResourcePickerMenu.tsx` (search ~line 583–589, drilldown ~line 1053–1058) and `AiHubFilePicker.tsx` (search ~line 363–369, drilldown ~line 594–599).

**Files:**
- Modify: `client/src/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx` (2 sites)
- Modify: `client/src/pages/automation/ai-hub/AiHubFilePicker.tsx` (2 sites)

- [ ] **Step 1: Replace all 4 occurrences of the inline execution row**

In EACH of the 4 sites, the current markup is exactly:
```tsx
                                                    <HistoryIcon className="mr-2 size-3.5" />

                                                    <span className="flex-1 truncate">{label}</span>

                                                    <span className="ml-2 text-xs text-muted-foreground">
                                                        {[projectName, status].filter(Boolean).join(' · ')}
                                                    </span>
```
Replace each with the stacked layout:
```tsx
                                                    <HistoryIcon className="mr-2 size-3.5" />

                                                    <div className="flex min-w-0 flex-1 flex-col">
                                                        <span className="truncate">{label}</span>

                                                        <span className="truncate text-xs text-muted-foreground">
                                                            {[projectName, status].filter(Boolean).join(' · ')}
                                                        </span>
                                                    </div>
```
Apply this to all 4 sites (2 in `ResourcePickerMenu.tsx`, 2 in `AiHubFilePicker.tsx`). The indentation differs slightly per site — match each site's surrounding indentation; only the structure (icon + a `flex-col` column wrapping the two spans) must match. Use `grep -n "filter(Boolean).join(' · ')"` in each file to locate the exact sites.

- [ ] **Step 2: Typecheck + lint the two files**

Run: `npx eslint src/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx src/pages/automation/ai-hub/AiHubFilePicker.tsx && npx tsc --noEmit 2>&1 | grep -E "ResourcePickerMenu|AiHubFilePicker" || echo "no errors in these files"`
Expected: eslint clean; no type errors referencing these two files.

- [ ] **Step 3: Run the picker tests (regression)**

Run: `npx vitest run src/pages/automation/ai-hub/resource-picker src/pages/automation/ai-hub/composer/tests/AiHubComposerResourcePicker.test.tsx`
Expected: PASS (these tests assert selection behavior, not the row layout, so they remain green).

- [ ] **Step 4: Commit**

```bash
git add client/src/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx client/src/pages/automation/ai-hub/AiHubFilePicker.tsx
git commit -m "hub - Stack AI Hub execution row subtitle beneath the name"
```

---

### Task 2: Load all workflows (remove 10-project cap) and carry projectName

Both pickers build the workflow list by looping workspace projects but slice to `PROJECT_LIMIT = 10`, so workflows in the 11th+ project never appear. Remove the cap. `ResourcePickerMenu`'s `WorkflowItemI` already has `projectName`; `AiHubFilePicker`'s does not — add it (needed for grouping in Task 3).

**Files:**
- Modify: `client/src/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx` (`useAllWorkspaceWorkflows`, `PROJECT_LIMIT`)
- Modify: `client/src/pages/automation/ai-hub/AiHubFilePicker.tsx` (`useAllWorkspaceWorkflows`, `WorkflowItemI`, `PROJECT_LIMIT`)

- [ ] **Step 1: ResourcePickerMenu — remove the cap**

In `ResourcePickerMenu.tsx`, delete the `PROJECT_LIMIT` constant (currently `const PROJECT_LIMIT = 10;`). In `useAllWorkspaceWorkflows`, replace:
```tsx
    const cappedProjects = useMemo(() => (projects ?? []).slice(0, PROJECT_LIMIT), [projects]);
```
with:
```tsx
    // All workspace projects (no cap) so workflows from every project are reachable. Project counts are
    // bounded (tens); each getProjectWorkflows is cheap and react-query-cached.
    const allProjects = useMemo(() => projects ?? [], [projects]);
```
Then replace every remaining reference to `cappedProjects` in that hook with `allProjects` (there are two more: the `useQueries` `.map` and the `forEach` in the return memo, plus the `}, [workflowDataFingerprint, cappedProjects]);` dependency array → `allProjects`).

- [ ] **Step 2: AiHubFilePicker — add projectName to WorkflowItemI and remove the cap**

In `AiHubFilePicker.tsx`:

(a) Find the `WorkflowItemI` interface (near the top, ~line 27). It currently is:
```tsx
interface WorkflowItemI {
    id: string;
    name: string;
    projectId: string;
    projectWorkflowId: number;
}
```
Add `projectName` (keep keys alphabetical):
```tsx
interface WorkflowItemI {
    id: string;
    name: string;
    projectId: string;
    projectName: string;
    projectWorkflowId: number;
}
```

(b) In `AiHubFilePicker`'s `useAllWorkspaceWorkflows`, delete the local `const PROJECT_LIMIT = …;` if present and replace the `cappedProjects` slice with the uncapped version (same as Step 1):
```tsx
    const allProjects = useMemo(() => projects ?? [], [projects]);
```
Replace all `cappedProjects` references in the hook with `allProjects` (the `useQueries` map, the `forEach`, and the dependency array).

(c) In that hook's `allWorkflows.push({...})`, add the `projectName` field (keep keys alphabetical):
```tsx
                allWorkflows.push({
                    id: String(workflow.id ?? ''),
                    name: workflow.label ?? workflow.id ?? '',
                    projectId: String(project.id),
                    projectName: project.name ?? `Project ${project.id}`,
                    // `workflow.id` is the workflow's UUID string; the numeric join-entity id the workflow
                    // editor needs lives on `projectWorkflowId`. Number(id) is NaN for real UUIDs.
                    projectWorkflowId: workflow.projectWorkflowId ?? 0,
                });
```

- [ ] **Step 3: Typecheck + lint**

Run: `npx eslint src/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx src/pages/automation/ai-hub/AiHubFilePicker.tsx && npx tsc --noEmit 2>&1 | grep -E "ResourcePickerMenu|AiHubFilePicker" || echo "no errors in these files"`
Expected: clean. (If `PROJECT_LIMIT` is now unused anywhere, ESLint `no-unused-vars` will flag it — ensure it's deleted from both files.)

- [ ] **Step 4: Run picker tests**

Run: `npx vitest run src/pages/automation/ai-hub/resource-picker src/pages/automation/ai-hub/composer/tests`
Expected: PASS. (The existing `ResourcePickerMenu` workflow test mocks ≤2 projects, so removing the cap does not change its assertions.)

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx client/src/pages/automation/ai-hub/AiHubFilePicker.tsx
git commit -m "hub - Load workflows from all projects in AI Hub pickers (remove 10-project cap)"
```

---

### Task 3: Group workflows by project (shared helper + AiHubFilePicker render)

`ResourcePickerMenu` already groups workflows by project via its drilldown (`filteredWorkflowProjects`, ~line 299–321). Extract that logic into a shared, tested pure helper and reuse it in both `ResourcePickerMenu` (refactor — no behavior change) and `AiHubFilePicker` (new grouped render replacing the flat list).

**Files:**
- Create: `client/src/pages/automation/ai-hub/resource-picker/groupWorkflowsByProject.ts`
- Create: `client/src/pages/automation/ai-hub/resource-picker/tests/groupWorkflowsByProject.test.ts`
- Modify: `client/src/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx` (use the helper)
- Modify: `client/src/pages/automation/ai-hub/AiHubFilePicker.tsx` (grouped render)

- [ ] **Step 1: Write the failing test for the helper**

Create `client/src/pages/automation/ai-hub/resource-picker/tests/groupWorkflowsByProject.test.ts`:

```ts
import {describe, expect, it} from 'vitest';

import {groupWorkflowsByProject} from '../groupWorkflowsByProject';

describe('groupWorkflowsByProject', () => {
    it('groups workflows by project, preserving first-seen project order', () => {
        const workflows = [
            {id: 'a', name: 'A', projectId: 'p1', projectName: 'Project One'},
            {id: 'b', name: 'B', projectId: 'p2', projectName: 'Project Two'},
            {id: 'c', name: 'C', projectId: 'p1', projectName: 'Project One'},
        ];

        const groups = groupWorkflowsByProject(workflows);

        expect(groups).toHaveLength(2);
        expect(groups[0]!.projectId).toBe('p1');
        expect(groups[0]!.projectName).toBe('Project One');
        expect(groups[0]!.workflows.map((workflow) => workflow.id)).toEqual(['a', 'c']);
        expect(groups[1]!.projectId).toBe('p2');
        expect(groups[1]!.workflows.map((workflow) => workflow.id)).toEqual(['b']);
    });

    it('returns an empty array for no workflows', () => {
        expect(groupWorkflowsByProject([])).toEqual([]);
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `npx vitest run src/pages/automation/ai-hub/resource-picker/tests/groupWorkflowsByProject.test.ts`
Expected: FAIL — module does not exist.

- [ ] **Step 3: Create the helper**

Create `client/src/pages/automation/ai-hub/resource-picker/groupWorkflowsByProject.ts`:

```ts
interface WorkflowProjectGroupI<T> {
    projectId: string;
    projectName: string;
    workflows: T[];
}

/**
 * Group workflow items by their parent project, preserving the order in which each project is first
 * seen (so the project list stays stable across re-renders). Generic over any item carrying
 * `projectId` + `projectName`, so both pickers' `WorkflowItemI` shapes work without coupling.
 */
export function groupWorkflowsByProject<T extends {projectId: string; projectName: string}>(
    workflows: T[]
): WorkflowProjectGroupI<T>[] {
    const order: string[] = [];
    const byProject = new Map<string, WorkflowProjectGroupI<T>>();

    for (const workflow of workflows) {
        const existing = byProject.get(workflow.projectId);

        if (existing) {
            existing.workflows.push(workflow);
        } else {
            order.push(workflow.projectId);

            byProject.set(workflow.projectId, {
                projectId: workflow.projectId,
                projectName: workflow.projectName,
                workflows: [workflow],
            });
        }
    }

    return order.map((projectId) => byProject.get(projectId)!);
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `npx vitest run src/pages/automation/ai-hub/resource-picker/tests/groupWorkflowsByProject.test.ts`
Expected: PASS.

- [ ] **Step 5: Refactor ResourcePickerMenu to use the helper (no behavior change)**

In `ResourcePickerMenu.tsx`, add the import (alphabetical within its import block):
```tsx
import {groupWorkflowsByProject} from '@/pages/automation/ai-hub/resource-picker/groupWorkflowsByProject';
```
Replace the body of `filteredWorkflowProjects` (the `useMemo` at ~line 299–321) with:
```tsx
    const filteredWorkflowProjects = useMemo(() => groupWorkflowsByProject(filteredWorkflows), [filteredWorkflows]);
```
(The helper returns the identical shape `{projectId, projectName, workflows}[]` in identical order, so the drilldown render is unchanged.)

- [ ] **Step 6: Group AiHubFilePicker's workflow render by project**

In `AiHubFilePicker.tsx`, add the import:
```tsx
import {groupWorkflowsByProject} from '@/pages/automation/ai-hub/resource-picker/groupWorkflowsByProject';
```
Add a grouped derivation next to the other `useMemo`s (after `filteredWorkflows`, ~line 164):
```tsx
    const workflowProjectGroups = useMemo(
        () => groupWorkflowsByProject(filteredWorkflows.slice(0, workflowsShowCount)),
        [filteredWorkflows, workflowsShowCount]
    );
```

Then replace BOTH workflow render blocks so each project renders as its own sub-group.

(a) SEARCH mode block (currently ~line 291–305):
```tsx
                                {filteredWorkflows.length > 0 && (
                                    <CommandGroup heading="Workflows">
                                        {visibleWorkflows.map((workflow) => (
                                            <CommandItem
                                                key={`workflow-${workflow.id}`}
                                                onSelect={() => handleSelectWorkflow(workflow)}
                                                value={`workflow-${workflow.id}-${workflow.name}`}
                                            >
                                                <WorkflowIcon className="mr-2 size-3.5" />

                                                {workflow.name}
                                            </CommandItem>
                                        ))}
                                    </CommandGroup>
                                )}
```
becomes:
```tsx
                                {filteredWorkflows.length > 0 &&
                                    workflowProjectGroups.map((group) => (
                                        <CommandGroup heading={group.projectName} key={`workflow-project-${group.projectId}`}>
                                            {group.workflows.map((workflow) => (
                                                <CommandItem
                                                    key={`workflow-${workflow.id}`}
                                                    onSelect={() => handleSelectWorkflow(workflow)}
                                                    value={`workflow-${workflow.id}-${workflow.name}`}
                                                >
                                                    <WorkflowIcon className="mr-2 size-3.5" />

                                                    {workflow.name}
                                                </CommandItem>
                                            ))}
                                        </CommandGroup>
                                    ))}
```

(b) DRILLDOWN `menuPath[0] === 'workflows'` block (currently ~line 434–462, which renders `visibleWorkflows` flat under one `CommandGroup heading="Workflows"` with the show-more at ~line 450). Replace its `visibleWorkflows.map(...)` body so it renders the grouped sub-headers, keeping the existing empty-state and the existing `workflows-show-more` CommandItem AFTER the groups. The block becomes:
```tsx
                                {menuPath[0] === 'workflows' && (
                                    <>
                                        {visibleWorkflows.length === 0 && <CommandEmpty>No workflows.</CommandEmpty>}

                                        {workflowProjectGroups.map((group) => (
                                            <CommandGroup heading={group.projectName} key={`workflow-project-${group.projectId}`}>
                                                {group.workflows.map((workflow) => (
                                                    <CommandItem
                                                        key={`workflow-${workflow.id}`}
                                                        onSelect={() => handleSelectWorkflow(workflow)}
                                                        value={`workflow-${workflow.id}-${workflow.name}`}
                                                    >
                                                        <WorkflowIcon className="mr-2 size-3.5" />

                                                        {workflow.name}
                                                    </CommandItem>
                                                ))}
                                            </CommandGroup>
                                        ))}

                                        {filteredWorkflows.length > workflowsShowCount && (
                                            <CommandGroup>
                                                <CommandItem
                                                    key="workflows-show-more"
                                                    onSelect={() =>
                                                        setWorkflowsShowCount((count) => count + SECTION_EXPAND_INCREMENT)
                                                    }
                                                    value="workflows-show-more"
                                                >
                                                    <span className="text-xs text-muted-foreground">
                                                        {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredWorkflows.length - workflowsShowCount)} more…`}
                                                    </span>
                                                </CommandItem>
                                            </CommandGroup>
                                        )}
                                    </>
                                )}
```
IMPORTANT: read the actual current drilldown block first (it currently wraps everything in a single `<CommandGroup heading="Workflows">`). Replace that whole wrapper with the `<>…</>` fragment above so each project gets its own `CommandGroup` heading and the show-more sits in its own trailing group. Keep the existing `handleSelectWorkflow` handler and `SECTION_EXPAND_INCREMENT` reference intact.

- [ ] **Step 7: Typecheck + lint + tests**

Run: `npx eslint src/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx src/pages/automation/ai-hub/AiHubFilePicker.tsx src/pages/automation/ai-hub/resource-picker/groupWorkflowsByProject.ts`
Run: `npx tsc --noEmit 2>&1 | grep -E "ResourcePickerMenu|AiHubFilePicker|groupWorkflowsByProject" || echo "no errors in these files"`
Run: `npx vitest run src/pages/automation/ai-hub/resource-picker src/pages/automation/ai-hub/composer/tests`
Expected: all clean / PASS. The `ResourcePickerMenu` drilldown still works (helper returns identical shape); `AiHubFilePicker` now renders one `CommandGroup` per project.

- [ ] **Step 8: Commit**

```bash
git add client/src/pages/automation/ai-hub/resource-picker/groupWorkflowsByProject.ts client/src/pages/automation/ai-hub/resource-picker/tests/groupWorkflowsByProject.test.ts client/src/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx client/src/pages/automation/ai-hub/AiHubFilePicker.tsx
git commit -m "hub - Group AI Hub picker workflows by project (shared helper)"
```

---

### Task 4: Workflow executions page in from the server via "Show more"

Both pickers fetch only page 0 of executions (page size 20, fixed by the backend), so most of the 33k+ executions are unreachable and there is no working "load more". Replace the single-page query with an accumulating `useInfiniteQuery`; "Show more" calls `fetchNextPage`, stopping at `totalPages`. Search continues to filter the loaded (accumulated) rows.

**Files:**
- Modify: `client/src/shared/queries/automation/workflowExecutions.queries.ts` (new infinite hook + a pure page-param helper)
- Create: `client/src/shared/queries/automation/tests/workflowExecutions.queries.test.ts`
- Modify: `client/src/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx` (executions consumption + show-more)
- Modify: `client/src/pages/automation/ai-hub/AiHubFilePicker.tsx` (executions consumption + show-more)

- [ ] **Step 1: Write the failing test for the page-param helper**

Create `client/src/shared/queries/automation/tests/workflowExecutions.queries.test.ts`:

```ts
import {describe, expect, it} from 'vitest';

import {getNextWorkflowExecutionsPageParam} from '../workflowExecutions.queries';

describe('getNextWorkflowExecutionsPageParam', () => {
    it('returns the next page number while more pages remain', () => {
        expect(getNextWorkflowExecutionsPageParam({number: 0, totalPages: 6} as never)).toBe(1);
        expect(getNextWorkflowExecutionsPageParam({number: 4, totalPages: 6} as never)).toBe(5);
    });

    it('returns undefined on the last page', () => {
        expect(getNextWorkflowExecutionsPageParam({number: 5, totalPages: 6} as never)).toBeUndefined();
    });

    it('returns undefined when totals are missing', () => {
        expect(getNextWorkflowExecutionsPageParam({} as never)).toBeUndefined();
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `npx vitest run src/shared/queries/automation/tests/workflowExecutions.queries.test.ts`
Expected: FAIL — `getNextWorkflowExecutionsPageParam` is not exported.

- [ ] **Step 3: Add the helper + infinite hook**

In `client/src/shared/queries/automation/workflowExecutions.queries.ts`:

(a) Change the react-query import to include `useInfiniteQuery`:
```ts
import {useInfiniteQuery, useQuery} from '@tanstack/react-query';
```
(b) Add, after the existing `useGetWorkspaceProjectWorkflowExecutionsQuery`:
```ts
/**
 * Compute the next page number for the executions infinite query, or `undefined` when the last page
 * has been loaded. The backend page size is fixed (20); paging is driven purely by page number.
 */
export function getNextWorkflowExecutionsPageParam(lastPage: Page): number | undefined {
    const current = lastPage.number ?? 0;
    const totalPages = lastPage.totalPages ?? 0;

    return current + 1 < totalPages ? current + 1 : undefined;
}

/**
 * Accumulating executions query: each `fetchNextPage()` loads the next server page (page size 20) and
 * appends it, so the picker can "Show more" across the full result set instead of seeing only page 0.
 * Keyed on the base request (sans pageNumber).
 */
export const useInfiniteWorkspaceProjectWorkflowExecutionsQuery = (
    request: GetWorkflowExecutionsPageRequest,
    enabled?: boolean
) =>
    useInfiniteQuery<Page, Error>({
        queryKey: [...WorkflowExecutionKeys.workflowExecutions, 'infinite', request],
        queryFn: ({pageParam}) =>
            new WorkflowExecutionApi().getWorkflowExecutionsPage({
                ...request,
                embedded: false,
                pageNumber: pageParam as number,
            }),
        initialPageParam: 0,
        getNextPageParam: getNextWorkflowExecutionsPageParam,
        enabled: enabled === undefined ? true : enabled,
    });
```
(`Page`, `GetWorkflowExecutionsPageRequest`, `WorkflowExecutionApi`, and `WorkflowExecutionKeys` are already imported/defined in this file.)

- [ ] **Step 4: Run the test to verify it passes**

Run: `npx vitest run src/shared/queries/automation/tests/workflowExecutions.queries.test.ts`
Expected: PASS (3 cases).

- [ ] **Step 5: Wire AiHubFilePicker to the infinite query**

In `AiHubFilePicker.tsx`:

(a) Change the executions import to the infinite hook:
```tsx
import {useInfiniteWorkspaceProjectWorkflowExecutionsQuery} from '@/shared/queries/automation/workflowExecutions.queries';
```
(b) Replace the executions query call (currently ~line 128–131):
```tsx
    const {
        data: workflowExecutionsData,
        fetchNextPage: fetchNextWorkflowExecutionsPage,
        hasNextPage: hasMoreWorkflowExecutions,
        isFetchingNextPage: isFetchingMoreWorkflowExecutions,
    } = useInfiniteWorkspaceProjectWorkflowExecutionsQuery({
        environmentId: environmentId ?? DEVELOPMENT_ENVIRONMENT,
        id: currentWorkspaceId ?? 0,
    });
```
(c) Replace the `filteredWorkflowExecutions` memo (currently ~line 166–182) to flatten accumulated pages:
```tsx
    const filteredWorkflowExecutions = useMemo(() => {
        // Flatten all loaded pages. The backend has no text search, so search filters the loaded rows.
        const executions = (workflowExecutionsData?.pages.flatMap((page) => page.content) ?? []) as Array<{
            id: number;
            job?: {status?: string};
            project?: {name?: string};
            workflow?: {label?: string};
        }>;

        return executions.filter((execution) => {
            const label = (execution.workflow?.label ?? '').toLowerCase();
            const projectName = (execution.project?.name ?? '').toLowerCase();

            return label.includes(lowerSearch) || projectName.includes(lowerSearch);
        });
    }, [workflowExecutionsData, lowerSearch]);
```
(d) Remove the client-side executions cap: delete `const [workflowExecutionsShowCount, setWorkflowExecutionsShowCount] = useState(SECTION_INITIAL_CAP);`, its reset in `closeAndReset` (`setWorkflowExecutionsShowCount(SECTION_INITIAL_CAP);`), and `const visibleWorkflowExecutions = filteredWorkflowExecutions.slice(0, workflowExecutionsShowCount);` (replace usages of `visibleWorkflowExecutions` with `filteredWorkflowExecutions`). The server now bounds how many are loaded.
(e) In BOTH executions render blocks (search ~line 350, drilldown ~line 578), change `visibleWorkflowExecutions.map(...)` to `filteredWorkflowExecutions.map(...)`, and AFTER the `.map(...)`, add a server-driven "Show more" inside the `<CommandGroup heading="Workflow Executions">`:
```tsx
                                        {hasMoreWorkflowExecutions && (
                                            <CommandItem
                                                key="workflow-executions-show-more"
                                                onSelect={() => {
                                                    void fetchNextWorkflowExecutionsPage();
                                                }}
                                                value="workflow-executions-show-more"
                                            >
                                                <span className="text-xs text-muted-foreground">
                                                    {isFetchingMoreWorkflowExecutions ? 'Loading…' : 'Show more…'}
                                                </span>
                                            </CommandItem>
                                        )}
```

- [ ] **Step 6: Wire ResourcePickerMenu to the infinite query (same pattern)**

In `ResourcePickerMenu.tsx`, apply the identical changes:
(a) Import `useInfiniteWorkspaceProjectWorkflowExecutionsQuery` (replace the `useGetWorkspaceProjectWorkflowExecutionsQuery` import if it is no longer used elsewhere in the file; if it IS still used, add the infinite import alongside).
(b) Replace the executions query call (~line 196–199) with the destructured infinite form from Step 5(b) (using `workspaceId` instead of `currentWorkspaceId`):
```tsx
    const {
        data: workflowExecutionsData,
        fetchNextPage: fetchNextWorkflowExecutionsPage,
        hasNextPage: hasMoreWorkflowExecutions,
        isFetchingNextPage: isFetchingMoreWorkflowExecutions,
    } = useInfiniteWorkspaceProjectWorkflowExecutionsQuery({
        environmentId: environmentId ?? DEVELOPMENT_ENVIRONMENT,
        id: workspaceId ?? 0,
    });
```
(c) Update `filteredWorkflowExecutions` (~line 253–270) to flatten `workflowExecutionsData?.pages.flatMap((page) => page.content)` exactly as in Step 5(c) (this file's narrow type also includes `job.startDate` — keep that field in the cast: `job?: {startDate?: string; status?: string};`).
(d) Remove the client cap: delete `workflowExecutionsShowCount` state + its reset + `visibleWorkflowExecutions`; replace `visibleWorkflowExecutions` with `filteredWorkflowExecutions` in both render blocks (search ~line 570, drilldown ~line 1040).
(e) Replace BOTH old `filteredWorkflowExecutions.length > workflowExecutionsShowCount && (<CommandItem … workflow-executions-show-more …>)` client-cap blocks with the server-driven show-more from Step 5(e).

- [ ] **Step 7: Typecheck + lint + tests**

Run: `npx eslint src/shared/queries/automation/workflowExecutions.queries.ts src/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx src/pages/automation/ai-hub/AiHubFilePicker.tsx`
Run: `npx tsc --noEmit 2>&1 | grep -E "workflowExecutions.queries|ResourcePickerMenu|AiHubFilePicker" || echo "no errors in these files"`
Run: `npx vitest run src/pages/automation/ai-hub/resource-picker src/pages/automation/ai-hub/composer/tests src/shared/queries/automation/tests/workflowExecutions.queries.test.ts`
Expected: clean / PASS. If a picker test mocked `useGetWorkspaceProjectWorkflowExecutionsQuery` and now needs `useInfiniteWorkspaceProjectWorkflowExecutionsQuery` mocked instead, update that mock to return `{data: {pages: [{content: [...]}]}, fetchNextPage: vi.fn(), hasNextPage: false, isFetchingNextPage: false}` — report any such test you adjust.

- [ ] **Step 8: Commit**

```bash
git add client/src/shared/queries/automation/workflowExecutions.queries.ts client/src/shared/queries/automation/tests/workflowExecutions.queries.test.ts client/src/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx client/src/pages/automation/ai-hub/AiHubFilePicker.tsx
git commit -m "hub - Page workflow executions from the server via Show more in AI Hub pickers"
```

---

### Task 5: Full verification + manual visual check

**Files:** none (verification only)

- [ ] **Step 1: Full client check**

Run: `npm run check`
Expected: lint + typecheck + all tests PASS. (Ignore pre-existing `functionSuggestion*` errors only if they originate from concurrent unrelated work and were failing before this plan; otherwise everything must be green.)

- [ ] **Step 2: Manual visual verification**

With the app running, open the resource-panel `+` ("Search resources…") and the composer `+`:
- Workflows: every project's workflows appear, grouped under project sub-headings; a workspace with >10 projects shows workflows beyond the 10th.
- Workflow executions: the list shows page 0; clicking "Show more…" loads the next page and appends; it stops once all pages are loaded; typing filters the loaded rows.
- Execution rows: the `project · STATUS` subtitle renders on a second line beneath the execution name (not to the right).

- [ ] **Step 3: Final commit (only if the visual check needed a tweak)**

```bash
git add -A
git commit -m "hub - Finalize AI Hub resource picker improvements"
```

---

## Self-review notes

- **Spec coverage:** workflows load-all + group-by-project (Tasks 2, 3) ✓; executions server-side paging via Show more (Task 4) ✓; executions subtitle below name (Task 1) ✓. Both pickers covered (`ResourcePickerMenu` + `AiHubFilePicker`). The spec's "client search over loaded rows" is honored (Task 4 search filters accumulated pages).
- **Type consistency:** `groupWorkflowsByProject<T extends {projectId, projectName}>` returns `{projectId, projectName, workflows}[]`, consumed identically in both pickers; `getNextWorkflowExecutionsPageParam(lastPage: Page): number | undefined` matches the `useInfiniteQuery` `getNextPageParam` contract; the infinite hook's destructured names (`fetchNextWorkflowExecutionsPage`, `hasMoreWorkflowExecutions`, `isFetchingMoreWorkflowExecutions`) are used consistently in both pickers.
- **No placeholders:** every code step has complete code; the only "read the current block first" notes are anchors for sites whose surrounding indentation varies, with the exact target markup supplied.
- **Known risk:** Task 4 changes a shared query module and two large files; if a picker test mocked the old single-page hook, Step 7 calls out updating that mock. The grouping refactor in `ResourcePickerMenu` (Task 3 Step 5) is behavior-preserving (helper returns the identical shape/order as the inlined logic it replaces).
