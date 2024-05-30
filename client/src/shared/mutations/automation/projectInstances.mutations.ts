import {
    EnableProjectInstanceRequest,
    ProjectInstanceApi,
    ProjectInstanceModel,
} from '@/shared/middleware/automation/configuration';
import {useMutation} from '@tanstack/react-query';

interface CreateProjectInstanceMutationProps {
    onSuccess?: (result: ProjectInstanceModel, variables: ProjectInstanceModel) => void;
    onError?: (error: Error, variables: ProjectInstanceModel) => void;
}

export const useCreateProjectInstanceMutation = (mutationProps?: CreateProjectInstanceMutationProps) =>
    useMutation({
        mutationFn: (projectInstanceModel: ProjectInstanceModel) => {
            return new ProjectInstanceApi().createProjectInstance({
                projectInstanceModel,
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
    onSuccess?: (result: ProjectInstanceModel, variables: ProjectInstanceModel) => void;
    onError?: (error: Error, variables: ProjectInstanceModel) => void;
}

export const useUpdateProjectInstanceMutation = (mutationProps?: UpdateProjectInstanceMutationProps) =>
    useMutation({
        mutationFn: (projectInstanceModel: ProjectInstanceModel) => {
            return new ProjectInstanceApi().updateProjectInstance({
                id: projectInstanceModel.id!,
                projectInstanceModel,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
