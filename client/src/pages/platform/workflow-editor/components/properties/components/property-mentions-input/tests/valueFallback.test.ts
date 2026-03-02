import {describe, expect, it} from 'vitest';

/**
 * Tests the value-fallback logic from PropertyMentionsInput.tsx.
 *
 * PropertyMentionsInput passes its value to PropertyMentionsInputEditor via:
 *   value={value ?? defaultValue}
 *
 * Previously this used `||` which treated empty string as falsy, causing the
 * defaultValue (often the previously saved value) to overwrite user input when
 * the user cleared the field to empty — making it impossible to delete the
 * last character or data pill.
 *
 * Using `??` ensures only null/undefined trigger the fallback. Empty string,
 * zero, and other falsy-but-valid values are preserved.
 */

/**
 * Replicates the value fallback logic: `value ?? defaultValue`
 */
const computeValueFallback = (
    value: string | number | undefined | null,
    defaultValue?: string
): string | number | undefined | null => {
    return value ?? defaultValue;
};

describe('PropertyMentionsInput value fallback (value ?? defaultValue)', () => {
    describe('should preserve falsy-but-valid values', () => {
        it('should keep empty string instead of falling back to defaultValue', () => {
            const result = computeValueFallback('', '=${trigger_1.name}');

            expect(result).toBe('');
        });

        it('should keep zero instead of falling back to defaultValue', () => {
            const result = computeValueFallback(0, 'default');

            expect(result).toBe(0);
        });

        it('should keep empty string even when defaultValue is a data pill formula', () => {
            const result = computeValueFallback('', '=${airtable_1.fields}');

            expect(result).toBe('');
        });
    });

    describe('should fall back to defaultValue for null/undefined', () => {
        it('should use defaultValue when value is undefined', () => {
            const result = computeValueFallback(undefined, 'default');

            expect(result).toBe('default');
        });

        it('should use defaultValue when value is null', () => {
            const result = computeValueFallback(null, 'default');

            expect(result).toBe('default');
        });
    });

    describe('should pass through truthy values unchanged', () => {
        it('should pass through a regular string', () => {
            const result = computeValueFallback('hello', 'default');

            expect(result).toBe('hello');
        });

        it('should pass through a formula value', () => {
            const result = computeValueFallback('=${trigger_1.id}', 'default');

            expect(result).toBe('=${trigger_1.id}');
        });

        it('should pass through a non-zero number', () => {
            const result = computeValueFallback(42, 'default');

            expect(result).toBe(42);
        });
    });

    describe('should handle missing defaultValue', () => {
        it('should return undefined when both value and defaultValue are undefined', () => {
            const result = computeValueFallback(undefined, undefined);

            expect(result).toBeUndefined();
        });

        it('should return empty string when value is empty and no defaultValue', () => {
            const result = computeValueFallback('', undefined);

            expect(result).toBe('');
        });
    });

    describe('documents the bug that || would cause', () => {
        it('with || operator, empty string would fall through to defaultValue (BUG)', () => {
            // Using typed variables so ESLint doesn't flag constant binary expressions
            const emptyString: string = '';
            const buggyResult = emptyString || '=${trigger_1.name}';

            expect(buggyResult).toBe('=${trigger_1.name}');

            // With ?? operator, empty string is preserved (FIX)
            const fixedResult = emptyString ?? '=${trigger_1.name}';

            expect(fixedResult).toBe('');
        });

        it('with || operator, zero would fall through to defaultValue (BUG)', () => {
            const zero: number = 0;
            const buggyResult = zero || 'default';

            expect(buggyResult).toBe('default');

            const fixedResult = zero ?? 'default';

            expect(fixedResult).toBe(0);
        });
    });
});
