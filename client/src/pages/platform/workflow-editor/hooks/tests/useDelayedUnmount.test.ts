import useDelayedUnmount from '@/pages/platform/workflow-editor/hooks/useDelayedUnmount';
import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const flushAnimationFrames = async () => {
    await act(async () => {
        await Promise.resolve();
    });
};

describe('useDelayedUnmount', () => {
    beforeEach(() => {
        vi.useFakeTimers();

        vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => {
            callback(0);

            return 1;
        });
        vi.stubGlobal('cancelAnimationFrame', vi.fn());
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.unstubAllGlobals();
    });

    it('mounts and shows an element that opens', async () => {
        const {result} = renderHook(() => useDelayedUnmount(true));

        await flushAnimationFrames();

        expect(result.current.mounted).toBe(true);
        expect(result.current.visible).toBe(true);
    });

    it('keeps a closed element unmounted', () => {
        const {result} = renderHook(() => useDelayedUnmount(false));

        expect(result.current.mounted).toBe(false);
        expect(result.current.visible).toBe(false);
    });

    it('hides before unmounting so the transition can run', async () => {
        const {rerender, result} = renderHook(({open}) => useDelayedUnmount(open, 300), {
            initialProps: {open: true},
        });

        await flushAnimationFrames();

        rerender({open: false});

        expect(result.current.visible).toBe(false);
        expect(result.current.mounted).toBe(true);

        act(() => {
            vi.advanceTimersByTime(300);
        });

        expect(result.current.mounted).toBe(false);
    });

    it('unmounts at once when the caller asks for no exit animation', async () => {
        const {rerender, result} = renderHook(({open}) => useDelayedUnmount(open, 0), {
            initialProps: {open: true},
        });

        await flushAnimationFrames();

        rerender({open: false});

        act(() => {
            vi.advanceTimersByTime(0);
        });

        expect(result.current.mounted).toBe(false);
    });

    it('cancels the pending unmount when it is reopened', async () => {
        const {rerender, result} = renderHook(({open}) => useDelayedUnmount(open, 300), {
            initialProps: {open: true},
        });

        await flushAnimationFrames();

        rerender({open: false});
        rerender({open: true});

        act(() => {
            vi.advanceTimersByTime(300);
        });

        expect(result.current.mounted).toBe(true);
    });
});
