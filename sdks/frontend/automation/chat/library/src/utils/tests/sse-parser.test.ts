import {describe, expect, it, vi} from 'vitest';

import {drainSseResponse, parseAndDispatchSSE} from '@/utils/sse-parser';

const encoder = new TextEncoder();

function createStreamResponse(chunks: string[]): Response {
    const stream = new ReadableStream<Uint8Array>({
        start(controller) {
            for (const chunk of chunks) {
                controller.enqueue(encoder.encode(chunk));
            }

            controller.close();
        },
    });

    return new Response(stream, {headers: {'Content-Type': 'text/event-stream'}});
}

describe('parseAndDispatchSSE', () => {
    it('dispatches a named event with parsed JSON data to its handler', () => {
        const onDefaultMessage = vi.fn();
        const onResult = vi.fn();

        parseAndDispatchSSE('event: result\ndata: {"message":"done"}\n\n', onDefaultMessage, {result: onResult});

        expect(onResult).toHaveBeenCalledWith({message: 'done'});
        expect(onDefaultMessage).not.toHaveBeenCalled();
    });

    it('sends an event without a name, or without a handler, to the default handler', () => {
        const onDefaultMessage = vi.fn();

        parseAndDispatchSSE('data: plain text\n\nevent: unknown\ndata: 42\n\n', onDefaultMessage, {});

        expect(onDefaultMessage).toHaveBeenNthCalledWith(1, 'plain text');
        expect(onDefaultMessage).toHaveBeenNthCalledWith(2, 42);
    });

    it('joins multi-line data with newlines and keeps text that is not JSON', () => {
        const onStream = vi.fn();

        parseAndDispatchSSE('event: stream\ndata: first line\ndata: second line\n\n', vi.fn(), {stream: onStream});

        expect(onStream).toHaveBeenCalledWith('first line\nsecond line');
    });

    it('strips only the single space after the field colon', () => {
        const onStream = vi.fn();

        parseAndDispatchSSE('event:stream\ndata:  indented\n\n', vi.fn(), {stream: onStream});

        expect(onStream).toHaveBeenCalledWith(' indented');
    });

    it('supports CRLF separators', () => {
        const onResult = vi.fn();

        parseAndDispatchSSE('event: result\r\ndata: {"ok":true}\r\n\r\n', vi.fn(), {result: onResult});

        expect(onResult).toHaveBeenCalledWith({ok: true});
    });

    it('keeps dispatching later events when a handler throws', () => {
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
        const onResult = vi.fn();

        parseAndDispatchSSE('event: boom\ndata: 1\n\nevent: result\ndata: 2\n\n', vi.fn(), {
            boom: () => {
                throw new Error('handler failed');
            },
            result: onResult,
        });

        expect(onResult).toHaveBeenCalledWith(2);
        expect(consoleErrorSpy).toHaveBeenCalled();

        consoleErrorSpy.mockRestore();
    });
});

describe('drainSseResponse', () => {
    it('dispatches events split across chunks and flushes the trailing event', async () => {
        const onStream = vi.fn();
        const onResult = vi.fn();

        await drainSseResponse(
            createStreamResponse(['event: stream\nda', 'ta: "Hel', 'lo"\n\nevent: result\n', 'data: {"message":"Hi"}']),
            {result: onResult, stream: onStream}
        );

        expect(onStream).toHaveBeenCalledWith('Hello');
        expect(onResult).toHaveBeenCalledWith({message: 'Hi'});
    });

    it('dispatches the approval request event of a resumed run', async () => {
        const onApprovalRequest = vi.fn();

        await drainSseResponse(
            createStreamResponse(['event: approval_request\ndata: {"resumeId":"abc","formTitle":"Approve"}\n\n']),
            {approval_request: onApprovalRequest}
        );

        expect(onApprovalRequest).toHaveBeenCalledWith({formTitle: 'Approve', resumeId: 'abc'});
    });

    it('returns without dispatching when the response has no body', async () => {
        const onResult = vi.fn();

        await drainSseResponse(new Response(null, {status: 204}), {result: onResult});

        expect(onResult).not.toHaveBeenCalled();
    });
});
