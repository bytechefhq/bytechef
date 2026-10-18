import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useCanManageResourceSharing} from '../useCanManageResourceSharing';

const {useIsResourceOwnerOrTenantAdminMock, useMyWorkspaceRoleQueryMock, useVisibilityFeatureEnabledMock} = vi.hoisted(
    () => ({
        useIsResourceOwnerOrTenantAdminMock: vi.fn(),
        useMyWorkspaceRoleQueryMock: vi.fn(),
        useVisibilityFeatureEnabledMock: vi.fn(),
    })
);

vi.mock('@/shared/hooks/useIsResourceOwnerOrTenantAdmin', () => ({
    useIsResourceOwnerOrTenantAdmin: useIsResourceOwnerOrTenantAdminMock,
}));

vi.mock('@/shared/hooks/useVisibilityFeatureEnabled', () => ({
    useVisibilityFeatureEnabled: useVisibilityFeatureEnabledMock,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useMyWorkspaceRoleQuery: useMyWorkspaceRoleQueryMock,
}));

describe('useCanManageResourceSharing', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        useIsResourceOwnerOrTenantAdminMock.mockReturnValue(false);
        useMyWorkspaceRoleQueryMock.mockReturnValue({data: {myWorkspaceRole: null}});
        useVisibilityFeatureEnabledMock.mockReturnValue({enabled: true, isAdmin: false, workspaceId: 7});
    });

    it('lets the owner or a tenant admin manage sharing without asking for the workspace role', () => {
        useIsResourceOwnerOrTenantAdminMock.mockReturnValue(true);

        const {result} = renderHook(() => useCanManageResourceSharing('owner@example.com'));

        expect(result.current).toBe(true);
        expect(useMyWorkspaceRoleQueryMock).toHaveBeenCalledWith({workspaceId: '7'}, {enabled: false});
    });

    it('lets a workspace admin manage sharing', () => {
        useMyWorkspaceRoleQueryMock.mockReturnValue({data: {myWorkspaceRole: 'ADMIN'}});

        const {result} = renderHook(() => useCanManageResourceSharing('someone-else@example.com'));

        expect(result.current).toBe(true);
        expect(useMyWorkspaceRoleQueryMock).toHaveBeenCalledWith({workspaceId: '7'}, {enabled: true});
    });

    it('does not let a member below workspace admin manage sharing', () => {
        useMyWorkspaceRoleQueryMock.mockReturnValue({data: {myWorkspaceRole: 'EDITOR'}});

        const {result} = renderHook(() => useCanManageResourceSharing('someone-else@example.com'));

        expect(result.current).toBe(false);
    });

    it('does not ask for the workspace role outside Enterprise', () => {
        useMyWorkspaceRoleQueryMock.mockReturnValue({data: undefined});
        useVisibilityFeatureEnabledMock.mockReturnValue({enabled: false, isAdmin: false, workspaceId: undefined});

        const {result} = renderHook(() => useCanManageResourceSharing('someone-else@example.com'));

        expect(result.current).toBe(false);
        expect(useMyWorkspaceRoleQueryMock).toHaveBeenCalledWith({workspaceId: 'undefined'}, {enabled: false});
    });
});
