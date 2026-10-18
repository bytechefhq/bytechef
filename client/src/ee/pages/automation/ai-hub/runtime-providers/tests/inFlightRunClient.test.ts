import {type AgentSubscriber, EventType} from '@ag-ui/client';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {attachToInFlightRun} from '../inFlightRunClient';

class FakeEventSource {
    static instances: FakeEventSource[] = [];

    close = vi.fn();
    onerror: ((event: Event) => void) | null = null;
    onmessage: ((event: MessageEvent) => void) | null = null;
    url: string;

    constructor(url: string) {
        this.url = url;

        FakeEventSource.instances.push(this);
    }

    emit(payload: unknown) {
        this.onmessage?.({data: ` ${JSON.stringify(payload)}`} as MessageEvent);
    }
}

async function flushHandlers() {
    await vi.waitFor(() => Promise.resolve());
}

describe('attachToInFlightRun', () => {
    beforeEach(() => {
        FakeEventSource.instances = [];

        vi.stubGlobal('EventSource', FakeEventSource);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('opens the attach endpoint with the encoded thread id', () => {
        attachToInFlightRun({subscriber: {}, threadId: 'thread/1'});

        expect(FakeEventSource.instances[0]!.url).toBe('/api/platform/internal/ai/chat/ai_hub/thread%2F1/attach');
    });

    it('reconstructs the text buffer for text content events', async () => {
        const onTextMessageContentEvent = vi.fn();
        const subscriber: AgentSubscriber = {onTextMessageContentEvent};

        attachToInFlightRun({subscriber, threadId: 'thread-1'});

        const eventSource = FakeEventSource.instances[0]!;

        eventSource.emit({messageId: 'm1', role: 'assistant', type: EventType.TEXT_MESSAGE_START});
        eventSource.emit({delta: 'Hel', messageId: 'm1', type: EventType.TEXT_MESSAGE_CONTENT});
        eventSource.emit({delta: 'lo', messageId: 'm1', type: EventType.TEXT_MESSAGE_CONTENT});

        await flushHandlers();

        expect(onTextMessageContentEvent).toHaveBeenCalledTimes(2);
        expect(onTextMessageContentEvent.mock.calls[1]![0].textMessageBuffer).toBe('Hel');
    });

    it('passes the accumulated tool call args to the end event', async () => {
        const onToolCallEndEvent = vi.fn();

        attachToInFlightRun({subscriber: {onToolCallEndEvent}, threadId: 'thread-1'});

        const eventSource = FakeEventSource.instances[0]!;

        eventSource.emit({toolCallId: 't1', toolCallName: 'openWorkflowTab', type: EventType.TOOL_CALL_START});
        eventSource.emit({delta: '{"workflowId":', toolCallId: 't1', type: EventType.TOOL_CALL_ARGS});
        eventSource.emit({delta: '"wf-1"}', toolCallId: 't1', type: EventType.TOOL_CALL_ARGS});
        eventSource.emit({toolCallId: 't1', type: EventType.TOOL_CALL_END});

        await flushHandlers();

        expect(onToolCallEndEvent.mock.calls[0]![0]).toMatchObject({
            toolCallArgs: {workflowId: 'wf-1'},
            toolCallName: 'openWorkflowTab',
        });
    });

    it('closes once on a terminal event and reports that events were received', async () => {
        const onClose = vi.fn();
        const onRunFinishedEvent = vi.fn();

        attachToInFlightRun({onClose, subscriber: {onRunFinishedEvent}, threadId: 'thread-1'});

        const eventSource = FakeEventSource.instances[0]!;

        eventSource.emit({runId: 'r1', threadId: 'thread-1', type: EventType.RUN_FINISHED});
        eventSource.onerror?.(new Event('error'));

        await flushHandlers();

        expect(onRunFinishedEvent).toHaveBeenCalledTimes(1);
        expect(eventSource.close).toHaveBeenCalledTimes(1);
        expect(onClose).toHaveBeenCalledTimes(1);
        expect(onClose).toHaveBeenCalledWith(true);
    });

    it('reports no events received when the stream errors before any event', () => {
        const onClose = vi.fn();

        attachToInFlightRun({onClose, subscriber: {}, threadId: 'thread-1'});

        FakeEventSource.instances[0]!.onerror?.(new Event('error'));

        expect(onClose).toHaveBeenCalledWith(false);
    });

    it('returns a disposer that closes the stream', () => {
        const dispose = attachToInFlightRun({subscriber: {}, threadId: 'thread-1'});

        dispose();

        expect(FakeEventSource.instances[0]!.close).toHaveBeenCalledTimes(1);
    });

    it('keeps the stream alive when a subscriber handler throws', async () => {
        const onToolCallResultEvent = vi.fn(() => {
            throw new Error('boom');
        });
        const onRunFinishedEvent = vi.fn();

        vi.spyOn(console, 'error').mockImplementation(() => undefined);

        attachToInFlightRun({subscriber: {onRunFinishedEvent, onToolCallResultEvent}, threadId: 'thread-1'});

        const eventSource = FakeEventSource.instances[0]!;

        eventSource.emit({
            content: '{}',
            messageId: 'm1',
            role: 'tool',
            toolCallId: 't1',
            type: EventType.TOOL_CALL_RESULT,
        });
        eventSource.emit({runId: 'r1', threadId: 'thread-1', type: EventType.RUN_FINISHED});

        await flushHandlers();

        expect(onRunFinishedEvent).toHaveBeenCalledTimes(1);
    });
});
