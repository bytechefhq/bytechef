import {afterEach, describe, expect, it, vi} from 'vitest';

import {parseWebhookEvents, parseWebhookEventsDetailed} from '../webhook-events';

afterEach(() => {
    vi.restoreAllMocks();
});

describe('parseWebhookEvents', () => {
    it('returns an empty array for null input', () => {
        expect(parseWebhookEvents(null)).toEqual([]);
    });

    it('returns an empty array for undefined input', () => {
        expect(parseWebhookEvents(undefined)).toEqual([]);
    });

    it('returns an empty array for an empty string', () => {
        expect(parseWebhookEvents('')).toEqual([]);
    });

    it('parses a JSON array of strings into the same array', () => {
        expect(parseWebhookEvents('["trace.completed","budget.exceeded"]')).toEqual([
            'trace.completed',
            'budget.exceeded',
        ]);
    });

    it('filters out non-string entries in a mixed array', () => {
        expect(parseWebhookEvents('["trace.completed",42,null,{"foo":1}]')).toEqual(['trace.completed']);
    });

    it('returns an empty array for malformed JSON rather than throwing', () => {
        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

        expect(parseWebhookEvents('{not:json}')).toEqual([]);
        expect(consoleSpy).toHaveBeenCalledOnce();
    });

    it('returns an empty array when the parsed value is not an array (e.g., a bare object)', () => {
        expect(parseWebhookEvents('{"events":["trace.completed"]}')).toEqual([]);
    });

    it('returns an empty array when the parsed value is a bare string', () => {
        expect(parseWebhookEvents('"trace.completed"')).toEqual([]);
    });
});

describe('parseWebhookEventsDetailed', () => {
    it('reports malformed=false for null/undefined/empty input', () => {
        expect(parseWebhookEventsDetailed(null)).toEqual({events: [], malformed: false});
        expect(parseWebhookEventsDetailed(undefined)).toEqual({events: [], malformed: false});
        expect(parseWebhookEventsDetailed('')).toEqual({events: [], malformed: false});
    });

    it('reports malformed=false for a well-formed empty array', () => {
        expect(parseWebhookEventsDetailed('[]')).toEqual({events: [], malformed: false});
    });

    it('reports malformed=false for a valid array of strings', () => {
        expect(parseWebhookEventsDetailed('["trace.completed"]')).toEqual({
            events: ['trace.completed'],
            malformed: false,
        });
    });

    it('reports malformed=true when some entries are non-string', () => {
        const result = parseWebhookEventsDetailed('["trace.completed",42]');

        expect(result.events).toEqual(['trace.completed']);
        expect(result.malformed).toBe(true);
    });

    it('reports malformed=true when JSON is unparseable', () => {
        vi.spyOn(console, 'error').mockImplementation(() => {});

        expect(parseWebhookEventsDetailed('{not:json}')).toEqual({events: [], malformed: true});
    });

    it('reports malformed=true when the parsed value is not an array', () => {
        expect(parseWebhookEventsDetailed('{"events":["trace.completed"]}')).toEqual({
            events: [],
            malformed: true,
        });
    });
});
