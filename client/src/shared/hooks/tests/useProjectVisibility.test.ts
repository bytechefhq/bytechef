import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useProjectVisibility} from '../useProjectVisibility';

const {
    grantProjectAccessMutateMock,
    invalidateQueriesMock,
    revokeProjectAccessMutateMock,
    setProjectVisibilityMutateMock,
    useGrantProjectAccessMutationMock,
    useMyWorkspaceRoleQueryMock,
    useProjectGrantsQueryMock,
    useRevokeProjectAccessMutationMock,
    useSetProjectVisibilityMutationMock,
    useVisibilityFeatureEnabledMock,
    useWorkspaceUsersQueryMock,
} = vi.hoisted(() => ({
    grantProjectAccessMutateMock: vi.fn(),
    invalidateQueriesMock: vi.fn(),
    revokeProjectAccessMutateMock: vi.fn(),
    setProjectVisibilityMutateMock: vi.fn(),
    useGrantProjectAccessMutationMock: vi.fn(),
    useMyWorkspaceRoleQueryMock: vi.fn(),
    useProjectGrantsQueryMock: vi.fn(),
    useRevokeProjectAccessMutationMock: vi.fn(),
    useSetProjectVisibilityMutationMock: vi.fn(),
    useVisibilityFeatureEnabledMock: vi.fn(),
    useWorkspaceUsersQueryMock: vi.fn(),
}));

vi.mock('@/shared/hooks/useVisibilityFeatureEnabled', () => ({
    useVisibilityFeatureEnabled: useVisibilityFeatureEnabledMock,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    ResourceVisibility: {Organization: 'ORGANIZATION', Private: 'PRIVATE', Workspace: 'WORKSPACE'},
    useGrantProjectAccessMutation: useGrantProjectAccessMutationMock,
    useMyWorkspaceRoleQuery: useMyWorkspaceRoleQueryMock,
    useProjectGrantsQuery: useProjectGrantsQueryMock,
    useRevokeProjectAccessMutation: useRevokeProjectAccessMutationMock,
    useSetProjectVisibilityMutation: useSetProjectVisibilityMutationMock,
    useWorkspaceUsersQuery: useWorkspaceUsersQueryMock,
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: invalidateQueriesMock}),
}));

describe('useProjectVisibility', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        useGrantProjectAccessMutationMock.mockReturnValue({mutate: grantProjectAccessMutateMock});
        useMyWorkspaceRoleQueryMock.mockReturnValue({data: {myWorkspaceRole: null}});
        useRevokeProjectAccessMutationMock.mockReturnValue({mutate: revokeProjectAccessMutateMock});
        useSetProjectVisibilityMutationMock.mockReturnValue({mutate: setProjectVisibilityMutateMock});
        useVisibilityFeatureEnabledMock.mockReturnValue({enabled: true, isAdmin: false, workspaceId: 7});
        useProjectGrantsQueryMock.mockReturnValue({data: {projectGrants: [1, 2, 3]}});
        useWorkspaceUsersQueryMock.mockReturnValue({data: {workspaceUsers: []}});
    });

    it('revokes only the removed users and grants only the added ones', () => {
        const {result} = renderHook(() => useProjectVisibility({projectId: 5, visibility: 'PRIVATE'}));

        result.current.onGrantedUserIdsChange([2, 3, 4]);

        expect(revokeProjectAccessMutateMock).toHaveBeenCalledTimes(1);
        expect(revokeProjectAccessMutateMock).toHaveBeenCalledWith({
            projectId: '5',
            userId: '1',
            workspaceId: '7',
        });

        expect(grantProjectAccessMutateMock).toHaveBeenCalledTimes(1);
        expect(grantProjectAccessMutateMock).toHaveBeenCalledWith({
            projectId: '5',
            userId: '4',
            workspaceId: '7',
        });

        const touchedUserIds = [...revokeProjectAccessMutateMock.mock.calls, ...grantProjectAccessMutateMock.mock.calls]
            .map(([variables]) => variables.userId)
            .sort();

        expect(touchedUserIds).toEqual(['1', '4']);
    });

    it('refreshes the shared audience after a grant, a revoke or a visibility change', () => {
        renderHook(() => useProjectVisibility({projectId: 5, visibility: 'PRIVATE'}));

        for (const mutationHookMock of [
            useGrantProjectAccessMutationMock,
            useRevokeProjectAccessMutationMock,
            useSetProjectVisibilityMutationMock,
        ]) {
            invalidateQueriesMock.mockClear();

            mutationHookMock.mock.calls[0][0].onSuccess();

            expect(invalidateQueriesMock).toHaveBeenCalledWith({queryKey: ['ProjectGrants']});
        }
    });

    it('sends the picked reach on a visibility change', () => {
        const {result} = renderHook(() => useProjectVisibility({projectId: 5, visibility: 'WORKSPACE'}));

        result.current.onVisibilityChange('PRIVATE');

        expect(setProjectVisibilityMutateMock).toHaveBeenCalledWith({
            projectId: '5',
            visibility: 'PRIVATE',
            workspaceId: '7',
        });
    });

    it('skips the grant and member lookups while the project is workspace-visible', () => {
        renderHook(() => useProjectVisibility({projectId: 5, visibility: 'WORKSPACE'}));

        expect(useProjectGrantsQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
        expect(useWorkspaceUsersQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
    });

    it('lets a workspace admin who does not own the project manage its sharing', () => {
        useMyWorkspaceRoleQueryMock.mockReturnValue({data: {myWorkspaceRole: 'ADMIN'}});

        const {result} = renderHook(() =>
            useProjectVisibility({createdBy: 'someone-else@example.com', projectId: 5, visibility: 'PRIVATE'})
        );

        expect(result.current.canManage).toBe(true);
        expect(useProjectGrantsQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: true});
    });

    it('does not let a workspace editor who does not own the project manage its sharing', () => {
        useMyWorkspaceRoleQueryMock.mockReturnValue({data: {myWorkspaceRole: 'EDITOR'}});

        const {result} = renderHook(() =>
            useProjectVisibility({createdBy: 'someone-else@example.com', projectId: 5, visibility: 'PRIVATE'})
        );

        expect(result.current.canManage).toBe(false);
    });

    it('reports itself disabled without a workspace', () => {
        useVisibilityFeatureEnabledMock.mockReturnValue({enabled: false, isAdmin: false, workspaceId: undefined});

        const {result} = renderHook(() => useProjectVisibility({projectId: 5, visibility: 'PRIVATE'}));

        expect(result.current.enabled).toBe(false);
    });

    it('reports itself disabled for a project that does not exist yet', () => {
        const {result} = renderHook(() => useProjectVisibility({projectId: undefined, visibility: 'PRIVATE'}));

        expect(result.current.enabled).toBe(false);
    });
});
