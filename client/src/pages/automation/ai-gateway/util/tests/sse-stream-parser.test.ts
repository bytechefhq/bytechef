import {afterEach, describe, expect, it, vi} from 'vitest';

import {parseStreamChunk, splitSseEvents} from '../sse-stream-parser';

afterEach(() => {
    vi.restoreAllMocks();
});

describe('splitSseEvents', () => {
    it('returns no events and keeps a partial buffer as remainder', () => {
        const result = splitSseEvents('data: {"content":"partial');

        expect(result.events).toEqual([]);
        expect(result.remainder).toBe('data: {"content":"partial');
    });

    it('extracts a single event with the default message name when event: is absent', () => {
        const result = splitSseEvents('data: {"content":"hello"}\n\n');

        expect(result.events).toEqual([{data: '{"content":"hello"}', event: 'message'}]);
        expect(result.remainder).toBe('');
    });

    it('preserves the event name when the server emits event: error', () => {
        const result = splitSseEvents('event: error\ndata: {"message":"budget exceeded"}\n\n');

        expect(result.events).toEqual([{data: '{"message":"budget exceeded"}', event: 'error'}]);
    });

    it('splits multiple complete events and keeps the tail as remainder', () => {
        const buffer = 'data: {"content":"a"}\n\ndata: {"content":"b"}\n\ndata: {"par';
        const result = splitSseEvents(buffer);

        expect(result.events).toEqual([
            {data: '{"content":"a"}', event: 'message'},
            {data: '{"content":"b"}', event: 'message'},
        ]);
        expect(result.remainder).toBe('data: {"par');
    });

    it('ignores events with no data: line so SSE keep-alive comments do not produce phantom chunks', () => {
        const result = splitSseEvents(': keepalive\n\ndata: {"content":"real"}\n\n');

        expect(result.events).toEqual([{data: '{"content":"real"}', event: 'message'}]);
    });

    it('ignores events whose data line is empty', () => {
        const result = splitSseEvents('data:\n\n');

        expect(result.events).toEqual([]);
    });

    it('handles CRLF line endings that some servers emit', () => {
        const result = splitSseEvents('event: error\r\ndata: {"message":"boom"}\r\n\r\n');

        expect(result.events).toEqual([{data: '{"message":"boom"}', event: 'error'}]);
        expect(result.remainder).toBe('');
    });

    it('concatenates multiple data: lines with \\n per the SSE spec', () => {
        const result = splitSseEvents('data: line-one\ndata: line-two\ndata: line-three\n\n');

        expect(result.events).toEqual([{data: 'line-one\nline-two\nline-three', event: 'message'}]);
    });

    it('preserves remainder across consecutive calls so a mid-event split is completed', () => {
        const firstRead = splitSseEvents('data: {"content":"hello ');

        expect(firstRead.events).toEqual([]);

        // Simulate the consumer appending the next chunk to the previous remainder.
        const continued = firstRead.remainder + 'world"}\n\ndata: {"content":"!"}\n\n';
        const secondRead = splitSseEvents(continued);

        expect(secondRead.events).toEqual([
            {data: '{"content":"hello world"}', event: 'message'},
            {data: '{"content":"!"}', event: 'message'},
        ]);
        expect(secondRead.remainder).toBe('');
    });
});

describe('parseStreamChunk', () => {
    it('parses valid JSON into the declared type', () => {
        const chunk = parseStreamChunk<{content: string}>('{"content":"hi"}');

        expect(chunk).toEqual({content: 'hi'});
    });

    it('returns null for malformed JSON and logs to console.error', () => {
        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

        expect(parseStreamChunk('{not json')).toBeNull();
        expect(consoleSpy).toHaveBeenCalledOnce();
    });
});
