# Copilot + AI Hub + Personal Agents `ModelPicker` — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Spec:** [2026-05-17-copilot-aihub-per-conversation-model-design.md](../specs/2026-05-17-copilot-aihub-per-conversation-model-design.md)

**Parent spec:** [2026-05-13-ai-hub-personal-agent-model-selection-design.md](../specs/2026-05-13-ai-hub-personal-agent-model-selection-design.md) (this is its §6 follow-up, broadened)

**Goal:** Add a per-conversation LLM provider/model override to Copilot and AI Hub chat. Replace the Personal Agent form's two-dropdown setup with the same unified picker. Server resolver precedence: `user-selected > agent-default > workspace default`.

**Architecture:** Server side reuses the existing AG-UI `State` envelope as wire format and the existing `OverrideChatClientResolver` pattern from `AiHubSpringAIAgent`. Widen `PersonalAgentChatClientResolver` (rename → `AiHubChatClientResolver`) to read user-selected keys first. Add a sibling `CopilotChatClientResolver` and slot the override-hook pattern into the three Copilot AG-UI agents. Client side adds a shared `ModelPicker` (shadcn `DropdownMenu` with right-cascading `DropdownMenuSub` per provider, controlled search input at the top) used in three places.

**Tech Stack:** Java 25 / Spring Boot 4, AG-UI agent framework, Spring AI `ChatClient`; React 19 + TypeScript, shadcn/ui (Radix `DropdownMenu` + `DropdownMenuSub`), TanStack Query + graphql-codegen, Zustand.

---

## Phase 1 — Server: AI Hub (resolver precedence widening)

### Task 1: Add user-selected state keys

**Files:**
- Modify: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/util/AiHubStateKeys.java`

- [ ] **Step 1:** Append two constants alongside the existing `PERSONAL_AGENT_LLM_*_KEY` entries:

```java
/**
 * User-selected LLM provider for the current conversation, supplied by the client via AG-UI
 * {@code state.userSelectedLlmProvider}. Takes precedence over per-agent override and workspace default
 * in {@link com.bytechef.ee.automation.aihub.agent.AiHubChatClientResolver}.
 */
public static final String USER_SELECTED_LLM_PROVIDER_KEY = "userSelectedLlmProvider";

/**
 * User-selected LLM model id for the current conversation. Paired with
 * {@link #USER_SELECTED_LLM_PROVIDER_KEY}; either both must be present or both absent. Half-set
 * states fall through to the next precedence layer.
 */
public static final String USER_SELECTED_LLM_MODEL_KEY = "userSelectedLlmModel";
```

- [ ] **Step 2:** Verify with `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:compileJava`.

---

### Task 2: Widen `PersonalAgentChatClientResolver.resolve()` precedence (rename deferred)

**Decision change from spec:** the original plan was to rename `PersonalAgentChatClientResolver` → `AiHubChatClientResolver`. On reading the class, it also holds a `validate(workspaceId, llmProvider, llmModel)` method that's personal-agent-specific (called from `AiHubPersonalAgentServiceImpl.create/update` at save time). Renaming the class without splitting it would make the name incoherent. Proper cleanup is a class split (resolver + validator) — that's its own follow-up. v1 keeps the class name and updates the Javadoc.

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/agent/PersonalAgentChatClientResolver.java`

- [ ] **Step 1: Write the precedence test first** (new file): `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/agent/PersonalAgentChatClientResolverTest.java`.

Test matrix (one `@Test` per row):

| user-selected | personal-agent override | Expected resolver output |
|---|---|---|
| `(OPENAI, gpt-4o)` | `(ANTHROPIC, sonnet)` | `ChatClient` built from OPENAI / gpt-4o |
| `(OPENAI, gpt-4o)` | none | `ChatClient` built from OPENAI / gpt-4o |
| none | `(ANTHROPIC, sonnet)` | `ChatClient` built from ANTHROPIC / sonnet |
| none | none | `null` (caller falls back to workspace default) |
| `(OPENAI, null)` — half-set | `(ANTHROPIC, sonnet)` | `ChatClient` built from ANTHROPIC / sonnet (half-set fell through) |
| `(unknown-provider, gpt-4o)` | none | `null` + warning log (resolver fall-through; caller picks workspace default) |

Stub `AiGatewayChatModelFactory` and `AiGatewayProviderResolver` with Mockito.

- [ ] **Step 2: Modify `resolve(State)` precedence.** Replace the body of the early read with:

```java
String llmProvider = asString(state.get(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY));
String llmModel    = asString(state.get(AiHubStateKeys.USER_SELECTED_LLM_MODEL_KEY));

if (llmProvider == null || llmModel == null) {
    if ((llmProvider == null) != (llmModel == null)) {
        logger.warn(
            "User-selected LLM half-set (provider={}, model={}); falling through to next precedence",
            llmProvider, llmModel);
    }
    llmProvider = asString(state.get(AiHubStateKeys.PERSONAL_AGENT_LLM_PROVIDER_KEY));
    llmModel    = asString(state.get(AiHubStateKeys.PERSONAL_AGENT_LLM_MODEL_KEY));
}
```

Keep the rest (workspaceId read, provider resolution, `ChatClient` construction) unchanged.

- [ ] **Step 3: Update the class Javadoc** to explain the dual-precedence role (user-selected first, personal-agent second). Add a one-line note about the deferred class-split cleanup.

- [ ] **Step 4:** Run `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test` and confirm new test class passes.

- [ ] **Step 5:** Update reference in `AiHubSpringAIAgent` Javadoc (one-line mention) so future readers know the override now serves user-selected as well as per-agent.

---

## Phase 2 — Server: Copilot (override hook + resolver, new)

### Task 3: Lift the `OverrideChatClientResolver` interface (or copy inline — decide at impl time)

**Files (lift variant — preferred):**
- Create: `server/ee/libs/platform/platform-ai-agent/.../OverridableChatClientAgent.java` (or similar shared location)
- Modify: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/agent/AiHubSpringAIAgent.java` — adopt the lifted interface
- Modify: each Copilot agent class (`CodeEditorSpringAIAgent`, `WorkflowEditorSpringAIAgent`, `ClusterElementSpringAIAgent`) — adopt the lifted interface

**Files (copy-inline variant — fallback):**
- Modify only the three Copilot agent classes (duplicate the 22-line `resolveChatClient` override from `AiHubSpringAIAgent:165-187`)

- [ ] **Step 1:** Open [AiHubSpringAIAgent.java:165-187](../../../server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/agent/AiHubSpringAIAgent.java) and read the `OverrideChatClientResolver` nested interface + `resolveChatClient` method.

- [ ] **Step 2:** Find the parent class of the Copilot agents (`grep -rn 'extends.*SpringAIAgent' server/ee/libs/ai/ai-copilot/`). If they extend the same AG-UI base as `AiHubSpringAIAgent`, the `resolveChatClient` override can be defined on either the lifted mixin or the agents directly.

- [ ] **Step 3:** **Decision point:**
  - If the three Copilot agents share a common Copilot-specific base class → put `resolveChatClient` there once.
  - Else if all four (Copilot ×3, AI Hub) share an AG-UI base → lift `OverrideChatClientResolver` interface into the AG-UI module.
  - Else → copy-inline into each of the three Copilot agents.

- [ ] **Step 4:** Whichever route, write a single-line test or compilation check verifying the override fires (use a stub `OverrideChatClientResolver` that returns a known-distinct `ChatClient` and assert `resolveChatClient` returns it).

---

### Task 4: New `CopilotChatClientResolver`

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/agent/CopilotChatClientResolver.java`
- Create test: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/agent/CopilotChatClientResolverTest.java`

- [ ] **Step 1: Test first.** Same matrix as `AiHubChatClientResolverTest` but without the personal-agent layer:

| user-selected | Expected |
|---|---|
| `(OPENAI, gpt-4o)` | `ChatClient` built from OPENAI / gpt-4o |
| none | `null` |
| `(OPENAI, null)` half-set | `null` + warning log |
| `(unknown-provider, gpt-4o)` | `null` + warning log |

- [ ] **Step 2: Implement.** Use raw state-key strings `"userSelectedLlmProvider"` / `"userSelectedLlmModel"` (Copilot has no `StateKeys` class today; keeping raw is fine for v1, refactor target for v2). Reuse `AiGatewayChatModelFactory` and provider resolution via dependency injection — same as `AiHubChatClientResolver`.

- [ ] **Step 3:** Reads workspaceId from `state.get("workspaceId")` (which Copilot already populates from the existing chat flow — verify via grep).

- [ ] **Step 4:** Annotate `@Component` and `@ConditionalOnProperty("bytechef.ai.copilot.enabled")` to mirror Copilot's existing conditional gating.

---

### Task 5: Wire `CopilotChatClientResolver` into the Copilot agents

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java`

- [ ] **Step 1:** For each agent `@Bean` method (workflow editor, code editor, cluster element, both BUILD and ASK variants — 6 total per the dispatcher in `CopilotApiController.java:60-78`), add an `ObjectProvider<CopilotChatClientResolver>` parameter and call `.ifAvailable(builder::overrideChatClientResolver)`.

- [ ] **Step 2:** If the `Builder` doesn't have `overrideChatClientResolver` yet, add the setter following the [AiHubSpringAIAgent.Builder.overrideChatClientResolver](../../../server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/agent/AiHubSpringAIAgent.java) pattern.

- [ ] **Step 3:** Integration test (new file): `CopilotChatClientResolverIntTest` — `@SpringBootTest`, posts an AG-UI request with `state.userSelectedLlmProvider="OPEN_AI"` + `state.userSelectedLlmModel="gpt-4o"`, asserts the resolver was consulted and produced a `ChatClient` distinct from the workspace default. Mock `AiGatewayChatModelFactory.getChatModel(Provider.OPEN_AI)` to return a recognizable sentinel `ChatModel`.

- [ ] **Step 4:** Run `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:check`.

---

## Phase 3 — Client: shared `ModelPicker` component

### Task 6: `ModelPicker` skeleton + iconography

**Files:**
- Create: `client/src/shared/components/ai/model-picker/ModelPicker.tsx`
- Create: `client/src/shared/components/ai/model-picker/ProviderIcon.tsx`
- Create: `client/src/shared/components/ai/model-picker/tests/ModelPicker.test.tsx`

- [ ] **Step 1: Failing test (Vitest + Testing Library).** Render `<ModelPicker workspaceId={1} selectedProvider={null} selectedModel={null} onChange={onChange} workspaceDefaultLabel="Workspace default" />`. Assert trigger text contains "Workspace default". Assert menu not open. Click trigger. Assert menu open with a search input and at least one provider row (mock GraphQL with 2 providers + 4 models).

- [ ] **Step 2: Mock GraphQL.** Use `vi.hoisted` + `vi.mock('@/shared/middleware/graphql', ...)` to mock `useWorkspaceAiGatewayProvidersQuery` and `useWorkspaceAiGatewayModelsQuery` (per [Vitest mock factory hoisting pattern](../../../CLAUDE.md) in CLAUDE.md).

- [ ] **Step 3: Implement trigger.** Compact button with `<ProviderIcon type={selectedProvider} />` + model name (or default label) + chevron. Tailwind classes via `twMerge`. Use `DropdownMenuTrigger` (shadcn) as the wrapper so Radix wires up the open/close state.

- [ ] **Step 4: Implement `DropdownMenu` shell.** `DropdownMenuContent` width set generously (e.g., `w-80`) to fit provider icon + name + chevron on one line. Render the controlled `<input>` search at the top, then the "Use {agent|workspace} default" sentinel row, then one `DropdownMenuSub` per provider.

- [ ] **Step 5: Each provider sub.** `DropdownMenuSubTrigger` renders `<ProviderIcon type={provider.type} />` + `provider.name`. Radix auto-appends the right chevron. `DropdownMenuSubContent` (positioned `side="right"` by default) renders one `DropdownMenuItem` per `model` whose `providerId === provider.id`. Click → `onChange(provider.type, model.name)` + close root menu (`setOpen(false)`).

- [ ] **Step 6: Search filter.** Controlled `useState` on the input's value. Filters which `DropdownMenuSub` entries render. Stop `onKeyDown` propagation on the input (`event.stopPropagation()`) so Radix's menu-nav keyboard handler doesn't swallow letter keys. When query is present, sort matching providers ahead of non-matching ones; optionally surface matching models inline under each matching provider (defer to v1.1 if time-constrained — v1 can just filter provider names).

- [ ] **Step 7: `ProviderIcon`.** Lookup `type` → SVG. Reuse SVGs from the existing AI Providers admin page if accessible (grep `client/src` for `OpenAI` / `Anthropic` SVG components). Fallback: circle with first letter.

- [ ] **Step 8:** Run `cd client && npm run test -- model-picker` until green.

---

### Task 7: `ModelPicker` agent-default sentinel + onChange wiring

- [ ] **Step 1: Failing test.** Render with `agentDefaultProvider="ANTHROPIC"` + `agentDefaultModel="claude-3-5-sonnet"` + selected both null. Assert trigger reads "Agent default" (or the agent's model name) instead of workspace default.

- [ ] **Step 2: Failing test.** Selected provider/model set to non-default. Render a "Use {agent|workspace} default" item at the top of the outer page. Click it, assert `onChange(null, null)` fires.

- [ ] **Step 3:** Implement both.

- [ ] **Step 4: Failing test.** Click a model row. Assert `onChange("OPEN_AI", "gpt-4o")` fires with the model's `name` (not `alias`). Assert popover closes.

- [ ] **Step 5:** Implement. Run tests.

---

### Task 8: ESLint, sort-keys, naming conventions

- [ ] **Step 1:** Check `npm run lint -- client/src/shared/components/ai/model-picker/`. Fix any `sort-keys`, `naming-convention`, `sort-import-destructures` violations manually (lint --fix doesn't auto-fix `sort-keys`).
- [ ] **Step 2:** Confirm all interface names end with `I` or `Props` (`ModelPickerPropsI`, `ProviderIconPropsI`, etc.) per CLAUDE.md.

---

## Phase 4 — Client: Personal Agent form refactor

### Task 9: Replace the two Selects in `AiHubPersonalAgentForm` with `ModelPicker`

**Files:**
- Modify: `client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentForm.tsx`

- [ ] **Step 1:** Read [lines 434-507](../../../client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentForm.tsx:434) (the existing two-`Select` block) — confirm wiring: `llmProvider` / `llmModel` local state, `setLlmProvider` / `setLlmModel` setters, the `"default"` sentinel string handling.

- [ ] **Step 2:** Replace the entire `<fieldset className="grid grid-cols-2 gap-3 border-0">…</fieldset>` block with a single `<ModelPicker layout="full" />`.

```tsx
<ModelPicker
    layout="full"
    onChange={(provider, model) => {
        setLlmProvider(provider ?? '');
        setLlmModel(model ?? '');
    }}
    selectedModel={llmModel || null}
    selectedProvider={llmProvider || null}
    workspaceDefaultLabel="Use workspace default"
    workspaceId={currentWorkspaceId ?? 0}
/>
```

- [ ] **Step 3:** Update the existing form test ([AiHubPersonalAgentsList.test.tsx](../../../client/src/pages/automation/ai-hub/personal-agents/tests/AiHubPersonalAgentsList.test.tsx) and/or sibling form tests) to interact with the new picker. If the form has its own dedicated test, update there; if it's only tested through the list, leave behavior tests as-is and rely on the `ModelPicker` unit tests for picker-specific coverage.

- [ ] **Step 4:** Manually verify the form: select provider+model in the picker, save, reload edit form, assert the picker re-renders with the same selection. Then pick "Use workspace default", save, reload, assert selection is null and picker shows "Use workspace default".

- [ ] **Step 5:** Remove now-unused imports (`Select`, `SelectContent`, `SelectItem`, `SelectTrigger`, `SelectValue`, `useWorkspaceAiGatewayModelsQuery`, `useWorkspaceAiGatewayProvidersQuery` — the queries move inside `ModelPicker`).

- [ ] **Step 6:** `npm run check` (lint + typecheck + tests).

---

## Phase 5 — Client: Copilot integration

### Task 10: Extend `useCopilotStore` with model selection

**Files:**
- Modify: `client/src/shared/components/copilot/stores/useCopilotStore.ts`

- [ ] **Step 1:** Add to store shape:
```ts
selectedLlmProvider: string | null;
selectedLlmModel: string | null;
setSelectedLlmModel: (provider: string | null, model: string | null) => void;
resetLlmSelection: () => void;
```

- [ ] **Step 2:** Wire `resetLlmSelection` into the existing conversation-reset path (called when a new conversation starts).

- [ ] **Step 3:** Add a store unit test for the reset behavior (follow existing Zustand test pattern in the file's siblings).

---

### Task 11: Wire `ModelPicker` into `CopilotPanel`

**Files:**
- Modify: `client/src/shared/components/copilot/CopilotPanel.tsx`

- [ ] **Step 1:** Import `ModelPicker`. Place above the chat input toolbar (or beside the send button — defer to the existing input area's layout).

- [ ] **Step 2:** Read `selectedLlmProvider` / `selectedLlmModel` from `useCopilotStore`; `onChange` calls `setSelectedLlmModel`.

- [ ] **Step 3:** Pass `workspaceId={currentWorkspaceId}` (read from the existing workspace store).

- [ ] **Step 4:** Visual smoke check: open Copilot in a running dev server, pick a model, confirm the trigger updates and stays sticky.

---

### Task 12: Send selection via AG-UI state

**Files:**
- Modify: `client/src/shared/components/copilot/runtime-providers/CopilotRuntimeProvider.tsx`

- [ ] **Step 1:** Locate the `agent.runAgent()` / `agent.addMessage()` call ([CopilotRuntimeProvider.tsx:20-62](../../../client/src/shared/components/copilot/runtime-providers/CopilotRuntimeProvider.tsx:20)). Find where the AG-UI `state` map is built.

- [ ] **Step 2:** Inject `userSelectedLlmProvider` / `userSelectedLlmModel` from the store when both are non-null. Omit (don't send empty strings) when null — the server interprets absence as "no override."

- [ ] **Step 3:** End-to-end manual test: pick a model, send a message, watch server logs for `CopilotChatClientResolver` activation; confirm the response uses the picked model (verify via response metadata or a model-specific behavior).

---

## Phase 6 — Client: AI Hub chat integration

### Task 13: Find AI Hub chat panel + its per-conversation store

**Files (read-only research):**
- Grep: `find client/src -path '*ai-hub*chat*' -name '*.tsx'`
- Read: the chat panel component and any AI Hub conversation Zustand stores.

- [ ] **Step 1:** Identify the AI Hub chat panel React component (the conversation view, not the sidebar).
- [ ] **Step 2:** Identify the per-conversation state store. Each conversation row needs its own `(selectedProvider, selectedModel)` because the user can switch conversations and each retains its own selection.

---

### Task 14: Wire `ModelPicker` into AI Hub chat panel

**Files:**
- Modify: the AI Hub chat panel component (identified in Task 13).
- Modify: the AI Hub conversation store (if needed — may already have per-conversation state).

- [ ] **Step 1:** Render `<ModelPicker>` in the chat input toolbar, hidden when `task.kind === 'WORKFLOW_CHAT'`.
- [ ] **Step 2:** For `task.kind === 'PERSONAL_AGENT'`, pass `agentDefaultProvider={task.agent?.llmProvider}` + `agentDefaultModel={task.agent?.llmModel}`.
- [ ] **Step 3:** Selection persists in the conversation's store, keyed by `taskId` / `threadId`.
- [ ] **Step 4:** Inject `userSelectedLlmProvider` / `userSelectedLlmModel` into the AG-UI state on every chat request from this panel.
- [ ] **Step 5:** Visual smoke test: open three AI Hub conversations, set different models on each, switch between them, confirm each retains its own selection. Open a WORKFLOW_CHAT conversation, confirm no picker.

---

## Phase 7 — Verification + checks

### Task 15: Server quality checks

- [ ] `./gradlew spotlessApply`
- [ ] `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:check`
- [ ] `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:check`
- [ ] `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:check`

### Task 16: Client quality checks

- [ ] `cd client && npm run check` (lint + typecheck + tests; fix any sort-keys, naming, or unused-import violations manually)
- [ ] Manually verify `npm run dev` golden paths: Copilot picker, AI Hub picker, Personal Agent form picker.

### Task 17: Commit

Two commits (server + client per [Commit Message Convention](../../../CLAUDE.md)):

- [ ] Server: `Add per-conversation LLM model selection to Copilot and AI Hub`
- [ ] Client: `client - Add ModelPicker for Copilot, AI Hub, and Personal Agent form`

---

## Risk register

| Risk | Likelihood | Mitigation |
|---|---|---|
| Copilot agents don't share a base class that supports `overrideChatClientResolver` | medium | Fall back to duplicating the 22-line override into each of the 3 agent classes (still less code than introducing a new shared module). |
| `DropdownMenuSub` + controlled `<input>` keyboard interaction is fiddly (Radix intercepts arrow/letter keys) | medium | Stop `onKeyDown` propagation on the search input. If that's insufficient, use Radix `Menu.Sub` directly (one level deeper than shadcn's wrapper) and re-wire keyboard focus manually. Fallback: drop the in-menu search for v1, add it in v1.1 once the cascade UX is stable. |
| GraphQL queries return enabled providers but model dropdown shows disabled ones | low | Filter `models.filter(m => providers.find(p => p.id === m.providerId && p.enabled))` in the component. |
| Personal Agent form regression (existing two-Select tests break) | medium | Keep `(llmProvider, llmModel)` form-state shape unchanged; only the UI layer changes. Existing form tests that assert save-payload shape continue to pass. |
| Provider icons missing for newer providers (xAI, Vercel AI Gateway, OpenRouter) | low | Fallback first-letter circle is fine for v1; add real SVGs in a follow-up. |
