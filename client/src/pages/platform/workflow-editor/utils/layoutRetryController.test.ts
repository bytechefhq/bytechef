import {describe, expect, it} from 'vitest';

import {createLayoutRetryState, onLayoutFailure, onLayoutSuccess} from './layoutRetryController';

describe('layoutRetryController', () => {
    it('retries the first failure', () => {
        const state = createLayoutRetryState();

        expect(onLayoutFailure(state)).toBe(true);
    });

    it('does not retry a second consecutive failure', () => {
        const state = createLayoutRetryState();

        onLayoutFailure(state);

        expect(onLayoutFailure(state)).toBe(false);
    });

    it('stops after a single retry even when the layout keeps failing forever', () => {
        const state = createLayoutRetryState();

        const retryDecisions = Array.from({length: 100}, () => onLayoutFailure(state));

        // Exactly one retry across an unbounded failure streak — the guard that
        // prevents an infinite relayout loop.
        expect(retryDecisions.filter(Boolean)).toHaveLength(1);
        expect(retryDecisions[0]).toBe(true);
    });

    it('re-arms the retry only after a success clears the guard', () => {
        const state = createLayoutRetryState();

        expect(onLayoutFailure(state)).toBe(true);
        expect(onLayoutFailure(state)).toBe(false);

        onLayoutSuccess(state);

        // A genuinely new failure after a recovery gets its own single retry.
        expect(onLayoutFailure(state)).toBe(true);
    });

    it('does not re-arm the retry when success is never reached (throwing success handler)', () => {
        const state = createLayoutRetryState();

        // First failure retries.
        expect(onLayoutFailure(state)).toBe(true);

        // The retried attempt's success handler throws before onLayoutSuccess is
        // reached, so the guard is never cleared — the next failure must NOT
        // retry again, or the effect would loop forever.
        expect(onLayoutFailure(state)).toBe(false);
        expect(onLayoutFailure(state)).toBe(false);
    });

    it('treats a fresh state as never having retried', () => {
        expect(createLayoutRetryState()).toEqual({hasRetried: false});
    });
});
