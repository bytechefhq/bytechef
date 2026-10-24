import {renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useDebouncedSave from './useDebouncedSave';

describe('useDebouncedSave', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('skips a value equal to the last one actually saved', () => {
        const save = vi.fn();

        const {rerender} = renderHook(({value}) => useDebouncedSave(value, save, 600), {
            initialProps: {value: {name: 'seeded'}},
        });

        rerender({value: {name: 'seeded'}});

        vi.advanceTimersByTime(600);

        expect(save).not.toHaveBeenCalled();
    });

    it('still saves when an identical-JSON new object arrives before the timeout elapses', () => {
        const save = vi.fn();

        const {rerender} = renderHook(({value}) => useDebouncedSave(value, save, 600), {
            initialProps: {value: {name: 'first'}},
        });

        rerender({value: {name: 'second'}});

        vi.advanceTimersByTime(300);

        rerender({value: {name: 'second'}});

        vi.advanceTimersByTime(600);

        expect(save).toHaveBeenCalledTimes(1);
        expect(save).toHaveBeenCalledWith({name: 'second'});
    });

    it('cancels a pending save when the value reverts before the timeout elapses', () => {
        const save = vi.fn();

        const {rerender} = renderHook(({value}) => useDebouncedSave(value, save, 600), {
            initialProps: {value: {name: 'first'}},
        });

        rerender({value: {name: 'second'}});

        vi.advanceTimersByTime(300);

        rerender({value: {name: 'first'}});

        vi.advanceTimersByTime(600);

        expect(save).not.toHaveBeenCalled();
    });
});
