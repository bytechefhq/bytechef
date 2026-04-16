import {describe, expect, it} from 'vitest';

/**
 * Replicates the `isFromAi` useMemo logic from useProperty.ts and verifies:
 * 1. `controlledFromAi` provides an optimistic override so "Automatically defined
 *    by the model" appears instantly on click, before the server round-trip.
 * 2. When metadata is missing after reload, a value matching `=fromAi(...)` still
 *    resolves to true so the overlay and disabled input are preserved.
 */

interface IsFromAiParamsI {
    controlledFromAi: boolean | undefined;
    fromAiPaths?: string[];
    path?: string;
    propertyParameterValue?: unknown;
}

function isFromAi({controlledFromAi, fromAiPaths, path, propertyParameterValue}: IsFromAiParamsI): boolean {
    if (controlledFromAi !== undefined) {
        return controlledFromAi;
    }

    if (path && fromAiPaths?.includes(path)) {
        return true;
    }

    return typeof propertyParameterValue === 'string' && propertyParameterValue.startsWith('=fromAi(');
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
        const fromAiValue = "=fromAi('lastname', 'STRING', {'required': false})";

        it('should return true when value starts with =fromAi( even if metadata is missing', () => {
            expect(
                isFromAi({
                    controlledFromAi: undefined,
                    fromAiPaths: [],
                    path,
                    propertyParameterValue: fromAiValue,
                })
            ).toBe(true);
        });

        it('should return true when value starts with =fromAi( even if fromAiPaths is undefined', () => {
            expect(
                isFromAi({
                    controlledFromAi: undefined,
                    fromAiPaths: undefined,
                    path,
                    propertyParameterValue: fromAiValue,
                })
            ).toBe(true);
        });

        it('should return false for plain string values that are not fromAi expressions', () => {
            expect(
                isFromAi({
                    controlledFromAi: undefined,
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
                    fromAiPaths: [],
                    path,
                    propertyParameterValue: 42,
                })
            ).toBe(false);
        });

        it('should allow controlledFromAi=false to override a fromAi-shaped value', () => {
            expect(
                isFromAi({
                    controlledFromAi: false,
                    fromAiPaths: [path],
                    path,
                    propertyParameterValue: fromAiValue,
                })
            ).toBe(false);
        });
    });
});
