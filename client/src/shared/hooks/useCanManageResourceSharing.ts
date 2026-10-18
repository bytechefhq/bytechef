import {useIsResourceOwnerOrTenantAdmin} from '@/shared/hooks/useIsResourceOwnerOrTenantAdmin';
import {useVisibilityFeatureEnabled} from '@/shared/hooks/useVisibilityFeatureEnabled';
import {useMyWorkspaceRoleQuery} from '@/shared/middleware/graphql';

export function useCanManageResourceSharing(createdBy?: string | null): boolean {
    const {enabled, workspaceId} = useVisibilityFeatureEnabled();
    const isResourceOwnerOrTenantAdmin = useIsResourceOwnerOrTenantAdmin(createdBy);

    const {data: myWorkspaceRoleData} = useMyWorkspaceRoleQuery(
        {workspaceId: String(workspaceId)},
        {enabled: enabled && !isResourceOwnerOrTenantAdmin}
    );

    return isResourceOwnerOrTenantAdmin || (enabled && myWorkspaceRoleData?.myWorkspaceRole === 'ADMIN');
}
