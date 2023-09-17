import {useMutation} from '@tanstack/react-query';
import {
    CreateProjectWorkflowRequest,
    EnableProjectInstanceRequest,
    EnableProjectInstanceWorkflowRequest,
    ProjectApi,
    ProjectInstanceApi,
    ProjectInstanceModel,
    ProjectInstanceTagApi,
    ProjectModel,
    ProjectTagApi,
    UpdateProjectInstanceTagsRequest,
    UpdateProjectTagsRequest,
    WorkflowModel,
} from 'middleware/helios/configuration';

type CreateProjectMutationProps = {
    onSuccess?: (result: ProjectModel, variables: ProjectModel) => void;
    onError?: (error: object, variables: ProjectModel) => void;
};

export const useCreateProjectMutation = (
    mutationProps?: CreateProjectMutationProps
) =>
    useMutation({
        mutationFn: (projectModel: ProjectModel) => {
            return new ProjectApi().createProject({
                projectModel,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

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

type CreateProjectWorkflowRequestMutationProps = {
    onSuccess?: (
        result: WorkflowModel,
        variables: CreateProjectWorkflowRequest
    ) => void;
    onError?: (error: object, variables: CreateProjectWorkflowRequest) => void;
};

export const useCreateProjectWorkflowRequestMutation = (
    mutationProps?: CreateProjectWorkflowRequestMutationProps
) => {
    return useMutation({
        mutationFn: async (request: CreateProjectWorkflowRequest) => {
            return new ProjectApi().createProjectWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
};

type DeleteProjectMutationProps = {
    onSuccess?: (result: void, variables: number) => void;
    onError?: (error: object, variables: number) => void;
};

export const useDeleteProjectMutation = (
    mutationProps?: DeleteProjectMutationProps
) =>
    useMutation({
        mutationFn: (id: number) => {
            return new ProjectApi().deleteProject({id: id});
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

type DuplicateProjectMutationProps = {
    onSuccess?: (result: ProjectModel, variables: number) => void;
    onError?: (error: object, variables: number) => void;
};

export const useDuplicateProjectMutation = (
    mutationProps?: DuplicateProjectMutationProps
) =>
    useMutation({
        mutationFn: (id: number) => {
            return new ProjectApi().duplicateProject({
                id: id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

type UpdateProjectMutationProps = {
    onSuccess?: (result: ProjectModel, variables: ProjectModel) => void;
    onError?: (error: object, variables: ProjectModel) => void;
};

export const useUpdateProjectMutation = (
    mutationProps?: UpdateProjectMutationProps
) =>
    useMutation({
        mutationFn: (projectModel: ProjectModel) => {
            return new ProjectApi().updateProject({
                id: projectModel.id!,
                projectModel,
            });
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

type UpdateProjectTagsMutationProps = {
    onSuccess?: (result: void, variables: UpdateProjectTagsRequest) => void;
    onError?: (error: object, variables: UpdateProjectTagsRequest) => void;
};

export const useUpdateProjectTagsMutation = (
    mutationProps?: UpdateProjectTagsMutationProps
) =>
    useMutation({
        mutationFn: (request: UpdateProjectTagsRequest) => {
            return new ProjectTagApi().updateProjectTags(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

type UpdateProjectInstanceTagsMutationProps = {
    onSuccess?: (
        result: void,
        variables: UpdateProjectInstanceTagsRequest
    ) => void;
    onError?: (
        error: object,
        variables: UpdateProjectInstanceTagsRequest
    ) => void;
};

export const useUpdateProjectInstanceTagsMutation = (
    mutationProps?: UpdateProjectInstanceTagsMutationProps
) =>
    useMutation({
        mutationFn: (request: UpdateProjectTagsRequest) => {
            return new ProjectInstanceTagApi().updateProjectInstanceTags(
                request
            );
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

type EnableProjectInstanceWorkflowMutationProps = {
    onSuccess?: (
        result: void,
        variables: EnableProjectInstanceWorkflowRequest
    ) => void;
    onError?: (
        error: object,
        variables: EnableProjectInstanceWorkflowRequest
    ) => void;
};

export const useEnableProjectInstanceWorkflowMutation = (
    mutationProps: EnableProjectInstanceWorkflowMutationProps
) =>
    useMutation({
        mutationFn: (request: EnableProjectInstanceWorkflowRequest) => {
            return new ProjectInstanceApi().enableProjectInstanceWorkflow(
                request
            );
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
