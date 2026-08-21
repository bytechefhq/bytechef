# Copilot Panel Interactive Pickers (2b) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax. **Branch `0_732` churns under parallel work — prefer INLINE execution and verify each commit lands on the active branch (see memory `feedback_subagent_worktree_stranding`).**

**Goal:** Give the in-editor Copilot panel the same interactive connection/property pickers AI Hub has — the Copilot workflow-editor agent emits the shared `select-*`/`ask-user-question`/`create-connection` tool markers, and the Copilot chat thread renders them as pickers whose choice returns as the user's next message.

**Architecture:** Server — register the shared `ai-copilot-tool` picker callbacks on the Copilot `WorkflowEditorSpringAIAgent` and populate a neutral `AgentToolInvocationContext` (workspaceId/userId/environmentId; conversationId stays null — no AI Hub task) into its ToolContext, **deriving userId server-side from the authenticated request** (never the client). Client — the generic `<Thread>` gains an optional `dataComponents` registry prop, `CopilotPanel` passes the shared `aiChatDataComponents`, and `CopilotRuntimeProvider` intercepts tool-result events through the shared `toToolResultDataPart` mapper (reusing the 2a shared `ai-chat` module).

**Tech Stack:** Java 25 / Spring Boot 4 / Spring AI tool API (server, EE under `server/ee/`), React 19 / TS / Vitest (client). 2a (shared `ai-chat` client module) is already merged and is the foundation.

---

## Critical facts established by investigation (do not re-derive)

- **2a is done.** Shared client module `client/src/shared/components/ai-chat/` exports: `toToolResultDataPart` + `parseJson` (`messages/toToolResultDataPart.ts`); `aiChatDataComponents` registry of 5 kinds (`messages/aiChatDataComponents.tsx`); stores `aiChatToolCallStore`/`useAiChatToolCallStore`, `aiChatRetryableErrorStore`/`useAiChatRetryableErrorStore`, `useAiChatAskedQuestionsStore` (`stores/`). AI Hub already consumes them.
- **Shared server picker tools** live in `ai-copilot-tool` (`com.bytechef.ee.ai.copilot.tool`): `ListConnectionsForComponentToolCallback`, `SelectConnectionToolCallback`, `LookupActionPropertyOptionsToolCallback`, `LookupTriggerPropertyOptionsToolCallback`, `SelectPropertyOptionToolCallback`, `SelectTriggerPropertyOptionToolCallback`, `AskUserQuestionToolCallback`, `CreateConnectionToolCallback`, plus the `PropertyOptionsResolver` and `ToolStateVisibilityMetrics` (has a `NOOP`). They read **only** `AgentToolInvocationContext.fromToolContext(toolContext)` for workspaceId/userId/environmentId, and `ListConnectionsForComponentToolCallback` errors with "Workspace context unavailable" when `invocationContext == null || workspaceId == null`. `PropertyOptionsResolver.withUserSecurityContext(userId, action)` rehydrates a Spring SecurityContext from `userId` via `SecurityUtils.runAs` — so **populating the ToolContext with workspaceId+userId+environmentId is sufficient**; no global security wrapper is needed.
- **Copilot tools run on Reactor scheduler threads without the HTTP SecurityContext** — so the agent thread cannot call `SecurityUtils.getCurrentUserId()`. userId must be captured on the request thread and threaded through the agent state.
- **Copilot agent toolContext today:** `WorkflowEditorSpringAIAgent.toolContext(input)` (`server/ee/libs/ai/ai-copilot/ai-copilot-service/.../agent/WorkflowEditorSpringAIAgent.java:96-99`) returns `CopilotToolContextUtils.toToolContext(state)` which carries ONLY `allowedComponentNames`.
- **Model to mirror for ToolContext population:** `WorkflowExecutionSpringAIAgent.toolContext` (`.../agent/WorkflowExecutionSpringAIAgent.java:130-143`) reads `state.get("parameters").{workspaceId,environmentId}` and puts them into ToolContext via `putLong`. And `AiHubSpringAIAgent.toolContext` (`server/ee/libs/ai/ai-hub/ai-hub-service/.../agent/AiHubSpringAIAgent.java:156-166,237-254`) builds `AgentToolInvocationContext` from controller-verified state keys (`VERIFIED_WORKSPACE_ID`, `AUTHENTICATED_USER_ID`, `environmentId`, `VERIFIED_THREAD_ID`).
- **Copilot request state (client → server):** `CopilotRuntimeProvider` (`client/src/shared/components/copilot/runtime-providers/CopilotRuntimeProvider.tsx:76-88`) sends `environmentId: String(currentEnvironmentId ?? 0)` + state contributors (`workflowId`, `currentSelectedNode`) + optional selected LLM. It does NOT send `workspaceId` or `userId`. `CopilotPanel` has `currentWorkspaceId` available (used for ModelPicker).
- **Copilot REST entry:** `CopilotApiController` (`server/ee/libs/ai/ai-copilot/ai-copilot-rest/.../web/rest/CopilotApiController.java:63-125`) — `POST /internal/ai/chat/{agentId}`, runs on the authenticated request thread, extracts `state.get("mode")`/`state.get("workflowId")` for auth; passes `state` straight to `agUiService.runAgent`. It does NOT inject workspace/user today.
- **Copilot agent beans:** `CopilotConfiguration` (`.../config/CopilotConfiguration.java:268-296` ASK, `300-324` BUILD) build `WorkflowEditorSpringAIAgent` with a `List<Object> tools`. Adding tools = add bean params + append to that list. The `ai-copilot-tool` callbacks may not be component-scanned into the copilot app context — VERIFY (Task 1 Step 0); if not, instantiate them in `CopilotConfiguration` `@Bean` methods (they need facades/services + `PropertyOptionsResolver` + a `ToolStateVisibilityMetrics`).
- **Generic `<Thread>`:** `client/src/components/assistant-ui/thread.tsx` — `ThreadPropsI` at lines 64-79; renders `<MessagePrimitive.Parts />` (~line 536) with no data registry. Callers: `CopilotPanel.tsx`, `AiSkillCreateWithAi.tsx`, `WorkflowTestChatPanel.tsx` (the latter two pass nothing → must stay unchanged). AI Hub uses its OWN `AiHubThread`, not this one.
- **Pattern to mirror for the data registry:** `AiHubMessageContent.tsx:28-42` renders `MessagePrimitive.Parts components={{..., data: {by_name: aiChatDataComponents}, ...}}`.
- **Client tool-result handling to mirror:** `AiHubRuntimeProvider.tsx` — `onToolCallStartEvent` records `toolCallNamesById`; `onToolCallResultEvent` (~lines 519,825-848) calls `toToolResultDataPart(toolCallName, event.content)` and either `addMessage({content:[{data, type}], role:'assistant'})` on ok, or sets `aiChatRetryableErrorStore` + `aiChatToolCallStore.completeToolCall(..., true)` on `!ok`.

## Security design (the load-bearing decision)

`userId` is an authorization principal — it drives `withUserSecurityContext`, which grants the tool the user's authorities. **It MUST be derived server-side from the authenticated request, never read from the client-supplied state.** `workspaceId` is client-contributed (the editor knows its workspace) but every privileged read runs under `withUserSecurityContext(userId)` and the workspace-connection facade enforces the user's access — so a forged workspaceId only ever exposes connections the authenticated user may already see. `environmentId` is low-sensitivity and already client-sent. `conversationId` stays null for Copilot (no AI Hub task; the artifact recorder and AI-Hub-only tools no-op).

Mechanism (mirrors AI Hub's verified-state keys): `CopilotApiController`, on the request thread, resolves the authenticated user id via `SecurityUtils` and puts it into the run `state` under a server-only key BEFORE `runAgent`. The client-supplied `workspaceId`/`environmentId` are read from state by the agent. The agent's `toolContext()` assembles `AgentToolInvocationContext` from these.

---

## File Structure

**Server (modify):**
- `ai-copilot-rest/.../web/rest/CopilotApiController.java` — inject authenticated userId into run state.
- `ai-copilot-service/.../util/CopilotToolContextUtils.java` — emit neutral `AgentToolInvocationContext` keys.
- `ai-copilot-service/.../config/CopilotConfiguration.java` — register the 8 picker tools (+ `PropertyOptionsResolver`, `ToolStateVisibilityMetrics`) on both `WorkflowEditor` agents.
- `ai-copilot-service/src/main/resources/prompt_workflow_editor_build.txt` (+ `..._ask.txt`) — note instructing use of the pickers.

**Server (create test):**
- `ai-copilot-service/src/test/.../util/CopilotToolContextUtilsTest.java` (extend existing) — neutral keys emitted.

**Client (modify):**
- `client/src/components/assistant-ui/thread.tsx` — optional `dataComponents` prop.
- `client/src/shared/components/copilot/CopilotPanel.tsx` — pass `aiChatDataComponents` + contribute `workspaceId`.
- `client/src/shared/components/copilot/runtime-providers/CopilotRuntimeProvider.tsx` — tool-result interception; send `workspaceId` in state.

**Client (create test):**
- `client/src/components/assistant-ui/tests/thread.test.tsx` — `dataComponents` renders a data part.

---

## Task 1: Server — populate neutral context + verify tool beans

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/util/CopilotToolContextUtils.java`
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java`
- Test: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/util/CopilotToolContextUtilsTest.java`

- [ ] **Step 0 (CONFIRMED): userId resolution.** `SecurityUtils` (`server/libs/platform/platform-api/.../security/util/SecurityUtils.java`) has **no** `getCurrentUserId()`/`fetchCurrentUserId()` — only `fetchCurrentUserLogin(): Optional<String>` / `getCurrentUserLogin(): String` (+ `runAs`). So the controller must resolve userId from the login: `SecurityUtils.fetchCurrentUserLogin()` → `UserService.fetchUserByLogin(login)` (or equivalent) → `user.getId()`. Inject `UserService` (`com.bytechef.platform.user.service.UserService`, in `platform-user-api`/`-service`) into `CopilotApiController` (confirm `fetchUserByLogin`/`getUser` accessor name on `UserService`). The controller already runs on the authenticated request thread.

- [ ] **Step 1: Write the failing test** for `CopilotToolContextUtils` emitting neutral keys.

```java
// Replace/extend CopilotToolContextUtilsTest with a case asserting neutral keys are emitted.
@Test
void testEmitsAgentToolInvocationContextKeys() {
    State state = new State();
    state.put("bytechef.copilot.authenticatedUserId", 42L);
    state.put("workspaceId", 7L);
    state.put("environmentId", "2");

    Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(state);

    assertThat(toolContext)
        .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, 42L)
        .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L)
        .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L);
}
```
> Use the real `State` type (`com.agui.core.state.State`) the existing test uses; match the existing test's construction idiom. Import `com.bytechef.ee.ai.copilot.tool.AgentToolInvocationContext`. Confirm the existing test's first case (allowedComponentNames passthrough) still passes after Step 2 — keep that behavior.
> Add `ai-copilot-tool` as a dependency of `ai-copilot-service` if not already present (it is, via the agents). Verify with `grep ai-copilot-tool server/ee/libs/ai/ai-copilot/ai-copilot-service/build.gradle.kts`.

- [ ] **Step 2: Run the test — expect FAIL** (`./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "*CopilotToolContextUtilsTest"`). Neutral keys not emitted.

- [ ] **Step 3: Implement `CopilotToolContextUtils`** — keep the allowedComponentNames entry AND add the neutral keys (workspaceId/userId/environmentId; conversationId omitted/null for Copilot). Define a server-only state key constant for the authenticated user id (e.g. `public static final String STATE_AUTHENTICATED_USER_ID = "bytechef.copilot.authenticatedUserId";`).

```java
public static Map<String, Object> toToolContext(@Nullable State state) {
    if (state == null) {
        return Map.of();
    }

    Map<String, Object> toolContext = new HashMap<>();

    Object allowedComponentNames = state.get(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY);

    if (allowedComponentNames != null) {
        toolContext.put(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, allowedComponentNames);
    }

    Long workspaceId = asLong(state.get("workspaceId"));
    Long userId = asLong(state.get(STATE_AUTHENTICATED_USER_ID));
    Long environmentId = asLong(state.get("environmentId"));

    toolContext.putAll(
        new AgentToolInvocationContext(workspaceId, userId, environmentId, null).toToolContext());

    return toolContext;
}
```
Add a private `asLong(Object)` helper mirroring `AgentToolInvocationContext.asLong` semantics (Number → longValue; numeric String → parse; else null). `toToolContext()` on the record already omits null fields, so partial context is fine.

- [ ] **Step 4: Inject the authenticated userId in `CopilotApiController`** — on the request thread, before `agUiService.runAgent`, resolve the user id via the accessor from Step 0 and put it in the run state under `CopilotToolContextUtils.STATE_AUTHENTICATED_USER_ID`. Do NOT read userId from the client payload. (If the AG-UI `State`/`AgUiParameters` is immutable, set it the same way the controller already mutates/reads state — match the existing pattern in this controller.)

```java
// after resolving agUiParameters, before runAgent:
SecurityUtils.fetchCurrentUserId()  // exact name from Step 0
    .ifPresent(userId -> agUiParameters.state().put(CopilotToolContextUtils.STATE_AUTHENTICATED_USER_ID, userId));
```
> If `state()` is null on some requests, guard with a null check / initialize per the controller's existing handling.

- [ ] **Step 5: Run the test — expect PASS.** Then `:ai-copilot-service:spotlessApply` + `:ai-copilot-rest:spotlessApply`.

- [ ] **Step 6 (CONFIRMED): the picker callbacks are plain classes, NOT `@Component`.** `SelectConnectionToolCallback`, `ListConnectionsForComponentToolCallback`, `AskUserQuestionToolCallback`, `CreateConnectionToolCallback`, the 2 lookup + 2 select-property callbacks must be **`new`-ed** in `CopilotConfiguration` (exactly as `AiHubConfiguration` does, e.g. `new SelectConnectionToolCallback(componentDefinitionService, jsonMapper)`, `new AskUserQuestionToolCallback(metrics, jsonMapper)`, `new ListConnectionsForComponentToolCallback(...)`). Only `PropertyOptionsResolver` is `@Component` (auto-wireable). For Task 2: read each callback's constructor in `ai-copilot-tool` and gather its deps; the exact dep set + construction is mirrored in `AiHubConfiguration` (`new ...ToolCallback(...)` at lines ~288, 476, 494, 756-762 and the lookup/select-property registrations) — copy that. Pass `ToolStateVisibilityMetrics.NOOP` for the metrics arg (Copilot has no metrics bean).

- [ ] **Step 7: Commit** (`git add` the 3 files; fresh commit `0_732 Populate neutral agent-tool context for Copilot editor agent`).

---

## Task 2: Server — register picker tools on the Copilot editor agents

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java`
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_editor_build.txt`
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_editor_ask.txt`

- [ ] **Step 1:** Based on Task 1 Step 6, obtain the 8 picker callbacks + `PropertyOptionsResolver` either as `@Bean`-method parameters (if component-scanned) or by constructing them. Append all 8 to the `List<Object> tools` in BOTH `workflowEditorAskSpringAIAgent` (lines ~278-281) and `workflowEditorBuildSpringAIAgent` (lines ~316-319). Pass a `ToolStateVisibilityMetrics` — use `ToolStateVisibilityMetrics.NOOP` unless a copilot metrics bean exists.
> The picker callbacks' constructor deps (connection facades, `ActionDefinitionFacade`/`TriggerDefinitionFacade`, `PropertyOptionsResolver`, user/authority services) are the same beans AI Hub wires in `AiHubConfiguration#registerToolAttachStateVisibilityToolCallbacks` — copy that wiring as the reference. Reuse one shared `PropertyOptionsResolver` bean.

- [ ] **Step 2:** Add a prompt note (both build + ask prompts) instructing the editor agent to use `selectConnection` for choosing a connection, `selectPropertyOption`/`selectTriggerPropertyOption` for dynamic option properties (so it never hallucinates channel ids), and `askUserQuestion` for free-form clarifications, when building via chat. Mirror the wording in `prompt_ai_hub_build.txt`'s lookup/picker paragraph.

- [ ] **Step 3:** Build + start-up wiring check: `./gradlew :server:apps:server-app:compileJava` and (if a copilot app context test exists) the agent wiring test. Add a wiring test asserting the editor agent's tool catalog contains the picker tool names (mirror `AiHubSpringAIAgentTest` if it asserts the catalog; else assert via the builder `tools` list in a `CopilotConfiguration` slice test).

- [ ] **Step 4:** `:ai-copilot-service:check`. Commit (`0_732 Register shared connection/property pickers on Copilot editor agent`).

---

## Task 3: Client — generic `<Thread>` gains optional `dataComponents`

**Files:**
- Modify: `client/src/components/assistant-ui/thread.tsx`
- Test: `client/src/components/assistant-ui/tests/thread.test.tsx`

- [ ] **Step 1: Failing test (Vitest):** render `<Thread dataComponents={{'select-connection': () => <div data-testid="picker"/>}}>` inside the required assistant-ui runtime, feed an assistant message carrying a `data-select-connection` part, assert `getByTestId('picker')` renders. (Mirror the assistant-ui test harness used by existing `ai-chat` renderer tests for providing a runtime + message.)
> If standing up the full runtime in a unit test is heavy, instead assert structurally that when `dataComponents` is passed, `MessagePrimitive.Parts` receives `components.data.by_name === dataComponents` (shallow render / prop spy). Pick the lighter approach that still proves the wiring.

- [ ] **Step 2: Run — FAIL** (`cd client && npx vitest run src/components/assistant-ui/tests/thread.test.tsx`).

- [ ] **Step 3: Implement.** Add to `ThreadPropsI`:
```ts
dataComponents?: Record<string, ComponentType>;
```
Thread it down to the assistant message renderer and wire:
```tsx
<MessagePrimitive.Parts
    components={dataComponents ? {data: {by_name: dataComponents}} : undefined}
/>
```
Keep existing `components` (Text/Source/tools) if the assistant message already sets any; merge rather than replace. Existing callers pass no `dataComponents` → `undefined` → unchanged behavior.

- [ ] **Step 4: Run — PASS.** Then `npm run check` (lint+typecheck+tests) in `client/`. Commit (`5169 client - Add optional dataComponents registry to Thread` — use the active client ticket prefix; confirm with the user/most-recent client commit).

> NOTE on commit prefix: client commits use `<ticket> client - <desc>`. Use the ticket the user is on for this work; if unknown, ask or match the most recent `client -` commit's number.

---

## Task 4: Client — CopilotPanel passes the registry; provider intercepts tool results

**Files:**
- Modify: `client/src/shared/components/copilot/CopilotPanel.tsx`
- Modify: `client/src/shared/components/copilot/runtime-providers/CopilotRuntimeProvider.tsx`

- [ ] **Step 1: CopilotPanel** — import `aiChatDataComponents` and pass `dataComponents={aiChatDataComponents}` to `<Thread>`. Also contribute `workspaceId` so the server can populate context: add `currentWorkspaceId` to the state the provider sends (see Step 2) — CopilotPanel already resolves `currentWorkspaceId`.

```tsx
import {aiChatDataComponents} from '@/shared/components/ai-chat/messages/aiChatDataComponents';
// ...
<Thread
    composerActions={...}
    dataComponents={aiChatDataComponents}
    leadingComposerActions={...}
/>
```

- [ ] **Step 2: CopilotRuntimeProvider** — (a) add `workspaceId: String(currentWorkspaceId)` (and keep `environmentId`) to `stateToSend` (lines ~76-88); (b) track tool-call names and intercept results, mirroring AiHubRuntimeProvider:

```ts
const toolCallNamesById = new Map<string, string>();

const subscriber: AgentSubscriber = {
    onTextMessageContentEvent: ({event, textMessageBuffer}) => {
        appendToLastAssistantMessage(textMessageBuffer + event.delta);
    },
    onTextMessageEndEvent: ({textMessageBuffer}) => {
        appendToLastAssistantMessage(textMessageBuffer);
    },
    onToolCallStartEvent: ({event}) => {
        toolCallNamesById.set(event.toolCallId, event.toolCallName);
    },
    onToolCallResultEvent: ({event}) => {
        const toolCallName = toolCallNamesById.get(event.toolCallId);
        toolCallNamesById.delete(event.toolCallId);

        const dataPart = toToolResultDataPart(toolCallName ?? '', event.content);

        if (!dataPart) {
            return;
        }

        if (!dataPart.ok) {
            aiChatRetryableErrorStore.getState().setError({
                errorMessage: dataPart.errorMessage,
                lastUserMessage: getLastUserMessage(),
                toolName: dataPart.toolName,
            });

            return;
        }

        addMessage({content: [{data: dataPart.data, type: dataPart.type as `data-${string}`}], role: 'assistant'});
    },
};
```
> Use the exact event-field names AiHubRuntimeProvider uses (`event.toolCallName` / `event.toolCallId` / `event.content`) — copy them verbatim from AiHubRuntimeProvider so the AG-UI types match. Resolve `addMessage`/`getLastUserMessage` the same way the Copilot provider already exposes message mutation (mirror how `appendToLastAssistantMessage` is obtained). Import the shared store + mapper from `@/shared/components/ai-chat/...`.

- [ ] **Step 3:** `npm run check` in `client/`. If a CopilotRuntimeProvider/CopilotPanel test exists, extend it to assert a `select-property-option` tool result becomes a `data-select-property-option` message; otherwise add a focused test mirroring the AI Hub provider test. Commit (`<ticket> client - Wire interactive pickers into Copilot panel`).

---

## Task 5: End-to-end verification

- [ ] `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:check :server:apps:server-app:compileJava` — green.
- [ ] `cd client && npm run check` — green.
- [ ] Manual smoke (if dev stack up): open the workflow editor Copilot panel, ask it to add a Slack action and pick a channel — confirm a channel **picker renders in the Copilot thread** (real channels, not hallucinated) and selecting one sends the channel id; confirm a connection picker renders when a component needs a connection. Confirm the three existing `<Thread>` callers (AiSkillCreateWithAi, WorkflowTestChatPanel) are unchanged.

---

## Self-Review (plan author)

- **Spec coverage** (spec `2026-06-10-copilot-panel-interactive-pickers-design.md`): server tool registration → Task 2; neutral context population → Task 1; prompt note → Task 2; `<Thread>` `dataComponents` prop + existing callers unaffected → Task 3; CopilotPanel passes registry → Task 4; provider tool-result interception via shared mapper + common stores → Task 4; tests → Tasks 1,3,4 + wiring test Task 2. ✓
- **Security**: userId server-derived (Task 1 Step 4), never client; workspaceId client-contributed but gated by `withUserSecurityContext(userId)`. Documented. ✓
- **Open items deliberately left as in-task investigations (not placeholders):** exact `SecurityUtils` accessor name (Task 1 Step 0); whether picker beans are component-scanned vs must be `new`-ed (Task 1 Step 6); the lightest viable `<Thread>` test harness (Task 3 Step 1); the client ticket prefix (Task 3 Step 4 note). Each has a concrete resolution path.
- **Type consistency:** `AgentToolInvocationContext` 4-arg ctor `(workspaceId, userId, environmentId, conversationId)` used consistently; `toToolResultDataPart(toolCallName, content)` → `{ok,type,data}|{ok:false,toolName,errorMessage}` matches 2a.
