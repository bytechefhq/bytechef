import {useLoadWorkspaceScopes} from '@/shared/hooks/useLoadWorkspaceScopes';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {renderHook} from '@testing-library/react';
import {toast} from 'sonner';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    eeEdition: true,
    myWorkspaceScopesQuery: vi.fn(),
}));

vi.mock('@/shared/edition/useEeEdition', () => ({default: () => hoisted.eeEdition}));

vi.mock('@/shared/middleware/graphql', () => ({
    EnvironmentEnum: {Development: 'DEVELOPMENT', Production: 'PRODUCTION', Staging: 'STAGING'},
    useMyWorkspaceScopesQuery: hoisted.myWorkspaceScopesQuery,
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 2}),
}));

vi.mock('sonner', () => ({toast: {error: vi.fn()}}));

describe('useLoadWorkspaceScopes', () => {
    beforeEach(() => {
        hoisted.eeEdition = true;
        hoisted.myWorkspaceScopesQuery.mockReset();

        permissionStore.setState({workspaceScopeStates: {}});
    });

    it('stores the scopes of the selected environment on Enterprise', () => {
        hoisted.myWorkspaceScopesQuery.mockReturnValue({
            data: {myWorkspaceScopes: ['API_KEY_VIEW']},
            isError: false,
            isLoading: false,
        });

        renderHook(() => useLoadWorkspaceScopes(7));

        expect(hoisted.myWorkspaceScopesQuery).toHaveBeenCalledWith(
            {environment: 'PRODUCTION', workspaceId: '7'},
            {enabled: true}
        );
        expect(permissionStore.getState().workspaceScopeStates[7][2]).toEqual({
            scopes: ['API_KEY_VIEW'],
            status: 'loaded',
        });
    });

    it('marks the scopes as loading while the query is in flight', () => {
        hoisted.myWorkspaceScopesQuery.mockReturnValue({data: undefined, isError: false, isLoading: true});

        const {result} = renderHook(() => useLoadWorkspaceScopes(7));

        expect(result.current).toEqual({error: false, loading: true});
        expect(permissionStore.getState().workspaceScopeStates[7][2]).toEqual({status: 'loading'});
    });

    it('marks the scopes as failed and tells the user when the query fails', () => {
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

        hoisted.myWorkspaceScopesQuery.mockReturnValue({data: undefined, isError: true, isLoading: false});

        const {result} = renderHook(() => useLoadWorkspaceScopes(7));

        expect(result.current).toEqual({error: true, loading: false});
        expect(permissionStore.getState().workspaceScopeStates[7][2]).toEqual({status: 'error'});
        expect(toast.error).toHaveBeenCalledWith(
            'Could not load workspace permissions. Some actions may appear restricted until you refresh.',
            {id: 'workspace-permissions-error'}
        );

        consoleErrorSpy.mockRestore();
    });

    it('never sends the query on Community', () => {
        hoisted.eeEdition = false;
        hoisted.myWorkspaceScopesQuery.mockReturnValue({data: undefined, isError: false, isLoading: false});

        renderHook(() => useLoadWorkspaceScopes(7));

        for (const call of hoisted.myWorkspaceScopesQuery.mock.calls) {
            expect(call[1]).toEqual({enabled: false});
        }

        expect(permissionStore.getState().workspaceScopeStates).toEqual({});
    });
});
