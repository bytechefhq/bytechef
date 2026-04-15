import {describe, expect, it} from 'vitest';

/**
 * Replicates the resolvedValue branch of saveInputValue in useProperty.ts.
 *
 * Why: issue #4768 — clearing a Date/Datetime/Time field previously persisted an
 * empty string. The backend expects null to represent "unset" for date/time
 * fields, so the save path coerces empty string to null for those control types
 * only (other string fields legitimately store "").
 */
const resolveSaveValue = ({
    controlType,
    isNumericalInput,
    valueToSave,
}: {
    controlType?: string;
    isNumericalInput: boolean;
    valueToSave: unknown;
}): unknown => {
    const isDateOrTimeControlType = controlType === 'DATE' || controlType === 'DATE_TIME' || controlType === 'TIME';

    if (isNumericalInput) {
        return parseFloat(valueToSave as string);
    }

    if (valueToSave === '' && isDateOrTimeControlType) {
        return null;
    }

    return valueToSave;
};

describe('saveInputValue resolved value', () => {
    it('returns null when a DATE field is cleared', () => {
        expect(resolveSaveValue({controlType: 'DATE', isNumericalInput: false, valueToSave: ''})).toBeNull();
    });

    it('returns null when a DATE_TIME field is cleared', () => {
        expect(resolveSaveValue({controlType: 'DATE_TIME', isNumericalInput: false, valueToSave: ''})).toBeNull();
    });

    it('returns null when a TIME field is cleared', () => {
        expect(resolveSaveValue({controlType: 'TIME', isNumericalInput: false, valueToSave: ''})).toBeNull();
    });

    it('preserves non-empty date values', () => {
        expect(resolveSaveValue({controlType: 'DATE', isNumericalInput: false, valueToSave: '2026-04-15'})).toBe(
            '2026-04-15'
        );
    });

    it('preserves empty string for regular text fields', () => {
        expect(resolveSaveValue({controlType: 'TEXT', isNumericalInput: false, valueToSave: ''})).toBe('');
    });

    it('preserves empty string when controlType is undefined', () => {
        expect(resolveSaveValue({isNumericalInput: false, valueToSave: ''})).toBe('');
    });

    it('parses numerical input to float regardless of control type', () => {
        expect(resolveSaveValue({controlType: 'INTEGER', isNumericalInput: true, valueToSave: '42'})).toBe(42);
    });
});
