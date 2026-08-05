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
import {useQueryClient} from '@tanstack/react-query';

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
