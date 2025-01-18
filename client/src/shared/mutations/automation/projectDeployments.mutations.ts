import {
    EnableProjectDeploymentRequest,
    ProjectDeployment,
    ProjectDeploymentApi,
} from '@/shared/middleware/automation/configuration';
import {useMutation} from '@tanstack/react-query';

interface CreateProjectDeploymentMutationProps {
    onSuccess?: (result: number, variables: ProjectDeployment) => void;
    onError?: (error: Error, variables: ProjectDeployment) => void;
}

export const useCreateProjectDeploymentMutation = (mutationProps?: CreateProjectDeploymentMutationProps) =>
    useMutation({
        mutationFn: (projectDeployment: ProjectDeployment) => {
            return new ProjectDeploymentApi().createProjectDeployment({
                projectDeployment,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DeleteProjectDeploymentMutationProps {
    onSuccess?: (result: void, variables: number) => void;
    onError?: (error: Error, variables: number) => void;
}

export const useDeleteProjectDeploymentMutation = (mutationProps?: DeleteProjectDeploymentMutationProps) =>
    useMutation({
        mutationFn: (id: number) => {
            return new ProjectDeploymentApi().deleteProjectDeployment({
                id: id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface EnableProjectDeploymentMutationProps {
    onSuccess?: (result: void, variables: EnableProjectDeploymentRequest) => void;
    onError?: (error: Error, variables: EnableProjectDeploymentRequest) => void;
}

export const useEnableProjectDeploymentMutation = (mutationProps: EnableProjectDeploymentMutationProps) =>
    useMutation({
        mutationFn: (request: EnableProjectDeploymentRequest) => {
            return new ProjectDeploymentApi().enableProjectDeployment(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateProjectDeploymentMutationProps {
    onSuccess?: (result: void, variables: ProjectDeployment) => void;
    onError?: (error: Error, variables: ProjectDeployment) => void;
}

export const useUpdateProjectDeploymentMutation = (mutationProps?: UpdateProjectDeploymentMutationProps) =>
    useMutation({
        mutationFn: (projectDeployment: ProjectDeployment) => {
            return new ProjectDeploymentApi().updateProjectDeployment({
                id: projectDeployment.id!,
                projectDeployment,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
