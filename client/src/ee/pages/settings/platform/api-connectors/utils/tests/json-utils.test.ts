import {toast} from 'sonner';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {safeJsonParse} from '../json-utils';

vi.mock('sonner', () => ({toast: {error: vi.fn()}}));

describe('safeJsonParse', () => {
    beforeEach(() => {
        vi.mocked(toast.error).mockReset();

        vi.spyOn(console, 'error').mockImplementation(() => undefined);
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('treats a missing or empty string as an empty object', () => {
        expect(safeJsonParse(undefined)).toEqual({data: {}, success: true});
        expect(safeJsonParse('')).toEqual({data: {}, success: true});
        expect(toast.error).not.toHaveBeenCalled();
    });

    it('parses valid JSON', () => {
        expect(safeJsonParse('{"type":"object","properties":{"id":{"type":"integer"}}}')).toEqual({
            data: {properties: {id: {type: 'integer'}}, type: 'object'},
            success: true,
        });
        expect(toast.error).not.toHaveBeenCalled();
    });

    it('returns an empty object and warns with context on invalid JSON', () => {
        expect(safeJsonParse('{invalid', 'request body schema')).toEqual({data: {}, success: false});

        expect(toast.error).toHaveBeenCalledWith('Invalid JSON', {
            description: 'Invalid JSON in request body schema. Using empty object.',
        });
        expect(console.error).toHaveBeenCalledWith(
            'Failed to parse JSON for request body schema:',
            expect.any(SyntaxError)
        );
    });

    it('uses a generic warning without context', () => {
        expect(safeJsonParse('{invalid')).toEqual({data: {}, success: false});

        expect(toast.error).toHaveBeenCalledWith('Invalid JSON', {
            description: 'Invalid JSON format. Using empty object.',
        });
        expect(console.error).toHaveBeenCalledWith('Failed to parse JSON:', expect.any(SyntaxError));
    });

    it('fails silently when toasts are disabled', () => {
        expect(safeJsonParse('{invalid', 'response schema', false)).toEqual({data: {}, success: false});

        expect(toast.error).not.toHaveBeenCalled();
        expect(console.error).toHaveBeenCalledTimes(1);
    });
});
