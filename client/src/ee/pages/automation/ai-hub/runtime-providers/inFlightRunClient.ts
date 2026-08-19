/**
 * Client-side adapter for the AI Hub's `/attach` and `/in-flight` endpoints. Lets the runtime provider
 * resume streaming after a page refresh and hydrate sidebar pulses for chats the user has running in other
 * threads.
 *
 * <p>
 * The matching server-side machinery lives in
 * <code>server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/agent/InFlightAiHubRunRegistry.java</code>
 * — see that file for the run-lifecycle and replay-buffer semantics this client relies on.
 * </p>
 */
import {type AgentSubscriber, EventType} from '@ag-ui/client';

const IN_FLIGHT_ENDPOINT = '/api/platform/internal/ai/chat/ai_hub/in-flight';

// The probe passes every visible chat's threadId as a repeated `threadIds` query param. Sent as one
// request, a large chat list overflows the servlet container's max request-line length and Tomcat
// rejects it with an HTML 400 *before* the controller runs. Chunking keeps each URL comfortably under
// common limits: at ~47 chars per id (`&threadIds=<uuid>`), 40 ids ≈ 1.9 KB of query string.
const IN_FLIGHT_BATCH_SIZE = 40;
const ATTACH_ENDPOINT = (threadId: string) =>
    `/api/platform/internal/ai/chat/ai_hub/${encodeURIComponent(threadId)}/attach`;

export interface AttachOptionsI {
    threadId: string;
    /**
     * The same subscriber shape that the AG-UI SDK's HttpAgent dispatches into. Reusing it avoids duplicating
     * the considerable assistant-message / tool-call / tab-open routing logic that lives in {@link
     * buildAiHubSubscriber}.
     */
    subscriber: AgentSubscriber;
    /**
     * Fires when the attach EventSource closes for ANY reason — terminal event (RUN_FINISHED / RUN_ERROR),
     * network error, or caller-side disposer. The runtime provider uses this to release its
     * {@code isAgentRunning} flag and clear the sidebar pulse.
     */
    onClose?: () => void;
}

/**
 * Opens an EventSource on the AI Hub attach endpoint and forwards each event into the supplied AG-UI
 * subscriber. Returns a disposer the caller invokes on chat switch / unmount.
 *
 * <p>
 * The AG-UI SDK's HttpAgent maintains a {@code textMessageBuffer} and a per-tool-call argument buffer that
 * many subscriber handlers read; this function reconstructs those locally so handlers see the same params
 * regardless of which transport (HttpAgent for live POST runs, or this EventSource for attach) delivered the
 * event.
 * </p>
 */
export function attachToInFlightRun({onClose, subscriber, threadId}: AttachOptionsI): () => void {
    const eventSource = new EventSource(ATTACH_ENDPOINT(threadId));

    let textMessageBuffer = '';
    const toolCallBufferById = new Map<string, string>();
    const toolCallNameById = new Map<string, string>();
    let closed = false;

    const close = () => {
        if (closed) {
            return;
        }

        closed = true;

        eventSource.close();
        onClose?.();
    };

    eventSource.onmessage = (rawEvent) => {
        // Server emits each event as `data:  {json}` (leading space mirrors the upstream AgUiService format).
        // EventSource strips the `data: ` prefix but the inner leading whitespace remains, so trim before parse.
        const trimmed = typeof rawEvent.data === 'string' ? rawEvent.data.trimStart() : '';

        if (!trimmed) {
            return;
        }

        let parsedEvent: Record<string, unknown>;

        try {
            parsedEvent = JSON.parse(trimmed);
        } catch (error) {
            console.error('[AiHub attach] failed to parse AG-UI event from attach stream', error);

            return;
        }

        // Dispatch handlers expect `AgentSubscriberParams` (messages / state / agent / input) alongside the
        // event. The existing AI Hub handlers don't actually read those four fields — their guards key off
        // the global store directly — so passing empty placeholders is functionally equivalent and avoids
        // duplicating the SDK's internal state machine here. The cast keeps TypeScript happy without
        // forging values the runtime would otherwise validate.
        const baseParams = {
            agent: undefined,
            input: undefined,
            messages: [],
            state: {},
        };

        void dispatchEvent(parsedEvent, baseParams);

        const type = parsedEvent.type;

        if (type === EventType.RUN_FINISHED || type === EventType.RUN_ERROR) {
            close();
        }
    };

    eventSource.onerror = (errorEvent) => {
        // EventSource fires onerror on (1) explicit close from server end, (2) network blip with retry, (3)
        // 4xx/5xx response. The browser-built-in retry would just hammer a 404 forever after the run
        // terminates, so we close decisively and let the caller decide whether to re-probe.
        console.debug('[AiHub attach] event source error; closing', errorEvent);

        close();
    };

    async function dispatchEvent(
        event: Record<string, unknown>,
        baseParams: {agent: unknown; input: unknown; messages: never[]; state: Record<string, never>}
    ): Promise<void> {
        const type = event.type;

        // The subscriber handler param shape varies per event type. Each case constructs only the shape its
        // target handler reads; the others fall back to baseParams via spread.
        try {
            switch (type) {
                case EventType.TEXT_MESSAGE_START: {
                    textMessageBuffer = '';
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    await subscriber.onTextMessageStartEvent?.({event, ...baseParams} as any);

                    break;
                }
                case EventType.TEXT_MESSAGE_CONTENT: {
                    const previousBuffer = textMessageBuffer;

                    textMessageBuffer += (event as {delta?: string}).delta ?? '';

                    await subscriber.onTextMessageContentEvent?.({
                        event,
                        textMessageBuffer: previousBuffer,
                        ...baseParams,
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    } as any);

                    break;
                }
                case EventType.TEXT_MESSAGE_END: {
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    await subscriber.onTextMessageEndEvent?.({event, ...baseParams} as any);

                    textMessageBuffer = '';

                    break;
                }
                case EventType.TOOL_CALL_START: {
                    const toolCallId = (event as {toolCallId: string}).toolCallId;
                    const toolCallName = (event as {toolCallName?: string}).toolCallName ?? '';

                    toolCallNameById.set(toolCallId, toolCallName);
                    toolCallBufferById.set(toolCallId, '');

                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    await subscriber.onToolCallStartEvent?.({event, ...baseParams} as any);

                    break;
                }
                case EventType.TOOL_CALL_ARGS: {
                    const toolCallId = (event as {toolCallId: string}).toolCallId;
                    const delta = (event as {delta?: string}).delta ?? '';
                    const accumulated = (toolCallBufferById.get(toolCallId) ?? '') + delta;

                    toolCallBufferById.set(toolCallId, accumulated);

                    let partial: Record<string, unknown> = {};

                    try {
                        partial = JSON.parse(accumulated);
                    } catch {
                        // Mid-stream delta isn't a valid JSON object yet — handlers tolerate this.
                    }

                    await subscriber.onToolCallArgsEvent?.({
                        event,
                        partialToolCallArgs: partial,
                        toolCallBuffer: accumulated,
                        toolCallName: toolCallNameById.get(toolCallId) ?? '',
                        ...baseParams,
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    } as any);

                    break;
                }
                case EventType.TOOL_CALL_END: {
                    const toolCallId = (event as {toolCallId: string}).toolCallId;
                    const finalBuffer = toolCallBufferById.get(toolCallId) ?? '';

                    toolCallBufferById.delete(toolCallId);

                    let toolCallArgs: Record<string, unknown> = {};

                    try {
                        toolCallArgs = finalBuffer.length > 0 ? JSON.parse(finalBuffer) : {};
                    } catch {
                        toolCallArgs = {};
                    }

                    await subscriber.onToolCallEndEvent?.({
                        event,
                        toolCallArgs,
                        toolCallName: toolCallNameById.get(toolCallId) ?? '',
                        ...baseParams,
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    } as any);

                    break;
                }
                case EventType.TOOL_CALL_RESULT: {
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    await subscriber.onToolCallResultEvent?.({event, ...baseParams} as any);

                    break;
                }
                case EventType.RUN_STARTED: {
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    await subscriber.onRunStartedEvent?.({event, ...baseParams} as any);

                    break;
                }
                case EventType.RUN_FINISHED: {
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    await subscriber.onRunFinishedEvent?.({event, ...baseParams} as any);

                    break;
                }
                case EventType.RUN_ERROR: {
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    await subscriber.onRunErrorEvent?.({event, ...baseParams} as any);

                    break;
                }
                case EventType.CUSTOM: {
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    await subscriber.onCustomEvent?.({event, ...baseParams} as any);

                    break;
                }
                // Other AG-UI event types (THINKING_*, REASONING_*, STEP_*) aren't surfaced in the AI Hub
                // UI today. Add cases here if a future renderer needs them.
                default:
                    break;
            }
        } catch (handlerError) {
            // A faulty subscriber handler must not blow up the EventSource — otherwise a tab-open failure
            // mid-replay would abort the rest of the stream, leaving the assistant message half-rendered.
            console.error('[AiHub attach] subscriber handler threw', handlerError);
        }
    }

    return close;
}

/**
 * Returns a `{threadId: boolean}` map. Threads missing from the response or not in flight resolve to
 * {@code false}. Network errors degrade silently to "no streams" — better to render a static history than
 * to surface a transient probe failure as a permanent block.
 *
 * <p>Thread ids are probed in batches (see {@link IN_FLIGHT_BATCH_SIZE}) so a large chat list doesn't
 * overflow the request-line length limit. Batches run concurrently and each degrades independently: a
 * single failed batch contributes no entries rather than failing the whole probe.</p>
 */
export async function probeInFlightStatus(threadIds: ReadonlyArray<string>): Promise<Record<string, boolean>> {
    if (threadIds.length === 0) {
        return {};
    }

    const batches: Array<ReadonlyArray<string>> = [];

    for (let index = 0; index < threadIds.length; index += IN_FLIGHT_BATCH_SIZE) {
        batches.push(threadIds.slice(index, index + IN_FLIGHT_BATCH_SIZE));
    }

    const batchResults = await Promise.all(batches.map((batch) => probeInFlightBatch(batch)));

    return Object.assign({}, ...batchResults) as Record<string, boolean>;
}

async function probeInFlightBatch(threadIds: ReadonlyArray<string>): Promise<Record<string, boolean>> {
    const params = new URLSearchParams();

    threadIds.forEach((threadId) => params.append('threadIds', threadId));

    try {
        const response = await fetch(`${IN_FLIGHT_ENDPOINT}?${params.toString()}`, {
            credentials: 'include',
        });

        if (!response.ok) {
            console.warn('[AiHub attach] probe returned non-OK status', response.status);

            return {};
        }

        return (await response.json()) as Record<string, boolean>;
    } catch (error) {
        console.warn('[AiHub attach] probe failed', error);

        return {};
    }
}
