import {
    EnableProjectInstanceRequest,
    ProjectInstance,
    ProjectInstanceApi,
} from '@/shared/middleware/automation/configuration';
import {useMutation} from '@tanstack/react-query';

interface CreateProjectInstanceMutationProps {
    onSuccess?: (result: number, variables: ProjectInstance) => void;
    onError?: (error: Error, variables: ProjectInstance) => void;
}

export const useCreateProjectInstanceMutation = (mutationProps?: CreateProjectInstanceMutationProps) =>
    useMutation({
        mutationFn: (projectInstance: ProjectInstance) => {
            return new ProjectInstanceApi().createProjectInstance({
                projectInstance,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DeleteProjectInstanceMutationProps {
    onSuccess?: (result: void, variables: number) => void;
    onError?: (error: Error, variables: number) => void;
}

export const useDeleteProjectInstanceMutation = (mutationProps?: DeleteProjectInstanceMutationProps) =>
    useMutation({
        mutationFn: (id: number) => {
            return new ProjectInstanceApi().deleteProjectInstance({
                id: id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface EnableProjectInstanceMutationProps {
    onSuccess?: (result: void, variables: EnableProjectInstanceRequest) => void;
    onError?: (error: Error, variables: EnableProjectInstanceRequest) => void;
}

export const useEnableProjectInstanceMutation = (mutationProps: EnableProjectInstanceMutationProps) =>
    useMutation({
        mutationFn: (request: EnableProjectInstanceRequest) => {
            return new ProjectInstanceApi().enableProjectInstance(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateProjectInstanceMutationProps {
    onSuccess?: (result: void, variables: ProjectInstance) => void;
    onError?: (error: Error, variables: ProjectInstance) => void;
}

export const useUpdateProjectInstanceMutation = (mutationProps?: UpdateProjectInstanceMutationProps) =>
    useMutation({
        mutationFn: (projectInstance: ProjectInstance) => {
            return new ProjectInstanceApi().updateProjectInstance({
                id: projectInstance.id!,
                projectInstance,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
