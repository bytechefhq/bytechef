import {describe, expect, it} from 'vitest';

import getTestNodeStateBorderClassName from './getTestNodeStateBorderClassName';

describe('getTestNodeStateBorderClassName', () => {
    it('maps a blocked call to an amber border', () => {
        expect(getTestNodeStateBorderClassName({status: 'BLOCKED'})).toBe('border-amber-500 hover:border-amber-500');
    });

    it('maps a withheld call to an amber border', () => {
        expect(getTestNodeStateBorderClassName({status: 'WITHHELD'})).toBe('border-amber-500 hover:border-amber-500');
    });

    it('maps a redacted call to a sky border', () => {
        expect(getTestNodeStateBorderClassName({status: 'REDACTED'})).toBe('border-sky-500 hover:border-sky-500');
    });

    it('keeps the running, completed and failed borders', () => {
        expect(getTestNodeStateBorderClassName({status: 'RUNNING'})).toBe('border-blue-500 hover:border-blue-500');
        expect(getTestNodeStateBorderClassName({status: 'COMPLETED'})).toBe('border-green-500 hover:border-green-500');
        expect(getTestNodeStateBorderClassName({status: 'FAILED'})).toBe('border-red-500 hover:border-red-500');
    });

    it('has no border without a state', () => {
        expect(getTestNodeStateBorderClassName(undefined)).toBeUndefined();
    });
});
