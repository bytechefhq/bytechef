import {useLoadWorkspaceScopes} from '@/shared/hooks/useLoadWorkspaceScopes';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {renderHook} from '@testing-library/react';
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
