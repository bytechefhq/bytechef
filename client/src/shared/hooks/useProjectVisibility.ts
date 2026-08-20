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
    projectId?: number;
    visibility?: string;
}

/**
 * The single owner of a project's visibility mutations and of the grant/revoke diff. Both edit surfaces — the
 * project list item's badge dropdown and the project header's Visibility dialog — consume this hook, so the two
 * cannot drift: the three mutations, their cache invalidation and the diff exist exactly once.
 */
export const useProjectVisibility = ({projectId, visibility}: UseProjectVisibilityPropsI) => {
    const {enabled, workspaceId} = useVisibilityFeatureEnabled();

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

    // Only a withheld project can have a meaningful audience, so the two lookups the picker needs are skipped
    // for the workspace-visible majority.
    const isWithheld = visibility === 'PRIVATE';
    const lookupsEnabled = enabled && isWithheld && !!projectId && !!workspaceId;

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

    // Diff rather than replace: the server has no set-grants operation.
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
            // The picker speaks plain strings; the generated enum's values are exactly those strings, so this is a
            // representation cast rather than a claim about the value.
            visibility: nextVisibility as ResourceVisibility,
            workspaceId: String(workspaceId),
        });

    return {
        enabled: enabled && !!projectId && !!workspaceId,
        grantedUserIds,
        onGrantedUserIdsChange: handleGrantedUserIdsChange,
        onVisibilityChange: handleVisibilityChange,
        workspaceMembers,
    };
};
