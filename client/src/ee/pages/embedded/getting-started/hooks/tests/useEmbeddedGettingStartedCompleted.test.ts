import {gettingStartedStore} from '@/shared/stores/useGettingStartedStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useEmbeddedGettingStartedCompleted from '../useEmbeddedGettingStartedCompleted';

const hoisted = vi.hoisted(() => ({
    queryOptions: undefined as {enabled: boolean} | undefined,
    queryResult: {} as {data?: {embeddedGettingStartedCompleted: boolean}},
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useEmbeddedGettingStartedCompletedQuery: (variables: unknown, options: {enabled: boolean}) => {
        hoisted.queryOptions = options;

        return hoisted.queryResult;
    },
}));

describe('useEmbeddedGettingStartedCompleted', () => {
    beforeEach(() => {
        localStorage.clear();

        gettingStartedStore.setState({completions: {}});

        hoisted.queryOptions = undefined;
        hoisted.queryResult = {};
    });

    it('queries the server while nothing is stored', () => {
        const {result} = renderHook(() => useEmbeddedGettingStartedCompleted(true));

        expect(hoisted.queryOptions).toEqual({enabled: true});
        expect(result.current).toBeUndefined();
    });

    it('does not query the server when disabled', () => {
        renderHook(() => useEmbeddedGettingStartedCompleted(false));

        expect(hoisted.queryOptions).toEqual({enabled: false});
    });

    it('skips the query once a completion is stored', () => {
        gettingStartedStore.getState().complete('embedded', 'DISMISSED');

        const {result} = renderHook(() => useEmbeddedGettingStartedCompleted(true));

        expect(hoisted.queryOptions).toEqual({enabled: false});
        expect(result.current).toBe(true);
    });

    it('stores the completion the server reports', () => {
        hoisted.queryResult = {data: {embeddedGettingStartedCompleted: true}};

        const {result} = renderHook(() => useEmbeddedGettingStartedCompleted(true));

        expect(result.current).toBe(true);
        expect(gettingStartedStore.getState().completions.embedded).toBe('COMPLETED');
    });

    it('stores nothing while the server reports the setup as unfinished', () => {
        hoisted.queryResult = {data: {embeddedGettingStartedCompleted: false}};

        const {result} = renderHook(() => useEmbeddedGettingStartedCompleted(true));

        expect(result.current).toBe(false);
        expect(gettingStartedStore.getState().completions.embedded).toBeUndefined();
    });
});
