import {
    ProjectGitApi,
    PullProjectFromGitRequest,
    UpdateProjectGitConfigurationRequest,
} from '@/ee/shared/middleware/automation/configuration';
import {useMutation} from '@tanstack/react-query';

interface PullProjectFromGitMutationProps {
    onError?: (error: Error, variables: PullProjectFromGitRequest) => void;
    onSuccess?: (result: void, variables: PullProjectFromGitRequest) => void;
}

export const usePullProjectFromGitMutation = (mutationProps?: PullProjectFromGitMutationProps) =>
    useMutation<void, Error, PullProjectFromGitRequest>({
        mutationFn: (requestParameters: PullProjectFromGitRequest) => {
            return new ProjectGitApi().pullProjectFromGit(requestParameters);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateProjectGitConfigurationMutationProps {
    onError?: (error: Error, variables: UpdateProjectGitConfigurationRequest) => void;
    onSuccess?: (result: void, variables: UpdateProjectGitConfigurationRequest) => void;
}

export const useUpdateProjectGitConfigurationMutation = (mutationProps?: UpdateProjectGitConfigurationMutationProps) =>
    useMutation<void, Error, UpdateProjectGitConfigurationRequest>({
        mutationFn: (requestParameters: UpdateProjectGitConfigurationRequest) => {
            return new ProjectGitApi().updateProjectGitConfiguration(requestParameters);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
