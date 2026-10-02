import {describe, expect, it} from 'vitest';

import computeBinaryCaseLabel from './computeBinaryCaseLabel';

const CONDITION_TOP_GHOST = 'condition_1-condition-top-ghost';
const ON_ERROR_TOP_GHOST = 'onError_1-onError-top-ghost';

const coordinates = {sourceX: 100, targetY: 400};

describe('computeBinaryCaseLabel', () => {
    describe('LR layout', () => {
        it('puts TRUE above the upper arm, centered on the bar and clear of the "+" button', () => {
            const result = computeBinaryCaseLabel({
                ...coordinates,
                layoutDirection: 'LR',
                source: CONDITION_TOP_GHOST,
                sourceHandleId: `${CONDITION_TOP_GHOST}-left`,
            });

            expect(result).toEqual({anchor: 'bottomCenter', text: 'TRUE', x: 100, y: 384});
        });

        it('puts FALSE below the lower arm, centered on the bar and clear of the "+" button', () => {
            const result = computeBinaryCaseLabel({
                ...coordinates,
                layoutDirection: 'LR',
                source: CONDITION_TOP_GHOST,
                sourceHandleId: `${CONDITION_TOP_GHOST}-right`,
            });

            expect(result).toEqual({anchor: 'topCenter', text: 'FALSE', x: 100, y: 416});
        });
    });

    it('leaves TB arms unlabeled, since TB labels sit on the dispatcher node', () => {
        const result = computeBinaryCaseLabel({
            ...coordinates,
            layoutDirection: 'TB',
            source: CONDITION_TOP_GHOST,
            sourceHandleId: `${CONDITION_TOP_GHOST}-left`,
        });

        expect(result).toBeUndefined();
    });

    it('labels on-error arms TRY and CATCH', () => {
        const tryLabel = computeBinaryCaseLabel({
            ...coordinates,
            layoutDirection: 'LR',
            source: ON_ERROR_TOP_GHOST,
            sourceHandleId: `${ON_ERROR_TOP_GHOST}-left`,
        });
        const catchLabel = computeBinaryCaseLabel({
            ...coordinates,
            layoutDirection: 'LR',
            source: ON_ERROR_TOP_GHOST,
            sourceHandleId: `${ON_ERROR_TOP_GHOST}-right`,
        });

        expect(tryLabel?.text).toBe('TRY');
        expect(catchLabel?.text).toBe('CATCH');
    });

    it('leaves edges of other dispatchers unlabeled', () => {
        const result = computeBinaryCaseLabel({
            ...coordinates,
            layoutDirection: 'LR',
            source: 'loop_1-loop-top-ghost',
            sourceHandleId: 'loop_1-loop-top-ghost-left',
        });

        expect(result).toBeUndefined();
    });

    it('leaves edges that do not leave the bar through a side handle unlabeled', () => {
        const withoutHandle = computeBinaryCaseLabel({
            ...coordinates,
            layoutDirection: 'LR',
            source: CONDITION_TOP_GHOST,
            sourceHandleId: null,
        });
        const throughOtherHandle = computeBinaryCaseLabel({
            ...coordinates,
            layoutDirection: 'LR',
            source: CONDITION_TOP_GHOST,
            sourceHandleId: `${CONDITION_TOP_GHOST}-bottom`,
        });

        expect(withoutHandle).toBeUndefined();
        expect(throughOtherHandle).toBeUndefined();
    });
});
