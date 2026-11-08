import {describe, expect, it} from 'vitest';

import {parseStreamChunk, splitSseEvents} from '../sse-stream-parser';

interface PlaygroundStreamChunkI {
    content: string | null;
    cost: number | null;
    finished: boolean;
    inputTokens: number | null;
    latencyMs: number | null;
    outputTokens: number | null;
    traceId: number | null;
}

interface ConsumeResultI {
    accumulated: string;
    error: string | null;
    finalChunk: PlaygroundStreamChunkI | null;
}

const consumeChunks = (rawChunks: string[]): ConsumeResultI => {
    let buffer = '';
    let accumulated = '';
    let finalChunk: PlaygroundStreamChunkI | null = null;
    let error: string | null = null;

    for (const raw of rawChunks) {
        buffer += raw;

        const {events, remainder} = splitSseEvents(buffer);

        buffer = remainder;

        for (const {data: json, event: eventName} of events) {
            if (eventName === 'error') {
                const errorChunk = parseStreamChunk<{message?: string}>(json);

                error = errorChunk?.message || 'Stream error from server';

                return {accumulated, error, finalChunk};
            }

            const chunk = parseStreamChunk<PlaygroundStreamChunkI>(json);

            if (chunk == null) {
                continue;
            }

            if (chunk.finished) {
                finalChunk = chunk;
            } else if (chunk.content) {
                accumulated += chunk.content;
            }
        }
    }

    return {accumulated, error, finalChunk};
};

describe('playground stream consumer', () => {
    it('accumulates content chunks into a single string and surfaces the final chunk', () => {
        const raw = [
            'data: {"content":"Hello","finished":false}\n\n',
            'data: {"content":" world","finished":false}\n\n',
            'data: {"content":null,"finished":true,"inputTokens":5,"outputTokens":2,"latencyMs":123,"cost":0.001,"traceId":42}\n\n',
        ];

        const result = consumeChunks(raw);

        expect(result.error).toBeNull();
        expect(result.accumulated).toBe('Hello world');
        expect(result.finalChunk).toEqual({
            content: null,
            cost: 0.001,
            finished: true,
            inputTokens: 5,
            latencyMs: 123,
            outputTokens: 2,
            traceId: 42,
        });
    });

    it('routes event: error frames to the error branch instead of mis-parsing as a content chunk', () => {
        const raw = [
            'data: {"content":"partial","finished":false}\n\n',
            'event: error\ndata: {"message":"Budget exceeded for workspace 42"}\n\n',
            'data: {"content":" never seen","finished":false}\n\n',
        ];

        const result = consumeChunks(raw);

        expect(result.error).toBe('Budget exceeded for workspace 42');
        expect(result.accumulated).toBe('partial');
    });

    it('survives a malformed chunk without aborting the stream', () => {
        const raw = [
            'data: {"content":"a","finished":false}\n\n',
            'data: {not-json\n\n',
            'data: {"content":"b","finished":false}\n\n',
        ];

        const result = consumeChunks(raw);

        expect(result.error).toBeNull();
        expect(result.accumulated).toBe('ab');
    });

    it('completes a stream whose boundary falls mid-event across two reads', () => {
        const raw = [
            'data: {"content":"hel',
            'lo","finished":false}\n\ndata: {"content":" world","finished":false}\n\n',
        ];

        const result = consumeChunks(raw);

        expect(result.accumulated).toBe('hello world');
    });
});
