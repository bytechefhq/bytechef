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
import {registerVariablesApi} from '@/shared/edition/variables/variablesApi';
import {useEmbeddedVariablesQuery, useWorkspaceVariablesQuery} from '@/shared/middleware/graphql';
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

registerVariablesApi({
    useWorkflowVariablesQuery: (scope, environmentId) => {
        // Both hooks are always called (rules of hooks); `enabled` picks the live one.
        const workspaceQuery = useWorkspaceVariablesQuery(
            {
                environmentId: `${environmentId}`,
                workspaceId: scope?.type === 'WORKSPACE' ? `${scope.workspaceId}` : '',
            },
            {enabled: scope?.type === 'WORKSPACE'}
        );
        const embeddedQuery = useEmbeddedVariablesQuery(
            {environmentId: `${environmentId}`},
            {enabled: scope?.type === 'EMBEDDED'}
        );

        if (scope?.type === 'WORKSPACE') {
            return {data: workspaceQuery.data?.workspaceVariables};
        }

        if (scope?.type === 'EMBEDDED') {
            return {data: embeddedQuery.data?.embeddedVariables};
        }

        return {data: undefined};
    },
});
