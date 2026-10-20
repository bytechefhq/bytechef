import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import {UNION_ENVIRONMENT_ID} from '@/shared/hooks/useHasEmbeddedScope';
import {useLoadEmbeddedScopes} from '@/shared/hooks/useLoadEmbeddedScopes';
import {useMyEmbeddedScopesQuery} from '@/shared/middleware/graphql';
import {MY_EMBEDDED_SCOPES_QUERY_KEY, invalidateMyEmbeddedScopeQueries} from '@/shared/queries/permissions.queries';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {WorkspaceScopePermissionStateType, permissionStore} from '@/shared/stores/usePermissionStore';
import {QueryClient, QueryObserver} from '@tanstack/react-query';
import {renderHook} from '@testing-library/react';
import {toast} from 'sonner';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    queryResult: {
        data: undefined as undefined | {myEmbeddedScopes: string[] | null},
        isError: false,
        isLoading: false,
    },
}));

vi.mock('@/shared/middleware/graphql', () => ({
    EnvironmentEnum: {Development: 'DEVELOPMENT', Production: 'PRODUCTION', Staging: 'STAGING'},
    useMyEmbeddedScopesQuery: vi.fn(() => hoisted.queryResult),
}));

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
    },
}));

function setEdition(edition: EditionType | null): void {
    applicationInfoStore.setState({application: edition === null ? null : {edition}});
}

function storedState(environmentId: number): WorkspaceScopePermissionStateType | undefined {
    return permissionStore.getState().embeddedScopeStates[environmentId];
}

function requestedCalls(): [unknown, unknown][] {
    return vi
        .mocked(useMyEmbeddedScopesQuery)
        .mock.calls.map(([variables, options]) => [variables?.environment, options?.enabled]);
}

describe('useLoadEmbeddedScopes', () => {
    beforeEach(() => {
        permissionStore.setState({embeddedScopeStates: {}, workspaceScopeStates: {}});
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
        hoisted.queryResult = {data: undefined, isError: false, isLoading: false};
        vi.clearAllMocks();

        setEdition(EditionType.EE);
    });

    it('testMarksTheUnionAsLoadingWhileTheQueryIsInFlight', () => {
        hoisted.queryResult = {data: undefined, isError: false, isLoading: true};

        renderHook(() => useLoadEmbeddedScopes(true));

        expect(storedState(UNION_ENVIRONMENT_ID)).toEqual({status: 'loading'});
    });

    it('testStoresFetchedScopes', () => {
        hoisted.queryResult = {data: {myEmbeddedScopes: ['WORKFLOW_VIEW']}, isError: false, isLoading: false};

        renderHook(() => useLoadEmbeddedScopes(true));

        expect(storedState(UNION_ENVIRONMENT_ID)).toEqual({scopes: ['WORKFLOW_VIEW'], status: 'loaded'});
        expect(storedState(DEVELOPMENT_ENVIRONMENT)).toEqual({scopes: ['WORKFLOW_VIEW'], status: 'loaded'});
    });

    it('testTreatsANullPayloadAsLoadedWithNoScopes', () => {
        hoisted.queryResult = {data: {myEmbeddedScopes: null}, isError: false, isLoading: false};

        renderHook(() => useLoadEmbeddedScopes(true));

        expect(storedState(UNION_ENVIRONMENT_ID)).toEqual({scopes: [], status: 'loaded'});
    });

    it('testAFirstFetchErrorStoresTheErrorAndRaisesTheToast', () => {
        permissionStore.setState({
            embeddedScopeStates: {[UNION_ENVIRONMENT_ID]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'}},
        });
        hoisted.queryResult = {data: undefined, isError: true, isLoading: false};

        renderHook(() => useLoadEmbeddedScopes(true));

        expect(storedState(UNION_ENVIRONMENT_ID)).toEqual({status: 'error'});
        expect(toast.error).toHaveBeenCalledWith(
            'Could not load embedded permissions. Some actions may appear restricted until you refresh.',
            {id: 'embedded-permissions-error'}
        );
    });

    it('testABackgroundRefetchErrorKeepsTheLastGoodScopes', () => {
        hoisted.queryResult = {data: {myEmbeddedScopes: ['WORKFLOW_EDIT']}, isError: true, isLoading: false};

        const {result} = renderHook(() => useLoadEmbeddedScopes(true));

        expect(storedState(UNION_ENVIRONMENT_ID)).toEqual({scopes: ['WORKFLOW_EDIT'], status: 'loaded'});
        expect(result.current).toEqual({error: false, loading: false});
        expect(toast.error).not.toHaveBeenCalled();
    });

    it('testLoadsTheUnionTheCurrentEnvironmentAndDevelopment', () => {
        environmentStore.setState({currentEnvironmentId: STAGING_ENVIRONMENT});

        renderHook(() => useLoadEmbeddedScopes(true));

        const requested = requestedCalls();

        expect(requested).toContainEqual([null, true]);
        expect(requested).toContainEqual(['STAGING', true]);
        expect(requested).toContainEqual(['DEVELOPMENT', true]);
        expect(requested).not.toContainEqual(['PRODUCTION', true]);
    });

    it('testAsksForTheUnionWithANullEnvironment', () => {
        renderHook(() => useLoadEmbeddedScopes(true));

        const [variables] = vi.mocked(useMyEmbeddedScopesQuery).mock.calls[0];

        expect(variables).toEqual({environment: null});
    });

    it('testStoresScopesUnderTheCurrentEnvironmentDevelopmentAndTheUnionOnly', () => {
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});
        hoisted.queryResult = {data: {myEmbeddedScopes: ['MCP_VIEW']}, isError: false, isLoading: false};

        renderHook(() => useLoadEmbeddedScopes(true));

        expect(storedState(PRODUCTION_ENVIRONMENT)).toEqual({scopes: ['MCP_VIEW'], status: 'loaded'});
        expect(storedState(DEVELOPMENT_ENVIRONMENT)).toEqual({scopes: ['MCP_VIEW'], status: 'loaded'});
        expect(storedState(UNION_ENVIRONMENT_ID)).toEqual({scopes: ['MCP_VIEW'], status: 'loaded'});
        expect(storedState(STAGING_ENVIRONMENT)).toBeUndefined();
    });

    it('testNothingLoadsWhenInactive', () => {
        hoisted.queryResult = {data: {myEmbeddedScopes: ['MCP_VIEW']}, isError: false, isLoading: true};

        renderHook(() => useLoadEmbeddedScopes(false));

        expect(requestedCalls().every(([, enabled]) => enabled === false)).toBe(true);
        expect(permissionStore.getState().embeddedScopeStates).toEqual({});
    });

    it('testNeverFiresOnCommunity', () => {
        setEdition(EditionType.CE);
        hoisted.queryResult = {data: undefined, isError: false, isLoading: true};

        renderHook(() => useLoadEmbeddedScopes(true));

        expect(requestedCalls().every(([, enabled]) => enabled === false)).toBe(true);
        expect(permissionStore.getState().embeddedScopeStates).toEqual({});
        expect(toast.error).not.toHaveBeenCalled();
    });

    it('testWaitsWhileTheEditionIsUnresolved', () => {
        setEdition(null);

        renderHook(() => useLoadEmbeddedScopes(true));

        expect(requestedCalls().every(([, enabled]) => enabled === false)).toBe(true);
        expect(permissionStore.getState().embeddedScopeStates).toEqual({});
    });
});

describe('invalidateMyEmbeddedScopeQueries', () => {
    it('testInvalidatingRefetchesEveryEmbeddedScopeQuery', async () => {
        const queryClient = new QueryClient();
        const fetchCounts: Record<string, number> = {};

        const unsubscribers = [null, 'DEVELOPMENT', 'PRODUCTION'].map((environment) => {
            const environmentName = String(environment);

            fetchCounts[environmentName] = 0;

            return new QueryObserver(queryClient, {
                queryFn: async () => {
                    fetchCounts[environmentName] += 1;

                    return {myEmbeddedScopes: []};
                },
                queryKey: [MY_EMBEDDED_SCOPES_QUERY_KEY, {environment}],
            }).subscribe(() => {});
        });

        await vi.waitFor(() => expect(Object.values(fetchCounts)).toEqual([1, 1, 1]));
        await vi.waitFor(() => expect(queryClient.isFetching()).toBe(0));

        invalidateMyEmbeddedScopeQueries(queryClient);

        await vi.waitFor(() => expect(Object.values(fetchCounts)).toEqual([2, 2, 2]));

        unsubscribers.forEach((unsubscribe) => unsubscribe());
    });
});
