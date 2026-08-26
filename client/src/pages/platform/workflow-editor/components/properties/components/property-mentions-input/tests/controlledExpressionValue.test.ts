import {describe, expect, it} from 'vitest';

import {reconstructControlledExpressionValue} from '../controlledExpressionValue';

describe('reconstructControlledExpressionValue', () => {
    it('prefixes = when missing', () => {
        expect(reconstructControlledExpressionValue('concat(a, b)')).toBe('=concat(a, b)');
    });

    it('keeps an existing = prefix', () => {
        expect(reconstructControlledExpressionValue('=concat(a, b)')).toBe('=concat(a, b)');
    });

    it('returns empty for blank content', () => {
        expect(reconstructControlledExpressionValue('   ')).toBe('');
        expect(reconstructControlledExpressionValue('')).toBe('');
    });

    it('coerces numbers to a prefixed string', () => {
        expect(reconstructControlledExpressionValue(42)).toBe('=42');
    });
});
