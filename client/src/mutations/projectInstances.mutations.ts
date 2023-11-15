import {
    EnableProjectInstanceRequest,
    ProjectInstanceApi,
    ProjectInstanceModel,
} from '@/middleware/helios/configuration';
import {useMutation} from '@tanstack/react-query';

type CreateProjectInstanceMutationProps = {
    onSuccess?: (
        result: ProjectInstanceModel,
        variables: ProjectInstanceModel
    ) => void;
    onError?: (error: object, variables: ProjectInstanceModel) => void;
};

export const useCreateProjectInstanceMutation = (
    mutationProps?: CreateProjectInstanceMutationProps
) =>
    useMutation({
        mutationFn: (projectInstanceModel: ProjectInstanceModel) => {
            return new ProjectInstanceApi().createProjectInstance({
                projectInstanceModel,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

type DeleteProjectInstanceMutationProps = {
    onSuccess?: (result: void, variables: number) => void;
    onError?: (error: object, variables: number) => void;
};

export const useDeleteProjectInstanceMutation = (
    mutationProps?: DeleteProjectInstanceMutationProps
) =>
    useMutation({
        mutationFn: (id: number) => {
            return new ProjectInstanceApi().deleteProjectInstance({
                id: id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

type EnableProjectInstanceMutationProps = {
    onSuccess?: (result: void, variables: EnableProjectInstanceRequest) => void;
    onError?: (error: object, variables: EnableProjectInstanceRequest) => void;
};

export const useEnableProjectInstanceMutation = (
    mutationProps: EnableProjectInstanceMutationProps
) =>
    useMutation({
        mutationFn: (request: EnableProjectInstanceRequest) => {
            return new ProjectInstanceApi().enableProjectInstance(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

type UpdateProjectInstanceMutationProps = {
    onSuccess?: (
        result: ProjectInstanceModel,
        variables: ProjectInstanceModel
    ) => void;
    onError?: (error: object, variables: ProjectInstanceModel) => void;
};

export const useUpdateProjectInstanceMutation = (
    mutationProps?: UpdateProjectInstanceMutationProps
) =>
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
