import {useLoadProjectPermissions} from '@/shared/hooks/useLoadProjectPermissions';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {renderHook} from '@testing-library/react';
import {toast} from 'sonner';
import {beforeEach, describe, expect, it, vi} from 'vitest';

// Hoisted handles so each test can configure the mocked query result before the component renders. Without hoisting
// the mock module would close over the initial values and ignore later mutations.
const hoisted = vi.hoisted(() => ({
    queryResult: {data: undefined as undefined | {myProjectScopes: string[] | null}, isError: false, isLoading: false},
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useMyProjectScopesQuery: () => hoisted.queryResult,
}));

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
    },
}));

const PROJECT_ID = 42;

function setQueryResult(result: typeof hoisted.queryResult): void {
    hoisted.queryResult = result;
}

describe('useLoadProjectPermissions', () => {
    beforeEach(() => {
        permissionStore.setState({
            projectStates: {},
            workspaceStates: {},
        });
        hoisted.queryResult = {data: undefined, isError: false, isLoading: false};
        vi.clearAllMocks();
    });

    it('marks the project as loading while the query is in flight', () => {
        // The discriminated union forces "loading" to drop any previously cached scopes. Without this, a user
        // demoted mid-session would keep stale ALLOW decisions while the re-fetch is in flight.
        setQueryResult({data: undefined, isError: false, isLoading: true});

        renderHook(() => useLoadProjectPermissions(PROJECT_ID));

        expect(permissionStore.getState().projectStates[PROJECT_ID]).toEqual({status: 'loading'});
    });

    it('stores fetched scopes when the query succeeds', () => {
        setQueryResult({
            data: {myProjectScopes: ['WORKFLOW_VIEW', 'WORKFLOW_EDIT']},
            isError: false,
            isLoading: false,
        });

        renderHook(() => useLoadProjectPermissions(PROJECT_ID));

        expect(permissionStore.getState().projectStates[PROJECT_ID]).toEqual({
            scopes: ['WORKFLOW_VIEW', 'WORKFLOW_EDIT'],
            status: 'loaded',
        });
    });

    it('stores empty scopes when the server reports no membership', () => {
        // A defined-but-empty scope list means the server authoritatively says the user has no access. Storing this
        // as 'loaded' (with empty scopes) lets useHasProjectScope distinguish "still loading" from "loaded but
        // denied".
        setQueryResult({data: {myProjectScopes: []}, isError: false, isLoading: false});

        renderHook(() => useLoadProjectPermissions(PROJECT_ID));

        expect(permissionStore.getState().projectStates[PROJECT_ID]).toEqual({
            scopes: [],
            status: 'loaded',
        });
    });

    it('treats a null scopes payload as loaded with empty scopes', () => {
        // The GraphQL schema defines myProjectScopes as `[String!]!` (non-nullable list of non-nullable strings),
        // but the type generator emits `string[] | null`. A null payload should be treated as "no membership".
        setQueryResult({data: {myProjectScopes: null}, isError: false, isLoading: false});

        renderHook(() => useLoadProjectPermissions(PROJECT_ID));

        expect(permissionStore.getState().projectStates[PROJECT_ID]).toEqual({
            scopes: [],
            status: 'loaded',
        });
    });

    it('marks the project as errored, fires a toast, and clears stale scopes on fetch failure', () => {
        // Critical fail-closed path: keeping the stale cache on error would let useHasProjectScope return true for
        // scopes the backend has since revoked. The discriminated union forces 'error' to drop scopes, and the
        // toast informs the user why gating tightened.
        permissionStore.setState({
            projectStates: {[PROJECT_ID]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'}},
            workspaceStates: {},
        });

        setQueryResult({data: undefined, isError: true, isLoading: false});

        renderHook(() => useLoadProjectPermissions(PROJECT_ID));

        expect(permissionStore.getState().projectStates[PROJECT_ID]).toEqual({status: 'error'});
        expect(toast.error).toHaveBeenCalledOnce();
    });

    it('does not touch the store when projectId is undefined', () => {
        // Caller may not yet know the projectId (route params still loading). The hook must no-op rather than
        // pollute the store with state for an unknown id.
        renderHook(() => useLoadProjectPermissions(undefined));

        expect(permissionStore.getState().projectStates).toEqual({});
    });

    it('returns the underlying loading and error flags for the caller', () => {
        // The hook exposes loading/error so callers that do their own UI gating do not have to subscribe to the
        // store. This is the convenience contract on top of the store-side state machine.
        setQueryResult({data: undefined, isError: false, isLoading: true});

        const {result} = renderHook(() => useLoadProjectPermissions(PROJECT_ID));

        expect(result.current).toEqual({error: false, loading: true});
    });
});
