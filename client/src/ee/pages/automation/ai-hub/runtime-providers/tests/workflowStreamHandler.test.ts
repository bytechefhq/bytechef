/* eslint-disable sort-keys */
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
        success: vi.fn(),
    },
}));

const {toast} = await import('sonner');
const mockToastError = vi.mocked(toast.error);

import {fetchWorkflowResponse, openWorkflowSseStream} from '../workflowStreamHandler';

// Helper to create a minimal ReadableStream from an array of chunks
function makeStream(chunks: string[]): ReadableStream<Uint8Array> {
    const encoder = new TextEncoder();

    return new ReadableStream({
        start(controller) {
            for (const chunk of chunks) {
                controller.enqueue(encoder.encode(chunk));
            }

            controller.close();
        },
    });
}

/**
 * Returns a ReadableStream + a deferred-controller pair so a test can drive a stream incrementally and
 * simulate mid-stream cancellation. The first chunk is enqueued before the test calls `abort()` so the
 * handler has at least one tick of read activity to interrupt.
 */
function makeControlledStream(initialChunks: string[]): {
    abort: () => void;
    emit: (chunk: string) => void;
    stream: ReadableStream<Uint8Array>;
} {
    const encoder = new TextEncoder();

    let streamController: ReadableStreamDefaultController<Uint8Array> | null = null;

    const stream = new ReadableStream<Uint8Array>({
        start(controller) {
            streamController = controller;

            for (const chunk of initialChunks) {
                controller.enqueue(encoder.encode(chunk));
            }
        },
    });

    let aborted = false;

    return {
        abort: () => {
            if (streamController && !aborted) {
                aborted = true;
                streamController.error(Object.assign(new Error('The user aborted a request.'), {name: 'AbortError'}));
            }
        },
        emit: (chunk: string) => {
            // After abort the underlying ReadableStream is closed — late emit() calls must be no-ops so
            // the test simulates "server kept sending" without throwing inside the test harness.
            if (streamController && !aborted) {
                streamController.enqueue(encoder.encode(chunk));
            }
        },
        stream,
    };
}

describe('openWorkflowSseStream', () => {
    beforeEach(() => {
        vi.resetAllMocks();
        mockToastError.mockReset();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('calls onComplete when the stream closes normally', async () => {
        const onComplete = vi.fn();
        const onError = vi.fn();
        const appendToLastAssistantMessage = vi.fn();

        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            body: makeStream([]),
        });

        await openWorkflowSseStream({
            onChunk: appendToLastAssistantMessage,
            onComplete,
            onError,
            streamUrl: '/webhooks/test-id/sse',
        });

        expect(onComplete).toHaveBeenCalledOnce();
        expect(onError).not.toHaveBeenCalled();
    });

    /**
     * Pins the {@code kind: 'empty'} discriminator. The runtime provider's tool-call card flips to an error state
     * when this kind is observed, instead of leaving the spinner running forever — that's the bug this branch
     * exists to prevent. Without this assertion, a regression that returned {@code kind: 'completed'} on a
     * zero-chunk stream would re-introduce the "panel spinner forever" bug and pass the
     * {@code calls onComplete when the stream closes normally} test above (which only asserts onComplete fired).
     */
    it('reports kind: empty when a stream opens, receives no chunks, and closes cleanly', async () => {
        const onComplete = vi.fn();
        const onError = vi.fn();
        const appendToLastAssistantMessage = vi.fn();

        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            body: makeStream([]),
        });

        await openWorkflowSseStream({
            onChunk: appendToLastAssistantMessage,
            onComplete,
            onError,
            streamUrl: '/webhooks/test-id/sse',
        });

        expect(onComplete).toHaveBeenCalledOnce();
        expect(onComplete.mock.calls[0][0]).toEqual({kind: 'empty'});
        expect(onError).not.toHaveBeenCalled();
        expect(appendToLastAssistantMessage).not.toHaveBeenCalled();
    });

    /**
     * Companion to the zero-chunk test above: confirms a stream with at least one chunk reports
     * {@code kind: 'completed'} with the correct count. The two tests together pin the empty/completed
     * discrimination — moving the zero-chunk check after the unconditional 'completed' callback would
     * fail at least one of them.
     */
    it('reports kind: completed with chunkCount > 0 when chunks arrived before the stream closed', async () => {
        const onComplete = vi.fn();
        const onError = vi.fn();
        const appendToLastAssistantMessage = vi.fn();

        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            body: makeStream(['event: stream\ndata: {"text": "Hi"}\n\n']),
        });

        await openWorkflowSseStream({
            onChunk: appendToLastAssistantMessage,
            onComplete,
            onError,
            streamUrl: '/webhooks/test-id/sse',
        });

        expect(onComplete).toHaveBeenCalledOnce();
        expect(onComplete.mock.calls[0][0]).toEqual({chunkCount: 1, kind: 'completed'});
        expect(onError).not.toHaveBeenCalled();
    });

    it('pipes stream event chunks to appendToLastAssistantMessage', async () => {
        const onComplete = vi.fn();
        const appendToLastAssistantMessage = vi.fn();

        const sseChunk = 'event: stream\ndata: {"text": "Hello"}\n\n';

        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            body: makeStream([sseChunk]),
        });

        await openWorkflowSseStream({
            onChunk: appendToLastAssistantMessage,
            onComplete,
            streamUrl: '/webhooks/test-id/sse',
        });

        expect(appendToLastAssistantMessage).toHaveBeenCalledWith('Hello');
        expect(onComplete).toHaveBeenCalledOnce();
    });

    it('calls onError and skips onComplete when the HTTP response is not ok', async () => {
        const onComplete = vi.fn();
        const onError = vi.fn();
        const appendToLastAssistantMessage = vi.fn();

        global.fetch = vi.fn().mockResolvedValue({
            ok: false,
            status: 500,
            statusText: 'Server Error',
            body: null,
        });

        await openWorkflowSseStream({
            onChunk: appendToLastAssistantMessage,
            onComplete,
            onError,
            streamUrl: '/webhooks/bad-id/sse',
        });

        expect(onComplete).not.toHaveBeenCalled();
        expect(onError).toHaveBeenCalledOnce();
        expect(onError.mock.calls[0][0].message).toMatch(/HTTP 500/);
        // The handler no longer toasts directly — it surfaces the failure only via onError so callers can
        // pick a single channel (toast OR retry banner) rather than showing both for the same failure.
        expect(mockToastError).not.toHaveBeenCalled();
        expect(appendToLastAssistantMessage).not.toHaveBeenCalled();
    });

    it('calls onError and skips onComplete when an error event is received during the stream', async () => {
        const onComplete = vi.fn();
        const onError = vi.fn();
        const appendToLastAssistantMessage = vi.fn();

        const sseChunk =
            'event: stream\ndata: {"text": "partial"}\n\n' + 'event: error\ndata: {"message": "Worker crashed"}\n\n';

        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            body: makeStream([sseChunk]),
        });

        await openWorkflowSseStream({
            onChunk: appendToLastAssistantMessage,
            onComplete,
            onError,
            streamUrl: '/webhooks/test-id/sse',
        });

        expect(appendToLastAssistantMessage).toHaveBeenCalledWith('partial');
        expect(onComplete).not.toHaveBeenCalled();
        expect(onError).toHaveBeenCalledOnce();
        // The handler now prefixes the message with the failure context so the caller's banner / toast
        // gets a self-describing string. The Worker-crashed substring still needs to be present.
        expect(onError.mock.calls[0][0].message).toMatch(/Worker crashed/);
        expect(onError.mock.calls[0][0].message).toMatch(/Workflow stream failed/);
        // The handler no longer toasts directly — it surfaces the failure only via onError so callers can
        // pick a single channel (toast OR retry banner) rather than showing both for the same failure.
        expect(mockToastError).not.toHaveBeenCalled();
    });

    it('calls onError when fetch rejects with a non-abort error and skips onComplete', async () => {
        const onComplete = vi.fn();
        const onError = vi.fn();
        const appendToLastAssistantMessage = vi.fn();

        global.fetch = vi.fn().mockRejectedValue(new Error('boom'));

        await openWorkflowSseStream({
            onChunk: appendToLastAssistantMessage,
            onComplete,
            onError,
            streamUrl: '/webhooks/test-id/sse',
        });

        expect(onComplete).not.toHaveBeenCalled();
        expect(onError).toHaveBeenCalledOnce();
        expect(onError.mock.calls[0][0].message).toMatch(/boom/);
        expect(onError.mock.calls[0][0].message).toMatch(/Workflow stream failed/);
        // The handler no longer toasts directly — it surfaces the failure only via onError so callers can
        // pick a single channel (toast OR retry banner) rather than showing both for the same failure.
        expect(mockToastError).not.toHaveBeenCalled();
    });

    it('handles plain-text data events (no event: field) as stream chunks', async () => {
        const onComplete = vi.fn();
        const appendToLastAssistantMessage = vi.fn();

        const sseChunk = 'data: chunk text\n\n';

        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            body: makeStream([sseChunk]),
        });

        await openWorkflowSseStream({
            onChunk: appendToLastAssistantMessage,
            onComplete,
            streamUrl: '/webhooks/test-id/sse',
        });

        expect(appendToLastAssistantMessage).toHaveBeenCalledWith('chunk text');
    });

    it('aborts mid-stream when the caller signals AbortController.abort()', async () => {
        // Regression guard for the "user closes the panel mid-stream" path. When the caller aborts, the
        // handler MUST call onComplete with `aborted: true` (so consumers can mark tool-call cards complete),
        // MUST NOT call onError (an abort is not a failure), and MUST stop reading from the stream so
        // subsequent chunks are not delivered. Without the `aborted: true` signal, tool-call cards remain
        // stuck in the "running" state forever once the task switches away.
        const onComplete = vi.fn();
        const onError = vi.fn();
        const appendToLastAssistantMessage = vi.fn();

        const {abort, emit, stream} = makeControlledStream(['event: stream\ndata: {"text": "first"}\n\n']);

        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            body: stream,
        });

        const callerController = new AbortController();

        const streamPromise = openWorkflowSseStream({
            onChunk: appendToLastAssistantMessage,
            onComplete,
            onError,
            signal: callerController.signal,
            streamUrl: '/webhooks/test-id/sse',
        });

        // Yield once so the handler can process the initial chunk before we abort.
        await Promise.resolve();
        await Promise.resolve();

        callerController.abort();
        abort();

        // Even after we emit a late chunk, it must not reach the chunk sink — the loop must have already
        // exited because of the abort.
        emit('event: stream\ndata: {"text": "should-not-arrive"}\n\n');

        await streamPromise;

        expect(onComplete).toHaveBeenCalledWith(expect.objectContaining({kind: 'aborted'}));
        expect(onError).not.toHaveBeenCalled();
        expect(appendToLastAssistantMessage).not.toHaveBeenCalledWith('should-not-arrive');

        // Pin the wiring: the handler MUST chain the caller's AbortSignal to whatever signal it passes to
        // fetch. A regression that wires a private controller without subscribing to the caller's abort
        // would still emit `aborted` here (the test aborts the underlying stream directly), but would
        // silently break panel-close cancellation in production where the caller aborts its own controller
        // and expects the fetch socket to close.
        const fetchMock = global.fetch as unknown as ReturnType<typeof vi.fn>;

        expect(fetchMock).toHaveBeenCalled();

        const fetchInit = fetchMock.mock.calls[0][1] as RequestInit;

        expect(fetchInit.signal).toBeInstanceOf(AbortSignal);
        expect(fetchInit.signal!.aborted).toBe(true);
    });

    it('aborts immediately when the caller signal is already aborted at call time', async () => {
        // The runtime provider re-uses a single AbortController across turns and aborts the previous one
        // when a new turn starts. If the handler is invoked with an already-aborted signal, it must surface
        // an aborted onComplete so callers can clean up — without it, tool-call cards stay running forever.
        const onComplete = vi.fn();
        const onError = vi.fn();
        const appendToLastAssistantMessage = vi.fn();

        global.fetch = vi.fn().mockImplementation((_url, init: RequestInit) => {
            if ((init.signal as AbortSignal | undefined)?.aborted) {
                return Promise.reject(Object.assign(new Error('Aborted'), {name: 'AbortError'}));
            }

            return Promise.resolve({
                ok: true,
                body: makeStream([]),
            });
        });

        const callerController = new AbortController();

        callerController.abort();

        await openWorkflowSseStream({
            onChunk: appendToLastAssistantMessage,
            onComplete,
            onError,
            signal: callerController.signal,
            streamUrl: '/webhooks/test-id/sse',
        });

        expect(onComplete).toHaveBeenCalledWith(expect.objectContaining({kind: 'aborted'}));
        expect(onError).not.toHaveBeenCalled();
        expect(appendToLastAssistantMessage).not.toHaveBeenCalled();
    });
});

describe('fetchWorkflowResponse', () => {
    beforeEach(() => {
        vi.resetAllMocks();
        mockToastError.mockReset();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('appends JSON message to assistant message', async () => {
        const onComplete = vi.fn();
        const appendToLastAssistantMessage = vi.fn();

        global.fetch = vi.fn().mockResolvedValue({
            headers: {get: () => 'application/json'},
            json: vi.fn().mockResolvedValue({message: 'Workflow result'}),
            ok: true,
        });

        await fetchWorkflowResponse({
            onChunk: appendToLastAssistantMessage,
            onComplete,
            responseUrl: '/webhooks/test-id',
        });

        expect(appendToLastAssistantMessage).toHaveBeenCalledWith('Workflow result');
        expect(onComplete).toHaveBeenCalledOnce();
    });

    it('appends plain-text response to assistant message', async () => {
        const onComplete = vi.fn();
        const appendToLastAssistantMessage = vi.fn();

        global.fetch = vi.fn().mockResolvedValue({
            headers: {get: () => 'text/plain'},
            ok: true,
            text: vi.fn().mockResolvedValue('Plain response'),
        });

        await fetchWorkflowResponse({
            onChunk: appendToLastAssistantMessage,
            onComplete,
            responseUrl: '/webhooks/test-id',
        });

        expect(appendToLastAssistantMessage).toHaveBeenCalledWith('Plain response');
        expect(onComplete).toHaveBeenCalledOnce();
    });

    it('calls onError and skips onComplete when fetch response is not ok', async () => {
        const onComplete = vi.fn();
        const onError = vi.fn();
        const appendToLastAssistantMessage = vi.fn();

        global.fetch = vi.fn().mockResolvedValue({
            ok: false,
            status: 404,
            statusText: 'Not Found',
        });

        await fetchWorkflowResponse({
            onChunk: appendToLastAssistantMessage,
            onComplete,
            onError,
            responseUrl: '/webhooks/bad-id',
        });

        expect(onComplete).not.toHaveBeenCalled();
        expect(onError).toHaveBeenCalledOnce();
        expect(onError.mock.calls[0][0].message).toMatch(/HTTP 404/);
        // The handler no longer toasts directly — it surfaces the failure only via onError so callers can
        // pick a single channel (toast OR retry banner) rather than showing both for the same failure.
        expect(mockToastError).not.toHaveBeenCalled();
        expect(appendToLastAssistantMessage).not.toHaveBeenCalled();
    });

    it('calls onError and skips onComplete when fetch rejects (network error)', async () => {
        const onComplete = vi.fn();
        const onError = vi.fn();
        const appendToLastAssistantMessage = vi.fn();

        global.fetch = vi.fn().mockRejectedValue(new Error('Network error'));

        await fetchWorkflowResponse({
            onChunk: appendToLastAssistantMessage,
            onComplete,
            onError,
            responseUrl: '/webhooks/test-id',
        });

        expect(onComplete).not.toHaveBeenCalled();
        expect(onError).toHaveBeenCalledOnce();
        expect(onError.mock.calls[0][0].message).toMatch(/Network error/);
        expect(onError.mock.calls[0][0].message).toMatch(/Workflow response failed/);
        // The handler no longer toasts directly — it surfaces the failure only via onError so callers can
        // pick a single channel (toast OR retry banner) rather than showing both for the same failure.
        expect(mockToastError).not.toHaveBeenCalled();
        expect(appendToLastAssistantMessage).not.toHaveBeenCalled();
    });

    it('treats AbortError as a clean cancellation: onComplete with aborted=true, no onError, no chunks delivered', async () => {
        // The non-streaming branch passes the caller signal directly to fetch; an abort surfaces as an
        // AbortError. The handler must surface a clean `aborted: true` onComplete (so the consumer can mark
        // tool-call cards complete) without firing onError (an abort is not a failure — no retry banner).
        const onComplete = vi.fn();
        const onError = vi.fn();
        const appendToLastAssistantMessage = vi.fn();

        global.fetch = vi.fn().mockRejectedValue(Object.assign(new Error('Aborted'), {name: 'AbortError'}));

        const controller = new AbortController();

        controller.abort();

        await fetchWorkflowResponse({
            onChunk: appendToLastAssistantMessage,
            onComplete,
            onError,
            responseUrl: '/webhooks/test-id',
            signal: controller.signal,
        });

        expect(onComplete).toHaveBeenCalledWith(expect.objectContaining({kind: 'aborted', chunkCount: 0}));
        expect(onError).not.toHaveBeenCalled();
        expect(appendToLastAssistantMessage).not.toHaveBeenCalled();
    });
});
