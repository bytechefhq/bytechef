import {useVisibilityFeatureEnabled} from '@/shared/hooks/useVisibilityFeatureEnabled';
import {
    ResourceVisibility,
    useAiAgentGrantsQuery,
    useGrantAiAgentAccessMutation,
    useRevokeAiAgentAccessMutation,
    useSetAiAgentVisibilityMutation,
    useWorkspaceUsersQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface UseAiAgentVisibilityPropsI {
    agentId?: string;
    visibility?: string;
}

/**
 * The agent twin of {@link useProjectVisibility}, and deliberately a separate hook rather than a generalisation
 * of it: the two speak different operations (agent-keyed, so no workspaceId argument — the server reads it off
 * the agent) and invalidate different caches. What they must not differ on is the diff-not-replace rule below,
 * which is the server's shape and not a choice either hook gets to make.
 *
 * The operations behind this reach ProjectSharingFacade through AiAgentSharingFacade: an agent's reach IS its
 * hidden backing project's, and there is one record for the question.
 */
export const useAiAgentVisibility = ({agentId, visibility}: UseAiAgentVisibilityPropsI) => {
    const {enabled, workspaceId} = useVisibilityFeatureEnabled();

    const queryClient = useQueryClient();

    // Both agent caches, because the reach decides which agents the LIST shows as well as what the detail page
    // renders — the server filters getAgents on it.
    const invalidateAgents = () => {
        queryClient.invalidateQueries({queryKey: ['aiAgent']});
        queryClient.invalidateQueries({queryKey: ['aiAgents']});
        queryClient.invalidateQueries({queryKey: ['aiAgentGrants']});
    };

    const grantAiAgentAccessMutation = useGrantAiAgentAccessMutation({onSuccess: invalidateAgents});
    const revokeAiAgentAccessMutation = useRevokeAiAgentAccessMutation({onSuccess: invalidateAgents});
    const setAiAgentVisibilityMutation = useSetAiAgentVisibilityMutation({onSuccess: invalidateAgents});

    // Only a withheld agent can have a meaningful audience, so the two lookups the picker needs are skipped for
    // the workspace-visible majority.
    const isWithheld = visibility === 'PRIVATE';
    const lookupsEnabled = enabled && isWithheld && !!agentId && !!workspaceId;

    const aiAgentGrantsQuery = useAiAgentGrantsQuery({agentId: String(agentId)}, {enabled: lookupsEnabled});

    const workspaceUsersQuery = useWorkspaceUsersQuery({workspaceId: String(workspaceId)}, {enabled: lookupsEnabled});

    const grantedUserIds = (aiAgentGrantsQuery.data?.aiAgentGrants ?? []).map(Number);

    const workspaceMembers = (workspaceUsersQuery.data?.workspaceUsers ?? []).map((workspaceUser) => ({
        label: workspaceUser.user?.email ?? `User ${workspaceUser.userId}`,
        userId: Number(workspaceUser.userId),
    }));

    // Diff rather than replace: the server has no set-grants operation.
    const handleGrantedUserIdsChange = (nextUserIds: number[]) => {
        grantedUserIds
            .filter((userId) => !nextUserIds.includes(userId))
            .forEach((userId) =>
                revokeAiAgentAccessMutation.mutate({agentId: String(agentId), userId: String(userId)})
            );

        nextUserIds
            .filter((userId) => !grantedUserIds.includes(userId))
            .forEach((userId) => grantAiAgentAccessMutation.mutate({agentId: String(agentId), userId: String(userId)}));
    };

    const handleVisibilityChange = (nextVisibility: string) =>
        setAiAgentVisibilityMutation.mutate({
            agentId: String(agentId),
            // The picker speaks plain strings; the generated enum's values are exactly those strings, so this is a
            // representation cast rather than a claim about the value.
            visibility: nextVisibility as ResourceVisibility,
        });

    return {
        // workspaceId is not sent to the server — the agent knows its own — but it still gates the picker, because
        // the member list the "Specific people" state offers is workspace-scoped.
        enabled: enabled && !!agentId && !!workspaceId,
        grantedUserIds,
        onGrantedUserIdsChange: handleGrantedUserIdsChange,
        onVisibilityChange: handleVisibilityChange,
        workspaceMembers,
    };
};
