import {describe, expect, it} from 'vitest';

/**
 * Tests the mentionInputValue sync logic from useProperty.ts.
 *
 * The value-sync useEffect determines how propertyParameterValue is distributed
 * to the different input state variables (mentionInputValue, inputValue, selectValue).
 * The regular-string sync block ensures that when mentionInput is active and the
 * saved value is a plain string (not a formula), it is properly synced to
 * mentionInputValue so the mention editor displays the saved value on initial load.
 *
 * A ref (mentionInputSyncedRef) tracks whether the initial sync has occurred to
 * prevent re-syncing when the user clears the input value.
 */

type SyncResultType = {
    mentionInputSynced?: boolean;
    mentionInputValue?: string;
};

/**
 * Replicates the mentionInputValue sync portion of the value-sync useEffect.
 *
 * Returns the mentionInputValue that would be set, or undefined if no sync occurs.
 * Also returns whether the synced ref would be set to true.
 */
const computeMentionInputValueSync = ({
    mentionInput,
    mentionInputSynced = false,
    propertyParameterValue,
}: {
    mentionInput: boolean;
    mentionInputSynced?: boolean;
    propertyParameterValue: string | number | undefined;
}): SyncResultType => {
    const result: SyncResultType = {};

    // Empty/undefined → clear mentionInputValue
    if (propertyParameterValue === '' || propertyParameterValue === undefined) {
        if (mentionInput) {
            result.mentionInputValue = '';
        }

        return result;
    }

    // Formula values (starting with '=') → strip prefix and set
    if (typeof propertyParameterValue === 'string' && propertyParameterValue.startsWith('=')) {
        result.mentionInputValue = propertyParameterValue.substring(1);

        return result;
    }

    // Regular string values → sync directly when mentionInput is active and not yet synced
    if (
        mentionInput &&
        !mentionInputSynced &&
        typeof propertyParameterValue === 'string' &&
        !propertyParameterValue.startsWith('=') &&
        propertyParameterValue !== ''
    ) {
        result.mentionInputValue = propertyParameterValue;
        result.mentionInputSynced = true;

        return result;
    }

    return result;
};

describe('mentionInputValue sync for regular strings', () => {
    describe('when mentionInput is active (initial sync)', () => {
        it('should sync a regular string value to mentionInputValue', () => {
            const result = computeMentionInputValueSync({
                mentionInput: true,
                propertyParameterValue: 'hello world',
            });

            expect(result.mentionInputValue).toBe('hello world');
            expect(result.mentionInputSynced).toBe(true);
        });

        it('should sync a string with special characters', () => {
            const result = computeMentionInputValueSync({
                mentionInput: true,
                propertyParameterValue: 'user@example.com',
            });

            expect(result.mentionInputValue).toBe('user@example.com');
        });

        it('should sync a string containing datapill references', () => {
            const result = computeMentionInputValueSync({
                mentionInput: true,
                propertyParameterValue: 'Hello ${trigger_1.name}',
            });

            expect(result.mentionInputValue).toBe('Hello ${trigger_1.name}');
        });
    });

    describe('when mentionInput is active but already synced', () => {
        it('should not re-sync a regular string value after initial sync', () => {
            const result = computeMentionInputValueSync({
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
            const result = computeMentionInputValueSync({
                mentionInput: true,
                mentionInputSynced: true,
                propertyParameterValue: 'test',
            });

            expect(result.mentionInputValue).toBeUndefined();
        });
    });

    describe('when mentionInput is active with formula values', () => {
        it('should strip the = prefix for formula values', () => {
            const result = computeMentionInputValueSync({
                mentionInput: true,
                propertyParameterValue: '=${trigger_1.output} + 1',
            });

            expect(result.mentionInputValue).toBe('${trigger_1.output} + 1');
        });

        it('should strip the = prefix for simple formula', () => {
            const result = computeMentionInputValueSync({
                mentionInput: true,
                propertyParameterValue: '=someExpression()',
            });

            expect(result.mentionInputValue).toBe('someExpression()');
        });

        it('should strip the = prefix even when already synced (formula sync is unconditional)', () => {
            const result = computeMentionInputValueSync({
                mentionInput: true,
                mentionInputSynced: true,
                propertyParameterValue: '=formula()',
            });

            expect(result.mentionInputValue).toBe('formula()');
        });
    });

    describe('when mentionInput is inactive', () => {
        it('should not sync a regular string value', () => {
            const result = computeMentionInputValueSync({
                mentionInput: false,
                propertyParameterValue: 'hello world',
            });

            expect(result.mentionInputValue).toBeUndefined();
        });

        it('should still sync formula values (formula sync is unconditional)', () => {
            const result = computeMentionInputValueSync({
                mentionInput: false,
                propertyParameterValue: '=formula()',
            });

            expect(result.mentionInputValue).toBe('formula()');
        });
    });

    describe('when propertyParameterValue is empty or undefined', () => {
        it('should clear mentionInputValue when value is empty string and mentionInput is active', () => {
            const result = computeMentionInputValueSync({
                mentionInput: true,
                propertyParameterValue: '',
            });

            expect(result.mentionInputValue).toBe('');
        });

        it('should clear mentionInputValue when value is undefined and mentionInput is active', () => {
            const result = computeMentionInputValueSync({
                mentionInput: true,
                propertyParameterValue: undefined,
            });

            expect(result.mentionInputValue).toBe('');
        });

        it('should not set mentionInputValue when value is empty and mentionInput is inactive', () => {
            const result = computeMentionInputValueSync({
                mentionInput: false,
                propertyParameterValue: '',
            });

            expect(result.mentionInputValue).toBeUndefined();
        });
    });

    describe('when propertyParameterValue is a number', () => {
        it('should not sync numeric values to mentionInputValue', () => {
            const result = computeMentionInputValueSync({
                mentionInput: true,
                propertyParameterValue: 42,
            });

            expect(result.mentionInputValue).toBeUndefined();
        });
    });
});
