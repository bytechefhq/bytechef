import {describe, expect, it} from 'vitest';

/**
 * Tests for handleInputTypeSwitchButtonClick fromAi restoration logic.
 *
 * When switching between constant and dynamic modes for properties that have
 * isFromAi set (via component metadata), the handler must restore the fromAi
 * expression instead of clearing values. This prevents the empty gray balloon
 * bug where the editor shows the fromAi balloon styling but with no content.
 *
 * Key invariants:
 * - Switching TO dynamic with isFromAi → restore fromAi expression
 * - Switching TO dynamic without isFromAi → clear values (normal behavior)
 * - Switching TO constant → always clear values regardless of isFromAi
 * - fromAiExpression always has `=` prefix; mentionInputValue needs it stripped
 */

interface InputTypeSwitchResultI {
    mentionInput: boolean;
    mentionInputValue: string;
    propertyParameterValue: string;
    saveValue: string | null | undefined;
    shouldSave: boolean;
}

/**
 * Replicates the decision logic from handleInputTypeSwitchButtonClick in useProperty.ts.
 * Determines the new state after clicking the input type switch button.
 */
const computeInputTypeSwitch = ({
    fromAiExpression = "=fromAi('fieldName')",
    isFromAi = false,
    mentionInput,
}: {
    fromAiExpression?: string;
    isFromAi?: boolean;
    mentionInput: boolean;
}): InputTypeSwitchResultI => {
    const switchingToDynamic = !mentionInput;

    if (switchingToDynamic && isFromAi) {
        return {
            mentionInput: true,
            mentionInputValue: fromAiExpression.substring(1),
            propertyParameterValue: fromAiExpression,
            saveValue: fromAiExpression,
            shouldSave: true,
        };
    }

    return {
        mentionInput: switchingToDynamic,
        mentionInputValue: '',
        propertyParameterValue: '',
        saveValue: null,
        shouldSave: true,
    };
};

describe('handleInputTypeSwitchButtonClick fromAi restoration', () => {
    describe('switching TO dynamic mode with isFromAi', () => {
        it('should restore fromAi expression as mentionInputValue without = prefix', () => {
            const result = computeInputTypeSwitch({
                isFromAi: true,
                mentionInput: false,
            });

            expect(result.mentionInputValue).toBe("fromAi('fieldName')");
        });

        it('should restore fromAi expression as propertyParameterValue with = prefix', () => {
            const result = computeInputTypeSwitch({
                isFromAi: true,
                mentionInput: false,
            });

            expect(result.propertyParameterValue).toBe("=fromAi('fieldName')");
        });

        it('should set mentionInput to true', () => {
            const result = computeInputTypeSwitch({
                isFromAi: true,
                mentionInput: false,
            });

            expect(result.mentionInput).toBe(true);
        });

        it('should save the fromAi expression to backend', () => {
            const result = computeInputTypeSwitch({
                isFromAi: true,
                mentionInput: false,
            });

            expect(result.shouldSave).toBe(true);
            expect(result.saveValue).toBe("=fromAi('fieldName')");
        });

        it('should handle fromAi expression with description', () => {
            const result = computeInputTypeSwitch({
                fromAiExpression: "=fromAi('amount', 'The total price')",
                isFromAi: true,
                mentionInput: false,
            });

            expect(result.mentionInputValue).toBe("fromAi('amount', 'The total price')");
            expect(result.propertyParameterValue).toBe("=fromAi('amount', 'The total price')");
            expect(result.saveValue).toBe("=fromAi('amount', 'The total price')");
        });
    });

    describe('switching TO dynamic mode without isFromAi', () => {
        it('should clear mentionInputValue', () => {
            const result = computeInputTypeSwitch({
                isFromAi: false,
                mentionInput: false,
            });

            expect(result.mentionInputValue).toBe('');
        });

        it('should clear propertyParameterValue', () => {
            const result = computeInputTypeSwitch({
                isFromAi: false,
                mentionInput: false,
            });

            expect(result.propertyParameterValue).toBe('');
        });

        it('should set mentionInput to true', () => {
            const result = computeInputTypeSwitch({
                isFromAi: false,
                mentionInput: false,
            });

            expect(result.mentionInput).toBe(true);
        });

        it('should save null to clear parameter value', () => {
            const result = computeInputTypeSwitch({
                isFromAi: false,
                mentionInput: false,
            });

            expect(result.saveValue).toBeNull();
        });
    });

    describe('switching TO constant mode', () => {
        it('should clear values regardless of isFromAi being true', () => {
            const result = computeInputTypeSwitch({
                isFromAi: true,
                mentionInput: true,
            });

            expect(result.mentionInputValue).toBe('');
            expect(result.propertyParameterValue).toBe('');
        });

        it('should clear values when isFromAi is false', () => {
            const result = computeInputTypeSwitch({
                isFromAi: false,
                mentionInput: true,
            });

            expect(result.mentionInputValue).toBe('');
            expect(result.propertyParameterValue).toBe('');
        });

        it('should set mentionInput to false', () => {
            const result = computeInputTypeSwitch({
                isFromAi: true,
                mentionInput: true,
            });

            expect(result.mentionInput).toBe(false);
        });

        it('should save null to clear parameter when switching to constant', () => {
            const result = computeInputTypeSwitch({
                isFromAi: true,
                mentionInput: true,
            });

            expect(result.saveValue).toBeNull();
        });
    });

    describe('round-trip: constant → dynamic → constant for fromAi property', () => {
        it('should restore fromAi expression after switching to constant and back', () => {
            const fromAiExpression = "=fromAi('price', 'Product price')";

            // Start in dynamic mode (fromAi), switch to constant
            const afterSwitchToConstant = computeInputTypeSwitch({
                fromAiExpression,
                isFromAi: true,
                mentionInput: true,
            });

            expect(afterSwitchToConstant.mentionInput).toBe(false);
            expect(afterSwitchToConstant.mentionInputValue).toBe('');
            expect(afterSwitchToConstant.propertyParameterValue).toBe('');

            // Switch back to dynamic — should restore fromAi expression
            const afterSwitchToDynamic = computeInputTypeSwitch({
                fromAiExpression,
                isFromAi: true,
                mentionInput: false,
            });

            expect(afterSwitchToDynamic.mentionInput).toBe(true);
            expect(afterSwitchToDynamic.mentionInputValue).toBe("fromAi('price', 'Product price')");
            expect(afterSwitchToDynamic.propertyParameterValue).toBe(fromAiExpression);
            expect(afterSwitchToDynamic.saveValue).toBe(fromAiExpression);
        });

        it('should handle multiple round-trips without data loss', () => {
            const fromAiExpression = "=fromAi('quantity')";

            for (let roundTrip = 0; roundTrip < 3; roundTrip++) {
                const toConstant = computeInputTypeSwitch({
                    fromAiExpression,
                    isFromAi: true,
                    mentionInput: true,
                });

                expect(toConstant.mentionInputValue).toBe('');

                const toDynamic = computeInputTypeSwitch({
                    fromAiExpression,
                    isFromAi: true,
                    mentionInput: false,
                });

                expect(toDynamic.mentionInputValue).toBe("fromAi('quantity')");
                expect(toDynamic.propertyParameterValue).toBe(fromAiExpression);
            }
        });
    });

    describe('fromAiExpression = prefix stripping', () => {
        it('should always strip exactly one leading = character', () => {
            const expression = "=fromAi('field')";
            const stripped = expression.substring(1);

            expect(stripped).toBe("fromAi('field')");
            expect(stripped.startsWith('=')).toBe(false);
        });

        it('should handle expression with escaped quotes in description', () => {
            const expression = "=fromAi('name', 'User''s full name')";

            const result = computeInputTypeSwitch({
                fromAiExpression: expression,
                isFromAi: true,
                mentionInput: false,
            });

            expect(result.mentionInputValue).toBe("fromAi('name', 'User''s full name')");
        });
    });
});
