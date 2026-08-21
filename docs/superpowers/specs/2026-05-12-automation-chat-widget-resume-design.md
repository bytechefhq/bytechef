# Automation chat widget — resume support design

**Status:** Draft | **Owner:** Ivica | **Created:** 2026-05-12 | **Last updated:** 2026-05-12

## Goal

Workflows already support a **suspend-and-ask-the-user** pattern: a task can call `askUserQuestion`, the workflow suspends, and the platform emits an `ask_user_question` SSE event carrying a one-shot `resumeUrl`. The editor's chat panel already honours this — when the next user message arrives, it POSTs to `resumeUrl` instead of starting a new test, and the workflow resumes.

The **external chat widget** (`AutomationChatModal`, [sdks/frontend/automation/chat/library/](../../sdks/frontend/automation/chat/library/)) — the one customers embed on their own sites to chat with a ByteChef workflow — has no resume support. A workflow that suspends with an `ask_user_question` reaches the widget as an unhandled SSE event; the widget's `useSSE` falls through to the default handler, treats it as a result, and the next user turn starts a brand-new conversation instead of resuming the suspended one.

This spec closes that gap.

## Non-goals

- **Multi-turn approvals.** Suspend-resume in this spec is "ask one question, resume." Chained suspends (Q1 → resume → Q2 → resume) work naturally as a side-effect because each `ask_user_question` carries its own `resumeUrl`, but we do not invent new wire-protocol semantics for batched questions.
- **Server-side wire-protocol changes.** The SSE event already exists. This is a pure widget-side change.
- **Custom rendering of the question.** If the platform emits a structured question (options, type hints), the widget renders it as plain text just like the editor panel does today via `formatAskUserQuestionMessage`. A dedicated structured-question UI is Tier 2.
- **Resume from a different browser / device.** Resume is bound to the current widget instance via the in-memory `resumeUrl`. If the user reloads the page mid-suspend, the conversation is lost. Persistent resume is Tier 2.
- **Multi-question parallelism.** Only one outstanding `resumeUrl` at a time; if the workflow somehow emits a second `ask_user_question` while one is pending (it shouldn't), the second replaces the first.
- **Voice resume.** Voice has its own spec; resume + voice interactions are out of scope here.

## Background

- The widget today uses [`useSSE`](../../sdks/frontend/automation/chat/library/src/hooks/useSSE.ts) to consume SSE events from the workflow's webhook endpoint when `webhookUrl` ends in `/sse`. Custom event handlers are wired through `eventHandlers: Record<string, (data) => void>`.
- The widget's [`AutomationChatProvider`](../../sdks/frontend/automation/chat/library/src/components/AutomationChatProvider.tsx) wires `error` / `result` / `stream` / `message` handlers; there is no `ask_user_question` handler.
- The platform emits the suspend event as `event: ask_user_question` with data `{questions: [{question: "…"}, …], resumeUrl: "https://…"}`.
- The platform's resume endpoint accepts `POST <resumeUrl>` with `{message: "<user-answer>"}` JSON. On success the workflow continues; the resumed run streams its result through the **same** SSE channel (the original POST that's still open), so the widget does not need to start a second SSE stream.
- The widget's `useChatStore` is module-scoped (singleton). Adding a `resumeUrl` slice mirrors the platform `useWorkflowTestChatStore` slice exactly.

### How the editor panel handles it today

The editor's [`WorkflowTestChatRuntimeProvider`](../../client/src/pages/platform/workflow-editor/components/workflow-test-chat/runtime-providers/WorkflowTestChatRuntimeProvider.tsx) is the reference implementation:

1. SSE handler for `ask_user_question` calls `setResumeUrl(event.resumeUrl)` and writes the formatted question as the assistant message.
2. On the next `onNew` user message: if `resumeUrl` is non-null, POST `{message: input}` to it as JSON, clear `resumeUrl`, render confirmation; otherwise start a new test stream.

The widget will replicate this almost verbatim.

## Design

### Feature 1 — `resumeUrl` slice in `useChatStore`

```ts
interface ChatState {
    // ...existing
    resumeUrl: string | null;
    setResumeUrl: (resumeUrl: string | null) => void;
}
```

Default `null`. `resetMessages` / `reset` clear it.

### Feature 2 — `ask_user_question` handler in `AutomationChatProvider`

Wire a new SSE event handler:

```ts
handleAskUserQuestion(data) {
    if (!data || typeof data !== 'object') return;
    const event = data as {questions?: {question: string}[]; resumeUrl?: string};

    const text = (event.questions ?? [])
        .map((question) => `- ${question.question ?? ''}`)
        .join('\n');

    if (text) {
        setLastAssistantMessageContent(text);
    }

    setResumeUrl(event.resumeUrl ?? null);
    setIsRunning(false);
    setStreamRequest(null);
}
```

Registered in the `eventHandlers` map alongside `error` / `result` / `stream` / `message`.

The handler **stops the SSE stream** by clearing `streamRequest`, mirroring the editor's behaviour. The customer's POST connection is closed; the resume path uses a fresh fetch. This is a deliberate trade-off — the alternative (keep the SSE open and have the platform stream the resumed-workflow's result over the original connection) requires the resume endpoint to refuse new SSE connections, which the platform doesn't guarantee. Closing and reopening is more robust.

### Feature 3 — `onNew` branches on `resumeUrl`

```ts
const onNew = async (message: AppendMessage) => {
    const currentResumeUrl = useChatStore.getState().resumeUrl;

    if (currentResumeUrl) {
        useChatStore.getState().setResumeUrl(null);

        setMessage({role: 'user', content: input, attachments: [...]});
        setIsRunning(true);

        try {
            const response = await fetch(currentResumeUrl, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({message: input}),
            });

            if (!response.ok) {
                throw new Error(`Resume request failed (${response.status})`);
            }

            // The resumed workflow may emit more events through a fresh SSE stream
            // if the resume endpoint returns one; in the common case the body is a
            // simple JSON result. Parse defensively and render as the assistant turn.
            const contentType = response.headers.get('content-type') ?? '';

            if (contentType.includes('text/event-stream')) {
                // Re-attach SSE — kick a new streamRequest with the resume response as source.
                // For simplicity we model this as a fresh GET against the resume URL with text/event-stream
                // semantics handled inside useSSE. Implementation note: response.body is already an SSE
                // ReadableStream; the existing useSSE expects a URL not a stream, so for Tier 1 we drain
                // the in-flight response synchronously instead of feeding it back through useSSE.
                await drainSseResponse(response, eventHandlers);
            } else {
                const result = await response.json().catch(() => null);
                const responseText = result?.message ?? 'Answer submitted.';
                setMessage({role: 'assistant', content: responseText});
            }
        } catch (error) {
            setMessage({
                role: 'assistant',
                content: 'Failed to submit your answer. Please try again.',
            });
        } finally {
            setIsRunning(false);
        }

        return;
    }

    // ...existing new-conversation path
};
```

### Feature 4 — `drainSseResponse` helper

If the resume endpoint returns SSE (the platform sometimes does, when the resumed workflow continues to stream), parse the response body line-by-line and dispatch events into the existing `eventHandlers`. We reuse the SSE parser already inside `useSSE` by extracting it into a pure helper:

```ts
// In utils/stream-utils.ts (extracted from useSSE.ts)
export async function drainSseResponse(
    response: Response,
    eventHandlers: Record<string, (data: unknown) => void>,
): Promise<void> {
    if (!response.body) return;

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';

    while (true) {
        const {done, value} = await reader.read();

        if (done) break;

        buffer += decoder.decode(value, {stream: true});
        const parts = buffer.split(/\r?\n\r?\n/);

        buffer = parts.pop() ?? '';

        for (const part of parts) {
            parseAndDispatchSSE(part, () => {}, eventHandlers);
        }
    }

    if (buffer.trim()) {
        parseAndDispatchSSE(buffer, () => {}, eventHandlers);
    }
}
```

`parseAndDispatchSSE` and the line constants live in `useSSE.ts` today as local helpers; we expose them via the shared util file.

### Feature 5 — Reset behaviour

When the customer closes and reopens the modal, the existing `reset()` should also clear `resumeUrl`. The `resetMessages` action gets extended to set `resumeUrl: null`.

## Migration / compatibility

- **No new server endpoints.** The platform already emits `ask_user_question`. The widget just handles a new event type.
- **No package-API breakage.** New `resumeUrl` is internal state; no new props on `AutomationChatModal` or `AutomationChat`.
- **`useSSE` stays.** The new `drainSseResponse` extraction is internal — `useSSE` continues to export the same surface.
- **Bundle size.** Adds ~150 lines (handler + drain + store slice). Negligible.

## Open questions / risks

- **Resume endpoint that returns non-SSE.** The platform endpoint currently returns a `204` after accepting the answer; the resumed workflow's events flow back on the **original** SSE channel (the still-open POST). If that channel is already closed when we POST to `resumeUrl`, the widget shows the confirmation message but never sees the next assistant turn. We accept this for Tier 1 — the next `ask_user_question` or final `result` would arrive on a new connection if the user types again. If users complain we'll switch to keep-original-stream-open semantics.
- **Cross-origin POST to `resumeUrl`.** The platform issues fully-qualified URLs. Customer pages embedding the widget need CORS on those URLs, which is already the case for the SSE endpoint (same origin).
- **Replay protection.** A `resumeUrl` is single-use server-side; if the user double-clicks send, the second POST 404s. The widget catches that via the `!response.ok` branch and shows the error. Tier 1 accepts this.

## Test plan

- Unit test the new `ask_user_question` handler — verify `resumeUrl` lands in the store and the formatted question shows as the assistant turn.
- Unit test `onNew` — when `resumeUrl` is set, POST to it; otherwise start a fresh SSE stream.
- Unit test `drainSseResponse` with a fixture-driven SSE body.
- Manual smoke: build a workflow with `askUserQuestion`, point the widget at it, verify the question shows up, the answer POSTs, and the resumed result renders.

## Implementation plan

1. **Extract** SSE parser into `utils/stream-utils.ts` (`parseAndDispatchSSE`, `drainSseResponse`).
2. **Store slice** — add `resumeUrl` / `setResumeUrl` to `useChatStore`.
3. **Handler** — add `ask_user_question` to `eventHandlers` in `AutomationChatProvider`.
4. **`onNew` branch** — POST to `resumeUrl` when present.
5. **Reset** — clear `resumeUrl` on `resetMessages` / `reset`.
