import useIsDarkMode from '@/shared/hooks/useIsDarkMode';
import {act, renderHook, waitFor} from '@testing-library/react';
import {afterEach, describe, expect, it} from 'vitest';

describe('useIsDarkMode', () => {
    afterEach(() => {
        document.documentElement.classList.remove('dark', 'light');
    });

    it('reports the mode already on the root element at mount', () => {
        document.documentElement.classList.add('dark');

        const {result} = renderHook(() => useIsDarkMode());

        expect(result.current).toBe(true);
    });

    it('reports light when the root element carries no dark class', () => {
        document.documentElement.classList.add('light');

        const {result} = renderHook(() => useIsDarkMode());

        expect(result.current).toBe(false);
    });

    it('follows the root element when the theme is switched after mount', async () => {
        document.documentElement.classList.add('light');

        const {result} = renderHook(() => useIsDarkMode());

        expect(result.current).toBe(false);

        act(() => {
            document.documentElement.classList.remove('light');
            document.documentElement.classList.add('dark');
        });

        await waitFor(() => expect(result.current).toBe(true));

        act(() => {
            document.documentElement.classList.remove('dark');
            document.documentElement.classList.add('light');
        });

        await waitFor(() => expect(result.current).toBe(false));
    });

    it('stops observing once unmounted', async () => {
        const {result, unmount} = renderHook(() => useIsDarkMode());

        unmount();

        document.documentElement.classList.add('dark');

        await new Promise((resolve) => setTimeout(resolve, 20));

        expect(result.current).toBe(false);
    });
});
