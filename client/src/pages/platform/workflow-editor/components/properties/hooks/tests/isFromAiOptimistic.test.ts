import {describe, expect, it} from 'vitest';

/**
 * Replicates the `isFromAi` useMemo logic from useProperty.ts (lines 386-396)
 * and verifies that `controlledFromAi` provides an optimistic override so the
 * "Automatically defined by the model" message appears instantly when the user
 * clicks the fromAi button, without waiting for the server round-trip.
 */

interface IsFromAiParamsI {
    controlledFromAi: boolean | undefined;
    fromAiPaths?: string[];
    path?: string;
}

function isFromAi({controlledFromAi, fromAiPaths, path}: IsFromAiParamsI): boolean {
    if (controlledFromAi !== undefined) {
        return controlledFromAi;
    }

    if (!fromAiPaths || !path) {
        return false;
    }

    return fromAiPaths.includes(path);
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
});
