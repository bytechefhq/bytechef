# Resumable AI Agent Tool Calls (Human-in-the-Loop)

**Date:** 2026-05-20
**Scope:** Make suspending tools work inside the AI agent. Today a tool that calls `context.suspend()` while invoked by `AiAgentChatAction` / `AiAgentStreamChatAction` does not suspend the agent's workflow task — the agent's tool-calling loop runs to completion with a placeholder result and the LLM never sees the real human input. This spec introduces a resumable tool-calling loop so that when a tool suspends, the agent's workflow task suspends; when a human responds, the agent resumes, the real result is fed back to the LLM as the pending tool call's result, and the LLM continues reasoning. The two first-class consumers are `requestApproval` (the `approval` component's tool cluster element) and `askUserQuestion` (the `ai/agent/utils` tool).

## Problem

`AiAgentChatAction.perform` runs the LLM tool-calling loop via Spring AI's `ChatClient.call()` + `ToolCallAdvisor`. The advisor loop is: call the model → if the response has tool calls, run `ToolCallingManager.executeToolCalls(...)` → feed results back → repeat until no tool calls.

When a tool calls `context.suspend(...)`:

- The suspend is recorded on the agent's `ActionContext`, but the tool still returns a value (a placeholder) to the loop.
- The `ToolCallAdvisor` loop has no notion of suspension — it feeds the placeholder back to the LLM, which produces a final (meaningless) response.
- Only *after* `perform` returns does `ActionDefinitionServiceImpl.checkSuspend` observe `actionContext.getSuspend()` and suspend the task.

Net effect: the LLM consumed a placeholder and already "finished" before the task suspended. On resume, `AiAgentChatAction.resumePerform` is a stub (`return ResumeResponse.of(data.toMap())`) — it returns the human's answer as the action output and never re-enters the LLM loop.

`requestApproval` is worse off than `askUserQuestion`: it is a `MultipleConnectionsToolFunction`, wrapped by `AiAgentToolFacade` as a plain `Function<Map,Object>` that never receives Spring AI's `ToolContext`, so `ApprovalRequestApprovalTool` cannot even reach the agent's `ActionContext` — it suspends a detached throwaway context that nobody reads, and the LLM receives `null`.

Streaming is broken for *both* tools: in `AiAgentStreamChatAction`, `perform` returns an `SseEmitterHandler` immediately and the `Flux` (model call + tool execution) is consumed later inside `SseStreamTaskExecutionPostOutputProcessor`. `checkSuspend` runs against the handler before the `Flux` is ever subscribed, so the suspend is never observed.

## Out of scope

- The subflow path (`WorkflowCallWorkflowTool` calling a suspending sub-workflow via `SubflowSyncExecutor`) — tracked separately as GitHub issue #5055.
- `AiAgentRealtimeChatAction` (realtime/voice) — realtime sessions have their own lifecycle; not addressed here.
- New product surfaces for approval (no new approval form UI); the existing approval form and SSE event patterns are reused.

## Architecture overview

A per-invocation decorator, **`SuspendableToolCallingManager`**, wraps the injected `ToolCallingManager` and carries the agent's `ActionContext`. It is wired into `ToolCallAdvisor.builder().toolCallingManager(...)` in place of the raw bean, so `perform` itself is almost unchanged.

```
perform()
  └─ ChatClient.call()/.stream()  →  ToolCallAdvisor loop
        └─ SuspendableToolCallingManager.executeToolCalls(prompt, chatResponse)
              ├─ delegate.executeToolCalls(...)        // runs the tool batch
              ├─ if actionContext.getSuspend() == null → return delegate result (loop continues)
              └─ else → capture conversation, enrich Suspend, return returnDirect=true (loop halts)
```

`ToolCallAdvisor` honors `ToolExecutionResult.returnDirect()` to break its loop in **both** the sync (`spring-ai-client-chat` `ToolCallAdvisor` line ~158) and streaming (line ~332) paths — verified against Spring AI 2.0.0-M6 sources. So returning `returnDirect=true` is the supported way to halt the loop at the suspend point.

On suspend, the decorator serializes the conversation accumulated so far into the `Suspend.continueParameters`. `resumePerform` deserializes it, patches the suspended tool's placeholder result with the human's real answer, and re-runs `ChatClient.call()/.stream()` seeded with that conversation — the `ToolCallAdvisor` loop continues naturally and may itself suspend again (multi-round HITL).

New/changed units:

| Unit | Location | Role |
|---|---|---|
| `SuspendableToolCallingManager` | `server/libs/modules/components/ai/agent/.../tool/` | `ToolCallingManager` decorator: detect suspend, capture conversation, halt loop |
| `ConversationState` | `server/libs/modules/components/ai/agent/.../tool/` | Serializable representation of the message list for `continueParameters` |
| `SuspendAwareSseEmitterHandler` | `server/libs/platform/platform-component/platform-component-api/.../definition/` | `SseEmitterHandler` wrapper carrying `ActionContextAware` for the streaming post-stream suspend check |
| `ToolSuspendConstants` | `server/libs/platform/platform-ai/platform-ai-api/.../constant/` | The sentinel result string + `continueParameters` keys |
| `AbstractAiAgentChatAction` | (modified) | Wire `SuspendableToolCallingManager`; add a resume entry point |
| `AiAgentChatAction` / `AiAgentStreamChatAction` | (modified) | Real `resumePerform` |
| `AiAgentToolFacade` | (modified) | Give `MultipleConnectionsToolFunction` / `ToolFunction` callbacks the `ToolContext` |
| `ActionDefinitionServiceImpl` | (modified) | Streaming branch wraps the handler instead of eagerly `checkSuspend` |
| `SseStreamTaskExecutionPostOutputProcessor` (worker + job-sync) | (modified) | Post-stream suspend check |
| `ApprovalRequestApprovalTool` | (modified) | Suspend the agent's context; emit sentinel |
| `AiAgentUtilsAskUserQuestionTool` | (modified) | Migrate to the sentinel suspend protocol |

## 1. `SuspendableToolCallingManager`

A decorator implementing Spring AI's `ToolCallingManager` (a two-method interface: `resolveToolDefinitions`, `executeToolCalls`). Constructed per `perform`/`resumePerform` invocation with the delegate (`DefaultToolCallingManager` bean) and the agent's `ActionContext`.

```java
final class SuspendableToolCallingManager implements ToolCallingManager {

    private final ToolCallingManager delegate;
    private final ActionContextAware actionContext;

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        ToolExecutionResult result = delegate.executeToolCalls(prompt, chatResponse);

        ActionContext.Suspend suspend = actionContext.getSuspend();

        if (suspend == null) {
            return result;
        }

        // A tool in this batch suspended. Capture the full conversation and the pending
        // tool-call id, enrich the Suspend, and halt the ToolCallAdvisor loop.
        List<Message> conversation = result.conversationHistory();
        String pendingToolCallId = findSentinelToolResponseId(conversation);

        ConversationState conversationState = ConversationState.from(conversation);

        Map<String, Object> continueParameters = new HashMap<>(suspend.continueParameters());
        continueParameters.put(ToolSuspendConstants.CONVERSATION_STATE, conversationState);
        continueParameters.put(ToolSuspendConstants.PENDING_TOOL_CALL_ID, pendingToolCallId);

        actionContext.suspend(new ActionContext.Suspend(continueParameters, suspend.expiresAt()));

        return ToolExecutionResult.builder()
            .conversationHistory(conversation)
            .returnDirect(true)
            .build();
    }
}
```

**Why delegate-then-check (rather than running tools individually):** `DefaultToolCallingManager.executeToolCalls` runs the whole tool batch and builds a protocol-correct `conversationHistory` (assistant message with tool calls + one `ToolResponseMessage`). Letting it run the full batch means sibling tools execute exactly once and their real results are captured; only the suspending tool yields the sentinel. This makes multi-tool-call turns work without special handling (Section 5).

After `perform` returns, the existing `ActionDefinitionServiceImpl.checkSuspend` sees the enriched `Suspend` on the agent context and suspends the agent's workflow task — unchanged for the sync action.

## 2. Reaching the agent's `ActionContext` from a `MultipleConnectionsToolFunction`

`requestApproval` must `suspend()` the agent's context. Today `AiAgentToolFacade.getFunctionToolCallback(...)` builds the callback around a plain `Function<Map<String,Object>,Object>`, so the wrapped function never receives the `ToolContext` and cannot read `AiAgentToolContextKey.ACTION_CONTEXT`.

Change: build the `FunctionToolCallback` around a `BiFunction<Map<String,Object>,ToolContext,Object>` (supported by Spring AI's `FunctionToolCallback`). The bi-function reads `AiAgentToolContextKey.ACTION_CONTEXT` from the `ToolContext` and passes the agent `ActionContext` into a new `ClusterElementDefinitionService.executeTool(...)` overload that makes it available to the tool function. This applies to both `MultipleConnectionsToolFunction` and `ToolFunction` tool wiring, so any single/multi-connection tool can suspend — not just `requestApproval`.

`ToolCallbackProviderFunction` tools (e.g. `askUserQuestion`) already receive the `ToolContext` and already reach `ACTION_CONTEXT`; they need no change here. Converting `requestApproval` to a `ToolCallbackProviderFunction` is explicitly rejected: that route receives neither `extensions` nor multiple `componentConnections`, both of which `requestApproval` needs for its approval-channel cluster elements.

## 3. Conversation serialization (`ConversationState`) and the suspend sentinel

Spring AI `Message` types (`SystemMessage`, `UserMessage`, `AssistantMessage` with `toolCalls`, `ToolResponseMessage`) are external classes; round-tripping them through `TaskState`'s Jackson layer is fragile. We define our own `ConversationState` record — an ordered list of message DTOs we control — with explicit `from(List<Message>)` and `toMessages()` mappers. `ConversationState` is what gets stored under `continueParameters[CONVERSATION_STATE]`, and `TaskState` persists it with type fidelity via the existing `TaskStateValue` converters.

The suspending tool returns a **sentinel string** (`ToolSuspendConstants.SUSPENDED_SENTINEL`, e.g. `"__bytechef_tool_suspended__"`) as its tool result. After `delegate.executeToolCalls`, the decorator scans the trailing `ToolResponseMessage` for the `ToolResponse` whose content equals the sentinel; that `ToolResponse.id()` is the pending tool-call id, stored under `continueParameters[PENDING_TOOL_CALL_ID]`.

`ToolSuspendConstants` (in `platform-ai-api`, alongside `AiAgentToolContextKey`) defines `SUSPENDED_SENTINEL`, `CONVERSATION_STATE`, `PENDING_TOOL_CALL_ID` — shared by the agent module, the approval component, and `ai/agent/utils`.

## 4. Resume: re-entering the loop

`resumePerform` receives `continueParameters` (carrying `ConversationState` + `pendingToolCallId`) and `data` (the human's answer). It is implemented once in `AbstractAiAgentChatAction` and called by both chat actions:

1. Deserialize `ConversationState` → `List<Message>`.
2. In the trailing `ToolResponseMessage`, replace the `ToolResponse` whose `id` equals `pendingToolCallId` — its sentinel content is swapped for the serialized `data`. The result is a protocol-correct conversation (assistant `tool_call`s + matching `ToolResponse`s, all real).
3. Rebuild the `ChatClientRequestSpec` exactly as `perform` does (same model, tools, advisors, `SuspendableToolCallingManager`) but seeded with `.messages(reconstructedConversation)` instead of fresh input. `getChatClientRequestSpec` gains an overload accepting an explicit message list.
4. Re-run `.call()` (sync) / `.stream()` (streaming). The `ToolCallAdvisor` loop continues — the model sees the real tool result and reasons onward, and may call more tools or suspend again.

Re-suspension works recursively: a second suspend simply produces another enriched `Suspend` and another `resumePerform` cycle.

## 5. Multiple tool calls in one turn

Because the decorator lets the delegate run the entire tool batch, an assistant turn with `[toolA, requestApproval, toolB]` produces real results for `toolA`/`toolB` and the sentinel for `requestApproval`, all in one captured conversation. On resume only the sentinel `ToolResponse` is patched; `toolA`/`toolB` are not re-run. v1 supports **one** suspending tool per assistant turn; if two tools in a batch both leave a sentinel, `resumePerform` cannot disambiguate — the decorator detects multiple sentinels and fails the turn with a clear error rather than corrupting the conversation.

## 6. Streaming suspend hand-off

In `AiAgentStreamChatAction`, the model call + tool execution run inside the `Flux` consumed by `SseStreamTaskExecutionPostOutputProcessor`, *after* `perform` returned and `checkSuspend` already ran. The suspend check must move to where the streamed work actually finishes.

1. `ActionDefinitionServiceImpl`'s streaming branches (`executeMultipleConnectionsStreamPerform`, `executeMultipleConnectionsSseStreamResponsePerform`) no longer call `checkSuspend` eagerly. Instead they wrap the returned `SseEmitterHandler` in `SuspendAwareSseEmitterHandler`, which also carries the `ActionContextAware`.
2. Both `SseStreamTaskExecutionPostOutputProcessor`s (the `platform-worker` one and the `platform-job-sync` one) — after `handler.handle(emitter)` + `awaitCompletion()` — check the carried `actionContext.getSuspend()`. If set, they run the `checkSuspend` logic (mint resume URL / `JobResumeId`) and return the resulting `Suspend` object so `SuspendTaskExecutionPostOutputProcessor` stamps `JOB_RESUME_ID`/`SUSPEND` and the job goes to `STOPPED`. This requires the SSE processor to run before the suspend processor in the post-output chain (a reordering in `JobSyncExecutor` and the worker wiring), or for the SSE processor to invoke the suspend-stamping directly.
3. The client learns the turn paused via an SSE event carrying the resume URL, emitted *before* the stream closes — the existing `AiAgentUtilsAskUserQuestionTool.sendQuestionEvent` pattern, reused by `requestApproval`.

The `SuspendableToolCallingManager` is identical in streaming — `ToolCallAdvisor`'s stream loop honors `returnDirect` the same way. Only the suspend *hand-off* differs between sync and streaming.

## 7. Streaming resume delivery

The **existing** resume endpoint — `JobResumeController` `POST /job/resume/{id}` — is extended; no new endpoint is added. Today it returns `204 No Content`. For a streaming-agent resume it instead returns `text/event-stream`: the controller registers an `SseStreamBridge` for the job, triggers the resume, and relays the resumed turn's stream events to the HTTP response over the same request.

- The client POSTs the human's answer to the resume URL with `Accept: text/event-stream`.
- The controller registers an `SseStreamBridge` for the job id, then triggers the resume (`JobResumeFacade.resumeJob`).
- `AiAgentStreamChatAction.resumePerform` produces an `SseEmitterHandler`; the `SseStreamTaskExecutionPostOutputProcessor` drives it and forwards events to the registered bridge, which the controller relays to the HTTP `SseEmitter`.
- Non-streaming resumes — or resume requests without the `text/event-stream` `Accept` header — keep the existing `204` behavior.

The continued turn streams back over the resume request itself: no client re-connection, no new endpoint.

## 8. `ApprovalRequestApprovalTool` changes

`ApprovalRequestApprovalTool` stays a `MultipleConnectionsToolFunction` (keeps approval-channel `extensions` + multi-connection support). With Section 2 in place its tool function receives the agent `ActionContext`. It runs `ApprovalRequestApprovalAction`'s existing logic against that context: generates the agent's resume URL, dispatches configured approval channels, and calls `suspend()` on the agent context. It returns `ToolSuspendConstants.SUSPENDED_SENTINEL`. For streaming agents it additionally emits an SSE event carrying the approval form URL (the `sendQuestionEvent` pattern). On resume, the approval result (`approved` + form fields) is the patched tool result the LLM continues from.

## 9. Migrate `AiAgentUtilsAskUserQuestionTool`

`askUserQuestion` already reaches the agent context and suspends it, but it returns placeholder empty-string answers. Under the new protocol that placeholder must become `ToolSuspendConstants.SUSPENDED_SENTINEL` so the decorator can identify its `ToolResponse`. On resume, `resumePerform` patches the sentinel with the real answers. This is the only `askUserQuestion`-specific change (~10 lines); the resumable loop, re-entry, and streaming hand-off are shared. Migrating it is mandatory — leaving it on the old placeholder behavior would break it under the new core.

## Testing

### Unit

- **`SuspendableToolCallingManager`** — with a stub `ToolCallingManager` delegate and a stub `ActionContextAware`:
  - no suspend on the context → returns the delegate's result unchanged, `returnDirect` false.
  - suspend present → returns `returnDirect=true`; the enriched `Suspend` carries a `ConversationState` and the correct `pendingToolCallId` (the id of the sentinel `ToolResponse`).
  - sentinel not last in a multi-tool batch → still located by content scan.
  - two sentinels in one batch → clear exception.
- **`ConversationState`** — round-trip `from(List<Message>).toMessages()` preserves `SystemMessage`/`UserMessage`/`AssistantMessage` (incl. `toolCalls`)/`ToolResponseMessage`; and a `ConversationState` survives a Jackson serialize/deserialize cycle (the `TaskState` path).
- **resume patching** — given a `ConversationState` with a sentinel `ToolResponse` and a `pendingToolCallId`, the patch replaces exactly that `ToolResponse`'s content with `data` and leaves siblings untouched.
- **`AiAgentToolFacade`** — a `MultipleConnectionsToolFunction` callback built as a `BiFunction` receives a `ToolContext` and the agent `ActionContext` reaches the tool.

### Integration

- **`AiAgentChatAction` IntTest** with a stub suspending tool and a stub `ChatModel`: the model emits a tool call → the tool suspends → assert the agent's task suspends (`Suspend` returned, `JOB_RESUME_ID` minted). Then drive `resumePerform` with answer `data` → assert the stub model is re-invoked with a conversation whose pending `ToolResponse` holds the real answer, and the final output is the continued response.
- **Multi-round** — the stub tool suspends twice across two resume cycles; assert each cycle re-enters cleanly.
- **`AiAgentStreamChatAction` IntTest** — a suspending tool inside the streamed `Flux`: assert the post-stream suspend check fires (task suspends after stream completion, not before), and that an SSE event with the resume URL is emitted before stream close.
- **`ApprovalRequestApprovalTool`** — invoked as an agent tool, asserts it suspends the agent context (not a detached one) and returns the sentinel; approval channels dispatched when configured.
- **`AiAgentUtilsAskUserQuestionTool`** — regression: still suspends; now returns the sentinel; resume injects real answers.

### Cluster-element definition snapshots

`ApprovalRequestApprovalTool` / agent action definition changes regenerate `src/test/resources/definition/*.json`; delete stale `build/resources/test/definition/` copies before re-running, per the component-test convention.

## Files touched

New: `SuspendableToolCallingManager`, `ConversationState`, `SuspendAwareSseEmitterHandler`, `ToolSuspendConstants`.
Modified: `AbstractAiAgentChatAction`, `AiAgentChatAction`, `AiAgentStreamChatAction`, `AiAgentToolFacade`, `ClusterElementDefinitionService(+Impl)` (new `executeTool` overload), `ActionDefinitionServiceImpl` (streaming branch), `SseStreamTaskExecutionPostOutputProcessor` ×2, `JobSyncExecutor` + worker post-output wiring (ordering), `ApprovalRequestApprovalTool`, `AiAgentUtilsAskUserQuestionTool`.

## Implementation phasing

1. **Core sync loop** — `ToolSuspendConstants`, `ConversationState`, `SuspendableToolCallingManager`, wire into `AbstractAiAgentChatAction`, real `resumePerform` for `AiAgentChatAction`. Tools still can't reach the context yet — tested with a `ToolCallbackProviderFunction` stub tool.
2. **`MultipleConnectionsToolFunction` context access** — `AiAgentToolFacade` `BiFunction` + `executeTool` overload.
3. **`requestApproval` + `askUserQuestion`** — migrate both tools to the sentinel protocol; sync end-to-end.
4. **Streaming** — `SuspendAwareSseEmitterHandler`, `ActionDefinitionServiceImpl` streaming branch, both `SseStreamTaskExecutionPostOutputProcessor`s, post-output ordering, `AiAgentStreamChatAction.resumePerform`, streaming resume delivery.

## Open items / risks

- **Streaming resume delivery** (Section 7) — the extended `JobResumeController` must register its `SseStreamBridge` on whichever executor (`JobSyncExecutor` vs the worker) actually runs the resumed job; confirm the bridge keying reaches the resumed turn's `SseStreamTaskExecutionPostOutputProcessor`.
- **AG-UI invocation path** — AI Hub / Copilot chat invoke the streaming agent through the AG-UI `LocalAgent` framework. The suspend mechanism assumes `streamChat` runs as a standard `TaskExecution` so the post-output processor fires; confirm the AG-UI path routes through it.
- **Spring AI version** — verified against `spring-ai` 2.0.0-M6 (`ToolCallAdvisor` honors `ToolExecutionResult.returnDirect()` in call + stream). A future Spring AI bump must re-verify this contract.
