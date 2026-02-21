import {describe, expect, it} from 'vitest';

/**
 * Helper function to replicate the parameter cleanup guard logic from useProperty.ts
 * (handleInputTypeSwitchButtonClick). This determines whether the saveProperty(null)
 * call should be skipped when toggling between dynamic (mention) and constant input modes.
 *
 * Returns true if the cleanup should be skipped (i.e., there is nothing to clean up).
 */
const shouldSkipParameterCleanup = ({
    controlType,
    inputValue,
    isNumericalInput,
    mentionInput,
    mentionInputValue,
    parentParameterValue,
    selectValue,
    type,
}: {
    controlType: string;
    inputValue: string;
    isNumericalInput: boolean;
    mentionInput: boolean;
    mentionInputValue: string;
    parentParameterValue: unknown;
    selectValue: string;
    type: string;
}): boolean => {
    if (mentionInput && !mentionInputValue && type !== 'ARRAY') {
        return true;
    } else if (!mentionInput && isNumericalInput && !inputValue) {
        return true;
    } else if (!mentionInput && controlType === 'SELECT' && !selectValue) {
        return true;
    } else if (!parentParameterValue) {
        return true;
    }

    return false;
};

describe('shouldSkipParameterCleanup', () => {
    const baseParams = {
        controlType: 'TEXT',
        inputValue: '',
        isNumericalInput: false,
        mentionInput: false,
        mentionInputValue: '',
        parentParameterValue: {someKey: 'someValue'},
        selectValue: '',
        type: 'STRING',
    };

    describe('mention input guards', () => {
        it('should skip cleanup when mention input has no value and type is not ARRAY', () => {
            const result = shouldSkipParameterCleanup({
                ...baseParams,
                mentionInput: true,
                mentionInputValue: '',
                type: 'STRING',
            });

            expect(result).toBe(true);
        });

        it('should NOT skip cleanup when mention input has a value', () => {
            const result = shouldSkipParameterCleanup({
                ...baseParams,
                mentionInput: true,
                mentionInputValue: '${someExpression}',
                type: 'STRING',
            });

            expect(result).toBe(false);
        });

        it('should NOT skip cleanup when mention input has no value but type is ARRAY', () => {
            const result = shouldSkipParameterCleanup({
                ...baseParams,
                mentionInput: true,
                mentionInputValue: '',
                type: 'ARRAY',
            });

            expect(result).toBe(false);
        });

        it('should NOT skip cleanup when mention input has a value and type is ARRAY', () => {
            const result = shouldSkipParameterCleanup({
                ...baseParams,
                mentionInput: true,
                mentionInputValue: '${items}',
                type: 'ARRAY',
            });

            expect(result).toBe(false);
        });
    });

    describe('numerical input guards', () => {
        it('should skip cleanup when not mention input, numerical input, and no value', () => {
            const result = shouldSkipParameterCleanup({
                ...baseParams,
                inputValue: '',
                isNumericalInput: true,
                mentionInput: false,
            });

            expect(result).toBe(true);
        });

        it('should NOT skip cleanup when numerical input has a value', () => {
            const result = shouldSkipParameterCleanup({
                ...baseParams,
                inputValue: '42',
                isNumericalInput: true,
                mentionInput: false,
            });

            expect(result).toBe(false);
        });
    });

    describe('select input guards', () => {
        it('should skip cleanup when not mention input, SELECT control type, and no value', () => {
            const result = shouldSkipParameterCleanup({
                ...baseParams,
                controlType: 'SELECT',
                mentionInput: false,
                selectValue: '',
            });

            expect(result).toBe(true);
        });

        it('should NOT skip cleanup when SELECT has a value', () => {
            const result = shouldSkipParameterCleanup({
                ...baseParams,
                controlType: 'SELECT',
                mentionInput: false,
                selectValue: 'option1',
            });

            expect(result).toBe(false);
        });
    });

    describe('parent parameter value guard', () => {
        it('should skip cleanup when parent parameter value is falsy', () => {
            const result = shouldSkipParameterCleanup({
                ...baseParams,
                mentionInput: false,
                parentParameterValue: undefined,
            });

            expect(result).toBe(true);
        });

        it('should skip cleanup when parent parameter value is null', () => {
            const result = shouldSkipParameterCleanup({
                ...baseParams,
                mentionInput: false,
                parentParameterValue: null,
            });

            expect(result).toBe(true);
        });

        it('should skip cleanup when parent parameter value is empty string', () => {
            const result = shouldSkipParameterCleanup({
                ...baseParams,
                mentionInput: false,
                parentParameterValue: '',
            });

            expect(result).toBe(true);
        });

        it('should NOT skip cleanup when parent parameter value exists', () => {
            const result = shouldSkipParameterCleanup({
                ...baseParams,
                mentionInput: false,
                parentParameterValue: {key: 'value'},
            });

            expect(result).toBe(false);
        });
    });

    describe('guard precedence', () => {
        it('should check mention input guard before parent parameter value', () => {
            const result = shouldSkipParameterCleanup({
                ...baseParams,
                mentionInput: true,
                mentionInputValue: '',
                parentParameterValue: {key: 'value'},
                type: 'STRING',
            });

            expect(result).toBe(true);
        });

        it('should fall through to parent parameter value when no other guard matches', () => {
            const result = shouldSkipParameterCleanup({
                ...baseParams,
                controlType: 'TEXT',
                isNumericalInput: false,
                mentionInput: false,
                parentParameterValue: undefined,
            });

            expect(result).toBe(true);
        });

        it('should allow cleanup when all guards pass', () => {
            const result = shouldSkipParameterCleanup({
                ...baseParams,
                controlType: 'TEXT',
                inputValue: 'some value',
                isNumericalInput: false,
                mentionInput: false,
                parentParameterValue: {key: 'value'},
            });

            expect(result).toBe(false);
        });
    });
});
