import {
    usePullProjectFromGitMutation,
    useUpdateProjectGitConfigurationMutation,
} from '@/ee/shared/mutations/automation/projectGit.mutations';
import {
    ProjectGitConfigurationKeys,
    useGetProjectGitConfigurationQuery,
    useGetWorkspaceProjectGitConfigurationsQuery,
} from '@/ee/shared/mutations/automation/projectGit.queries';
import {registerProjectGitApi} from '@/shared/edition/project-git/projectGitApi';
import {registerVariablesApi} from '@/shared/edition/variables/variablesApi';
import {useEmbeddedVariablesQuery, useWorkspaceVariablesQuery} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

registerProjectGitApi({
    useProjectGitConfigurationQuery: (projectId) => useGetProjectGitConfigurationQuery(projectId),
    usePullProjectFromGitMutation: (callbacks) => usePullProjectFromGitMutation(callbacks),
    useUpdateProjectGitConfigurationMutation: (callbacks) => {
        const queryClient = useQueryClient();

        const mutation = useUpdateProjectGitConfigurationMutation({
            onSuccess: (result, variables) => {
                void queryClient.invalidateQueries({
                    queryKey: ProjectGitConfigurationKeys.projectGitConfiguration(variables.id!),
                });
                void queryClient.invalidateQueries({
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

registerVariablesApi({
    useWorkflowVariablesQuery: (scope, environmentId) => {
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
