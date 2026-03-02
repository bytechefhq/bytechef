import {describe, expect, it, vi} from 'vitest';

/**
 * Model of the validation gate + normalization logic from
 * PropertyMentionsInputEditor.tsx's saveMentionInputValue callback.
 *
 * Bug: When deleting a data pill from an INTEGER/NUMBER property, the editor
 * value becomes "". The validateBeforeSave function rejects empty strings for
 * numeric types (they fail the /^-?\d+$/ regex), causing saveMentionInputValue
 * to return early without saving null. The backend keeps the old value, which
 * reappears on tab switch when the workflow definition is refetched.
 *
 * Fix: Skip validation when editorValue is "" — an empty editor means "delete
 * this property", which should always be allowed regardless of type validation.
 */

interface ComputeSaveValueParamsI {
    editorValue: string | number;
    isFormulaMode: boolean;
    type: string;
    validateBeforeSave?: (value: string | number) => boolean;
}

/**
 * Replicates the validation gate and value normalization from
 * saveMentionInputValue. Returns the normalized value that would be
 * saved, or 'BLOCKED' if validation prevented the save.
 */
const computeSaveValue = ({
    editorValue,
    isFormulaMode,
    type,
    validateBeforeSave,
}: ComputeSaveValueParamsI): string | number | null | 'BLOCKED' => {
    const valueForValidation =
        isFormulaMode && typeof editorValue === 'string' && !editorValue.startsWith('=')
            ? `=${editorValue}`
            : editorValue;

    if (validateBeforeSave && editorValue !== '' && !validateBeforeSave(valueForValidation)) {
        return 'BLOCKED';
    }

    let transformedValue: string | number | null = editorValue;

    if (
        !isFormulaMode &&
        (type === 'INTEGER' || type === 'NUMBER') &&
        typeof transformedValue === 'string' &&
        !transformedValue.includes('${')
    ) {
        transformedValue = parseInt(transformedValue);
    }

    const normalizedValue: string | number | null = transformedValue ? transformedValue : null;

    return normalizedValue;
};

/**
 * Simulates validatePropertyValue from useProperty.ts for INTEGER type.
 * Returns false for empty strings and non-numeric strings.
 */
const integerValidator = (value: string | number): boolean => {
    if (typeof value === 'string' && (value.startsWith('=') || value.includes('${'))) {
        return true;
    }

    if (typeof value === 'string') {
        return /^-?\d+$/.test(value);
    }

    return true;
};

describe('emptyValueValidationBypass', () => {
    describe('empty editor value should bypass validation and produce null', () => {
        it('should save null for empty INTEGER property (data pill deleted)', () => {
            const result = computeSaveValue({
                editorValue: '',
                isFormulaMode: false,
                type: 'INTEGER',
                validateBeforeSave: integerValidator,
            });

            expect(result).toBeNull();
        });

        it('should save null for empty NUMBER property (data pill deleted)', () => {
            const result = computeSaveValue({
                editorValue: '',
                isFormulaMode: false,
                type: 'NUMBER',
                validateBeforeSave: integerValidator,
            });

            expect(result).toBeNull();
        });

        it('should save null for empty STRING property', () => {
            const result = computeSaveValue({
                editorValue: '',
                isFormulaMode: false,
                type: 'STRING',
            });

            expect(result).toBeNull();
        });

        it('should not call validateBeforeSave when editor is empty', () => {
            const validator = vi.fn().mockReturnValue(false);

            computeSaveValue({
                editorValue: '',
                isFormulaMode: false,
                type: 'INTEGER',
                validateBeforeSave: validator,
            });

            expect(validator).not.toHaveBeenCalled();
        });
    });

    describe('non-empty values should still be validated', () => {
        it('should block invalid integer values', () => {
            const result = computeSaveValue({
                editorValue: 'abc',
                isFormulaMode: false,
                type: 'INTEGER',
                validateBeforeSave: integerValidator,
            });

            expect(result).toBe('BLOCKED');
        });

        it('should allow valid integer values', () => {
            const result = computeSaveValue({
                editorValue: '42',
                isFormulaMode: false,
                type: 'INTEGER',
                validateBeforeSave: integerValidator,
            });

            expect(result).toBe(42);
        });

        it('should allow datapill values without validation blocking', () => {
            const result = computeSaveValue({
                editorValue: '${trigger_1.output}',
                isFormulaMode: false,
                type: 'INTEGER',
                validateBeforeSave: integerValidator,
            });

            expect(result).toBe('${trigger_1.output}');
        });

        it('should allow expression values', () => {
            const result = computeSaveValue({
                editorValue: '3+3',
                isFormulaMode: true,
                type: 'INTEGER',
                validateBeforeSave: integerValidator,
            });

            expect(result).toBe('3+3');
        });
    });

    describe('demonstrates the bug scenario without the fix', () => {
        /**
         * Without the `editorValue !== ''` guard, this model would return
         * 'BLOCKED' instead of null — the exact bug where deleting a data
         * pill from a numeric property fails silently.
         */
        const computeSaveValueWithoutFix = ({
            editorValue,
            isFormulaMode,
            type,
            validateBeforeSave,
        }: ComputeSaveValueParamsI): string | number | null | 'BLOCKED' => {
            const valueForValidation =
                isFormulaMode && typeof editorValue === 'string' && !editorValue.startsWith('=')
                    ? `=${editorValue}`
                    : editorValue;

            // BUG: No empty check — validation rejects "" for INTEGER
            if (validateBeforeSave && !validateBeforeSave(valueForValidation)) {
                return 'BLOCKED';
            }

            let transformedValue: string | number | null = editorValue;

            if (
                !isFormulaMode &&
                (type === 'INTEGER' || type === 'NUMBER') &&
                typeof transformedValue === 'string' &&
                !transformedValue.includes('${')
            ) {
                transformedValue = parseInt(transformedValue);
            }

            const normalizedValue: string | number | null = transformedValue ? transformedValue : null;

            return normalizedValue;
        };

        it('old code blocks empty INTEGER values (the bug)', () => {
            const result = computeSaveValueWithoutFix({
                editorValue: '',
                isFormulaMode: false,
                type: 'INTEGER',
                validateBeforeSave: integerValidator,
            });

            expect(result).toBe('BLOCKED');
        });

        it('new code allows empty INTEGER values (the fix)', () => {
            const result = computeSaveValue({
                editorValue: '',
                isFormulaMode: false,
                type: 'INTEGER',
                validateBeforeSave: integerValidator,
            });

            expect(result).toBeNull();
        });
    });
});
