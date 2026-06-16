import {describe, expect, it} from 'vitest';

import {getJsonViewCollapsed} from './JsonView';

describe('getJsonViewCollapsed', () => {
    it('keeps small payloads fully expanded', () => {
        expect(getJsonViewCollapsed({key: 'value', nested: {data: 123}})).toBe(false);
    });

    it('collapses large payloads so the tree is not mounted eagerly', () => {
        // An array large enough to serialize beyond the threshold — mirrors the large-file
        // execution output that froze the execution detail view (issue #3378).
        const largeOutput = Array.from({length: 5000}, (_, index) => ({
            id: index,
            name: `branch-${index}`,
            value: 'x'.repeat(40),
        }));

        expect(getJsonViewCollapsed(largeOutput)).toBe(1);
    });

    it('collapses a single very long string value', () => {
        expect(getJsonViewCollapsed({content: 'x'.repeat(200_000)})).toBe(1);
    });

    it('collapses non-serializable (circular) structures instead of throwing', () => {
        const circular: Record<string, unknown> = {};
        circular.self = circular;

        expect(getJsonViewCollapsed(circular)).toBe(1);
    });
});
