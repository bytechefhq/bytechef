import {describe, expect, it} from 'vitest';

/**
 * Tests for the shouldSaveHiddenProperty condition in useProperty.ts.
 *
 * The inline logic is:
 *   hidden &&
 *   encodedPath &&
 *   (objectName === undefined || dynamicPropertySource === objectName) &&
 *   (hasMutation) &&
 *   JSON.stringify(existingValue) !== JSON.stringify(defaultValue)
 *
 * This was changed from reference equality (existingValue !== defaultValue)
 * to deep equality via JSON.stringify to prevent an infinite save loop
 * where {} !== {} always returned true.
 */

function shouldSaveHiddenProperty({
    defaultValue,
    dynamicPropertySource,
    encodedPath,
    existingValue,
    hasMutation,
    hidden,
    objectName,
}: {
    defaultValue: unknown;
    dynamicPropertySource?: string;
    encodedPath?: string;
    existingValue: unknown;
    hasMutation: boolean;
    hidden: boolean;
    objectName?: string;
}): boolean {
    return (
        hidden &&
        !!encodedPath &&
        (objectName === undefined || dynamicPropertySource === objectName) &&
        hasMutation &&
        JSON.stringify(existingValue) !== JSON.stringify(defaultValue)
    );
}

const baseArgs = {
    defaultValue: 'default',
    encodedPath: 'parameters.field',
    existingValue: undefined as unknown,
    hasMutation: true,
    hidden: true,
    objectName: undefined as string | undefined,
};

describe('shouldSaveHiddenProperty', () => {
    describe('deep equality (Fix 2 — prevents infinite save loop)', () => {
        it('should return false when both existingValue and defaultValue are empty objects', () => {
            expect(
                shouldSaveHiddenProperty({
                    ...baseArgs,
                    defaultValue: {},
                    existingValue: {},
                })
            ).toBe(false);
        });

        it('should return false when both are structurally equal objects', () => {
            expect(
                shouldSaveHiddenProperty({
                    ...baseArgs,
                    defaultValue: {a: 1, b: 2},
                    existingValue: {a: 1, b: 2},
                })
            ).toBe(false);
        });

        it('should return true when objects have different values', () => {
            expect(
                shouldSaveHiddenProperty({
                    ...baseArgs,
                    defaultValue: {a: 2},
                    existingValue: {a: 1},
                })
            ).toBe(true);
        });

        it('should return true when existingValue is undefined and defaultValue is an object', () => {
            expect(
                shouldSaveHiddenProperty({
                    ...baseArgs,
                    defaultValue: {},
                    existingValue: undefined,
                })
            ).toBe(true);
        });

        it('should return false when both are identical strings', () => {
            expect(
                shouldSaveHiddenProperty({
                    ...baseArgs,
                    defaultValue: 'hello',
                    existingValue: 'hello',
                })
            ).toBe(false);
        });

        it('should return true when strings differ', () => {
            expect(
                shouldSaveHiddenProperty({
                    ...baseArgs,
                    defaultValue: 'hello',
                    existingValue: '',
                })
            ).toBe(true);
        });

        it('should return false when both are null', () => {
            expect(
                shouldSaveHiddenProperty({
                    ...baseArgs,
                    defaultValue: null,
                    existingValue: null,
                })
            ).toBe(false);
        });

        it('should return false when both are empty strings', () => {
            expect(
                shouldSaveHiddenProperty({
                    ...baseArgs,
                    defaultValue: '',
                    existingValue: '',
                })
            ).toBe(false);
        });

        it('should return true when existingValue is empty string and defaultValue is object', () => {
            expect(
                shouldSaveHiddenProperty({
                    ...baseArgs,
                    defaultValue: {key: 'value'},
                    existingValue: '',
                })
            ).toBe(true);
        });

        it('should return false when both are deeply nested equal objects', () => {
            expect(
                shouldSaveHiddenProperty({
                    ...baseArgs,
                    defaultValue: {nested: {deep: [1, 2, 3]}},
                    existingValue: {nested: {deep: [1, 2, 3]}},
                })
            ).toBe(false);
        });
    });

    describe('guard conditions', () => {
        it('should return false when hidden is false', () => {
            expect(
                shouldSaveHiddenProperty({
                    ...baseArgs,
                    existingValue: 'different',
                    hidden: false,
                })
            ).toBe(false);
        });

        it('should return false when encodedPath is empty string', () => {
            expect(
                shouldSaveHiddenProperty({
                    ...baseArgs,
                    encodedPath: '',
                    existingValue: 'different',
                })
            ).toBe(false);
        });

        it('should return false when encodedPath is undefined', () => {
            expect(
                shouldSaveHiddenProperty({
                    ...baseArgs,
                    encodedPath: undefined,
                    existingValue: 'different',
                })
            ).toBe(false);
        });

        it('should return false when hasMutation is false', () => {
            expect(
                shouldSaveHiddenProperty({
                    ...baseArgs,
                    existingValue: 'different',
                    hasMutation: false,
                })
            ).toBe(false);
        });

        it('should return false when objectName is set but does not match dynamicPropertySource', () => {
            expect(
                shouldSaveHiddenProperty({
                    ...baseArgs,
                    dynamicPropertySource: 'sourceA',
                    existingValue: 'different',
                    objectName: 'sourceB',
                })
            ).toBe(false);
        });

        it('should return true when objectName is undefined (always matches)', () => {
            expect(
                shouldSaveHiddenProperty({
                    ...baseArgs,
                    existingValue: 'different',
                    objectName: undefined,
                })
            ).toBe(true);
        });

        it('should return true when objectName matches dynamicPropertySource', () => {
            expect(
                shouldSaveHiddenProperty({
                    ...baseArgs,
                    dynamicPropertySource: 'mySource',
                    existingValue: 'different',
                    objectName: 'mySource',
                })
            ).toBe(true);
        });
    });
});
