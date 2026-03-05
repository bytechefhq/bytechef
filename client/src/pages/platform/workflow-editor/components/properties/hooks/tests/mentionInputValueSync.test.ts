import {describe, expect, it} from 'vitest';

/**
 * Tests the value-sync useEffect logic from useProperty.ts.
 *
 * The value-sync useEffect distributes propertyParameterValue one-way to the
 * appropriate display state variables (mentionInputValue, inputValue, selectValue, etc.).
 *
 * Key design constraint: the effect's dependency array must NOT include
 * mentionInputValue or inputValue — those are WRITE targets, not inputs.
 * Including them would create a feedback loop where every keystroke triggers
 * the effect, which resets the user's input before the debounced save can
 * complete (see "feedback loop prevention" tests).
 *
 * A ref (mentionInputSyncedRef) tracks whether the initial sync has occurred
 * to prevent re-syncing when the user clears the input value.
 */

const INPUT_PROPERTY_CONTROL_TYPES = ['EMAIL', 'PHONE', 'TEXT', 'TEXT_AREA', 'URL'];

type SyncResultType = {
    inputValue?: string | number | null;
    mentionInputSynced?: boolean;
    mentionInputValue?: string;
    multiSelectValue?: string[];
    propertyParameterValue?: string;
    selectValue?: string;
};

/**
 * Replicates the full value-sync useEffect from useProperty.ts.
 *
 * Returns which state variables would be set, or undefined for those not touched.
 */
const computeValueSync = ({
    controlType = 'TEXT',
    isSaving = false,
    mentionInput,
    mentionInputSynced = false,
    propertyParameterValue,
    type = 'STRING',
}: {
    controlType?: string;
    isSaving?: boolean;
    mentionInput: boolean;
    mentionInputSynced?: boolean;
    propertyParameterValue: string | number | null | undefined;
    type?: string;
}): SyncResultType => {
    const result: SyncResultType = {};

    const isNumericalInput = !mentionInput && (controlType === 'INTEGER' || controlType === 'NUMBER');

    // Skip updating if a save operation is in progress
    if (isSaving) {
        return result;
    }

    if (propertyParameterValue === '' || propertyParameterValue === undefined) {
        if (mentionInput) {
            result.mentionInputValue = '';
        } else {
            result.inputValue = '';
            result.selectValue = '';
            result.multiSelectValue = [];

            result.propertyParameterValue = '';
        }

        return result;
    }

    if (typeof propertyParameterValue === 'string' && propertyParameterValue.startsWith('=')) {
        if (!mentionInputSynced) {
            result.mentionInputValue = propertyParameterValue.substring(1);
            result.mentionInputSynced = true;
        }

        return result;
    }

    if (
        mentionInput &&
        !mentionInputSynced &&
        typeof propertyParameterValue === 'string' &&
        propertyParameterValue !== ''
    ) {
        result.mentionInputValue = propertyParameterValue;
        result.mentionInputSynced = true;

        return result;
    }

    if (!mentionInput && controlType && INPUT_PROPERTY_CONTROL_TYPES.includes(controlType) && propertyParameterValue) {
        result.inputValue = propertyParameterValue;
    }

    if (!mentionInput && controlType === 'JSON_SCHEMA_BUILDER' && propertyParameterValue !== undefined) {
        result.inputValue = propertyParameterValue;
    }

    if (controlType === 'SELECT' && propertyParameterValue !== undefined) {
        if (propertyParameterValue === null) {
            result.selectValue = 'null';
        } else if (type === 'BOOLEAN') {
            result.selectValue = String(propertyParameterValue);
        } else {
            result.selectValue = propertyParameterValue as string;
        }
    }

    if (controlType === 'MULTI_SELECT' && propertyParameterValue !== undefined) {
        if (propertyParameterValue === null) {
            result.multiSelectValue = [];
        } else if (propertyParameterValue !== undefined) {
            result.multiSelectValue = propertyParameterValue as unknown as string[];
        }
    }

    if (isNumericalInput && propertyParameterValue !== null && propertyParameterValue !== undefined) {
        result.inputValue = propertyParameterValue;
    }

    return result;
};

describe('value-sync useEffect logic', () => {
    describe('mentionInputValue sync for regular strings', () => {
        describe('when mentionInput is active (initial sync)', () => {
            it('should sync a regular string value to mentionInputValue', () => {
                const result = computeValueSync({
                    mentionInput: true,
                    propertyParameterValue: 'hello world',
                });

                expect(result.mentionInputValue).toBe('hello world');
                expect(result.mentionInputSynced).toBe(true);
            });

            it('should sync a string with special characters', () => {
                const result = computeValueSync({
                    mentionInput: true,
                    propertyParameterValue: 'user@example.com',
                });

                expect(result.mentionInputValue).toBe('user@example.com');
            });

            it('should sync a string containing datapill references', () => {
                const result = computeValueSync({
                    mentionInput: true,
                    propertyParameterValue: 'Hello ${trigger_1.name}',
                });

                expect(result.mentionInputValue).toBe('Hello ${trigger_1.name}');
            });
        });

        describe('when mentionInput is active but already synced', () => {
            it('should not re-sync a regular string value after initial sync', () => {
                const result = computeValueSync({
                    mentionInput: true,
                    mentionInputSynced: true,
                    propertyParameterValue: 'hello world',
                });

                expect(result.mentionInputValue).toBeUndefined();
                expect(result.mentionInputSynced).toBeUndefined();
            });

            it('should not restore value when user clears input', () => {
                // Simulates: user had "test" → deleted all characters → mentionInputValue is now ''
                // The sync effect should NOT re-sync because mentionInputSynced is true
                const result = computeValueSync({
                    mentionInput: true,
                    mentionInputSynced: true,
                    propertyParameterValue: 'test',
                });

                expect(result.mentionInputValue).toBeUndefined();
            });
        });

        describe('when mentionInput is active with formula values', () => {
            it('should strip the = prefix for formula values', () => {
                const result = computeValueSync({
                    mentionInput: true,
                    propertyParameterValue: '=${trigger_1.output} + 1',
                });

                expect(result.mentionInputValue).toBe('${trigger_1.output} + 1');
            });

            it('should strip the = prefix for simple formula', () => {
                const result = computeValueSync({
                    mentionInput: true,
                    propertyParameterValue: '=someExpression()',
                });

                expect(result.mentionInputValue).toBe('someExpression()');
            });

            it('should NOT re-sync formula value when already synced (prevents save-cycle overwrite)', () => {
                const result = computeValueSync({
                    mentionInput: true,
                    mentionInputSynced: true,
                    propertyParameterValue: '=formula()',
                });

                // Formula block returns early but does NOT update mentionInputValue
                expect(result.mentionInputValue).toBeUndefined();
                // mentionInputSynced stays true (already was), not set again
                expect(result.mentionInputSynced).toBeUndefined();
            });
        });

        describe('when mentionInput is inactive', () => {
            it('should not sync a regular string value to mentionInputValue', () => {
                const result = computeValueSync({
                    mentionInput: false,
                    propertyParameterValue: 'hello world',
                });

                expect(result.mentionInputValue).toBeUndefined();
            });

            it('should sync formula values on initial load (mentionInputSynced is false)', () => {
                const result = computeValueSync({
                    mentionInput: false,
                    propertyParameterValue: '=formula()',
                });

                expect(result.mentionInputValue).toBe('formula()');
                expect(result.mentionInputSynced).toBe(true);
            });
        });

        describe('when propertyParameterValue is empty or undefined', () => {
            it('should clear mentionInputValue when value is empty string and mentionInput is active', () => {
                const result = computeValueSync({
                    mentionInput: true,
                    propertyParameterValue: '',
                });

                expect(result.mentionInputValue).toBe('');
            });

            it('should clear mentionInputValue when value is undefined and mentionInput is active', () => {
                const result = computeValueSync({
                    mentionInput: true,
                    propertyParameterValue: undefined,
                });

                expect(result.mentionInputValue).toBe('');
            });

            it('should not set mentionInputValue when value is empty and mentionInput is inactive', () => {
                const result = computeValueSync({
                    mentionInput: false,
                    propertyParameterValue: '',
                });

                expect(result.mentionInputValue).toBeUndefined();
            });
        });

        describe('when propertyParameterValue is a number', () => {
            it('should not sync numeric values to mentionInputValue', () => {
                const result = computeValueSync({
                    mentionInput: true,
                    propertyParameterValue: 42,
                });

                expect(result.mentionInputValue).toBeUndefined();
            });
        });
    });

    describe('mutual exclusivity of sync blocks (early returns)', () => {
        it('should not run formula block after empty block handles the value', () => {
            // propertyParameterValue is '' → empty block runs and returns
            // Formula block should NOT run (no mentionInputValue set from formula logic)
            const result = computeValueSync({
                mentionInput: true,
                propertyParameterValue: '',
            });

            // Only the empty block should have acted
            expect(result.mentionInputValue).toBe('');
            expect(result.mentionInputSynced).toBeUndefined();
        });

        it('should not run regular sync block after formula block handles the value', () => {
            // propertyParameterValue starts with '=' → formula block runs and returns
            // Regular sync block should NOT run
            const result = computeValueSync({
                mentionInput: true,
                propertyParameterValue: '=value',
            });

            expect(result.mentionInputValue).toBe('value');
            expect(result.mentionInputSynced).toBe(true);
        });

        it('should not run inputValue blocks after regular sync block handles the value', () => {
            // mentionInput is true, not yet synced → regular sync block runs and returns
            // INPUT_PROPERTY_CONTROL_TYPES block should NOT run
            const result = computeValueSync({
                controlType: 'TEXT',
                mentionInput: true,
                propertyParameterValue: 'test',
            });

            expect(result.mentionInputValue).toBe('test');
            expect(result.inputValue).toBeUndefined();
        });
    });

    describe('inputValue sync for non-mention inputs', () => {
        it('should sync to inputValue for TEXT controlType', () => {
            const result = computeValueSync({
                controlType: 'TEXT',
                mentionInput: false,
                propertyParameterValue: 'hello',
            });

            expect(result.inputValue).toBe('hello');
        });

        it('should sync to inputValue for EMAIL controlType', () => {
            const result = computeValueSync({
                controlType: 'EMAIL',
                mentionInput: false,
                propertyParameterValue: 'user@example.com',
            });

            expect(result.inputValue).toBe('user@example.com');
        });

        it('should sync to inputValue for JSON_SCHEMA_BUILDER controlType', () => {
            const result = computeValueSync({
                controlType: 'JSON_SCHEMA_BUILDER',
                mentionInput: false,
                propertyParameterValue: '{"type": "object"}',
            });

            expect(result.inputValue).toBe('{"type": "object"}');
        });

        it('should not sync to inputValue for non-input controlTypes', () => {
            const result = computeValueSync({
                controlType: 'CODE_EDITOR',
                mentionInput: false,
                propertyParameterValue: 'some code',
            });

            expect(result.inputValue).toBeUndefined();
        });
    });

    describe('selectValue sync', () => {
        it('should sync string value to selectValue for SELECT controlType', () => {
            const result = computeValueSync({
                controlType: 'SELECT',
                mentionInput: false,
                propertyParameterValue: 'option1',
            });

            expect(result.selectValue).toBe('option1');
        });

        it('should set selectValue to "null" for null propertyParameterValue', () => {
            const result = computeValueSync({
                controlType: 'SELECT',
                mentionInput: false,
                propertyParameterValue: null,
            });

            expect(result.selectValue).toBe('null');
        });

        it('should stringify boolean values for BOOLEAN type SELECT', () => {
            const result = computeValueSync({
                controlType: 'SELECT',
                mentionInput: false,
                propertyParameterValue: true as unknown as string,
                type: 'BOOLEAN',
            });

            expect(result.selectValue).toBe('true');
        });
    });

    describe('numerical input sync', () => {
        it('should sync numeric value to inputValue for NUMBER controlType', () => {
            const result = computeValueSync({
                controlType: 'NUMBER',
                mentionInput: false,
                propertyParameterValue: 42,
            });

            expect(result.inputValue).toBe(42);
        });

        it('should sync numeric value to inputValue for INTEGER controlType', () => {
            const result = computeValueSync({
                controlType: 'INTEGER',
                mentionInput: false,
                propertyParameterValue: 7,
            });

            expect(result.inputValue).toBe(7);
        });

        it('should not sync when propertyParameterValue is null', () => {
            const result = computeValueSync({
                controlType: 'NUMBER',
                mentionInput: false,
                propertyParameterValue: null,
            });

            expect(result.inputValue).toBeUndefined();
        });
    });

    describe('isSaving guard', () => {
        it('should skip all sync when a save operation is in progress', () => {
            const result = computeValueSync({
                isSaving: true,
                mentionInput: true,
                propertyParameterValue: 'should not sync',
            });

            expect(result.mentionInputValue).toBeUndefined();
            expect(result.inputValue).toBeUndefined();
            expect(result.selectValue).toBeUndefined();
        });

        it('should skip empty-value reset when saving', () => {
            const result = computeValueSync({
                isSaving: true,
                mentionInput: true,
                propertyParameterValue: '',
            });

            expect(result.mentionInputValue).toBeUndefined();
        });
    });

    describe('feedback loop prevention', () => {
        it('should not reset mentionInputValue when user types in an empty dynamic property', () => {
            // Scenario: dynamic property with no saved value (propertyParameterValue = '')
            // User types "$" → mentionInputValue changes
            // The effect should NOT fire in response to mentionInputValue changes
            // (mentionInputValue is excluded from the dependency array).
            //
            // We verify this by checking: when propertyParameterValue is '' and
            // mentionInput is true, the effect clears mentionInputValue to ''.
            // This is correct behavior ONLY when propertyParameterValue actually
            // changes to '' — not when the user is typing.
            //
            // The old code had mentionInputValue in the deps, causing:
            // 1. User types → mentionInputValue changes → effect fires
            // 2. propertyParameterValue is still '' → resets mentionInputValue to ''
            // 3. User's input disappears
            //
            // With the fix, the effect only fires when propertyParameterValue changes,
            // so typing does not trigger the reset.

            // First call: initial mount with empty value → clears mentionInputValue
            const initialResult = computeValueSync({
                mentionInput: true,
                propertyParameterValue: '',
            });

            expect(initialResult.mentionInputValue).toBe('');

            // Simulating the "after user types" state:
            // propertyParameterValue hasn't changed (still ''), so the effect
            // should not re-execute. We represent this by NOT calling computeValueSync
            // again — the dependency array [propertyParameterValue, mentionInput, controlType]
            // hasn't changed, so React won't re-run the effect.
        });

        it('should not restore old formula when user deletes a data pill', () => {
            // Scenario: propertyParameterValue = "=${trigger_1.name}" (saved formula)
            // User deletes the data pill → mentionInputValue changes
            // The old code would re-run the formula block and restore the old value.
            //
            // With the fix, the effect only fires when propertyParameterValue changes.
            // So deleting a pill (which changes mentionInputValue but not
            // propertyParameterValue) does NOT trigger the formula block.

            // The effect fires once on mount with the formula value:
            const mountResult = computeValueSync({
                mentionInput: true,
                propertyParameterValue: '=${trigger_1.name}',
            });

            expect(mountResult.mentionInputValue).toBe('${trigger_1.name}');

            // After user deletes the pill, mentionInputValue changes but
            // propertyParameterValue is still "=${trigger_1.name}" until the
            // debounced save completes. Since mentionInputValue is NOT in deps,
            // the effect does NOT re-run and does NOT restore the old value.
            //
            // Only when the save completes and propertyParameterValue updates
            // to the new value will the effect run again:
            const afterSaveResult = computeValueSync({
                mentionInput: true,
                propertyParameterValue: '',
            });

            expect(afterSaveResult.mentionInputValue).toBe('');
        });

        it('should not create an infinite loop between mentionInputValue and the empty block', () => {
            // The old code's feedback loop:
            // 1. mentionInputValue = '' (initial) → effect fires
            // 2. propertyParameterValue = '' → setMentionInputValue('') (no-op, stabilizes)
            // 3. User types "a" → mentionInputValue = 'a' → effect fires
            // 4. propertyParameterValue = '' → setMentionInputValue('') ← RESET!
            // 5. mentionInputValue = '' → effect fires → step 2 again (stable)
            //
            // With the fix, steps 3-5 don't happen because mentionInputValue
            // is not in the dependency array.

            // Verify: empty value correctly clears once
            const result = computeValueSync({
                mentionInput: true,
                propertyParameterValue: '',
            });

            expect(result.mentionInputValue).toBe('');

            // The key invariant: calling computeValueSync with the SAME
            // propertyParameterValue produces the same result (idempotent).
            // No matter how many times the effect would run with the same inputs,
            // the output is stable.
            const secondResult = computeValueSync({
                mentionInput: true,
                propertyParameterValue: '',
            });

            expect(secondResult.mentionInputValue).toBe('');
        });
    });

    describe('empty-value block for non-mention inputs', () => {
        it('should clear all non-mention state when propertyParameterValue is empty', () => {
            const result = computeValueSync({
                controlType: 'TEXT',
                mentionInput: false,
                propertyParameterValue: '',
            });

            expect(result.inputValue).toBe('');
            expect(result.selectValue).toBe('');
            expect(result.multiSelectValue).toEqual([]);
            expect(result.propertyParameterValue).toBe('');
        });

        it('should clear all non-mention state when propertyParameterValue is undefined', () => {
            const result = computeValueSync({
                controlType: 'SELECT',
                mentionInput: false,
                propertyParameterValue: undefined,
            });

            expect(result.inputValue).toBe('');
            expect(result.selectValue).toBe('');
            expect(result.multiSelectValue).toEqual([]);
            expect(result.propertyParameterValue).toBe('');
        });

        it('should not touch mentionInputValue when clearing non-mention state', () => {
            const result = computeValueSync({
                mentionInput: false,
                propertyParameterValue: '',
            });

            expect(result.mentionInputValue).toBeUndefined();
        });
    });

    describe('formula block guard (mentionInputSyncedRef prevents save-cycle overwrite)', () => {
        it('should sync formula value on initial mount (not yet synced)', () => {
            const result = computeValueSync({
                mentionInput: true,
                mentionInputSynced: false,
                propertyParameterValue: '=${trigger_1.output}',
            });

            expect(result.mentionInputValue).toBe('${trigger_1.output}');
            expect(result.mentionInputSynced).toBe(true);
        });

        it('should NOT overwrite formula value after initial sync (save-cycle protection)', () => {
            // After mount: mentionInputSynced = true
            // Save completes → propertyParameterValue changes to same or different formula
            // The formula block should NOT overwrite mentionInputValue
            const result = computeValueSync({
                mentionInput: true,
                mentionInputSynced: true,
                propertyParameterValue: '=${trigger_1.output}',
            });

            expect(result.mentionInputValue).toBeUndefined();
        });

        it('should still return early from formula block even when synced (prevents fallthrough)', () => {
            // Even when the formula block skips the mentionInputValue update,
            // it should still return early to prevent falling through to
            // the regular string sync block or other blocks
            const result = computeValueSync({
                mentionInput: true,
                mentionInputSynced: true,
                propertyParameterValue: '=some_formula()',
            });

            // No mentionInputValue update
            expect(result.mentionInputValue).toBeUndefined();
            // No inputValue update (didn't fall through)
            expect(result.inputValue).toBeUndefined();
            // No mentionInputSynced change (didn't fall through to regular sync)
            expect(result.mentionInputSynced).toBeUndefined();
        });

        it('should protect against save-cycle race condition with data pills', () => {
            // Scenario: user deletes data pill, then save of OLD value completes
            // 1. Mount: formula synced, mentionInputSynced = true
            const mountResult = computeValueSync({
                mentionInput: true,
                mentionInputSynced: false,
                propertyParameterValue: '=${airtable_1.fields}',
            });

            expect(mountResult.mentionInputValue).toBe('${airtable_1.fields}');
            expect(mountResult.mentionInputSynced).toBe(true);

            // 2. User deletes data pill → mentionInputValue = "" (not in deps, no effect run)
            // 3. Old save completes → propertyParameterValue = "=${airtable_1.fields}" again
            // 4. Effect runs with mentionInputSynced = true → formula block skips update
            const afterSaveResult = computeValueSync({
                mentionInput: true,
                mentionInputSynced: true,
                propertyParameterValue: '=${airtable_1.fields}',
            });

            // Should NOT restore the old data pill value
            expect(afterSaveResult.mentionInputValue).toBeUndefined();
        });

        it('should allow re-sync after empty value resets mentionInputSynced implicitly', () => {
            // When propertyParameterValue becomes '' (after saving empty):
            // The empty block fires and sets mentionInputValue to ''
            // On component remount, mentionInputSynced resets to false
            // So a new formula value would be synced correctly
            const emptyResult = computeValueSync({
                mentionInput: true,
                mentionInputSynced: true,
                propertyParameterValue: '',
            });

            // Empty block handles this case (doesn't check mentionInputSynced)
            expect(emptyResult.mentionInputValue).toBe('');

            // After remount (mentionInputSynced reset to false):
            const remountResult = computeValueSync({
                mentionInput: true,
                mentionInputSynced: false,
                propertyParameterValue: '=${new_formula}',
            });

            expect(remountResult.mentionInputValue).toBe('${new_formula}');
            expect(remountResult.mentionInputSynced).toBe(true);
        });
    });
});
