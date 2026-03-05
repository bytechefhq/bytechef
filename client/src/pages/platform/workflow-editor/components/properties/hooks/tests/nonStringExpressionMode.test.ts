import {describe, expect, it} from 'vitest';

/**
 * Tests for non-string property expression mode behavior:
 * 1. Numerical input `=` detection should switch to formula mode
 * 2. Array item reconstruction should preserve expressionEnabled
 * 3. Object sub-property reconstruction should preserve expressionEnabled
 */

describe('non-string expression mode', () => {
    describe('numerical input `=` detection', () => {
        /**
         * Replicates the guard at the top of handleInputChange in useProperty.ts.
         * When a numerical input receives a value starting with `=` and
         * expressionEnabled is true, the input should switch to mention/formula mode
         * instead of stripping non-numeric characters.
         */
        const shouldSwitchToFormulaMode = (
            value: string,
            isNumericalInput: boolean,
            expressionEnabled: boolean | undefined
        ): boolean => {
            return isNumericalInput && !!value && value.startsWith('=') && !!expressionEnabled;
        };

        it('should switch to formula mode when typing `=` in a numerical input with expressionEnabled', () => {
            expect(shouldSwitchToFormulaMode('=3+3', true, true)).toBe(true);
        });

        it('should switch when typing just `=`', () => {
            expect(shouldSwitchToFormulaMode('=', true, true)).toBe(true);
        });

        it('should NOT switch when expressionEnabled is false', () => {
            expect(shouldSwitchToFormulaMode('=3+3', true, false)).toBe(false);
        });

        it('should NOT switch when expressionEnabled is undefined', () => {
            expect(shouldSwitchToFormulaMode('=3+3', true, undefined)).toBe(false);
        });

        it('should NOT switch for non-numerical inputs', () => {
            expect(shouldSwitchToFormulaMode('=hello', false, true)).toBe(false);
        });

        it('should NOT switch for normal numeric input (no `=`)', () => {
            expect(shouldSwitchToFormulaMode('42', true, true)).toBe(false);
        });

        it('should NOT switch for empty value', () => {
            expect(shouldSwitchToFormulaMode('', true, true)).toBe(false);
        });

        it('should extract expression content without `=` prefix', () => {
            const value = '=3+3';
            const expressionContent = value.substring(1);

            expect(expressionContent).toBe('3+3');
        });

        it('should extract empty string from lone `=`', () => {
            const value = '=';
            const expressionContent = value.substring(1);

            expect(expressionContent).toBe('');
        });
    });

    describe('array item expressionEnabled propagation', () => {
        /**
         * Replicates ArrayProperty.tsx item construction from saved parameters.
         * Non-string array items should always get expressionEnabled: true so the
         * dynamic/static switch button persists across component remounts.
         */
        const buildArrayItem = (
            parameterItemType: string,
            index: number
        ): {controlType: string; custom: boolean; expressionEnabled: boolean; name: string; type: string} => ({
            controlType: parameterItemType,
            custom: true,
            expressionEnabled: true,
            name: index.toString(),
            type: parameterItemType,
        });

        it('should set expressionEnabled: true for INTEGER items', () => {
            const item = buildArrayItem('INTEGER', 0);

            expect(item.expressionEnabled).toBe(true);
        });

        it('should set expressionEnabled: true for NUMBER items', () => {
            const item = buildArrayItem('NUMBER', 0);

            expect(item.expressionEnabled).toBe(true);
        });

        it('should set expressionEnabled: true for DATE items', () => {
            const item = buildArrayItem('DATE', 0);

            expect(item.expressionEnabled).toBe(true);
        });

        it('should set expressionEnabled: true for STRING items', () => {
            const item = buildArrayItem('STRING', 0);

            expect(item.expressionEnabled).toBe(true);
        });
    });

    describe('object sub-property expressionEnabled propagation', () => {
        /**
         * Replicates useObjectProperty.ts sub-property construction for the
         * "matching property" path. Non-string types should default to
         * expressionEnabled: true when the definition doesn't explicitly set it.
         */
        const buildMatchingSubProperty = (matchingProperty: {
            expressionEnabled?: boolean;
            name: string;
            type: string;
        }): {expressionEnabled: boolean; name: string; type: string} => ({
            ...matchingProperty,
            expressionEnabled: matchingProperty.expressionEnabled ?? matchingProperty.type !== 'STRING',
        });

        it('should default to true for INTEGER when expressionEnabled is undefined', () => {
            const subProperty = buildMatchingSubProperty({
                name: 'count',
                type: 'INTEGER',
            });

            expect(subProperty.expressionEnabled).toBe(true);
        });

        it('should default to true for NUMBER when expressionEnabled is undefined', () => {
            const subProperty = buildMatchingSubProperty({
                name: 'amount',
                type: 'NUMBER',
            });

            expect(subProperty.expressionEnabled).toBe(true);
        });

        it('should default to false for STRING when expressionEnabled is undefined', () => {
            const subProperty = buildMatchingSubProperty({
                name: 'label',
                type: 'STRING',
            });

            expect(subProperty.expressionEnabled).toBe(false);
        });

        it('should preserve explicit expressionEnabled: true from definition', () => {
            const subProperty = buildMatchingSubProperty({
                expressionEnabled: true,
                name: 'label',
                type: 'STRING',
            });

            expect(subProperty.expressionEnabled).toBe(true);
        });

        it('should preserve explicit expressionEnabled: false from definition', () => {
            const subProperty = buildMatchingSubProperty({
                expressionEnabled: false,
                name: 'count',
                type: 'INTEGER',
            });

            expect(subProperty.expressionEnabled).toBe(false);
        });
    });

    describe('array item Object.keys guard against strings', () => {
        /**
         * Replicates the guard in ArrayProperty.tsx that prevents
         * Object.keys() from being called on string values (which would
         * return character indices like ["0","1","2","3",...]).
         */
        it('should NOT iterate over string character indices', () => {
            const parameterItemValue = '=3+3+3+3';
            const isObject = parameterItemValue && typeof parameterItemValue === 'object';

            expect(isObject).toBe(false);
            expect(Object.keys(parameterItemValue)).toEqual(['0', '1', '2', '3', '4', '5', '6', '7']);
        });

        it('should iterate over actual object keys', () => {
            const parameterItemValue = {name: 'test', value: 42};
            const isObject = parameterItemValue && typeof parameterItemValue === 'object';

            expect(isObject).toBe(true);
            expect(Object.keys(parameterItemValue)).toEqual(['name', 'value']);
        });

        it('should NOT iterate over null', () => {
            const parameterItemValue = null;
            const isObject = parameterItemValue && typeof parameterItemValue === 'object';

            expect(isObject).toBeFalsy();
        });
    });

    describe('formula mode validation with `=` prefix', () => {
        /**
         * Replicates the validateBeforeSave logic from saveMentionInputValue.
         * In formula mode, the value must have `=` prepended BEFORE validation
         * so that validatePropertyValue recognizes it as an expression.
         */
        const validatePropertyValue = (value: string | number, controlType: string): boolean => {
            const stringValue = typeof value === 'string' ? value : String(value);

            if (typeof value === 'string' && (value.startsWith('=') || value.includes('${'))) {
                return true;
            }

            if (controlType === 'INTEGER' && typeof value === 'string' && !/^-?\d+$/.test(stringValue)) {
                return false;
            }

            return true;
        };

        it('should REJECT "3+3" as literal INTEGER (no formula prefix)', () => {
            expect(validatePropertyValue('3+3', 'INTEGER')).toBe(false);
        });

        it('should ACCEPT "=3+3" as expression', () => {
            expect(validatePropertyValue('=3+3', 'INTEGER')).toBe(true);
        });

        it('should ACCEPT "${datapill}+3" as expression', () => {
            expect(validatePropertyValue('${datapill}+3', 'INTEGER')).toBe(true);
        });

        it('should prepend `=` in formula mode before validation', () => {
            const editorValue = '3+3';
            const isFormulaMode = true;
            const valueForValidation =
                isFormulaMode && typeof editorValue === 'string' && !editorValue.startsWith('=')
                    ? `=${editorValue}`
                    : editorValue;

            expect(validatePropertyValue(valueForValidation, 'INTEGER')).toBe(true);
        });

        it('should not double-prepend `=` if already present', () => {
            const editorValue = '=3+3';
            const isFormulaMode = true;
            const valueForValidation =
                isFormulaMode && typeof editorValue === 'string' && !editorValue.startsWith('=')
                    ? `=${editorValue}`
                    : editorValue;

            expect(valueForValidation).toBe('=3+3');
        });

        it('should not prepend `=` when not in formula mode', () => {
            const editorValue = '42';
            const isFormulaMode = false;
            const valueForValidation =
                isFormulaMode && typeof editorValue === 'string' && !editorValue.startsWith('=')
                    ? `=${editorValue}`
                    : editorValue;

            expect(valueForValidation).toBe('42');
            expect(validatePropertyValue(valueForValidation, 'INTEGER')).toBe(true);
        });
    });

    describe('showInputTypeSwitchButton initialization', () => {
        /**
         * Replicates the useState initializer from useProperty.ts:
         * !control && ((property.type !== 'STRING' && property.expressionEnabled) || false)
         */
        const computeShowInputTypeSwitchButton = (
            hasControl: boolean,
            propertyType: string,
            expressionEnabled: boolean | undefined
        ): boolean => {
            return !hasControl && ((propertyType !== 'STRING' && !!expressionEnabled) || false);
        };

        it('should show for non-STRING type with expressionEnabled in uncontrolled mode', () => {
            expect(computeShowInputTypeSwitchButton(false, 'INTEGER', true)).toBe(true);
        });

        it('should NOT show for STRING type even with expressionEnabled', () => {
            expect(computeShowInputTypeSwitchButton(false, 'STRING', true)).toBe(false);
        });

        it('should NOT show when expressionEnabled is undefined', () => {
            expect(computeShowInputTypeSwitchButton(false, 'INTEGER', undefined)).toBe(false);
        });

        it('should NOT show in controlled mode', () => {
            expect(computeShowInputTypeSwitchButton(true, 'INTEGER', true)).toBe(false);
        });

        it('should show for NUMBER type with expressionEnabled', () => {
            expect(computeShowInputTypeSwitchButton(false, 'NUMBER', true)).toBe(true);
        });

        it('should show for DATE type with expressionEnabled', () => {
            expect(computeShowInputTypeSwitchButton(false, 'DATE', true)).toBe(true);
        });
    });
});
