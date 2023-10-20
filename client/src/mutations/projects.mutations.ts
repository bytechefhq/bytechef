import {useMutation} from '@tanstack/react-query';
import {
    CreateProjectWorkflowRequest,
    ProjectInstanceModel,
    ProjectInstancesApi,
    ProjectModel,
    ProjectsApi,
    UpdateProjectInstanceTagsRequest,
    UpdateProjectTagsRequest,
    WorkflowModel,
} from 'middleware/project';

type CreateProjectMutationProps = {
    onSuccess?: (result: ProjectModel, variables: ProjectModel) => void;
    onError?: (error: object, variables: ProjectModel) => void;
};

export const useCreateProjectMutation = (
    mutationProps?: CreateProjectMutationProps
) =>
    useMutation({
        mutationFn: (projectModel: ProjectModel) => {
            return new ProjectsApi().createProject({
                projectModel,
            });
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
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
            return new ProjectInstancesApi().createProjectInstance({
                projectInstanceModel,
            });
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
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
            return new ProjectsApi().createProjectWorkflow(request);
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
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
            return new ProjectsApi().deleteProject({id: id});
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
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
            return new ProjectInstancesApi().deleteProjectInstance({id: id});
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
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
            return new ProjectsApi().duplicateProject({
                id: id,
            });
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
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
            return new ProjectsApi().updateProject({
                projectModel,
                id: projectModel.id!,
            });
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
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
            return new ProjectInstancesApi().updateProjectInstance({
                projectInstanceModel,
                id: projectInstanceModel.id!,
            });
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
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
            return new ProjectsApi().updateProjectTags(request);
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
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
            return new ProjectInstancesApi().updateProjectInstanceTags(request);
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });
