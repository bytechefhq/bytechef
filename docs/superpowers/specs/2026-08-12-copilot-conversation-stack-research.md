# Copilot conversation stack — research notes (phase 5 input)

Gathered 2026-08-12 against `claude/copilot-phase4-asset-slice`. These notes correct several
assumptions carried in the phase 5 sketch; the plan must be written from these, not from the
original one-line description ("replace save/restore across 5 surfaces").

## Correction 1 — it is 7 local surfaces plus a global one, not 5

Seven surfaces mount their own `<CopilotPanel>` with a **local** `copilotPanelOpen` boolean (not the
global `useCopilotPanelStore`), and each owns one `saveConversationState()` call:

1. `client/src/ee/pages/embedded/workflow-executions/components/workflow-execution-sheet/hooks/useWorkflowExecutionSheet.ts`
2. `client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/hooks/useWorkflowExecutionSheet.ts`
3. `client/src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/hooks/useSampleOutputCopilot.ts`
4. `client/src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/hooks/usePropertyJsonSchemaBuilderCopilot.ts`
5. `client/src/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/hooks/usePropertyCodeEditorDialogToolbar.ts`
   (save only — its **restore** lives in the sibling `usePropertyCodeEditorDialog.ts`)
6. `client/src/pages/platform/workflow-editor/components/hooks/useClusterElementsCanvasDialog.ts`
   (one hook, three mount points inside `ClusterElementsCanvasDialog.tsx`)
7. `client/src/pages/platform/workflow-editor/hooks/useWorkflowCodeEditorSheet.ts`

The **eighth** path is the global panel mounted once in `App.tsx`, opened by `CopilotButton` →
`useOpenCopilot`. It never calls save or restore at all — it simply overwrites `context` and opens
the panel. Every listing page added in phases 1–4 uses this path.

## Correction 2 — save and restore call-site counts differ

`saveConversationState`: **7** call sites (one per surface, always from the open handler).
`restoreConversationState`: **13** call sites across the same 7 files — several surfaces restore
from more than one exit path (close button, `onOpenChange` outside-click/escape, unsaved-changes
confirm). Restores fire whenever the _host_ dialog closes, whether or not Copilot was opened inside
it; that is safe today only because `restoreConversationState` no-ops when `savedState` is `null`.
A stack must preserve that "pop on empty is a no-op" property or every one of those 13 sites needs
an explicit guard.

## The bug is already documented in the codebase

`client/src/pages/automation/workflow-executions/.../useWorkflowExecutionSheet.ts` force-closes the
global panel before opening its own, with this comment:

> The sheet runs its own panel over the shared copilot store. Close the global panel first so one
> conversation is never rendered by two panels, whose independent close handlers would each restore
> saved state over the other.

That workaround exists precisely because `savedState` is one slot. It should be removable once a
stack lands — and whether it is, is a good acceptance check for the phase.

## Second, independent clobbering vector — do not conflate it with the slot

`CopilotRuntimeProvider` resolves `sourceKey = sourceProp ?? context?.source ?? Source.WORKFLOW_EDITOR`.
Only `WorkflowCodeEditorSheet.tsx` passes a `source` prop; every other surface falls through to
`context.source` from the single shared store. So whichever surface last called `setContext` wins
for **every** mounted provider that does not pass its own prop. Fixing the save/restore slot does
not fix this. The plan should decide explicitly whether phase 5 also threads `source` per panel, or
scopes that out and records it.

## State shape today

`savedState: {conversationId, context, composerPlaceholder, messages, selectedLlmProvider,
selectedLlmModel} | null` — one nullable slot on `useCopilotStore`, not keyed by anything. A second
`save()` before a `restore()` silently overwrites the first snapshot.

`context` is `{source, parameters, mode, environmentId?, workflowExecutionError?}`. `mode` has no
separate store slot; it only ever travels inside `context`.

Store middleware is `devtools` only — **no `persist`**. Nothing is written to local/session storage,
and `conversationId` is re-randomized on every module load. A stack can be plain in-memory Zustand;
it does not need to survive reload, and making it survive would be a behaviour change, not a
carry-over.

## Lifecycle hazards a stack implementation must respect

- **Panel unmount lags the store by 300ms.** `CopilotPanelImpl` keeps `shouldRender` true for
  `ANIMATION_DURATION_MS` after `open` flips false so the close animation can play. A stack must not
  assume the provider unmounts synchronously with the pop.
- **Route change silently drops the conversation.** `CopilotPanelContent`'s effect on
  `location.pathname` calls `generateConversationId()` + `resetMessages()` with no save or restore.
- **A third distinct close path exists.** `handleCloseClick`'s fallback branch (the global panel,
  where no `onClose` prop is given) resets `context` to a hardcoded
  `{source: WORKFLOW_EDITOR, mode: ASK, parameters: {}}` rather than restoring anything.

## Required behaviour added by the user (2026-08-12)

**Navigating to a different page must close the Copilot panel if it is open.** Reported concretely:
open Copilot on Projects, click through to AI Hub, and the panel is still open.

This is worse than a cosmetic leftover. The route-change effect in `CopilotPanelContent` today calls
`generateConversationId()` + `resetMessages()` but never closes the panel and never resets
`context`. So after navigating, the panel is open on the new page while `context.source` still names
the _old_ surface — and `CopilotRuntimeProvider` derives its agent id and its
`/api/platform/internal/ai/chat/{source}` URL from exactly that value. The user's first message on
the new page is therefore routed to the previous page's agent, with the previous page's
`parameters`. Closing the panel fixes the visible symptom and the mis-routing together.

Implementation subtlety the plan must resolve: that effect runs inside `CopilotPanelContent`, which
backs **both** the global `App.tsx` panel and all 7 local-panel surfaces. Calling
`useCopilotPanelStore.setCopilotPanelOpen(false)` unconditionally from there would let a local
panel close the global one. The local panels own their own open flags and are mounted inside
dialogs that unmount on navigation anyway, so the close should be scoped to the global panel — the
existing `onClose == null` test (already used by `handleCloseClick` to distinguish the two cases) is
the natural discriminator. Reset `context` and `composerPlaceholder` on the same path, matching what
`handleCloseClick`'s fallback branch already does, so the next open starts clean.

## Tests that constrain the change

- `useCopilotStore.test.ts` covers only `appendToLastAssistantMessage` — **zero** coverage of
  save/restore/`savedState`.
- `useCopilotPostTurnRegistry.test.ts` — six assertions on the registry's shape and unregister
  semantics. The registry is orthogonal to the slot and should not need changing; if its
  `callbacks` shape or `register`/`runFor` signatures move, all six break.
- Three hook tests spy on the store's save/restore directly and would need updating if the methods
  are renamed: `useSampleOutputCopilot.test.ts`,
  `usePropertyJsonSchemaBuilderCopilot.test.ts`, and
  `usePropertyCodeEditorDialogToolbar.test.ts`.
- **`usePropertyCodeEditorDialogToolbar.test.ts` pins an ordering contract** —
  `expect(callOrder).toEqual(['save', 'reset', 'generate'])`. A push-based replacement is exactly
  the kind of change most likely to invert this accidentally. Keep the ordering or change it
  deliberately and update the test with a stated reason.

## What shipped (2026-08-13)

`useCopilotStore` replaced the single nullable `savedState` slot with `conversationStack:
ConversationSnapshotI[]`. `saveConversationState` and `restoreConversationState` kept their names
and `() => void` signatures — none of the 7 local save sites or 13 local restore sites needed to
change. Popping an empty stack remains a deliberate no-op. The stack has a depth cap of 10; a push
beyond that logs a console warning and drops the _oldest_ entry (bottom of stack), not the newest.

The global panel now participates in the same stack. `useOpenCopilot` pushes, resets messages, and
mints a fresh conversation id before installing the new context — matching what the 7 local surfaces
already did. `closeGlobalPanel` (in `CopilotPanelImpl.tsx`) pops on close, restoring whatever was
underneath; only when the stack is empty does it fall back to the old hardcoded reset
(`{source: WORKFLOW_EDITOR, mode: ASK, parameters: {}}`).

The force-close workaround this note originally flagged (`useCopilotPanelStore.getState()
.setCopilotPanelOpen(false)` in the automation `useWorkflowExecutionSheet.ts`) was removed. It is
safe because the sheet that owns it is a modal Radix `Sheet` — a portalled, `z-50`, pointer-capturing
overlay with no `modal={false}` override — so the global panel's own close affordance is unreachable
while the sheet is open, and the two panels' close handlers can no longer race. Removing it also
fixes a latent regression the workaround caused: previously, closing the sheet's local panel left the
global panel force-collapsed even after its conversation was restored underneath it; now the global
panel simply resumes showing whatever the stack handed back to it.

The `sourceProp ?? context.source` clobbering vector (Correction 2 / "Second, independent clobbering
vector" above) was **not** addressed by this phase and remains open. It is a separate mechanism from
the save/restore slot and was explicitly scoped out.

## Correction 3 (2026-08-13) — the empty-stack guard was not the property that made restores safe

The first cut of this phase, described above, kept the single slot's "restoring is a no-op when
there is nothing saved" guard and reasoned that it still covered the 13 restore sites once the slot
became a stack. That reasoning does not carry over: with a single slot, depth could never exceed 1,
so "the slot is empty" and "nothing I pushed is on it" were the same condition. With a stack they are
not — five of the seven local surfaces fire two restores for one save on an ordinary close (once when
their own Copilot panel closes, once when their host dialog closes), and a sixth restores on every
dialog close whether or not Copilot was ever opened. Once any surface reached stack depth 2 — which
`useOpenCopilot` made easy to hit, since it pushed unconditionally even when the global panel was
already open — a surface's second, logically-redundant restore call would pop an entry it never
pushed: a different surface's conversation, one level further down. Concretely: workflow editor →
open the cluster-elements canvas dialog's Copilot (pushes the editor's conversation) → inside it,
open a script property's Copilot (pushes the cluster dialog's conversation) → close the script
Copilot (correctly pops back to the cluster dialog's conversation) → close the script dialog itself
(pops _again_, handing the cluster dialog the workflow editor's conversation and `context.source`
instead of its own). The cluster dialog's panel was still open, now showing the wrong agent.

The fix (see this repo's `copilot-phase5-conversation-stack` branch) replaced the empty-stack check
with token pairing: `saveConversationState()` now returns a token identifying the entry it pushed,
`ConversationSnapshotI` carries that token, and `restoreConversationState(token?)` was meant to pop
only when the token matches the top of the stack — a mismatched or absent-when-expected token being a
no-op even on a non-empty stack. Each of the 14 restore call sites now threads the token it captured
from its own save (via a `useRef`, or — for the two cases where save and restore live in sibling
hooks with no shared component instance, the property-code-editor toolbar/dialog pair and the global
panel's `useOpenCopilot`/`closeGlobalPanel` — via a field on a shared store).

## Correction 4 (2026-08-13) — the token cut still let an absent token take the unconditional pop

The token pairing above shipped with a loophole that reproduced the same class of bug it was meant to
close. `restoreConversationState` kept the optional overload — `(token?: string)` — with a guard of
`if (token !== undefined && top?.token !== token) return state;`, and every call site was written as
`restoreConversationState(conversationTokenRef.current ?? undefined)`. A surface that never opened
Copilot holds `null` in that ref, which is exactly the case the guard needed to catch, but `?? undefined`
turned "I pushed nothing" into "no token supplied," and the guard's `token !== undefined` check took the
old unconditional-pop branch for exactly that input. So the guard only ever fired on a *stale* token
(one already popped) and never on an *absent* one — eight of the fourteen restore sites could still pop
a snapshot they never pushed, reproducing the cluster-dialog steal scenario above. The fix dropped the
optional overload entirely: `restoreConversationState(token: string | null)` is now required at every
call site, and the guard is unconditional — `if (top?.token !== token) return state;` — so a `null`
token, which can never equal a real token string, always no-ops regardless of stack depth.
