import {describe, expect, it} from 'vitest';

/**
 * Replicates the `isFromAi` useMemo logic from useProperty.ts and verifies:
 * 1. `controlledFromAi` provides an optimistic override so "Automatically defined
 *    by the model" appears instantly on click, before the server round-trip.
 * 2. When metadata is missing after reload, a value that exactly equals the
 *    computed default `fromAiExpression` still resolves to true so the overlay
 *    is preserved. Modified fromAi expressions stay editable.
 */

interface IsFromAiParamsI {
    controlledFromAi: boolean | undefined;
    fromAiExpression?: string;
    fromAiPaths?: string[];
    path?: string;
    propertyParameterValue?: unknown;
}

// In production `fromAiExpression` is always a non-empty string computed from
// the property's name/type/etc. Using a sentinel default mirrors that so tests
// that don't care about the value-based fallback don't collide with undefined.
const DEFAULT_FROM_AI_EXPRESSION = "=fromAi('message', 'STRING', {'required': false})";

function isFromAi({
    controlledFromAi,
    fromAiExpression = DEFAULT_FROM_AI_EXPRESSION,
    fromAiPaths,
    path,
    propertyParameterValue,
}: IsFromAiParamsI): boolean {
    if (controlledFromAi !== undefined) {
        return controlledFromAi;
    }

    if (path && fromAiPaths?.includes(path)) {
        return true;
    }

    return propertyParameterValue === fromAiExpression;
}

describe('isFromAi optimistic override', () => {
    const path = 'parameters.message';

    describe('without optimistic override (controlledFromAi = undefined)', () => {
        it('should return true when server metadata includes the path', () => {
            expect(isFromAi({controlledFromAi: undefined, fromAiPaths: [path], path})).toBe(true);
        });

        it('should return false when server metadata does not include the path', () => {
            expect(isFromAi({controlledFromAi: undefined, fromAiPaths: ['other.path'], path})).toBe(false);
        });

        it('should return false when fromAiPaths is undefined', () => {
            expect(isFromAi({controlledFromAi: undefined, fromAiPaths: undefined, path})).toBe(false);
        });

        it('should return false when path is undefined', () => {
            expect(isFromAi({controlledFromAi: undefined, fromAiPaths: [path], path: undefined})).toBe(false);
        });
    });

    describe('with optimistic override (controlledFromAi defined)', () => {
        it('should return true immediately when controlledFromAi is true, even before server update', () => {
            expect(isFromAi({controlledFromAi: true, fromAiPaths: undefined, path})).toBe(true);
        });

        it('should return true when controlledFromAi is true regardless of server metadata', () => {
            expect(isFromAi({controlledFromAi: true, fromAiPaths: [], path})).toBe(true);
        });

        it('should return false when controlledFromAi is false, overriding server metadata', () => {
            expect(isFromAi({controlledFromAi: false, fromAiPaths: [path], path})).toBe(false);
        });
    });

    describe('fromAi button click race condition', () => {
        it('should show "Automatically defined" immediately after click, not after server response', () => {
            // Before click: server has no fromAi for this path, controlledFromAi is undefined
            const beforeClick = isFromAi({controlledFromAi: undefined, fromAiPaths: [], path});

            expect(beforeClick).toBe(false);

            // After click: handleFromAiClick sets controlledFromAi=true immediately
            // Server hasn't responded yet, so fromAiPaths still doesn't include the path
            const afterClick = isFromAi({controlledFromAi: true, fromAiPaths: [], path});

            expect(afterClick).toBe(true);

            // After server response: both controlledFromAi and server metadata agree
            const afterServerResponse = isFromAi({controlledFromAi: true, fromAiPaths: [path], path});

            expect(afterServerResponse).toBe(true);
        });

        it('should hide "Automatically defined" immediately when toggling off', () => {
            // Before toggle off: server still has fromAi for this path
            const beforeToggle = isFromAi({controlledFromAi: undefined, fromAiPaths: [path], path});

            expect(beforeToggle).toBe(true);

            // After toggle off: controlledFromAi=false overrides server metadata
            const afterToggle = isFromAi({controlledFromAi: false, fromAiPaths: [path], path});

            expect(afterToggle).toBe(false);
        });
    });

    describe('value-based fallback after reload', () => {
        const fromAiExpression = "=fromAi('lastname', 'STRING', {'required': false})";

        it('should return true when value exactly matches the computed fromAiExpression (metadata missing)', () => {
            expect(
                isFromAi({
                    controlledFromAi: undefined,
                    fromAiExpression,
                    fromAiPaths: [],
                    path,
                    propertyParameterValue: fromAiExpression,
                })
            ).toBe(true);
        });

        it('should return true when value matches default even if fromAiPaths is undefined', () => {
            expect(
                isFromAi({
                    controlledFromAi: undefined,
                    fromAiExpression,
                    fromAiPaths: undefined,
                    path,
                    propertyParameterValue: fromAiExpression,
                })
            ).toBe(true);
        });

        it('should return false when user has modified the fromAi expression from the default', () => {
            const modified = "=fromAi('lastname', 'STRING', {'required': true, 'description': 'custom'})";

            expect(
                isFromAi({
                    controlledFromAi: undefined,
                    fromAiExpression,
                    fromAiPaths: [],
                    path,
                    propertyParameterValue: modified,
                })
            ).toBe(false);
        });

        it('should return false for plain string values that are not fromAi expressions', () => {
            expect(
                isFromAi({
                    controlledFromAi: undefined,
                    fromAiExpression,
                    fromAiPaths: [],
                    path,
                    propertyParameterValue: 'hello world',
                })
            ).toBe(false);
        });

        it('should return false for non-string values', () => {
            expect(
                isFromAi({
                    controlledFromAi: undefined,
                    fromAiExpression,
                    fromAiPaths: [],
                    path,
                    propertyParameterValue: 42,
                })
            ).toBe(false);
        });

        it('should allow controlledFromAi=false to override a default fromAi value', () => {
            expect(
                isFromAi({
                    controlledFromAi: false,
                    fromAiExpression,
                    fromAiPaths: [path],
                    path,
                    propertyParameterValue: fromAiExpression,
                })
            ).toBe(false);
        });
    });
});
