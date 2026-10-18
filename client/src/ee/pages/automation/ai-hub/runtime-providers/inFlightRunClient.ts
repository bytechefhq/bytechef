/**
 * Client-side adapter for the AI Hub's {@code /attach} endpoint, which streams the continuation turn the server
 * starts after a gated tool call is approved or rejected. The server side is {@code AiHubApiController} and
 * {@code InFlightAiHubRunRegistry}.
 */
import {type AgentSubscriber, EventType} from '@ag-ui/client';

const ATTACH_ENDPOINT = (threadId: string) =>
    `/api/platform/internal/ai/chat/ai_hub/${encodeURIComponent(threadId)}/attach`;

export interface AttachOptionsI {
    /**
     * Fires once when the attach EventSource closes for any reason: a terminal event (RUN_FINISHED / RUN_ERROR), a
     * network error, or the caller's disposer. {@code eventsReceived} says whether at least one parsable AG-UI event
     * arrived before the close.
     */
    onClose?: (eventsReceived: boolean) => void;
    /** The same subscriber shape the AG-UI HttpAgent dispatches into, so live and attached turns share one router. */
    subscriber: AgentSubscriber;
    threadId: string;
}

type SubscriberParamsType = Parameters<NonNullable<AgentSubscriber['onRunFinalized']>>[0];

const BASE_SUBSCRIBER_PARAMS = {
    agent: undefined,
    input: undefined,
    messages: [],
    state: {},
} as unknown as SubscriberParamsType;

/**
 * Opens an EventSource on the attach endpoint and forwards each event into the supplied AG-UI subscriber. The
 * HttpAgent keeps a text buffer and per-tool-call argument buffers that subscriber handlers read; they are rebuilt
 * here so handlers see the same params regardless of transport. Returns a disposer.
 */
export function attachToInFlightRun({onClose, subscriber, threadId}: AttachOptionsI): () => void {
    const eventSource = new EventSource(ATTACH_ENDPOINT(threadId));

    const toolCallBufferById = new Map<string, string>();
    const toolCallNameById = new Map<string, string>();

    let closed = false;
    let eventsReceived = false;
    let textMessageBuffer = '';

    const close = () => {
        if (closed) {
            return;
        }

        closed = true;

        eventSource.close();

        onClose?.(eventsReceived);
    };

    const parseArgs = (buffer: string): Record<string, unknown> => {
        try {
            return buffer.length > 0 ? JSON.parse(buffer) : {};
        } catch {
            return {};
        }
    };

    const dispatchEvent = async (event: Record<string, unknown>): Promise<void> => {
        const params = {...BASE_SUBSCRIBER_PARAMS, event};
        const toolCallId = event.toolCallId as string;

        try {
            switch (event.type) {
                case EventType.TEXT_MESSAGE_START: {
                    textMessageBuffer = '';

                    await subscriber.onTextMessageStartEvent?.(params as never);

                    break;
                }
                case EventType.TEXT_MESSAGE_CONTENT: {
                    const previousBuffer = textMessageBuffer;

                    textMessageBuffer += (event.delta as string | undefined) ?? '';

                    await subscriber.onTextMessageContentEvent?.({
                        ...params,
                        textMessageBuffer: previousBuffer,
                    } as never);

                    break;
                }
                case EventType.TEXT_MESSAGE_END: {
                    await subscriber.onTextMessageEndEvent?.({...params, textMessageBuffer} as never);

                    textMessageBuffer = '';

                    break;
                }
                case EventType.TOOL_CALL_START: {
                    toolCallNameById.set(toolCallId, (event.toolCallName as string | undefined) ?? '');
                    toolCallBufferById.set(toolCallId, '');

                    await subscriber.onToolCallStartEvent?.(params as never);

                    break;
                }
                case EventType.TOOL_CALL_ARGS: {
                    const accumulated = (toolCallBufferById.get(toolCallId) ?? '') + ((event.delta as string) ?? '');

                    toolCallBufferById.set(toolCallId, accumulated);

                    await subscriber.onToolCallArgsEvent?.({
                        ...params,
                        partialToolCallArgs: parseArgs(accumulated),
                        toolCallBuffer: accumulated,
                        toolCallName: toolCallNameById.get(toolCallId) ?? '',
                    } as never);

                    break;
                }
                case EventType.TOOL_CALL_END: {
                    const finalBuffer = toolCallBufferById.get(toolCallId) ?? '';

                    toolCallBufferById.delete(toolCallId);

                    await subscriber.onToolCallEndEvent?.({
                        ...params,
                        toolCallArgs: parseArgs(finalBuffer),
                        toolCallName: toolCallNameById.get(toolCallId) ?? '',
                    } as never);

                    break;
                }
                case EventType.TOOL_CALL_RESULT: {
                    await subscriber.onToolCallResultEvent?.(params as never);

                    break;
                }
                case EventType.RUN_STARTED: {
                    await subscriber.onRunStartedEvent?.(params as never);

                    break;
                }
                case EventType.RUN_FINISHED: {
                    await subscriber.onRunFinishedEvent?.(params as never);

                    break;
                }
                case EventType.RUN_ERROR: {
                    await subscriber.onRunErrorEvent?.(params as never);

                    break;
                }
                case EventType.CUSTOM: {
                    await subscriber.onCustomEvent?.(params as never);

                    break;
                }
                default:
                    break;
            }
        } catch (handlerError) {
            console.error('[AiHub attach] subscriber handler threw', handlerError);
        }
    };

    eventSource.onmessage = (rawEvent) => {
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

        eventsReceived = true;

        void dispatchEvent(parsedEvent);

        if (parsedEvent.type === EventType.RUN_FINISHED || parsedEvent.type === EventType.RUN_ERROR) {
            close();
        }
    };

    eventSource.onerror = () => {
        close();
    };

    return close;
}
