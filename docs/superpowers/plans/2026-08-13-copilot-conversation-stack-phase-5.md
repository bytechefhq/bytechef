# Copilot Conversation Stack — Phase 5 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Copilot panel's single-slot conversation save/restore with a LIFO stack, so a
surface that opens Copilot over another surface's conversation no longer destroys it — and bring the
global panel, which today participates in neither save nor restore, into the same discipline.

**Architecture:** `useCopilotStore.savedState` (one nullable snapshot) becomes
`conversationStack` (an array of the same snapshot type). `saveConversationState` pushes,
`restoreConversationState` pops. **Both keep their current names and signatures**, so all 7 save
call sites, all 13 restore call sites, and the three existing hook tests — including the one pinning
the `save → reset → generate` ordering contract — continue to work untouched. The global panel path
(`useOpenCopilot` to open, `closeGlobalPanel` to close) is then wired into the same push/pop pair.

**Tech Stack:** React 19, TypeScript 5.9, Zustand (with `devtools`, no `persist`), Vitest,
`@assistant-ui/react`.

## Global Constraints

- Worktree `/Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-phase5`, branch
  `claude/copilot-phase5-conversation-stack`, based on `claude/copilot-phase4-asset-slice`. Never
  rebase onto `0_732` — it is rewritten frequently.
- **Never invent a ticket number.** No commit in this phase carries one. Client commits use the form
  `client - <description>`.
- Client style: object keys sorted alphabetically (`sort-keys` is NOT auto-fixable — fix by hand);
  named imports sorted alphabetically inside `{}` with `type` imports sorting by name; Lucide icons
  imported with the `Icon` suffix; `useRef` variables end in `Ref`; interfaces end in `I` or `Props`;
  `twMerge` rather than a `cn()` helper.
- Hook order: `useState` → `useRef` → store hooks → other custom hooks → derived values /
  `useMemo` / `useCallback` → `useEffect` → `return`, with all effects last.
- Verify with `npm run check` from `client/` (prettier, eslint `--max-warnings=0`, tsc, vitest).
  `node_modules` may not exist in a fresh worktree — run `npm install` first if `npm run check`
  fails with `command not found`.
- Before committing, run `git status --porcelain` and confirm only your files are staged.
  `git commit` commits the whole index, not the paths passed to `git add`.

## Background — read this before Task 1

The full research is committed at
`docs/superpowers/specs/2026-08-12-copilot-conversation-stack-research.md`. The load-bearing facts:

- **Eight surfaces, not five.** Seven mount their own `<CopilotPanel>` with a *local* open flag and
  call `saveConversationState()` exactly once each. The eighth is the global panel in `App.tsx`,
  opened by `CopilotButton` → `useOpenCopilot`, which calls **neither** save nor restore.
- **7 save call sites, 13 restore call sites.** Several surfaces restore from more than one exit
  (close button, `onOpenChange`, unsaved-changes confirm), and those fire whenever the *host* dialog
  closes, whether or not Copilot was opened inside it. That is safe today only because
  `restoreConversationState` no-ops on an empty slot. **The stack must preserve that property** or
  all 13 sites need guards.
- **The bug is already worked around in the codebase.** `useWorkflowExecutionSheet.ts` (the
  automation one) force-closes the global panel before opening its own, with a comment saying two
  panels sharing one saved slot "would each restore saved state over the other." Removing that
  workaround is Task 3, and it is the phase's acceptance test.
- **No persistence.** The store uses `devtools` only. The stack is in-memory for the tab's lifetime;
  that is not a regression, because `conversationId` is already re-randomised on every page load.
- **The panel unmounts 300ms after close** (`ANIMATION_DURATION_MS`), so the store update and the
  unmount are not simultaneous. Do not assume synchronous teardown.

## Explicitly out of scope

**The `sourceProp ?? context.source` vector.** `CopilotRuntimeProvider` resolves its agent as
`sourceProp ?? context?.source ?? Source.WORKFLOW_EDITOR`, and only `WorkflowCodeEditorSheet` passes
the prop — so every other mounted provider reads the shared store. That is a *different* problem
from the saved-state slot (it concerns concurrently mounted providers, not conversation lifecycle),
and the stack neither fixes nor worsens it. Risk is low in practice because `CopilotPanelContent`
only mounts while `shouldRender` is true, so a closed panel has no live provider. Leave it; it is
recorded in the research note.

## File Structure

**Modify:**

| Path | Change |
|---|---|
| `client/src/shared/components/copilot/stores/useCopilotStore.ts` | `savedState` → `conversationStack`; push/pop semantics; depth cap |
| `client/src/shared/components/copilot/hooks/useOpenCopilot.ts` | Push before installing the new context; reset messages and mint a new conversation id |
| `client/src/shared/components/copilot/CopilotPanelImpl.tsx` | `closeGlobalPanel` pops instead of hard-resetting |
| `client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/hooks/useWorkflowExecutionSheet.ts` | Remove the force-close workaround |
| `docs/superpowers/specs/2026-08-12-copilot-conversation-stack-research.md` | Record what shipped |

**Create:**

| Path | Responsibility |
|---|---|
| `client/src/shared/components/copilot/stores/tests/useCopilotConversationStack.test.ts` | LIFO ordering, empty-pop no-op, depth cap, snapshot completeness |
| `client/src/shared/components/copilot/hooks/tests/useOpenCopilotStack.test.ts` | The global path pushes on open and the ordering contract holds |

---

### Task 1: Turn the saved slot into a stack

**Files:**
- Modify: `client/src/shared/components/copilot/stores/useCopilotStore.ts`
- Test: `client/src/shared/components/copilot/stores/tests/useCopilotConversationStack.test.ts`

**Interfaces:**
- Consumes: nothing new.
- Produces: `conversationStack: ConversationSnapshotI[]` on the store, with
  `saveConversationState(): void` (push) and `restoreConversationState(): void` (pop, no-op on
  empty). **Both names and signatures are unchanged** — Tasks 2 and 3 and all 20 existing call sites
  depend on that.

**Design decisions, already made — do not re-litigate:**

- **Keep the method names.** Renaming to `push`/`pop` would touch 7 save sites, 13 restore sites and
  three test files, including `usePropertyCodeEditorDialogToolbar.test.ts`, which pins
  `expect(callOrder).toEqual(['save', 'reset', 'generate'])`. The names remain accurate: saving
  pushes, restoring pops. The stack semantics belong in the store's documentation, not in churn.
- **Pop on empty is a no-op returning the state unchanged.** Thirteen restore call sites fire on
  generic dialog close, whether or not Copilot was ever opened. This property is what makes them
  safe, and it must survive.
- **Depth cap of 10, dropping the *deepest* entry.** An unpaired save (a surface unmounted without
  its cleanup running) leaks one entry. A cap prevents unbounded growth in a long session; dropping
  the deepest discards the least likely to be returned to. Emit a `console.warn` when it trips so
  the leak is visible rather than silent.

- [ ] **Step 1: Write the failing tests**

Create the test file. Follow the existing `useCopilotStore.test.ts` conventions in the same tree
(reset store state in `beforeEach` via `useCopilotStore.setState({...})`, access imperatively via
`useCopilotStore.getState()`).

```ts
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const baseContext = {mode: MODE.ASK, parameters: {}, source: Source.WORKFLOW_EDITOR};

describe('copilot conversation stack', () => {
    beforeEach(() => {
        useCopilotStore.setState({
            composerPlaceholder: undefined,
            context: baseContext,
            conversationId: 'conversation-0',
            conversationStack: [],
            messages: [],
            selectedLlmModel: null,
            selectedLlmProvider: null,
        });
    });

    it('restores the most recently saved conversation first', () => {
        const {saveConversationState} = useCopilotStore.getState();

        useCopilotStore.setState({conversationId: 'first', messages: [{content: 'a', role: 'user'}]});
        saveConversationState();

        useCopilotStore.setState({conversationId: 'second', messages: [{content: 'b', role: 'user'}]});
        saveConversationState();

        useCopilotStore.setState({conversationId: 'third', messages: []});

        useCopilotStore.getState().restoreConversationState();

        expect(useCopilotStore.getState().conversationId).toBe('second');

        useCopilotStore.getState().restoreConversationState();

        expect(useCopilotStore.getState().conversationId).toBe('first');
    });

    it('leaves state untouched when restoring with an empty stack', () => {
        useCopilotStore.setState({conversationId: 'current', messages: [{content: 'keep', role: 'user'}]});

        useCopilotStore.getState().restoreConversationState();

        expect(useCopilotStore.getState().conversationId).toBe('current');
        expect(useCopilotStore.getState().messages).toHaveLength(1);
    });

    it('restores every field of the snapshot', () => {
        useCopilotStore.setState({
            composerPlaceholder: 'describe the workflow',
            context: {...baseContext, source: Source.DATA_TABLE},
            conversationId: 'rich',
            messages: [{content: 'x', role: 'user'}],
            selectedLlmModel: 'model-a',
            selectedLlmProvider: 'provider-a',
        });

        useCopilotStore.getState().saveConversationState();

        useCopilotStore.setState({
            composerPlaceholder: undefined,
            context: baseContext,
            conversationId: 'other',
            messages: [],
            selectedLlmModel: null,
            selectedLlmProvider: null,
        });

        useCopilotStore.getState().restoreConversationState();

        const restored = useCopilotStore.getState();

        expect(restored.composerPlaceholder).toBe('describe the workflow');
        expect(restored.context.source).toBe(Source.DATA_TABLE);
        expect(restored.conversationId).toBe('rich');
        expect(restored.messages).toHaveLength(1);
        expect(restored.selectedLlmModel).toBe('model-a');
        expect(restored.selectedLlmProvider).toBe('provider-a');
    });

    it('caps the stack depth and drops the deepest entry', () => {
        const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

        for (let index = 0; index < 12; index += 1) {
            useCopilotStore.setState({conversationId: `conversation-${index}`});
            useCopilotStore.getState().saveConversationState();
        }

        expect(useCopilotStore.getState().conversationStack).toHaveLength(10);
        expect(useCopilotStore.getState().conversationStack[0]?.conversationId).toBe('conversation-2');
        expect(warnSpy).toHaveBeenCalled();

        warnSpy.mockRestore();
    });

    it('does not carry the stack into a saved snapshot', () => {
        useCopilotStore.getState().saveConversationState();
        useCopilotStore.getState().saveConversationState();

        const [firstEntry] = useCopilotStore.getState().conversationStack;

        expect(firstEntry).not.toHaveProperty('conversationStack');
    });
});
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
cd client && npx vitest run src/shared/components/copilot/stores/tests/useCopilotConversationStack.test.ts
```

Expected: failures — `conversationStack` does not exist and `saveConversationState` overwrites a
single slot rather than pushing.

- [ ] **Step 3: Implement the stack**

In `useCopilotStore.ts`, replace the `savedState` field and the two action implementations. Keep the
snapshot shape exactly as it is today (`composerPlaceholder`, `conversationId`, `context`,
`messages`, `selectedLlmModel`, `selectedLlmProvider`) — Task 2 depends on it being complete.

Name the snapshot type and export it, so the tests and any future consumer share one definition:

```ts
export interface ConversationSnapshotI {
    composerPlaceholder: string | undefined;
    context: ContextType;
    conversationId: string | undefined;
    messages: ThreadMessageLike[];
    selectedLlmModel: string | null;
    selectedLlmProvider: string | null;
}
```

In the state interface, replace the `savedState` field with:

```ts
    // A LIFO stack rather than one slot: a surface that opens Copilot over another surface's conversation
    // pushes, and its close pops, so the underlying conversation survives. Restoring with an empty stack is
    // deliberately a no-op — several surfaces call restore whenever their host dialog closes, whether or not
    // Copilot was ever opened inside it, and that is the property that makes those calls safe.
    conversationStack: ConversationSnapshotI[];
    saveConversationState: () => void;
    restoreConversationState: () => void;
```

Implement push with the depth cap:

```ts
        conversationStack: [],
        saveConversationState: () =>
            set((state) => {
                const snapshot: ConversationSnapshotI = {
                    composerPlaceholder: state.composerPlaceholder,
                    context: state.context,
                    conversationId: state.conversationId,
                    messages: state.messages,
                    selectedLlmModel: state.selectedLlmModel,
                    selectedLlmProvider: state.selectedLlmProvider,
                };

                const nextStack = [...state.conversationStack, snapshot];

                // A surface that unmounts without its restore running leaks one entry. Dropping the deepest
                // keeps a long session bounded, and warning makes the leak visible rather than silent.
                if (nextStack.length > MAX_CONVERSATION_STACK_DEPTH) {
                    console.warn(
                        `Copilot conversation stack exceeded ${MAX_CONVERSATION_STACK_DEPTH} entries; dropping the oldest.`
                    );

                    nextStack.shift();
                }

                return {...state, conversationStack: nextStack};
            }),
        restoreConversationState: () =>
            set((state) => {
                if (state.conversationStack.length === 0) {
                    return state;
                }

                const nextStack = [...state.conversationStack];
                const snapshot = nextStack.pop() as ConversationSnapshotI;

                return {
                    ...state,
                    composerPlaceholder: snapshot.composerPlaceholder,
                    context: snapshot.context,
                    conversationId: snapshot.conversationId,
                    conversationStack: nextStack,
                    messages: snapshot.messages,
                    selectedLlmModel: snapshot.selectedLlmModel,
                    selectedLlmProvider: snapshot.selectedLlmProvider,
                };
            }),
```

Declare `const MAX_CONVERSATION_STACK_DEPTH = 10;` near the top of the module, beside the other
module-level constants.

Note the file carries `/* eslint-disable sort-keys */` near the top, so the store's own object
literals are exempt — but keep them readable and grouped as the existing code does.

- [ ] **Step 4: Run the tests and the full client check**

```bash
cd client && npm run check
```

Expected: exit 0. The three existing hook tests that spy on `saveConversationState` /
`restoreConversationState` must still pass unchanged — if any fails, you changed a signature you
were not supposed to change.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/copilot/stores
git commit -m "client - Replace the Copilot saved conversation slot with a stack"
```

---

### Task 2: Bring the global panel into the stack

**Files:**
- Modify: `client/src/shared/components/copilot/hooks/useOpenCopilot.ts`
- Modify: `client/src/shared/components/copilot/CopilotPanelImpl.tsx`
- Test: `client/src/shared/components/copilot/hooks/tests/useOpenCopilotStack.test.ts`

**Interfaces:**
- Consumes: `saveConversationState` / `restoreConversationState` from Task 1.
- Produces: no new exports; behaviour only.

**The defect being fixed.** `useOpenCopilot` — the path every listing-page `CopilotButton` uses —
calls neither save nor restore. It also calls neither `resetMessages` nor `generateConversationId`,
so opening the global panel installs a *fresh context* over the *previous conversation's messages*
and its conversation id. The agent id and the `/ai/chat/{source}` URL both derive from the new
context, so the previous surface's messages are replayed against a different agent.

The seven local surfaces already do this correctly, in a fixed order:
`saveConversationState()` → `resetMessages()` → `generateConversationId()` → `setContext(...)`.
`usePropertyCodeEditorDialogToolbar.test.ts` pins that ordering. Match it exactly.

- [ ] **Step 1: Write the failing test**

```ts
import useOpenCopilot from '@/shared/components/copilot/hooks/useOpenCopilot';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {renderHook} from '@testing-library/react';
import {act} from 'react';
import {beforeEach, describe, expect, it} from 'vitest';

describe('useOpenCopilot conversation stack', () => {
    beforeEach(() => {
        useCopilotStore.setState({
            composerPlaceholder: undefined,
            context: {mode: MODE.ASK, parameters: {}, source: Source.WORKFLOW_EDITOR},
            conversationId: 'existing',
            conversationStack: [],
            messages: [{content: 'earlier turn', role: 'user'}],
        });
    });

    it('pushes the current conversation before installing the new context', () => {
        const {result} = renderHook(() => useOpenCopilot());

        act(() => {
            result.current({source: Source.PROJECT});
        });

        const state = useCopilotStore.getState();

        expect(state.conversationStack).toHaveLength(1);
        expect(state.conversationStack[0]?.conversationId).toBe('existing');
        expect(state.conversationStack[0]?.messages).toHaveLength(1);
    });

    it('starts the new surface with no messages and a fresh conversation id', () => {
        const {result} = renderHook(() => useOpenCopilot());

        act(() => {
            result.current({source: Source.PROJECT});
        });

        const state = useCopilotStore.getState();

        expect(state.messages).toHaveLength(0);
        expect(state.conversationId).not.toBe('existing');
        expect(state.context.source).toBe(Source.PROJECT);
    });
});
```

- [ ] **Step 2: Run it to verify it fails**

```bash
cd client && npx vitest run src/shared/components/copilot/hooks/tests/useOpenCopilotStack.test.ts
```

Expected: both fail — nothing is pushed, and the previous messages survive.

- [ ] **Step 3: Push on open**

In `useOpenCopilot.ts`, read the extra actions from the store and call them in the established
order before `setContext`. Read them via `useCopilotStore.getState()` inside the returned function
rather than as hook selectors, so the hook's identity does not change on every store update:

```ts
    return ({composerPlaceholder, mode = MODE.ASK, parameters = {}, source}: UseOpenCopilotOptionsI) => {
        const {generateConversationId, resetMessages, saveConversationState} = useCopilotStore.getState();

        // Push, then clear: the surface being opened starts a genuinely new conversation, and the one it
        // covers stays recoverable on the stack. Without the push the previous surface's messages would be
        // replayed against this surface's agent, since both the agent id and the /ai/chat/{source} URL derive
        // from the context installed just below. Order matches the seven local-panel surfaces.
        saveConversationState();
        resetMessages();
        generateConversationId();

        setContext({
            mode,
            parameters,
            source,
        });
```

Leave the rest of the function unchanged.

- [ ] **Step 4: Pop on close**

In `CopilotPanelImpl.tsx`, `closeGlobalPanel` currently hard-resets the context to
`Source.WORKFLOW_EDITOR`. Make it pop instead, falling back to the reset only when the stack is
empty:

```ts
    // Shared by the close button and the navigation effect below. Local-panel surfaces pass an onClose and
    // drive their own open flag, so this only ever applies to the global panel mounted in App.tsx.
    const closeGlobalPanel = useCallback(() => {
        const {conversationStack, restoreConversationState} = useCopilotStore.getState();

        if (conversationStack.length > 0) {
            // Something was open underneath — hand it back rather than discarding it.
            restoreConversationState();
        } else {
            setContext({
                mode: MODE.ASK,
                parameters: {},
                source: Source.WORKFLOW_EDITOR,
            });
            setComposerPlaceholder(undefined);
        }

        setCopilotPanelOpen(false);
    }, [setComposerPlaceholder, setContext, setCopilotPanelOpen]);
```

`closeGlobalPanel` is already used by both the close button and the navigation effect added in the
previous phase, so both paths pick this up with no further change. Confirm that is still true by
reading the file rather than trusting this plan.

- [ ] **Step 5: Verify**

```bash
cd client && npm run check
```

Expected: exit 0, including the two new tests and every pre-existing copilot test.

- [ ] **Step 6: Commit**

```bash
git add client/src/shared/components/copilot
git commit -m "client - Push and pop the conversation stack from the global Copilot panel"
```

---

### Task 3: Remove the single-slot workaround

**Files:**
- Modify: `client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/hooks/useWorkflowExecutionSheet.ts`
- Modify: `docs/superpowers/specs/2026-08-12-copilot-conversation-stack-research.md`

**Interfaces:** none.

**Why this is the acceptance test.** That hook force-closes the *global* panel before opening its
own local one, with this comment:

> The sheet runs its own panel over the shared copilot store. Close the global panel first so one
> conversation is never rendered by two panels, whose independent close handlers would each restore
> saved state over the other.

That workaround exists *only* because `savedState` was a single slot. With a stack, two panels can
each hold their own snapshot and pop their own. If the workaround cannot be removed safely, the
stack has not actually solved the problem — so removing it is how we find out.

- [ ] **Step 1: Read both hooks before editing**

Read the automation `useWorkflowExecutionSheet.ts` and its EE twin at
`client/src/ee/pages/embedded/workflow-executions/components/workflow-execution-sheet/hooks/useWorkflowExecutionSheet.ts`.
Only the automation one carries the workaround. Note how each opens and closes its local panel.

- [ ] **Step 2: Remove the force-close**

Delete the `useCopilotPanelStore.getState().setCopilotPanelOpen(false);` line and its explanatory
comment from `handleCopilotClick`. Leave the rest of the handler — including the
`saveConversationState()` / `resetMessages()` / `generateConversationId()` sequence — exactly as it
is.

If removing the line orphans the `useCopilotPanelStore` import, remove the import too. Check whether
anything else in the file uses it before deleting.

- [ ] **Step 3: Verify**

```bash
cd client && npm run check
```

Expected: exit 0.

- [ ] **Step 4: Record what shipped**

In the research note, add a short section stating: the stack shipped with `saveConversationState` /
`restoreConversationState` retaining their names; the global panel now participates via
`useOpenCopilot` and `closeGlobalPanel`; the force-close workaround was removed; the depth cap is 10
with the deepest entry dropped; and the `sourceProp ?? context.source` vector remains open and out
of scope. Keep it factual — this note is what a future reader will trust.

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/automation/workflow-executions docs/superpowers/specs
git commit -m "client - Remove the single-slot Copilot panel workaround"
```

---

## Self-review notes

**Coverage.** The research note's requirements all map to a task: the stack itself (Task 1), the
global panel's absent save/restore (Task 2), the close-on-navigation requirement (already shipped in
the previous phase, and Task 2 makes it pop rather than hard-reset), and the workaround removal
(Task 3).

**Deliberately unchanged.** The 7 local save sites and 13 local restore sites are not touched: they
already call the right methods, and the methods keep their names. That is the point of the
name-preserving design — a 20-call-site rename would be the riskiest part of this phase and buys
nothing.

**Type consistency.** `ConversationSnapshotI` is defined and exported in Task 1 and used by name in
Tasks 1 and 2. `MAX_CONVERSATION_STACK_DEPTH` is defined in Task 1 and referenced only there.
`saveConversationState` and `restoreConversationState` keep `() => void` throughout.

**Known risk to watch in review.** Task 2 makes every `useOpenCopilot` call push, including a second
click of a `CopilotButton` while the panel is already open. That grows the stack by one per click
and is bounded by the depth cap. It is defensible (each click starts a new conversation and the
previous stays recoverable) but it is the design decision most worth a reviewer's challenge.
