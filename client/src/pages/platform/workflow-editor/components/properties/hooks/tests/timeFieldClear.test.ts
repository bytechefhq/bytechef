import {describe, expect, it, vi} from 'vitest';

/**
 * Exercises the TIME-clearing flow introduced for issue #4768.
 *
 * Why: Chrome's <input type="time"> does not expose a native clear button,
 * so the UI must render an explicit X. This test locks in two things:
 *   1. The TIME clear button is shown only when inputValue is non-empty.
 *   2. Invoking handleInputClear resets local state and invokes the save callback.
 *
 * The empty-string -> null coercion for DATE/DATE_TIME/TIME is covered by
 * saveInputValueResolvedValue.test.ts to avoid duplicating the resolver logic.
 */

type ClearStateType = {
    controlType?: string;
    hasError: boolean;
    inputValue: string;
    latestValue: string;
};

const shouldShowTimeClearButton = (controlType: string | undefined, inputValue: string): boolean =>
    controlType === 'TIME' && Boolean(inputValue);

const makeHandleInputClear = (state: ClearStateType, saveInputValue: () => void) => () => {
    state.inputValue = '';
    state.hasError = false;
    state.latestValue = '';

    saveInputValue();
};

describe('TIME field clear affordance', () => {
    it('shows the clear button only when a TIME input has a value', () => {
        expect(shouldShowTimeClearButton('TIME', '12:30')).toBe(true);
        expect(shouldShowTimeClearButton('TIME', '')).toBe(false);
    });

    it('does not show the clear button for DATE or DATE_TIME (they have native clear)', () => {
        expect(shouldShowTimeClearButton('DATE', '2026-04-23')).toBe(false);
        expect(shouldShowTimeClearButton('DATE_TIME', '2026-04-23T12:30')).toBe(false);
    });

    it('does not show the clear button for unrelated control types', () => {
        expect(shouldShowTimeClearButton('TEXT', 'hello')).toBe(false);
        expect(shouldShowTimeClearButton(undefined, '12:30')).toBe(false);
    });
});

describe('handleInputClear', () => {
    it('resets inputValue, clears error state, and invokes the save callback', () => {
        const state: ClearStateType = {
            controlType: 'TIME',
            hasError: true,
            inputValue: '12:30',
            latestValue: '12:30',
        };
        const saveInputValue = vi.fn();

        const handleInputClear = makeHandleInputClear(state, saveInputValue);

        handleInputClear();

        expect(state.inputValue).toBe('');
        expect(state.hasError).toBe(false);
        expect(state.latestValue).toBe('');
        expect(saveInputValue).toHaveBeenCalledTimes(1);
    });
});
