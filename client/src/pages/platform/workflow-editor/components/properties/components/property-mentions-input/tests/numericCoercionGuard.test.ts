import {describe, expect, it} from 'vitest';

/**
 * Helper function to replicate the numeric coercion logic from
 * PropertyMentionsInputEditor.tsx's saveMentionInputValue callback.
 *
 * This tests the fix that uses includes('${') instead of startsWith('${')
 * to prevent parseInt() from being applied to strings that contain datapill
 * references embedded anywhere (not just at the start).
 *
 * The original bug: a value like "text ${trigger_1.output} more" would pass
 * the startsWith('${') check and get parseInt()'d, corrupting it to NaN.
 */
const applyNumericCoercion = ({
    isFormulaMode,
    type,
    value,
}: {
    isFormulaMode: boolean;
    type: string;
    value: string | number | null;
}): string | number | null => {
    let transformedValue: string | number | null = value;

    if (
        !isFormulaMode &&
        (type === 'INTEGER' || type === 'NUMBER') &&
        typeof transformedValue === 'string' &&
        !transformedValue.includes('${')
    ) {
        transformedValue = parseInt(transformedValue);
    }

    return transformedValue;
};

describe('numericCoercionGuard', () => {
    describe('should NOT coerce strings containing datapill references', () => {
        it('should preserve value when datapill is at the start', () => {
            const result = applyNumericCoercion({
                isFormulaMode: false,
                type: 'INTEGER',
                value: '${trigger_1.output}',
            });

            expect(result).toBe('${trigger_1.output}');
        });

        it('should preserve value when datapill is in the middle', () => {
            const result = applyNumericCoercion({
                isFormulaMode: false,
                type: 'INTEGER',
                value: 'prefix ${trigger_1.output} suffix',
            });

            expect(result).toBe('prefix ${trigger_1.output} suffix');
        });

        it('should preserve value when datapill is at the end', () => {
            const result = applyNumericCoercion({
                isFormulaMode: false,
                type: 'NUMBER',
                value: 'total: ${step_1.amount}',
            });

            expect(result).toBe('total: ${step_1.amount}');
        });

        it('should preserve value with multiple datapill references', () => {
            const result = applyNumericCoercion({
                isFormulaMode: false,
                type: 'INTEGER',
                value: '${a_1.x} + ${b_1.y}',
            });

            expect(result).toBe('${a_1.x} + ${b_1.y}');
        });
    });

    describe('should coerce plain numeric strings', () => {
        it('should coerce a plain integer string for INTEGER type', () => {
            const result = applyNumericCoercion({
                isFormulaMode: false,
                type: 'INTEGER',
                value: '42',
            });

            expect(result).toBe(42);
        });

        it('should coerce a plain numeric string for NUMBER type', () => {
            const result = applyNumericCoercion({
                isFormulaMode: false,
                type: 'NUMBER',
                value: '99',
            });

            expect(result).toBe(99);
        });

        it('should coerce negative integer string', () => {
            const result = applyNumericCoercion({
                isFormulaMode: false,
                type: 'INTEGER',
                value: '-7',
            });

            expect(result).toBe(-7);
        });
    });

    describe('should skip coercion in formula mode', () => {
        it('should not coerce when isFormulaMode is true', () => {
            const result = applyNumericCoercion({
                isFormulaMode: true,
                type: 'INTEGER',
                value: '42',
            });

            expect(result).toBe('42');
        });
    });

    describe('should skip coercion for non-numeric types', () => {
        it('should not coerce STRING type', () => {
            const result = applyNumericCoercion({
                isFormulaMode: false,
                type: 'STRING',
                value: '42',
            });

            expect(result).toBe('42');
        });

        it('should not coerce BOOLEAN type', () => {
            const result = applyNumericCoercion({
                isFormulaMode: false,
                type: 'BOOLEAN',
                value: 'true',
            });

            expect(result).toBe('true');
        });
    });

    describe('should handle non-string values', () => {
        it('should pass through numeric values unchanged', () => {
            const result = applyNumericCoercion({
                isFormulaMode: false,
                type: 'INTEGER',
                value: 42,
            });

            expect(result).toBe(42);
        });

        it('should pass through null unchanged', () => {
            const result = applyNumericCoercion({
                isFormulaMode: false,
                type: 'INTEGER',
                value: null,
            });

            expect(result).toBeNull();
        });
    });
});
