import {GitConfigurationApi, UpdateGitConfigurationRequest} from '@/ee/shared/middleware/platform/configuration';
import {useMutation} from '@tanstack/react-query';

interface UpdateGitConfigurationMutationProps {
    onError?: (error: Error, variables: UpdateGitConfigurationRequest) => void;
    onSuccess?: (result: void, variables: UpdateGitConfigurationRequest) => void;
}

export const useUpdateWorkspaceGitConfigurationMutation = (mutationProps?: UpdateGitConfigurationMutationProps) =>
    useMutation<void, Error, UpdateGitConfigurationRequest>({
        mutationFn: (requestParameters: UpdateGitConfigurationRequest) => {
            return new GitConfigurationApi().updateGitConfiguration(requestParameters);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
