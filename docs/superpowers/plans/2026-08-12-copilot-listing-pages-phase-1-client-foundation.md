# Phase 1 — Client Foundation and the Five Agent-Backed Listing Pages — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Copilot opens from the Data Tables, Knowledge Bases, Context Stores, AI Skills, and Workflow Executions **listing** pages, scoped to each page's existing server agent, and the list refreshes when a BUILD turn changes something.

**Architecture:** The Copilot panel is already mounted once globally in `App.tsx` behind `ai.copilot.enabled`, so no page mounts anything: a page sets `{source, parameters, mode}` on `useCopilotStore`, flips `useCopilotPanelStore`, and registers a post-turn cache invalidation. All five sources (`DATA_TABLE`, `KNOWLEDGE_BASE`, `CONTEXT_STORE`, `SKILLS`, `WORKFLOW_EXECUTION`) already have server agents, so this phase is client-only. Two shared pieces come first because every page depends on them: the post-turn registry must hold **many** callbacks per source (a listing page and its detail page share one source — today the second registration silently evicts the first), and the orphaned `CopilotButton` becomes the one trigger component instead of five hand-rolled buttons.

**Tech Stack:** React 19, TypeScript 5.9, Zustand, TanStack Query v5, Radix UI, TailwindCSS, Vitest 4, Testing Library.

## Global Constraints

- Base branch: `0_732`. Worktree: `/Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732`, branch `claude/copilot-listing-pages-spec`. All commands run from `client/`.
- Spec: `docs/superpowers/specs/2026-08-12-copilot-automation-listing-pages-design.md`, sections "Client design" and decision 4.
- **Gate on `ai.copilot.enabled` only.** Do not add `ff-1570` — `App.tsx` gates the panel itself on `ai.copilot.enabled` alone, and the newer detail pages match that. Leave `ff-1570` untouched at its ~10 workflow-editor call sites.
- Object keys must be in ascending alphabetical order (ESLint `sort-keys`); `--fix` does **not** repair this — order them by hand.
- Named imports must be sorted alphabetically inside `{}` (`bytechef/sort-import-destructures`); `type` imports sort by name, not grouped.
- Interface names end in `I` or `Props`. `useRef` variables end in `Ref`.
- Import Lucide icons with the `Icon` suffix (`SparklesIcon`).
- Use `twMerge` for conditional classes — never a `cn()` helper.
- Hook order inside a component: `useState` → `useRef` → store hooks → other custom hooks → derived/`useMemo`/`useCallback` → `useEffect` → `return`. All `useEffect`s go last.
- Descriptive names everywhere, including arrow-function parameters (`(state) =>`, not `(s) =>`).
- Run `npm run check` (lint + typecheck + tests) before every commit; `npm run format` first.
- Commit message prefix: `--- client - ` (no ticket number on this work).

---

## File Structure

| File | Responsibility | Task |
|---|---|---|
| `client/src/shared/components/copilot/stores/useCopilotPostTurnRegistry.ts` | Many callbacks per `Source` | 1 |
| `client/src/shared/components/copilot/stores/useCopilotPostTurnRegistry.test.ts` | New — registry semantics | 1 |
| `client/src/shared/components/copilot/CopilotButton.tsx` | The one listing-page trigger | 2 |
| `client/src/shared/components/copilot/tests/CopilotButton.test.tsx` | New — gating + click behavior | 2 |
| `client/src/pages/automation/datatables/DataTables.tsx` | Button + post-turn | 3 |
| `client/src/pages/automation/knowledge-bases/KnowledgeBases.tsx` | Button + post-turn | 3 |
| `client/src/pages/automation/context-store/ContextStores.tsx` | Button + post-turn | 4 |
| `client/src/pages/automation/ai/skills/AiSkills.tsx` | Button on the list branch; registry call retrofitted to the hook-selector form | 5 |
| `client/src/pages/automation/workflow-executions/WorkflowExecutions.tsx` | Button + post-turn | 6 |
| `client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/hooks/useWorkflowExecutionSheet.ts` | Close the global panel when the sheet's own panel opens | 6 |

**No `Source` enum change in this phase** — all five values exist. New values arrive in phases 2–4.

## Decisions already made (do not re-litigate during implementation)

1. **`setContext` receives a fresh object**, not `{...currentContext, …}`. Switching surfaces must drop the previous surface's `parameters` and `workflowExecutionError`. This matches the three live detail pages; `CopilotButton` currently spreads and is corrected in Task 2.
2. **Listing pages pass `parameters: {}`** — these agents operate workspace-wide.
3. **Listing pages open in `MODE.ASK`.** `AiSkillDetail` opens in `MODE.BUILD`; that is out of scope and stays as it is.
4. **Context Stores: no `isAdmin` gate on the button.** The page gates *New Context Store* on `isAdmin`, but ASK mode is read-only and useful to everyone, and every mutation behind the agent is `@PreAuthorize`-guarded server-side. The UI gate would add nothing but a worse experience for viewers.
5. **AI Skills: leave the detail-branch button alone** (it is gated on `ff-4554` and opens `MODE.BUILD`). Only the list branch gains a button, gated on `ai.copilot.enabled` like the rest.

---

### Task 1: Post-turn registry holds many callbacks per source

Today `register` writes `{[source]: callback}`, so a listing page mounting after its detail page evicts the detail page's callback — and the evicted page's cleanup is a no-op because unregister bails on `state.callbacks[source] !== callback`. Mirror the array shape `useCopilotStateContributorRegistry` already uses.

**Files:**
- Modify: `client/src/shared/components/copilot/stores/useCopilotPostTurnRegistry.ts`
- Create: `client/src/shared/components/copilot/stores/useCopilotPostTurnRegistry.test.ts`

**Interfaces:**
- Consumes: nothing (first task).
- Produces: `register(source: Source, callback: () => void) => () => void` — signature unchanged, semantics now additive. `runFor(source: Source) => void` runs every callback registered for that source. Tasks 3–6 call `register`; no caller changes shape.

- [ ] **Step 1: Write the failing test**

Create `client/src/shared/components/copilot/stores/useCopilotPostTurnRegistry.test.ts`:

```ts
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useCopilotPostTurnRegistry from './useCopilotPostTurnRegistry';

describe('useCopilotPostTurnRegistry', () => {
    beforeEach(() => {
        useCopilotPostTurnRegistry.setState({callbacks: {}});
    });

    it('should run every callback registered for a source', () => {
        const listCallback = vi.fn();
        const detailCallback = vi.fn();

        const {register} = useCopilotPostTurnRegistry.getState();

        register(Source.DATA_TABLE, listCallback);
        register(Source.DATA_TABLE, detailCallback);

        useCopilotPostTurnRegistry.getState().runFor(Source.DATA_TABLE);

        expect(listCallback).toHaveBeenCalledTimes(1);
        expect(detailCallback).toHaveBeenCalledTimes(1);
    });

    it('should unregister only the callback that owns the returned cleanup', () => {
        const listCallback = vi.fn();
        const detailCallback = vi.fn();

        const {register} = useCopilotPostTurnRegistry.getState();

        const unregisterList = register(Source.DATA_TABLE, listCallback);

        register(Source.DATA_TABLE, detailCallback);

        unregisterList();

        useCopilotPostTurnRegistry.getState().runFor(Source.DATA_TABLE);

        expect(listCallback).not.toHaveBeenCalled();
        expect(detailCallback).toHaveBeenCalledTimes(1);
    });

    it('should not run callbacks registered for another source', () => {
        const dataTableCallback = vi.fn();

        useCopilotPostTurnRegistry.getState().register(Source.DATA_TABLE, dataTableCallback);

        useCopilotPostTurnRegistry.getState().runFor(Source.KNOWLEDGE_BASE);

        expect(dataTableCallback).not.toHaveBeenCalled();
    });

    it('should tolerate a source with no registrations', () => {
        expect(() => useCopilotPostTurnRegistry.getState().runFor(Source.SKILLS)).not.toThrow();
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732/client && npx vitest run src/shared/components/copilot/stores/useCopilotPostTurnRegistry.test.ts
```

Expected: FAIL — "should run every callback registered for a source" fails because `listCallback` is never called (the second `register` overwrote it).

- [ ] **Step 3: Make the registry additive**

Replace the whole body of `useCopilotPostTurnRegistry.ts`:

```ts
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {create} from 'zustand';

type PostTurnCallbackType = () => void;

interface PostTurnRegistryStateI {
    callbacks: Partial<Record<Source, PostTurnCallbackType[]>>;
    register: (source: Source, callback: PostTurnCallbackType) => () => void;
    runFor: (source: Source) => void;
}

// Many surfaces share one Source: a listing page and its detail page both refresh on DATA_TABLE turns, and each
// needs its own invalidation. Callbacks accumulate per source, and the cleanup returned by register removes only
// its own entry.
const useCopilotPostTurnRegistry = create<PostTurnRegistryStateI>((set, get) => ({
    callbacks: {},
    register: (source, callback) => {
        set((state) => ({callbacks: {...state.callbacks, [source]: [...(state.callbacks[source] ?? []), callback]}}));

        return () => {
            set((state) => {
                const sourceCallbacks = state.callbacks[source];

                if (!sourceCallbacks) {
                    return state;
                }

                const remainingCallbacks = sourceCallbacks.filter((entry) => entry !== callback);

                const nextCallbacks = {...state.callbacks};

                if (remainingCallbacks.length > 0) {
                    nextCallbacks[source] = remainingCallbacks;
                } else {
                    delete nextCallbacks[source];
                }

                return {callbacks: nextCallbacks};
            });
        };
    },
    runFor: (source) => {
        // Copy before iterating: a callback may unregister during the run.
        const sourceCallbacks = [...(get().callbacks[source] ?? [])];

        for (const callback of sourceCallbacks) {
            callback();
        }
    },
}));

export default useCopilotPostTurnRegistry;
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732/client && npx vitest run src/shared/components/copilot/stores/useCopilotPostTurnRegistry.test.ts
```

Expected: PASS, 4 tests.

- [ ] **Step 5: Verify no existing caller regressed**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732/client && npm run check
```

Expected: clean. `runFor` is called from exactly one place (`CopilotRuntimeProvider.tsx:177`) and `register`'s signature is unchanged, so the five existing callers need no edit.

- [ ] **Step 6: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732/client && npm run format
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732
git add client/src/shared/components/copilot/stores/useCopilotPostTurnRegistry.ts client/src/shared/components/copilot/stores/useCopilotPostTurnRegistry.test.ts
git commit -m "--- client - Let multiple surfaces register a copilot post-turn callback per source"
```

---

### Task 2: Make CopilotButton the shared listing-page trigger

`CopilotButton.tsx` exists, is gated on `ff-1570`, has no `aria-label`, spreads the previous context, and has zero importers. Four changes make it the component the five pages use: drop `ff-1570`, add `aria-label="Ask Copilot"` (what existing page tests query), set a fresh context, and early-return `null` when disabled instead of rendering `false` into a Radix `asChild` trigger slot.

**Files:**
- Modify: `client/src/shared/components/copilot/CopilotButton.tsx`
- Create: `client/src/shared/components/copilot/tests/CopilotButton.test.tsx`

**Interfaces:**
- Consumes: `useCopilotPostTurnRegistry` unchanged; `useCopilotStore.setContext`, `useCopilotPanelStore.setCopilotPanelOpen`.
- Produces: `<CopilotButton source={Source} parameters?={Record<string, any>} mode?={MODE} />`, default `parameters = {}`, default `mode = MODE.ASK`. Renders `null` when `ai.copilot.enabled` is false. Tasks 3–6 render it.

- [ ] **Step 1: Write the failing test**

Create `client/src/shared/components/copilot/tests/CopilotButton.test.tsx`:

```tsx
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import {MODE, Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    return {
        mockCopilotEnabled: {value: true},
        mockSetContext: vi.fn(),
        mockSetCopilotPanelOpen: vi.fn(),
    };
});

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {ai: {copilot: {enabled: boolean}}}) => unknown) =>
        selector({ai: {copilot: {enabled: hoisted.mockCopilotEnabled.value}}}),
}));

vi.mock('@/shared/components/copilot/stores/useCopilotStore', async () => {
    const actual = await vi.importActual<typeof import('@/shared/components/copilot/stores/useCopilotStore')>(
        '@/shared/components/copilot/stores/useCopilotStore'
    );

    return {
        ...actual,
        useCopilotStore: (selector: (state: {setContext: typeof hoisted.mockSetContext}) => unknown) =>
            selector({setContext: hoisted.mockSetContext}),
    };
});

vi.mock('@/shared/components/copilot/stores/useCopilotPanelStore', () => ({
    default: (selector: (state: {setCopilotPanelOpen: typeof hoisted.mockSetCopilotPanelOpen}) => unknown) =>
        selector({setCopilotPanelOpen: hoisted.mockSetCopilotPanelOpen}),
}));

describe('CopilotButton', () => {
    beforeEach(() => {
        hoisted.mockCopilotEnabled.value = true;
        hoisted.mockSetContext.mockClear();
        hoisted.mockSetCopilotPanelOpen.mockClear();
    });

    it('should render nothing when copilot is disabled', () => {
        hoisted.mockCopilotEnabled.value = false;

        render(<CopilotButton source={Source.DATA_TABLE} />);

        expect(screen.queryByLabelText('Ask Copilot')).not.toBeInTheDocument();
    });

    it('should set an ASK context for the source and open the panel', async () => {
        render(<CopilotButton source={Source.DATA_TABLE} />);

        await userEvent.click(screen.getByLabelText('Ask Copilot'));

        expect(hoisted.mockSetContext).toHaveBeenCalledWith({
            mode: MODE.ASK,
            parameters: {},
            source: Source.DATA_TABLE,
        });
        expect(hoisted.mockSetCopilotPanelOpen).toHaveBeenCalledWith(true);
    });

    it('should forward parameters and an explicit mode', async () => {
        render(<CopilotButton mode={MODE.BUILD} parameters={{dataTableId: '7'}} source={Source.DATA_TABLE} />);

        await userEvent.click(screen.getByLabelText('Ask Copilot'));

        expect(hoisted.mockSetContext).toHaveBeenCalledWith({
            mode: MODE.BUILD,
            parameters: {dataTableId: '7'},
            source: Source.DATA_TABLE,
        });
    });
});
```

**The `TooltipProvider` wrapper is required, not optional.** `client/src/components/ui/tooltip.tsx` exports `Tooltip` as a bare `TooltipPrimitive.Root` with no built-in provider, and `test-utils.render` wraps children only in `ThemeProvider` + `QueryClientProvider`. The app supplies the provider globally, so tests must supply it themselves. Add the import:

```tsx
import {TooltipProvider} from '@/components/ui/tooltip';
```

and wrap every render in this file, for example:

```tsx
        render(
            <TooltipProvider>
                <CopilotButton source={Source.DATA_TABLE} />
            </TooltipProvider>
        );
```

`render`, `screen`, and `userEvent` all come from `@/shared/util/test-utils` (`DataTable.test.tsx` imports exactly these from there).

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732/client && npx vitest run src/shared/components/copilot/tests/CopilotButton.test.tsx
```

Expected: FAIL — no element with label "Ask Copilot" (the button has no `aria-label` and is additionally gated behind `ff-1570`, which the test does not mock).

- [ ] **Step 3: Rewrite the component**

Replace the whole of `CopilotButton.tsx`:

```tsx
import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {SparklesIcon} from 'lucide-react';

export interface CopilotButtonProps {
    mode?: MODE;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters?: Record<string, any>;
    source: Source;
}

const CopilotButton = ({mode = MODE.ASK, parameters = {}, source}: CopilotButtonProps) => {
    const copilotEnabled = useApplicationInfoStore((state) => state.ai.copilot.enabled);
    const setContext = useCopilotStore((state) => state.setContext);
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);

    const handleClick = () => {
        // A fresh context, not a spread of the previous one: switching surfaces must drop the prior surface's
        // parameters and workflowExecutionError rather than leaking them into this agent's state.
        setContext({
            mode,
            parameters,
            source,
        });

        setCopilotPanelOpen(true);
    };

    if (!copilotEnabled) {
        return null;
    }

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    aria-label="Ask Copilot"
                    className="[&_svg]:size-5"
                    icon={<SparklesIcon />}
                    onClick={handleClick}
                    size="icon"
                    variant="ghost"
                />
            </TooltipTrigger>

            <TooltipContent>Open Copilot panel</TooltipContent>
        </Tooltip>
    );
};

export default CopilotButton;
```

The early return sits after the hooks (React requires unconditional hook calls) and before the `return`, so `TooltipTrigger asChild` always receives exactly one element child.

- [ ] **Step 4: Run the test to verify it passes**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732/client && npx vitest run src/shared/components/copilot/tests/CopilotButton.test.tsx
```

Expected: PASS, 3 tests.

- [ ] **Step 5: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732/client && npm run format && npm run check
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732
git add client/src/shared/components/copilot/CopilotButton.tsx client/src/shared/components/copilot/tests/CopilotButton.test.tsx
git commit -m "--- client - Make CopilotButton the shared trigger gated on ai.copilot.enabled"
```

---

### Task 3: Data Tables and Knowledge Bases listing pages

Structurally identical pages: a header `right` slot whose ternary collapses to a bare `EnvironmentSelect` when the list is empty, and a tag-filtered sidebar that goes stale if Copilot creates a tagged entity. The button must appear in the empty state too — that is exactly when a user wants Copilot to create the first table.

**Files:**
- Modify: `client/src/pages/automation/datatables/DataTables.tsx`
- Modify: `client/src/pages/automation/knowledge-bases/KnowledgeBases.tsx`

**Interfaces:**
- Consumes: `CopilotButton` (Task 2), `useCopilotPostTurnRegistry.register` (Task 1).
- Produces: nothing consumed later.

- [ ] **Step 1: Wire DataTables.tsx**

Add these imports (keep the file's existing alphabetical import order):

```tsx
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect} from 'react';
```

Inside the component, after the existing `useDataTables()` / `useDataTableStorageUsageQuery()` calls and before the `return`:

```tsx
    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const queryClient = useQueryClient();

    // Refresh the list and the tag sidebar after a BUILD-mode copilot turn creates or retags a table, so the page
    // reflects the change without a manual reload.
    useEffect(() => {
        return registerPostTurn(Source.DATA_TABLE, () => {
            queryClient.invalidateQueries({queryKey: ['dataTables']});
            queryClient.invalidateQueries({queryKey: ['dataTableTags']});
            queryClient.invalidateQueries({queryKey: ['dataTableTagsByTable']});
        });
    }, [queryClient, registerPostTurn]);
```

Replace the header's `right` prop so the button renders in both states (loading-with-no-tables still renders nothing):

```tsx
                    right={
                        (tables.length > 0 || !isLoading) && (
                            <div className="flex items-center gap-4">
                                <EnvironmentSelect />

                                <CopilotButton source={Source.DATA_TABLE} />

                                {tables.length > 0 && (
                                    <CreateDataTableDialog trigger={<Button>New Table</Button>} />
                                )}
                            </div>
                        )
                    }
```

- [ ] **Step 2: Wire KnowledgeBases.tsx**

Same imports as Step 1. Add the same hook block, with the knowledge-base keys:

```tsx
    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const queryClient = useQueryClient();

    // Refresh the list and the tag sidebar after a BUILD-mode copilot turn creates or retags a knowledge base.
    useEffect(() => {
        return registerPostTurn(Source.KNOWLEDGE_BASE, () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBases']});
            queryClient.invalidateQueries({queryKey: ['knowledgeBaseTags']});
            queryClient.invalidateQueries({queryKey: ['knowledgeBaseTagsByKnowledgeBase']});
        });
    }, [queryClient, registerPostTurn]);
```

Replace the header's `right` prop:

```tsx
                    right={
                        (knowledgeBases.length > 0 || !isLoading) && (
                            <div className="flex items-center gap-4">
                                <EnvironmentSelect />

                                <CopilotButton source={Source.KNOWLEDGE_BASE} />

                                {knowledgeBases.length > 0 && (
                                    <CreateKnowledgeBaseDialog
                                        trigger={<Button>New Knowledge Base</Button>}
                                        workspaceId={currentWorkspaceId}
                                    />
                                )}
                            </div>
                        )
                    }
```

- [ ] **Step 3: Verify both pages compile and the suite is green**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732/client && npm run check
```

Expected: clean lint, typecheck, and tests. If `sort-keys` or `sort-import-destructures` complain, fix the ordering by hand — `--fix` does not repair `sort-keys`.

- [ ] **Step 4: Verify in the running app**

Start the dev server, open `/automation/datatables`, confirm the sparkles button sits between the environment select and *New Table*, that clicking it opens the panel, and that the button is still present when the list is empty. Repeat for `/automation/knowledge-bases`. Check the spacing reads correctly at `gap-4`; tighten to `gap-2` only if it looks wrong next to the select.

- [ ] **Step 5: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732/client && npm run format
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732
git add client/src/pages/automation/datatables/DataTables.tsx client/src/pages/automation/knowledge-bases/KnowledgeBases.tsx
git commit -m "--- client - Add Copilot to the data tables and knowledge bases listing pages"
```

---

### Task 4: Context Stores listing page

Same shape as Task 3. Per decision 4 above, the button is **not** `isAdmin`-gated even though *New Context Store* is.

**Files:**
- Modify: `client/src/pages/automation/context-store/ContextStores.tsx`

**Interfaces:**
- Consumes: `CopilotButton` (Task 2), `useCopilotPostTurnRegistry.register` (Task 1).

- [ ] **Step 1: Add the imports**

```tsx
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {useQueryClient} from '@tanstack/react-query';
```

`useMemo` is already imported from `react`; extend that import to `{useEffect, useMemo}`.

- [ ] **Step 2: Register the post-turn refresh**

Place the store hook and `queryClient` with the other hooks near the top of the component, and the `useEffect` after the last `useMemo` (all `useEffect`s go last, immediately before the `return`):

```tsx
    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const queryClient = useQueryClient();
```

```tsx
    // Refresh the store list and its sources after a BUILD-mode copilot turn creates or updates either, so the page
    // reflects the change without a manual reload.
    useEffect(() => {
        return registerPostTurn(Source.CONTEXT_STORE, () => {
            queryClient.invalidateQueries({queryKey: ['contextStores']});
            queryClient.invalidateQueries({queryKey: ['contextStoreSources']});
        });
    }, [queryClient, registerPostTurn]);
```

This is the first registration that coexists with `ContextStoreSources.tsx`'s registration on the same source — it works only because of Task 1.

- [ ] **Step 3: Add the button to the header**

```tsx
                    right={
                        (contextStores.length > 0 || !isLoading) && (
                            <div className="flex items-center gap-4">
                                <EnvironmentSelect />

                                <CopilotButton source={Source.CONTEXT_STORE} />

                                {contextStores.length > 0 && isAdmin && (
                                    <ContextStoreFormDialog trigger={<Button>New Context Store</Button>} />
                                )}
                            </div>
                        )
                    }
```

- [ ] **Step 4: Verify**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732/client && npm run check
```

Expected: clean. Then open `/automation/context-stores` (EE build) and confirm the button appears and opens the panel. Drill into a store and confirm the detail page's own Copilot still works — this is the live proof that two registrations on `CONTEXT_STORE` now coexist.

- [ ] **Step 5: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732/client && npm run format
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732
git add client/src/pages/automation/context-store/ContextStores.tsx
git commit -m "--- client - Add Copilot to the context stores listing page"
```

---

### Task 5: AI Skills list branch

`AiSkills.tsx` serves both the list and detail routes from one component. It already registers a `SKILLS` post-turn callback covering every skill query, so **no new registration is needed** — only a button on the list branch. This task also retrofits the existing registration from `useCopilotPostTurnRegistry.getState().register(...)` to the hook-selector form, because the page-test convention mocks the registry module as a selector-only function with no `.getState()`.

**Files:**
- Modify: `client/src/pages/automation/ai/skills/AiSkills.tsx`

**Interfaces:**
- Consumes: `CopilotButton` (Task 2).

- [ ] **Step 1: Add the import**

```tsx
import CopilotButton from '@/shared/components/copilot/CopilotButton';
```

- [ ] **Step 2: Retrofit the registry call to the hook-selector form**

Add the selector with the other store hooks near the top of the component:

```tsx
    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);
```

and change the existing effect (currently `useCopilotPostTurnRegistry.getState().register(...)`) to:

```tsx
    useEffect(() => {
        return registerPostTurn(Source.SKILLS, () => {
            invalidateSkillQueries(queryClient);
        });
    }, [queryClient, registerPostTurn, skillsView]);
```

Leave `skillsView` in the dependency array — it is pre-existing behavior, and re-registering is now harmless rather than evicting.

- [ ] **Step 3: Add the button to the list toolbar**

In the `if (showToolbar)` branch, between the search input and the create dropdown:

```tsx
        toolbarRight = (
            <div className="flex items-center gap-2">
                <div className="relative">
                    <SearchIcon className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-content-neutral-tertiary" />

                    <Input
                        className="w-64 pl-9"
                        onChange={(event) => setSearchQuery(event.target.value)}
                        placeholder="Search skills..."
                        value={searchQuery}
                    />
                </div>

                <CopilotButton source={Source.SKILLS} />

                <AiSkillsCreateDropdown />
            </div>
        );
```

No external `copilotEnabled &&` guard — `CopilotButton` gates itself. Do not touch the `route === 'detail'` branch.

- [ ] **Step 4: Verify**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732/client && npm run check
```

Expected: clean. Then open `/automation/ai/skills`, confirm the button appears next to the search box on the list view, opens the panel in ASK mode, and that the detail view's own button (behind `ff-4554`) is unchanged.

- [ ] **Step 5: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732/client && npm run format
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732
git add client/src/pages/automation/ai/skills/AiSkills.tsx
git commit -m "--- client - Add Copilot to the AI skills list view"
```

---

### Task 6: Workflow Executions listing page and the sheet conflict

The executions page renders `<WorkflowExecutionSheet />`, whose `useWorkflowExecutionSheet` hook drives a **local** copilot panel via `useState` while writing to the same shared `useCopilotStore`, and saves/restores conversation state around it. Adding a page-level button that opens the **global** panel means both can be open at once, rendering one conversation twice — and whichever closes first calls `restoreConversationState()` and clobbers the other. Closing the global panel when the sheet's panel opens resolves it in one line, preserving the sheet's scoped-sub-conversation behavior.

**Files:**
- Modify: `client/src/pages/automation/workflow-executions/WorkflowExecutions.tsx`
- Modify: `client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/hooks/useWorkflowExecutionSheet.ts`

**Interfaces:**
- Consumes: `CopilotButton` (Task 2), `useCopilotPostTurnRegistry.register` (Task 1), `WorkflowExecutionKeys` from `@/shared/queries/automation/workflowExecutions.queries`.

- [ ] **Step 1: Add the button and post-turn refresh to the page**

Add these imports to `WorkflowExecutions.tsx`:

```tsx
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {WorkflowExecutionKeys} from '@/shared/queries/automation/workflowExecutions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect} from 'react';
```

After the `useWorkflowExecutions()` destructure:

```tsx
    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const queryClient = useQueryClient();

    // Refresh the executions list after a BUILD-mode copilot turn fixes and re-runs a workflow. The key factory is
    // imported rather than hardcoded because this list uses a typed key, not a raw string.
    useEffect(() => {
        return registerPostTurn(Source.WORKFLOW_EXECUTION, () => {
            queryClient.invalidateQueries({queryKey: WorkflowExecutionKeys.workflowExecutions});
        });
    }, [queryClient, registerPostTurn]);
```

Add the button to the existing `gap-1` header row, after `EnvironmentSelect`:

```tsx
                            <EnvironmentSelect />

                            <CopilotButton source={Source.WORKFLOW_EXECUTION} />
```

- [ ] **Step 2: Close the global panel when the sheet's panel opens**

In `useWorkflowExecutionSheet.ts`, import the panel store:

```ts
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
```

and add one line inside `handleCopilotClick`, immediately before `setCopilotPanelOpen(true)` (which sets the hook's **local** state):

```ts
        // The sheet runs its own panel over the shared copilot store. Close the global panel first so one
        // conversation is never rendered by two panels, whose independent close handlers would each restore
        // saved state over the other.
        useCopilotPanelStore.getState().setCopilotPanelOpen(false);

        setCopilotPanelOpen(true);
```

Use `.getState()` here rather than a hook selector: this is an event handler inside a `useCallback`, and adding a store subscription would re-create the callback on every panel toggle.

- [ ] **Step 3: Verify**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732/client && npm run check
```

Expected: clean.

- [ ] **Step 4: Verify the conflict is gone in the running app**

On `/automation/executions`: open the page-level Copilot panel, then click an execution row and click the sheet's Copilot button. Exactly one panel must be visible (the sheet's). Close the sheet and confirm the conversation restores without errors. Then confirm the page-level button still opens the global panel when no sheet is open.

- [ ] **Step 5: Run the full client suite and commit**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732/client && npm run format && npm run check
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732
git add client/src/pages/automation/workflow-executions
git commit -m "--- client - Add Copilot to the workflow executions listing page"
```

---

## Phase verification

After all six tasks:

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732/client && npm run check
```

Then walk all five pages with `ai.copilot.enabled=true`: the sparkles button appears on each, opens the panel scoped to that page's agent, and a BUILD turn that creates or changes an entity refreshes the list without a reload. Finally set `ai.copilot.enabled=false` and confirm all five buttons disappear along with the panel.
