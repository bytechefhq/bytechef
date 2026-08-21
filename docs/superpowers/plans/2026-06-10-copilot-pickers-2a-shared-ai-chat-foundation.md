# Copilot pickers — 2a: shared `ai-chat` client foundation + tool relocation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.
>
> **Git note:** the user commits to `0_732` in parallel. NEVER `git commit --amend`. Fresh commits. Stage only each task's files.
>
> **This is plan 2a of spec #2.** It builds the shared, behavior-preserving foundation: relocate the last 2 tools into `ai-copilot-tool`, extract AI Hub's interactive-chat UI pipeline into a shared `ai-chat` module (renderers + common stores + mapper + registry), and refactor AI Hub to consume it — **with no AI Hub behavior change**. Plan **2b** (Copilot `WorkflowEditorSpringAIAgent` registration + `CopilotRuntimeProvider`/panel wiring) is a follow-on that depends on this.

**Goal:** Make AI Hub's interactive-chat UI pipeline (the 5 data-part renderers, the tool-result→`data-*` interception mapper, the `kind`→component registry, and the 3 supporting stores) a shared `src/shared/components/ai-chat/` module, and relocate the `askUserQuestion`/`createConnection` server tools into `ai-copilot-tool` — all behavior-preserving for AI Hub.

**Architecture:** Server: `git mv` the 2 remaining tool callbacks into `ai-copilot-tool` (package + neutral-context/metrics swaps, mirroring the spec-#1 relocation); AI Hub re-imports them. Client: move the 5 renderers + rename the 3 stores into a shared module; extract the parse/validate interception into a `toToolResultDataPart` mapper (the one new unit, TDD); AI Hub's runtime provider + message-content registry consume the shared pieces. AI Hub's existing test suite is the behavior guard throughout.

**Tech Stack:** Java 25 / Spring AI (server); React 19 + TS, Vitest (client). EE server files keep the Enterprise header + `@version ee`.

---

## Background facts (verified)

- Spec #1 already moved `PropertyOptionsResolver` + 6 tools to `ai-copilot-tool` (`com.bytechef.ee.ai.copilot.tool`) behind `AgentToolInvocationContext` + `ToolStateVisibilityMetrics`. `ai-hub-service` depends on `ai-copilot-tool`; `ai-copilot-tool` has the platform/connection/user/automation deps + spring-security-core.
- Remaining tools to move: `AskUserQuestionToolCallback`, `CreateConnectionToolCallback` (both in `ai-hub-service/.../tool/`). Their AI-Hub couplings: `AiHubToolAttachMetrics` (askUserQuestion) and the connection facades (createConnection); audit each for `AiHubToolInvocationContext` usage during the move and swap to neutral if present.
- Client renderers (all under `client/src/pages/automation/ai-hub/`):
  - `connect/AiHubSelectConnectionMessage.tsx`, `connect/AiHubCreateConnectionMessage.tsx`
  - `messages/AiHubSelectPropertyOptionMessage.tsx`, `messages/AiHubAskUserQuestionMessage.tsx`, `messages/AiHubRunErrorMessage.tsx`
- Stores: `messages/stores/useAiHubToolCallStore.ts` (exports `useAiHubToolCallStore` + `aiHubToolCallStore`), `retry/stores/useAiHubRetryableErrorStore.ts` (`useAiHubRetryableErrorStore` + `aiHubRetryableErrorStore`), `messages/stores/useAiHubAskedQuestionsStore.ts` (`useAiHubAskedQuestionsStore` + `aiHubAskedQuestionsStore`). Reference counts: toolCall ~8 files, retryableError ~7, askedQuestions ~5.
- `AiHubMessageContent.tsx` holds the `data.by_name` registry (`ask-user-question`, `create-connection`, `run-error`, `select-connection`, `select-property-option`). `AiHubRuntimeProvider.tsx` holds the tool-result interception branches (`createConnection`, `selectConnection`, `askUserQuestion`, `selectPropertyOption`/`selectTriggerPropertyOption`) + the RUN_ERROR→`data-run-error` handling.
- Shared client module convention: `src/shared/components/...`.

Run: client `npm run test -- <path>`, `npx tsc --project tsconfig.json --noEmit`, `npx eslint <files> --max-warnings=0`. Server `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:test :server:ee:libs:ai:ai-hub:ai-hub-service:test`.

## File Structure (target)

```
client/src/shared/components/ai-chat/
  messages/
    SelectConnectionMessage.tsx        (from AiHubSelectConnectionMessage)
    CreateConnectionMessage.tsx        (from AiHubCreateConnectionMessage)
    SelectPropertyOptionMessage.tsx    (from AiHubSelectPropertyOptionMessage)
    AskUserQuestionMessage.tsx         (from AiHubAskUserQuestionMessage)
    RunErrorMessage.tsx                (from AiHubRunErrorMessage)
    toToolResultDataPart.ts            (NEW — extracted interception mapper)
    aiChatDataComponents.tsx           (NEW — kind→component by_name registry)
    tests/...                          (moved renderer tests + toToolResultDataPart.test.ts)
  stores/
    useAiChatToolCallStore.ts          (from useAiHubToolCallStore)
    useAiChatRetryableErrorStore.ts    (from useAiHubRetryableErrorStore)
    useAiChatAskedQuestionsStore.ts    (from useAiHubAskedQuestionsStore)
```
`ai-copilot-tool/.../copilot/tool/`: `AskUserQuestionToolCallback`, `CreateConnectionToolCallback` (+ tests) added.

---

## Task 1: Relocate `askUserQuestion` + `createConnection` server tools to `ai-copilot-tool`

Mechanical relocation mirroring spec #1 (see `docs/superpowers/plans/2026-06-10-shared-component-tools-server-centralization.md` Task 4 for the exact transform pattern). The moved tests are the guard. `ai-hub-service` will not compile until Step 4.

**Files:** move `AskUserQuestionToolCallback.java` (+ `AskUserQuestionToolCallbackTest.java`) and `CreateConnectionToolCallback.java` (+ `CreateConnectionToolCallbackTest.java` if it exists) from `ai-hub-service/.../com/bytechef/ee/ai/hub/tool/` to `ai-copilot-tool/.../com/bytechef/ee/ai/copilot/tool/`.

- [ ] **Step 1: `git mv` the 2 tools + their tests** (use `ls` first to confirm which tests exist).
- [ ] **Step 2: In each moved file:** change `package com.bytechef.ee.ai.hub.tool;` → `package com.bytechef.ee.ai.copilot.tool;`. Swap `AiHubToolInvocationContext`→`AgentToolInvocationContext` and `AiHubToolAttachMetrics`→`ToolStateVisibilityMetrics` (constructor param type) wherever they appear — same rules as spec #1 (static `resolveEnvironmentOrDefault(ctx)`→instance; test fixtures `new AiHubToolInvocationContext(...6-arg...)`→`new AgentToolInvocationContext(workspaceId, userId, environmentId, threadId)`; `mock(AiHubToolAttachMetrics.class)`→`mock(ToolStateVisibilityMetrics.class)`). Remove now-stale imports. If `createConnection` references no context/metrics, only the package line changes — let the compiler guide. If `createConnection` needs a connection facade not yet on `ai-copilot-tool`'s classpath, add the `implementation`/`testImplementation` dep to `ai-copilot-tool/build.gradle.kts` (note it in your report).
- [ ] **Step 3: Compile + run the moved tests in `ai-copilot-tool`.** `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:compileJava :server:ee:libs:ai:ai-copilot:ai-copilot-tool:compileTestJava :server:ee:libs:ai:ai-copilot:ai-copilot-tool:test` → green. Do NOT run ai-hub-service yet.
- [ ] **Step 4: Re-import in AI Hub.** In `ai-hub-service` (the compiler lists the broken files — likely `AiHubConfiguration.java` + any subscriber referencing these tools): change imports of `AskUserQuestionToolCallback`/`CreateConnectionToolCallback` from `com.bytechef.ee.ai.hub.tool.*` to `com.bytechef.ee.ai.copilot.tool.*`. Constructions unchanged (they pass `aiHubToolAttachMetrics` which implements the interface). Then `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava :server:ee:libs:ai:ai-hub:ai-hub-service:compileTestJava` → green.
- [ ] **Step 5: Commit (fresh):**
```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-tool server/ee/libs/ai/ai-hub/ai-hub-service
git commit -m "0_732 Relocate askUserQuestion + createConnection tools to ai-copilot-tool"
```

---

## Task 2: Move the 3 stores into shared `ai-chat/stores/` (rename)

Mechanical rename. The stores are framework-free Zustand stores; renaming is `git mv` + rename the exported symbols + update every reference.

- [ ] **Step 1: `git mv` each store file** to `client/src/shared/components/ai-chat/stores/`:
  - `useAiHubToolCallStore.ts` → `useAiChatToolCallStore.ts`
  - `useAiHubRetryableErrorStore.ts` → `useAiChatRetryableErrorStore.ts`
  - `useAiHubAskedQuestionsStore.ts` → `useAiChatAskedQuestionsStore.ts`
  Also `git mv` their test files if any (`tests/` siblings) to `ai-chat/stores/tests/`.
- [ ] **Step 2: Rename exported symbols** inside each moved file: `useAiHubToolCallStore`→`useAiChatToolCallStore`, `aiHubToolCallStore`→`aiChatToolCallStore`; `useAiHubRetryableErrorStore`→`useAiChatRetryableErrorStore`, `aiHubRetryableErrorStore`→`aiChatRetryableErrorStore`; `useAiHubAskedQuestionsStore`→`useAiChatAskedQuestionsStore`, `aiHubAskedQuestionsStore`→`aiChatAskedQuestionsStore`. (Keep internal store-key strings unchanged unless they leak across surfaces — they don't.)
- [ ] **Step 3: Update every reference** across the codebase. Find them:
```bash
cd client && grep -rln "useAiHubToolCallStore\|aiHubToolCallStore\|useAiHubRetryableErrorStore\|aiHubRetryableErrorStore\|useAiHubAskedQuestionsStore\|aiHubAskedQuestionsStore" src
```
For each file: update the import path to `@/shared/components/ai-chat/stores/useAiChat<X>Store` and the symbol names to the renamed ones.
- [ ] **Step 4: Verify.** `npx tsc --project tsconfig.json --noEmit` (your renamed files clean — ignore pre-existing unrelated errors) and run the moved store tests + any test that imports them: `npm run test -- src/shared/components/ai-chat/stores src/pages/automation/ai-hub`. Green.
- [ ] **Step 5: Commit (fresh):**
```bash
git add client/src/shared/components/ai-chat/stores client/src/pages/automation/ai-hub
git commit -m "5169 client - Rename AI Hub chat stores to common ai-chat stores"
```

---

## Task 3: Move the 5 renderers into shared `ai-chat/messages/` (de-prefix)

- [ ] **Step 1: `git mv` each renderer (+ its test)** to `client/src/shared/components/ai-chat/messages/` (tests to `messages/tests/`), renaming the file to drop the `AiHub` prefix:
  - `AiHubSelectConnectionMessage.tsx` → `SelectConnectionMessage.tsx`
  - `AiHubCreateConnectionMessage.tsx` → `CreateConnectionMessage.tsx`
  - `AiHubSelectPropertyOptionMessage.tsx` → `SelectPropertyOptionMessage.tsx`
  - `AiHubAskUserQuestionMessage.tsx` → `AskUserQuestionMessage.tsx`
  - `AiHubRunErrorMessage.tsx` → `RunErrorMessage.tsx`
- [ ] **Step 2: In each moved file:** rename the default-exported component (`AiHubXxxMessage`→`XxxMessage`); keep the exported `*DataI` interface names (other code imports them). Update internal imports of the 3 stores to the new `@/shared/components/ai-chat/stores/...` paths + renamed symbols (from Task 2). Update any cross-renderer import to the new sibling path. Keep all other imports (shared queries, components) as-is. Match client conventions (interface names end `I`/`Props`; `twMerge`; Lucide `*Icon`).
- [ ] **Step 3: Update consumers' imports.** `AiHubMessageContent.tsx` imports all 5 renderers + their `*DataI` — repoint to `@/shared/components/ai-chat/messages/<Name>`. Find any other importer:
```bash
cd client && grep -rln "AiHubSelectConnectionMessage\|AiHubCreateConnectionMessage\|AiHubSelectPropertyOptionMessage\|AiHubAskUserQuestionMessage\|AiHubRunErrorMessage" src
```
- [ ] **Step 4: Verify.** `npx tsc --project tsconfig.json --noEmit` clean for touched files; `npm run test -- src/shared/components/ai-chat/messages src/pages/automation/ai-hub/messages` green (moved renderer tests pass under new paths).
- [ ] **Step 5: Commit (fresh):**
```bash
git add client/src/shared/components/ai-chat/messages client/src/pages/automation/ai-hub
git commit -m "5169 client - Move AI Hub chat renderers to shared ai-chat module"
```

---

## Task 4: Extract `toToolResultDataPart` mapper (TDD — the one new unit)

**Files:**
- Create: `client/src/shared/components/ai-chat/messages/toToolResultDataPart.ts`
- Test: `client/src/shared/components/ai-chat/messages/tests/toToolResultDataPart.test.ts`
- Modify: `client/src/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider.tsx`

The mapper centralizes the parse+validate logic currently inline in `AiHubRuntimeProvider`'s tool-result branches. It returns a discriminated result the caller applies.

- [ ] **Step 1: Write the failing test**
```typescript
import {describe, expect, it} from 'vitest';

import {toToolResultDataPart} from '../toToolResultDataPart';

describe('toToolResultDataPart', () => {
    it('maps a valid select-property-option result to a data part', () => {
        const result = toToolResultDataPart(
            'selectPropertyOption',
            JSON.stringify({
                componentName: 'slack',
                kind: 'select-property-option',
                options: [{label: 'general', value: 'C1'}],
                propertyName: 'channel',
                truncated: false,
            })
        );

        expect(result.ok).toBe(true);

        if (result.ok) {
            expect(result.type).toBe('data-select-property-option');
            expect(result.data.options).toHaveLength(1);
        }
    });

    it('maps selectTriggerPropertyOption (same kind) to the same data part', () => {
        const result = toToolResultDataPart(
            'selectTriggerPropertyOption',
            JSON.stringify({componentName: 'slack', kind: 'select-property-option', options: [], propertyName: 'x'})
        );

        expect(result.ok).toBe(true);
    });

    it('maps a valid select-connection result', () => {
        const result = toToolResultDataPart(
            'selectConnection',
            JSON.stringify({componentLabel: 'Slack', componentName: 'slack', kind: 'select-connection'})
        );

        expect(result.ok && result.type).toBe('data-select-connection');
    });

    it('maps a valid ask-user-question result', () => {
        const result = toToolResultDataPart(
            'askUserQuestion',
            JSON.stringify({kind: 'ask-user-question', questions: [{multiSelect: false, options: [], question: 'Q?'}]})
        );

        expect(result.ok && result.type).toBe('data-ask-user-question');
    });

    it('returns an error result for a malformed payload', () => {
        const result = toToolResultDataPart('selectPropertyOption', 'not json{');

        expect(result.ok).toBe(false);

        if (!result.ok) {
            expect(result.toolName).toBe('selectPropertyOption');
            expect(result.errorMessage).toMatch(/unparseable|malformed/i);
        }
    });

    it('returns an error result when kind is wrong', () => {
        const result = toToolResultDataPart('selectConnection', JSON.stringify({kind: 'nope'}));

        expect(result.ok).toBe(false);
    });

    it('returns undefined ok=false-less passthrough for an unhandled tool name', () => {
        expect(toToolResultDataPart('someOtherTool', '{}')).toBeUndefined();
    });
});
```

- [ ] **Step 2: Run to verify it fails** — `npm run test -- src/shared/components/ai-chat/messages/tests/toToolResultDataPart.test.ts` → COMPILE/import failure.

- [ ] **Step 3: Implement the mapper.** Create `toToolResultDataPart.ts`. Read the current branches in `AiHubRuntimeProvider.tsx` for `createConnection`, `selectConnection`, `askUserQuestion`, `selectPropertyOption`/`selectTriggerPropertyOption` (the `parseJson`/validate/`addMessage({data, type})` shape) and lift the parse+validate into:
```typescript
import {parseJson} from '@/...';   // reuse the same parseJson helper AiHubRuntimeProvider uses; if it's local, export it or inline an equivalent

export type ToolResultDataPart =
    | {data: Record<string, unknown>; ok: true; type: string}
    | {errorMessage: string; ok: false; toolName: string};

/**
 * Parses a tool-result payload into the assistant-ui `data-*` part the chat thread renders, or an error result the
 * caller surfaces. Returns undefined for tool names that don't produce an interactive data part. Shared by AI Hub and
 * the Copilot panel so both render the same pickers from identical logic.
 */
export function toToolResultDataPart(toolCallName: string, eventContent: string): ToolResultDataPart | undefined {
    // one branch per interactive tool — mirror the exact kind/array validations from AiHubRuntimeProvider:
    //   selectConnection                       → kind 'select-connection'        → type 'data-select-connection'
    //   selectPropertyOption | selectTriggerPropertyOption → kind 'select-property-option' → 'data-select-property-option'
    //   askUserQuestion                        → kind 'ask-user-question' + Array.isArray(questions) → 'data-ask-user-question'
    //   createConnection                       → kind 'create-connection'        → 'data-create-connection'
    // unhandled tool name → return undefined
}
```
Implement each branch to return `{ok:true, type, data}` on a valid payload (the `data` object = the exact fields the matching `addMessage` currently forwards) and `{ok:false, toolName, errorMessage}` on parse/validation failure. Use the same field sets the current `AiHubRuntimeProvider` branches build.

- [ ] **Step 4: Run to verify it passes** — same command → all mapper tests green.

- [ ] **Step 5: Refactor `AiHubRuntimeProvider` to use the mapper.** Replace each inline tool-result branch's parse/validate with a call to `toToolResultDataPart(toolCallName, event.content)`; on `ok` → `addMessage({content:[{data: result.data, type: result.type}], role:'assistant'})`; on `!ok` → the EXISTING AI Hub error handling (`aiChatToolCallStore.getState().completeToolCall(...)` + `aiChatRetryableErrorStore.getState().setError({errorMessage: result.errorMessage, lastUserMessage: getLastUserMessage(), toolName: result.toolName})`). The RUN_ERROR→`data-run-error` path stays as-is (not a tool result). Behavior unchanged.

- [ ] **Step 6: Verify AI Hub behavior** — `npm run test -- src/pages/automation/ai-hub` green (the runtime-provider + message tests are the guard).

- [ ] **Step 7: Commit (fresh):**
```bash
git add client/src/shared/components/ai-chat/messages/toToolResultDataPart.ts \
        client/src/shared/components/ai-chat/messages/tests/toToolResultDataPart.test.ts \
        client/src/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider.tsx
git commit -m "5169 client - Extract shared toToolResultDataPart mapper; AI Hub consumes it"
```

---

## Task 5: Shared `aiChatDataComponents` registry; AI Hub consumes it

**Files:**
- Create: `client/src/shared/components/ai-chat/messages/aiChatDataComponents.tsx`
- Modify: `client/src/pages/automation/ai-hub/messages/AiHubMessageContent.tsx`

- [ ] **Step 1: Create the registry.** It maps each `kind` to its renderer (the `by_name` map AI Hub currently inlines):
```typescript
import AskUserQuestionMessage, {AskUserQuestionDataI} from '@/shared/components/ai-chat/messages/AskUserQuestionMessage';
import CreateConnectionMessage, {CreateConnectionDataI} from '@/shared/components/ai-chat/messages/CreateConnectionMessage';
import RunErrorMessage, {RunErrorDataI} from '@/shared/components/ai-chat/messages/RunErrorMessage';
import SelectConnectionMessage, {SelectConnectionDataI} from '@/shared/components/ai-chat/messages/SelectConnectionMessage';
import SelectPropertyOptionMessage, {
    SelectPropertyOptionDataI,
} from '@/shared/components/ai-chat/messages/SelectPropertyOptionMessage';
import {DataMessagePartProps} from '@assistant-ui/react';

export const aiChatDataComponents = {
    'ask-user-question': (props: DataMessagePartProps<AskUserQuestionDataI>) => <AskUserQuestionMessage {...props} />,
    'create-connection': (props: DataMessagePartProps<CreateConnectionDataI>) => <CreateConnectionMessage {...props} />,
    'run-error': (props: DataMessagePartProps<RunErrorDataI>) => <RunErrorMessage {...props} />,
    'select-connection': (props: DataMessagePartProps<SelectConnectionDataI>) => <SelectConnectionMessage {...props} />,
    'select-property-option': (props: DataMessagePartProps<SelectPropertyOptionDataI>) => (
        <SelectPropertyOptionMessage {...props} />
    ),
};
```
(Keys alphabetical for sort-keys. Confirm the exact `*DataI` export names match the moved renderers.)

- [ ] **Step 2: AiHubMessageContent consumes it.** Replace the inline `data.by_name` object with `aiChatDataComponents` (imported). The rest of `MessagePrimitive.Parts` (Source, Text, tools.Fallback) stays.

- [ ] **Step 3: Verify** — `npm run test -- src/pages/automation/ai-hub` green; `npx tsc --noEmit` clean for touched files.

- [ ] **Step 4: Commit (fresh):**
```bash
git add client/src/shared/components/ai-chat/messages/aiChatDataComponents.tsx \
        client/src/pages/automation/ai-hub/messages/AiHubMessageContent.tsx
git commit -m "5169 client - Share ai-chat data-component registry; AI Hub consumes it"
```

---

## Task 6: Full verification

- [ ] **Step 1: Server** — `./gradlew spotlessApply :server:ee:libs:ai:ai-copilot:ai-copilot-tool:spotlessApply :server:ee:libs:ai:ai-hub:ai-hub-service:spotlessApply :server:ee:libs:ai:ai-copilot:ai-copilot-tool:check :server:ee:libs:ai:ai-hub:ai-hub-service:check` (run module-scoped to avoid touching client files). Both green. Stage ONLY the touched server files for any spotless reformat (fresh commit; never amend; never stage the user's parallel files).
- [ ] **Step 2: Client** — `npx eslint src --max-warnings=0`, `npx tsc --project tsconfig.json --noEmit`, `npx vitest run` (note: pre-existing prettier-drift in unrelated user files may keep `npm run check` red — run the stages directly; confirm YOUR files are prettier-clean via `npx prettier --check <touched files>`).
- [ ] **Step 3: Stale-reference grep** (source only):
```bash
grep -rn "AiHubSelectConnectionMessage\|AiHubCreateConnectionMessage\|AiHubSelectPropertyOptionMessage\|AiHubAskUserQuestionMessage\|AiHubRunErrorMessage\|useAiHubToolCallStore\|aiHubToolCallStore\|useAiHubRetryableErrorStore\|aiHubRetryableErrorStore\|useAiHubAskedQuestionsStore\|aiHubAskedQuestionsStore" client/src
grep -rn "com.bytechef.ee.ai.hub.tool.AskUserQuestionToolCallback\|com.bytechef.ee.ai.hub.tool.CreateConnectionToolCallback" server --include=*.java | grep -v /.claude/
```
Expected: no matches.
- [ ] **Step 4: Final formatting commit if needed** (fresh, only your files).

---

## Self-Review

**Spec coverage (2a portion):** relocate askUserQuestion+createConnection tools → Task 1 ✓; common stores renamed → Task 2 ✓; renderers shared → Task 3 ✓; interception mapper shared → Task 4 ✓; registry shared → Task 5 ✓; AI Hub consumes all (behavior-preserving) → Tasks 4–5 + the AI Hub suite as guard ✓. **Deferred to 2b:** Copilot `WorkflowEditorSpringAIAgent` tool registration + `toolContext` neutral keys + prompt note; `CopilotRuntimeProvider` interception; `<Thread>` `dataComponents` prop + `CopilotPanel` wiring. (Documented in the header.)

**Placeholder scan:** Task 4's mapper body is described as "mirror the exact validations from AiHubRuntimeProvider" with the precise kind→type table — the implementer reads the 4 existing branches and lifts them; this is a faithful extraction, not a vague placeholder (the discriminated return type + every branch's kind/type/validation is specified, and the TDD test pins the contract). Moves/renames use precise `git mv` + symbol-rename steps + green-test verification (the spec-#1 relocation precedent).

**Type consistency:** store symbols renamed consistently across Tasks 2–5 (`aiChat*`/`useAiChat*`). Renderer default-export renames (`Xxx Message`) + preserved `*DataI` names used identically in Task 5's registry. `ToolResultDataPart` discriminated type defined in Task 4 and consumed in the AiHubRuntimeProvider refactor. Data-part `type` strings (`data-select-connection`, etc.) match between the mapper (Task 4) and the registry kinds (Task 5).
