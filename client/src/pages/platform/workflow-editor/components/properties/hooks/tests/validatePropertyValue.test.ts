import {describe, expect, it} from 'vitest';

/**
 * Helper function to replicate the validatePropertyValue logic from useProperty.ts.
 * This tests the validation that determines whether a property value is acceptable
 * before saving, including the fix that uses includes('${') instead of startsWith('${')
 * to correctly handle datapill references embedded anywhere in the string.
 */
const validatePropertyValue = ({
    controlType,
    maxLength,
    maxNumberPrecision,
    maxValue,
    minLength,
    minNumberPrecision,
    minValue,
    numberPrecision,
    regex,
    type,
    value,
}: {
    controlType?: string;
    maxLength?: number;
    maxNumberPrecision?: number;
    maxValue?: number;
    minLength?: number;
    minNumberPrecision?: number;
    minValue?: number;
    numberPrecision?: number;
    regex?: string;
    type?: string;
    value: string | number;
}): boolean => {
    const stringValue = typeof value === 'string' ? value : String(value);

    if (typeof value === 'string' && (value.startsWith('=') || value.includes('${'))) {
        return true;
    }

    if ((type === 'INTEGER' || type === 'NUMBER') && typeof value === 'string' && !value.includes('${')) {
        const numericValue = parseFloat(value);

        if (minValue != null && numericValue < minValue) {
            return false;
        }

        if (maxValue != null && numericValue > maxValue) {
            return false;
        }

        if (controlType === 'INTEGER' && !/^-?\d+$/.test(value)) {
            return false;
        }

        if (controlType === 'NUMBER' && !/^-?\d+(\.\d+)?$/.test(value)) {
            return false;
        }

        if (numberPrecision != null && value.includes('.')) {
            const decimalLength = value.split('.')[1]?.length ?? 0;

            if (numberPrecision === 0 || decimalLength > numberPrecision) {
                return false;
            }
        }

        if (value.includes('.')) {
            const decimalLength = value.split('.')[1]?.length ?? 0;

            if (minNumberPrecision != null && decimalLength < minNumberPrecision) {
                return false;
            }

            if (maxNumberPrecision != null && decimalLength > maxNumberPrecision) {
                return false;
            }
        }

        return true;
    }

    if (minLength != null && stringValue.length < minLength) {
        return false;
    }

    if (maxLength != null && stringValue.length > maxLength) {
        return false;
    }

    if (regex) {
        try {
            if (new RegExp(regex).test(stringValue)) {
                return false;
            }
        } catch {
            // Invalid regex from backend; skip regex validation
        }
    }

    return true;
};

describe('validatePropertyValue', () => {
    const baseNumericParams = {
        controlType: 'INTEGER' as string,
        type: 'INTEGER' as string,
    };

    describe('datapill expression bypass', () => {
        it('should bypass validation when value starts with ${', () => {
            const result = validatePropertyValue({
                ...baseNumericParams,
                value: '${trigger_1.output}',
            });

            expect(result).toBe(true);
        });

        it('should bypass validation when value contains ${ in the middle', () => {
            const result = validatePropertyValue({
                ...baseNumericParams,
                value: 'prefix ${trigger_1.output} suffix',
            });

            expect(result).toBe(true);
        });

        it('should bypass validation when value contains multiple datapill references', () => {
            const result = validatePropertyValue({
                ...baseNumericParams,
                value: '${airtable_1.id} and ${airtable_1.name}',
            });

            expect(result).toBe(true);
        });

        it('should bypass validation for formula expressions starting with =', () => {
            const result = validatePropertyValue({
                ...baseNumericParams,
                value: '=someExpression()',
            });

            expect(result).toBe(true);
        });

        it('should bypass validation for formula with embedded datapill', () => {
            const result = validatePropertyValue({
                ...baseNumericParams,
                value: '=${trigger_1.output} + 1',
            });

            expect(result).toBe(true);
        });
    });

    describe('numeric validation when no datapill is present', () => {
        it('should validate a plain integer string', () => {
            const result = validatePropertyValue({
                ...baseNumericParams,
                value: '42',
            });

            expect(result).toBe(true);
        });

        it('should reject non-integer string for INTEGER control type', () => {
            const result = validatePropertyValue({
                ...baseNumericParams,
                value: '42.5',
            });

            expect(result).toBe(false);
        });

        it('should reject value below minValue', () => {
            const result = validatePropertyValue({
                ...baseNumericParams,
                minValue: 10,
                value: '5',
            });

            expect(result).toBe(false);
        });

        it('should reject value above maxValue', () => {
            const result = validatePropertyValue({
                ...baseNumericParams,
                maxValue: 100,
                value: '150',
            });

            expect(result).toBe(false);
        });

        it('should validate NUMBER type with decimal', () => {
            const result = validatePropertyValue({
                controlType: 'NUMBER',
                type: 'NUMBER',
                value: '42.5',
            });

            expect(result).toBe(true);
        });

        it('should reject NUMBER with invalid format', () => {
            const result = validatePropertyValue({
                controlType: 'NUMBER',
                type: 'NUMBER',
                value: '42.5.3',
            });

            expect(result).toBe(false);
        });

        it('should reject when numberPrecision is 0 and value has decimal', () => {
            const result = validatePropertyValue({
                controlType: 'NUMBER',
                numberPrecision: 0,
                type: 'NUMBER',
                value: '42.5',
            });

            expect(result).toBe(false);
        });

        it('should reject when decimal places exceed numberPrecision', () => {
            const result = validatePropertyValue({
                controlType: 'NUMBER',
                numberPrecision: 2,
                type: 'NUMBER',
                value: '42.555',
            });

            expect(result).toBe(false);
        });

        it('should reject when decimal places are below minNumberPrecision', () => {
            const result = validatePropertyValue({
                controlType: 'NUMBER',
                minNumberPrecision: 2,
                type: 'NUMBER',
                value: '42.5',
            });

            expect(result).toBe(false);
        });

        it('should reject when decimal places exceed maxNumberPrecision', () => {
            const result = validatePropertyValue({
                controlType: 'NUMBER',
                maxNumberPrecision: 2,
                type: 'NUMBER',
                value: '42.555',
            });

            expect(result).toBe(false);
        });
    });

    describe('string validation', () => {
        it('should reject when value is shorter than minLength', () => {
            const result = validatePropertyValue({
                minLength: 5,
                type: 'STRING',
                value: 'abc',
            });

            expect(result).toBe(false);
        });

        it('should reject when value exceeds maxLength', () => {
            const result = validatePropertyValue({
                maxLength: 5,
                type: 'STRING',
                value: 'abcdefgh',
            });

            expect(result).toBe(false);
        });

        it('should reject when value matches regex pattern', () => {
            const result = validatePropertyValue({
                regex: '^\\d+$',
                type: 'STRING',
                value: '12345',
            });

            expect(result).toBe(false);
        });

        it('should accept when value does not match regex pattern', () => {
            const result = validatePropertyValue({
                regex: '^\\d+$',
                type: 'STRING',
                value: 'hello',
            });

            expect(result).toBe(true);
        });

        it('should handle invalid regex gracefully', () => {
            const result = validatePropertyValue({
                regex: '[invalid',
                type: 'STRING',
                value: 'hello',
            });

            expect(result).toBe(true);
        });
    });

    describe('number type value with embedded datapill should not enter numeric validation', () => {
        it('should not fail numeric validation for NUMBER type with embedded datapill', () => {
            const result = validatePropertyValue({
                controlType: 'NUMBER',
                maxValue: 100,
                minValue: 0,
                type: 'NUMBER',
                value: 'total is ${trigger_1.amount}',
            });

            expect(result).toBe(true);
        });

        it('should not fail numeric validation for INTEGER type with embedded datapill', () => {
            const result = validatePropertyValue({
                controlType: 'INTEGER',
                maxValue: 100,
                minValue: 0,
                type: 'INTEGER',
                value: 'count: ${step_1.count}',
            });

            expect(result).toBe(true);
        });
    });
});
