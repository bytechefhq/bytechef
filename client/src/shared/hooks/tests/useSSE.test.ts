import {act, renderHook, waitFor} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {SSERequestType, useSSE} from '../useSSE';

const encoder = new TextEncoder();

function makeStream() {
    let controller: ReadableStreamDefaultController<Uint8Array> | null = null;
    const stream = new ReadableStream<Uint8Array>({
        start(c) {
            controller = c;
        },
    });
    return {
        close: () => controller?.close(),
        controller: () => controller!,
        enqueue: (s: string) => controller?.enqueue(encoder.encode(s)),
        stream,
    } as const;
}

describe('useSSE', () => {
    const originalFetch = global.fetch;

    afterEach(() => {
        global.fetch = originalFetch;
        vi.restoreAllMocks();
    });

    it('dispatches custom handlers and sets connection state', async () => {
        const rs = makeStream();
        const fetchSpy = vi
            .fn<typeof fetch>()
            .mockImplementation(() =>
                Promise.resolve(new Response(rs.stream, {headers: {'Content-Type': 'text/event-stream'}, status: 200}))
            );

        global.fetch = fetchSpy;

        const onStart = vi.fn();
        const onResult = vi.fn();

        const req: SSERequestType = {init: {method: 'GET'}, url: '/sse'};
        const {result} = renderHook(() => useSSE(req, {eventHandlers: {result: onResult, start: onStart}}));

        // initial state
        expect(result.current.connectionState).toBe('CONNECTING');

        // push two events
        act(() => {
            rs.enqueue('event: start\n');
            rs.enqueue('data: {"jobId":"123"}\n\n');
            rs.enqueue('event: result\n');
            rs.enqueue('data: {"ok":true}\n\n');
            rs.close();
        });

        // handlers called with objects (async)
        await waitFor(() => expect(onStart).toHaveBeenCalledWith({jobId: '123'}));
        await waitFor(() => expect(onResult).toHaveBeenCalledWith({ok: true}));

        // stream closed
        await waitFor(() => expect(result.current.connectionState).toBe('CLOSED'));
        expect(result.current.error).toBeNull();
        expect(fetchSpy).toHaveBeenCalledTimes(1);
    });

    it('uses default handler when no custom event matches and exposes parsed data', async () => {
        const rs = makeStream();

        const fetchSpy = vi.fn<typeof fetch>().mockResolvedValue(new Response(rs.stream, {status: 200}));
        global.fetch = fetchSpy;

        const req: SSERequestType = {init: {method: 'GET'}, url: '/sse'};
        const {result} = renderHook(() => useSSE<{num: number}>(req));

        act(() => {
            rs.enqueue('data: {"num":7}\n\n');
            rs.close();
        });

        await waitFor(() => expect(result.current.data).toEqual({num: 7}));
        await waitFor(() => expect(result.current.connectionState).toBe('CLOSED'));
    });

    it('close() aborts the fetch and moves to CLOSED without error', async () => {
        const rs = makeStream();
        const abortSpy = vi.fn();
        /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
        const fetchMock = vi.fn<typeof fetch>().mockImplementation((input: any, init?: RequestInit) => {
            // Capture abort signal and spy on abort
            (init?.signal as AbortSignal | undefined)?.addEventListener('abort', abortSpy);
            return Promise.resolve(new Response(rs.stream, {status: 200}));
        });

        global.fetch = fetchMock;

        const req: SSERequestType = {init: {method: 'GET'}, url: '/sse'};
        const {result} = renderHook(() => useSSE(req));

        act(() => {
            result.current.close();
        });

        expect(abortSpy).toHaveBeenCalled();
        await waitFor(() => expect(result.current.connectionState).toBe('CLOSED'));
        expect(result.current.error).toBeNull();
    });

    it('updates to latest handlers without reconnecting', async () => {
        const rs = makeStream();
        const fetchSpy = vi.fn<typeof fetch>().mockResolvedValue(new Response(rs.stream, {status: 200}));

        global.fetch = fetchSpy;

        const h1 = {start: vi.fn()};
        const req: SSERequestType = {init: {method: 'GET'}, url: '/sse'};
        const {rerender} = renderHook(({handlers}) => useSSE(req, {eventHandlers: handlers}), {
            initialProps: {handlers: h1},
        });

        // Update handlers after mount
        const h2 = {start: vi.fn()};
        rerender({handlers: h2});

        // Emit start event after update -> only new handler should be called
        act(() => {
            rs.enqueue('event: start\n');
            rs.enqueue('data: {"jobId":"99"}\n\n');
            rs.close();
        });

        expect(h1.start).not.toHaveBeenCalled();
        await waitFor(() => expect(h2.start).toHaveBeenCalledWith({jobId: '99'}));
        expect(fetchSpy).toHaveBeenCalledTimes(1); // no reconnect
    });

    it('preserves spaces between words in streamed data', async () => {
        const rs = makeStream();
        const fetchSpy = vi
            .fn<typeof fetch>()
            .mockImplementation(() =>
                Promise.resolve(new Response(rs.stream, {headers: {'Content-Type': 'text/event-stream'}, status: 200}))
            );

        global.fetch = fetchSpy;

        const onStream = vi.fn();

        const req: SSERequestType = {init: {method: 'GET'}, url: '/sse'};
        renderHook(() => useSSE(req, {eventHandlers: {stream: onStream}}));

        // Push events with leading spaces
        act(() => {
            // Typical SSE data line has a space after "data:"
            // Specification says: "If line starts with 'data:', then: Let value be the subset of line from the first character after the colon to the end of the line. If value starts with a U+0020 SPACE character, remove it from value."

            // Case 1: "data:Hello" -> should result in "Hello"
            rs.enqueue('event: stream\n');
            rs.enqueue('data:Hello\n\n');

            // Case 2: "data: Hello" (one space) -> should result in "Hello" (spec says remove it)
            rs.enqueue('event: stream\n');
            rs.enqueue('data: Hello\n\n');

            // Case 3: "data:  how" (two spaces) -> should result in " how"
            rs.enqueue('event: stream\n');
            rs.enqueue('data:  how\n\n');

            // Case 4: JSON encoded " world" (robust way)
            // "data: \" world\"" (one space before quote from protocol) -> results in "\" world\"" -> JSON.parse -> " world"
            rs.enqueue('event: stream\n');
            rs.enqueue('data: " world"\n\n');

            rs.close();
        });

        await waitFor(() => expect(onStream).toHaveBeenNthCalledWith(1, 'Hello'));
        await waitFor(() => expect(onStream).toHaveBeenNthCalledWith(2, 'Hello'));
        await waitFor(() => expect(onStream).toHaveBeenNthCalledWith(3, ' how'));
        await waitFor(() => expect(onStream).toHaveBeenNthCalledWith(4, ' world'));
    });

    it('supports CRLF as event and line separator', async () => {
        const rs = makeStream();
        const fetchSpy = vi
            .fn<typeof fetch>()
            .mockImplementation(() =>
                Promise.resolve(new Response(rs.stream, {headers: {'Content-Type': 'text/event-stream'}, status: 200}))
            );

        global.fetch = fetchSpy;

        const onStream = vi.fn();

        const req: SSERequestType = {init: {method: 'GET'}, url: '/sse'};
        renderHook(() => useSSE(req, {eventHandlers: {stream: onStream}}));

        act(() => {
            // Use CRLF
            rs.enqueue('event: stream\r\n');
            rs.enqueue('data: line1\r\n');
            rs.enqueue('data: line2\r\n\r\n');

            // Mixed CRLF and LF just in case
            rs.enqueue('event: stream\n');
            rs.enqueue('data: line3\r\n\r\n');

            rs.close();
        });

        await waitFor(() => expect(onStream).toHaveBeenCalledWith('line1\nline2'));
        await waitFor(() => expect(onStream).toHaveBeenCalledWith('line3'));
    });
});
