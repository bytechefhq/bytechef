import {gettingStartedStore} from '@/shared/stores/useGettingStartedStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useAutomationGettingStartedCompleted from '../useAutomationGettingStartedCompleted';

const hoisted = vi.hoisted(() => ({
    queryOptions: undefined as {enabled: boolean} | undefined,
    queryResult: {} as {data?: {automationGettingStartedCompleted: boolean}},
    queryVariables: undefined as unknown,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationGettingStartedCompletedQuery: (variables: unknown, options: {enabled: boolean}) => {
        hoisted.queryOptions = options;
        hoisted.queryVariables = variables;

        return hoisted.queryResult;
    },
}));

describe('useAutomationGettingStartedCompleted', () => {
    beforeEach(() => {
        localStorage.clear();

        gettingStartedStore.setState({completions: {}});

        hoisted.queryOptions = undefined;
        hoisted.queryResult = {};
        hoisted.queryVariables = undefined;
    });

    it('queries the server for the workspace while nothing is stored', () => {
        const {result} = renderHook(() => useAutomationGettingStartedCompleted({enabled: true, workspaceId: 5}));

        expect(hoisted.queryVariables).toEqual({workspaceId: 5});
        expect(hoisted.queryOptions).toEqual({enabled: true});
        expect(result.current).toBeUndefined();
    });

    it('does not query the server when disabled', () => {
        renderHook(() => useAutomationGettingStartedCompleted({enabled: false, workspaceId: 5}));

        expect(hoisted.queryOptions).toEqual({enabled: false});
    });

    it('skips the query once a completion is stored', () => {
        gettingStartedStore.getState().complete('automation', 'DISMISSED');

        const {result} = renderHook(() => useAutomationGettingStartedCompleted({enabled: true, workspaceId: 5}));

        expect(hoisted.queryOptions).toEqual({enabled: false});
        expect(result.current).toBe(true);
    });

    it('stores the completion the server reports', () => {
        hoisted.queryResult = {data: {automationGettingStartedCompleted: true}};

        const {result} = renderHook(() => useAutomationGettingStartedCompleted({enabled: true, workspaceId: 5}));

        expect(result.current).toBe(true);
        expect(gettingStartedStore.getState().completions.automation).toBe('COMPLETED');
    });

    it('stores nothing while the server reports the guide as unfinished', () => {
        hoisted.queryResult = {data: {automationGettingStartedCompleted: false}};

        const {result} = renderHook(() => useAutomationGettingStartedCompleted({enabled: true, workspaceId: 5}));

        expect(result.current).toBe(false);
        expect(gettingStartedStore.getState().completions.automation).toBeUndefined();
    });
});
