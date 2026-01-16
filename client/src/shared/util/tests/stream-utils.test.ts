import {describe, expect, it} from 'vitest';

import {extractStreamChunk} from '../stream-utils';

describe('stream-utils', () => {
    describe('extractStreamChunk', () => {
        it('extracts text field from JSON object', () => {
            const data = {text: 'Hello world'};

            const result = extractStreamChunk(data);

            expect(result).toBe('Hello world');
        });

        it('extracts delta field from JSON object', () => {
            const data = {delta: 'streaming content'};

            const result = extractStreamChunk(data);

            expect(result).toBe('streaming content');
        });

        it('extracts token field from JSON object', () => {
            const data = {token: 'token content'};

            const result = extractStreamChunk(data);

            expect(result).toBe('token content');
        });

        it('extracts message field from JSON object', () => {
            const data = {message: 'message content'};

            const result = extractStreamChunk(data);

            expect(result).toBe('message content');
        });

        it('extracts content[0].text field from nested JSON object', () => {
            const data = {content: [{text: 'nested text'}]};

            const result = extractStreamChunk(data);

            expect(result).toBe('nested text');
        });

        it('extracts content field when it is a string', () => {
            const data = {content: 'direct content'};

            const result = extractStreamChunk(data);

            expect(result).toBe('direct content');
        });

        it('prioritizes text over other fields', () => {
            const data = {content: 'content', delta: 'delta', message: 'message', text: 'text', token: 'token'};

            const result = extractStreamChunk(data);

            expect(result).toBe('text');
        });

        it('falls back to delta when text is not present', () => {
            const data = {content: 'content', delta: 'delta', message: 'message', token: 'token'};

            const result = extractStreamChunk(data);

            expect(result).toBe('delta');
        });

        it('falls back to token when text and delta are not present', () => {
            const data = {content: 'content', message: 'message', token: 'token'};

            const result = extractStreamChunk(data);

            expect(result).toBe('token');
        });

        it('falls back to message when text, delta, and token are not present', () => {
            const data = {content: 'content', message: 'message'};

            const result = extractStreamChunk(data);

            expect(result).toBe('message');
        });

        it('parses JSON string and extracts text', () => {
            const data = '{"text":"parsed text"}';

            const result = extractStreamChunk(data);

            expect(result).toBe('parsed text');
        });

        it('returns raw string when JSON parsing fails', () => {
            const data = 'plain text string';

            const result = extractStreamChunk(data);

            expect(result).toBe('plain text string');
        });

        it('returns empty string for empty object', () => {
            const data = {};

            const result = extractStreamChunk(data);

            expect(result).toBe('');
        });

        it('returns empty string for null', () => {
            const data = null;

            const result = extractStreamChunk(data);

            expect(result).toBe('');
        });

        it('returns empty string for undefined', () => {
            const data = undefined;

            const result = extractStreamChunk(data);

            expect(result).toBe('');
        });

        it('converts non-string chunk to string', () => {
            const data = {text: 12345};

            const result = extractStreamChunk(data);

            expect(result).toBe('12345');
        });

        it('handles boolean values', () => {
            const data = {text: true};

            const result = extractStreamChunk(data);

            expect(result).toBe('true');
        });

        it('handles nested content array with empty first element', () => {
            const data = {content: [{}]};

            const result = extractStreamChunk(data);

            expect(result).toBe('[object Object]');
        });

        it('handles nested content with non-array value', () => {
            const data = {content: 'string content'};

            const result = extractStreamChunk(data);

            expect(result).toBe('string content');
        });

        it('handles nested content array with missing text field', () => {
            const data = {content: [{message: 'no text field'}]};

            const result = extractStreamChunk(data);

            expect(result).toBe('[object Object]');
        });

        it('handles empty string in text field', () => {
            const data = {text: ''};

            const result = extractStreamChunk(data);

            expect(result).toBe('');
        });

        it('handles whitespace-only string', () => {
            const data = {text: '   '};

            const result = extractStreamChunk(data);

            expect(result).toBe('   ');
        });

        it('handles complex nested objects', () => {
            const data = {
                metadata: {timestamp: 12345},
                other: {nested: {data: 'value'}},
                text: 'target text',
            };

            const result = extractStreamChunk(data);

            expect(result).toBe('target text');
        });

        it('handles malformed JSON string', () => {
            const data = '{invalid json}';

            const result = extractStreamChunk(data);

            expect(result).toBe('{invalid json}');
        });

        it('handles array data', () => {
            const data = ['array', 'content'];

            const result = extractStreamChunk(data);

            expect(result).toBe('');
        });

        it('handles empty content array', () => {
            const data = {content: []};

            const result = extractStreamChunk(data);

            expect(result).toBe('');
        });
    });
});
