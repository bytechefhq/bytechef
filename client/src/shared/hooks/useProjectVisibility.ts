import {useIsResourceOwnerOrTenantAdmin} from '@/shared/hooks/useIsResourceOwnerOrTenantAdmin';
import {useVisibilityFeatureEnabled} from '@/shared/hooks/useVisibilityFeatureEnabled';
import {
    ResourceVisibility,
    useGrantProjectAccessMutation,
    useProjectGrantsQuery,
    useRevokeProjectAccessMutation,
    useSetProjectVisibilityMutation,
    useWorkspaceUsersQuery,
} from '@/shared/middleware/graphql';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';

interface UseProjectVisibilityPropsI {
    createdBy?: string;
    projectId?: number;
    visibility?: string;
}

export const useProjectVisibility = ({createdBy, projectId, visibility}: UseProjectVisibilityPropsI) => {
    const {enabled, workspaceId} = useVisibilityFeatureEnabled();
    const canManage = useIsResourceOwnerOrTenantAdmin(createdBy);

    const queryClient = useQueryClient();

    const invalidateProjects = () => {
        queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

        if (projectId) {
            queryClient.invalidateQueries({queryKey: ProjectKeys.project(projectId)});
        }
    };

    const grantProjectAccessMutation = useGrantProjectAccessMutation({onSuccess: invalidateProjects});
    const revokeProjectAccessMutation = useRevokeProjectAccessMutation({onSuccess: invalidateProjects});
    const setProjectVisibilityMutation = useSetProjectVisibilityMutation({onSuccess: invalidateProjects});

    const isWithheld = visibility === 'PRIVATE';
    const lookupsEnabled = enabled && canManage && isWithheld && !!projectId && !!workspaceId;

    const projectGrantsQuery = useProjectGrantsQuery(
        {projectId: String(projectId), workspaceId: String(workspaceId)},
        {enabled: lookupsEnabled}
    );

    const workspaceUsersQuery = useWorkspaceUsersQuery({workspaceId: String(workspaceId)}, {enabled: lookupsEnabled});

    const grantedUserIds = (projectGrantsQuery.data?.projectGrants ?? []).map(Number);

    const workspaceMembers = (workspaceUsersQuery.data?.workspaceUsers ?? []).map((workspaceUser) => ({
        label: workspaceUser.user?.email ?? `User ${workspaceUser.userId}`,
        userId: Number(workspaceUser.userId),
    }));

    const handleGrantedUserIdsChange = (nextUserIds: number[]) => {
        const identifiers = {projectId: String(projectId), workspaceId: String(workspaceId)};

        grantedUserIds
            .filter((userId) => !nextUserIds.includes(userId))
            .forEach((userId) => revokeProjectAccessMutation.mutate({...identifiers, userId: String(userId)}));

        nextUserIds
            .filter((userId) => !grantedUserIds.includes(userId))
            .forEach((userId) => grantProjectAccessMutation.mutate({...identifiers, userId: String(userId)}));
    };

    const handleVisibilityChange = (nextVisibility: string) =>
        setProjectVisibilityMutation.mutate({
            projectId: String(projectId),
            visibility: nextVisibility as ResourceVisibility,
            workspaceId: String(workspaceId),
        });

    return {
        canManage: enabled && canManage && !!projectId && !!workspaceId,
        enabled: enabled && !!projectId && !!workspaceId,
        grantedUserIds,
        onGrantedUserIdsChange: handleGrantedUserIdsChange,
        onVisibilityChange: handleVisibilityChange,
        workspaceMembers,
    };
};
