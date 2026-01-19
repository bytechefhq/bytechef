import {describe, expect, it} from 'vitest';

/**
 * Helper function to replicate the display condition index replacement logic from useProperty.ts
 * This tests the logic that replaces [index] placeholders with actual indices from the path.
 */
const replaceDisplayConditionIndexes = (displayCondition: string, path: string): string => {
    const displayConditionIndexes: number[] = [];
    const bracketedNumberRegex = /\[(\d+)\]/g;
    let match;

    while ((match = bracketedNumberRegex.exec(path)) !== null) {
        displayConditionIndexes.push(parseInt(match[1], 10));
    }

    // Replace [index] placeholders with actual indices from path
    // For nested arrays, replace sequentially (first [index] with first path index, etc.)
    displayConditionIndexes.forEach((index) => {
        displayCondition = displayCondition.replace('[index]', `[${index}]`);
    });

    // Handle compound conditions with multiple same-level array references
    // e.g., "inputs[index].fieldType == 7 and inputs[index].multipleChoice == true"
    // After sequential replacement, any remaining [index] should use the last index
    if (displayConditionIndexes.length > 0 && displayCondition.includes('[index]')) {
        const lastIndex = displayConditionIndexes[displayConditionIndexes.length - 1];

        while (displayCondition.includes('[index]')) {
            displayCondition = displayCondition.replace('[index]', `[${lastIndex}]`);
        }
    }

    return displayCondition;
};

describe('Display Condition Index Replacement', () => {
    describe('single [index] placeholder', () => {
        it('should replace single [index] with array index from path', () => {
            const displayCondition = 'inputs[index].fieldType != 12';
            const path = 'inputs[0].fieldType';

            const result = replaceDisplayConditionIndexes(displayCondition, path);

            expect(result).toBe('inputs[0].fieldType != 12');
        });

        it('should replace with different index values', () => {
            const displayCondition = 'inputs[index].fieldType == 7';
            const path = 'inputs[2].multipleChoice';

            const result = replaceDisplayConditionIndexes(displayCondition, path);

            expect(result).toBe('inputs[2].fieldType == 7');
        });
    });

    describe('compound conditions with same-level array references', () => {
        it('should replace all [index] with same value for compound condition with "and"', () => {
            const displayCondition = 'inputs[index].fieldType == 7 and inputs[index].multipleChoice == true';
            const path = 'inputs[0].multipleChoice';

            const result = replaceDisplayConditionIndexes(displayCondition, path);

            expect(result).toBe('inputs[0].fieldType == 7 and inputs[0].multipleChoice == true');
        });

        it('should replace all [index] with same value for compound condition with "or"', () => {
            const displayCondition = 'inputs[index].fieldType == 7 or inputs[index].fieldType == 11';
            const path = 'inputs[1].fieldName';

            const result = replaceDisplayConditionIndexes(displayCondition, path);

            expect(result).toBe('inputs[1].fieldType == 7 or inputs[1].fieldType == 11');
        });

        it('should handle triple compound conditions', () => {
            const displayCondition = 'inputs[index].a == 1 and inputs[index].b == 2 and inputs[index].c == 3';
            const path = 'inputs[0].c';

            const result = replaceDisplayConditionIndexes(displayCondition, path);

            expect(result).toBe('inputs[0].a == 1 and inputs[0].b == 2 and inputs[0].c == 3');
        });

        it('should handle contains function with compound condition', () => {
            const displayCondition = 'contains({11,7}, inputs[index].fieldType) and inputs[index].required == true';
            const path = 'inputs[3].fieldOptions';

            const result = replaceDisplayConditionIndexes(displayCondition, path);

            expect(result).toBe('contains({11,7}, inputs[3].fieldType) and inputs[3].required == true');
        });
    });

    describe('nested arrays', () => {
        it('should handle nested arrays with sequential replacement', () => {
            const displayCondition = 'conditions[index][index].operation != "EMPTY"';
            const path = 'conditions[0][1].value';

            const result = replaceDisplayConditionIndexes(displayCondition, path);

            expect(result).toBe('conditions[0][1].operation != "EMPTY"');
        });

        it('should handle deeply nested arrays', () => {
            const displayCondition = 'data[index][index][index].enabled == true';
            const path = 'data[1][2][3].enabled';

            const result = replaceDisplayConditionIndexes(displayCondition, path);

            expect(result).toBe('data[1][2][3].enabled == true');
        });
    });

    describe('edge cases', () => {
        it('should handle condition without [index] placeholder', () => {
            const displayCondition = 'someProperty == true';
            const path = 'inputs[0].fieldType';

            const result = replaceDisplayConditionIndexes(displayCondition, path);

            expect(result).toBe('someProperty == true');
        });

        it('should handle path without array indices', () => {
            const displayCondition = 'inputs[index].fieldType == 7';
            const path = 'someProperty';

            const result = replaceDisplayConditionIndexes(displayCondition, path);

            // No indices found in path, so [index] remains
            expect(result).toBe('inputs[index].fieldType == 7');
        });

        it('should handle empty display condition', () => {
            const displayCondition = '';
            const path = 'inputs[0].fieldType';

            const result = replaceDisplayConditionIndexes(displayCondition, path);

            expect(result).toBe('');
        });
    });
});
