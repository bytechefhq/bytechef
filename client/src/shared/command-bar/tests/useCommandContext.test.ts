import {useCommandContext} from '@/shared/command-bar/useCommandContext';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    return {
        storeState: {
            application: {edition: 'CE'} as {edition: string} | null,
            flags: {} as Record<string, boolean>,
            pathname: '/automation',
        },
    };
});

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {application: {edition: string} | null}) => unknown) =>
        selector({application: hoisted.storeState.application}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    // Mirrors the real store, which returns a brand-new function identity on every call.
    useFeatureFlagsStore: () => (featureFlag: string) => hoisted.storeState.flags[featureFlag] ?? false,
}));

vi.mock('react-router-dom', () => ({
    useLocation: () => ({pathname: hoisted.storeState.pathname}),
}));

beforeEach(() => {
    hoisted.storeState.application = {edition: 'CE'};
    hoisted.storeState.flags = {};
    hoisted.storeState.pathname = '/automation';
});

describe('useCommandContext', () => {
    it('keeps reference equality across a rerender when edition and pathname are unchanged', () => {
        const {rerender, result} = renderHook(() => useCommandContext());

        const first = result.current;

        rerender();

        expect(result.current).toBe(first);
    });

    it('produces a new context when pathname changes', () => {
        const {rerender, result} = renderHook(() => useCommandContext());

        const first = result.current;

        hoisted.storeState.pathname = '/embedded';
        rerender();

        expect(result.current).not.toBe(first);
        expect(result.current.pathname).toBe('/embedded');
    });

    it('exposes feature flags through a stable accessor that reads live state', () => {
        const {rerender, result} = renderHook(() => useCommandContext());

        expect(result.current.featureFlags('ff-1')).toBe(false);

        hoisted.storeState.flags = {'ff-1': true};
        rerender();

        expect(result.current.featureFlags('ff-1')).toBe(true);
    });
});
