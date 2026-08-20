import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useProjectVisibility} from '../useProjectVisibility';

const {
    grantProjectAccessMutateMock,
    revokeProjectAccessMutateMock,
    setProjectVisibilityMutateMock,
    useProjectGrantsQueryMock,
    useVisibilityFeatureEnabledMock,
    useWorkspaceUsersQueryMock,
} = vi.hoisted(() => ({
    grantProjectAccessMutateMock: vi.fn(),
    revokeProjectAccessMutateMock: vi.fn(),
    setProjectVisibilityMutateMock: vi.fn(),
    useProjectGrantsQueryMock: vi.fn(),
    useVisibilityFeatureEnabledMock: vi.fn(),
    useWorkspaceUsersQueryMock: vi.fn(),
}));

vi.mock('@/shared/hooks/useVisibilityFeatureEnabled', () => ({
    useVisibilityFeatureEnabled: useVisibilityFeatureEnabledMock,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    ResourceVisibility: {Organization: 'ORGANIZATION', Private: 'PRIVATE', Workspace: 'WORKSPACE'},
    useGrantProjectAccessMutation: () => ({mutate: grantProjectAccessMutateMock}),
    useProjectGrantsQuery: useProjectGrantsQueryMock,
    useRevokeProjectAccessMutation: () => ({mutate: revokeProjectAccessMutateMock}),
    useSetProjectVisibilityMutation: () => ({mutate: setProjectVisibilityMutateMock}),
    useWorkspaceUsersQuery: useWorkspaceUsersQueryMock,
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: vi.fn()}),
}));

describe('useProjectVisibility', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        useVisibilityFeatureEnabledMock.mockReturnValue({enabled: true, isAdmin: false, workspaceId: 7});
        useProjectGrantsQueryMock.mockReturnValue({data: {projectGrants: [1, 2, 3]}});
        useWorkspaceUsersQueryMock.mockReturnValue({data: {workspaceUsers: []}});
    });

    it('revokes only the removed users and grants only the added ones', () => {
        const {result} = renderHook(() => useProjectVisibility({projectId: 5, visibility: 'PRIVATE'}));

        result.current.onGrantedUserIdsChange([2, 3, 4]);

        // 1 left the audience, 4 joined it, and 2 and 3 must be touched by neither mutation - a revoke for an
        // unchanged user silently removes access someone still has.
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
