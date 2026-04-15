import {useLoadWorkspacePermissions} from '@/shared/hooks/useLoadWorkspacePermissions';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {renderHook} from '@testing-library/react';
import {toast} from 'sonner';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    queryResult: {
        data: undefined as undefined | {myWorkspaceRole: string | null},
        isError: false,
        isLoading: false,
    },
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useMyWorkspaceRoleQuery: () => hoisted.queryResult,
}));

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
    },
}));

const WORKSPACE_ID = 7;

function setQueryResult(result: typeof hoisted.queryResult): void {
    hoisted.queryResult = result;
}

describe('useLoadWorkspacePermissions', () => {
    beforeEach(() => {
        permissionStore.setState({
            projectStates: {},
            workspaceStates: {},
        });
        hoisted.queryResult = {data: undefined, isError: false, isLoading: false};
        vi.clearAllMocks();
    });

    it('marks the workspace as loading while the query is in flight', () => {
        setQueryResult({data: undefined, isError: false, isLoading: true});

        renderHook(() => useLoadWorkspacePermissions(WORKSPACE_ID));

        expect(permissionStore.getState().workspaceStates[WORKSPACE_ID]).toEqual({status: 'loading'});
    });

    it('stores the fetched role when the query succeeds', () => {
        setQueryResult({data: {myWorkspaceRole: 'EDITOR'}, isError: false, isLoading: false});

        renderHook(() => useLoadWorkspacePermissions(WORKSPACE_ID));

        expect(permissionStore.getState().workspaceStates[WORKSPACE_ID]).toEqual({
            role: 'EDITOR',
            status: 'loaded',
        });
    });

    it('clears the workspace state when the server reports no membership', () => {
        // myWorkspaceRole returns null when the user is not a member \u2014 this is distinct from an error. Clearing
        // the entry (rather than storing 'error') lets useHasWorkspaceRole return false from a missing-key lookup,
        // which is the correct fail-closed behavior for "you are not a member".
        permissionStore.setState({
            projectStates: {},
            workspaceStates: {[WORKSPACE_ID]: {role: 'ADMIN', status: 'loaded'}},
        });

        setQueryResult({data: {myWorkspaceRole: null}, isError: false, isLoading: false});

        renderHook(() => useLoadWorkspacePermissions(WORKSPACE_ID));

        expect(permissionStore.getState().workspaceStates[WORKSPACE_ID]).toBeUndefined();
    });

    it('marks the workspace as errored, fires a toast, and clears stale role on fetch failure', () => {
        permissionStore.setState({
            projectStates: {},
            workspaceStates: {[WORKSPACE_ID]: {role: 'ADMIN', status: 'loaded'}},
        });

        setQueryResult({data: undefined, isError: true, isLoading: false});

        renderHook(() => useLoadWorkspacePermissions(WORKSPACE_ID));

        expect(permissionStore.getState().workspaceStates[WORKSPACE_ID]).toEqual({status: 'error'});
        expect(toast.error).toHaveBeenCalledOnce();
    });

    it('does not touch the store when workspaceId is undefined', () => {
        renderHook(() => useLoadWorkspacePermissions(undefined));

        expect(permissionStore.getState().workspaceStates).toEqual({});
    });

    it('returns the underlying loading and error flags for the caller', () => {
        setQueryResult({data: undefined, isError: false, isLoading: true});

        const {result} = renderHook(() => useLoadWorkspacePermissions(WORKSPACE_ID));

        expect(result.current).toEqual({error: false, loading: true});
    });
});
