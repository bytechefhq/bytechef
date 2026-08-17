import useOpenAgentChat from '@/ee/shared/edition/useOpenAgentChat';
import {
    usePullProjectFromGitMutation,
    useUpdateProjectGitConfigurationMutation,
} from '@/ee/shared/mutations/automation/projectGit.mutations';
import {
    ProjectGitConfigurationKeys,
    useGetProjectGitConfigurationQuery,
    useGetWorkspaceProjectGitConfigurationsQuery,
} from '@/ee/shared/mutations/automation/projectGit.queries';
import {registerAgentChatApi} from '@/shared/edition/agent-chat/agentChatApi';
import {registerProjectGitApi} from '@/shared/edition/project-git/projectGitApi';
import {useQueryClient} from '@tanstack/react-query';

/**
 * EE-side registrations for the CE edition seams. Imported (dynamically) exactly once from main.tsx's bootstrap when
 * the application reports the EE edition — before the first render, so the hook implementations the CE surfaces pick
 * up never change identity afterwards.
 *
 * Git cache invalidation happens here rather than in the CE callers: the EE cache keys are an implementation detail
 * of this side of the seam.
 */
registerProjectGitApi({
    useProjectGitConfigurationQuery: (projectId) => useGetProjectGitConfigurationQuery(projectId),
    usePullProjectFromGitMutation: (callbacks) => usePullProjectFromGitMutation(callbacks),
    useUpdateProjectGitConfigurationMutation: (callbacks) => {
        const queryClient = useQueryClient();

        const mutation = useUpdateProjectGitConfigurationMutation({
            onSuccess: (result, variables) => {
                queryClient.invalidateQueries({
                    queryKey: ProjectGitConfigurationKeys.projectGitConfiguration(variables.id!),
                });
                queryClient.invalidateQueries({
                    queryKey: ProjectGitConfigurationKeys.projectGitConfigurations,
                });

                callbacks.onSuccess?.();
            },
        });

        return {
            mutate: (input, mutateCallbacks) => mutation.mutate(input, mutateCallbacks),
        };
    },
    useWorkspaceProjectGitConfigurationsQuery: (workspaceId, enabled) =>
        useGetWorkspaceProjectGitConfigurationsQuery(workspaceId, enabled),
});

registerAgentChatApi({
    useOpenAgentChat,
});
