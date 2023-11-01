import {useMutation} from '@tanstack/react-query';
import {
    CreateProjectWorkflowRequest,
    DeleteProjectWorkflowRequest,
    DuplicateWorkflowRequest,
    ProjectApi,
    ProjectModel,
    ProjectTagApi,
    PublishProjectRequest,
    UpdateProjectTagsRequest,
    UpdateWorkflowRequest,
    WorkflowApi,
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
        mutationFn: (request: CreateProjectWorkflowRequest) => {
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

type DeleteWorkflowMutationProps = {
    onSuccess?: (result: void, variables: DeleteProjectWorkflowRequest) => void;
    onError?: (error: object, variables: DeleteProjectWorkflowRequest) => void;
};

export const useDeleteWorkflowMutation = (
    mutationProps?: DeleteWorkflowMutationProps
) =>
    useMutation({
        mutationFn: (request: DeleteProjectWorkflowRequest) => {
            return new WorkflowApi().deleteProjectWorkflow(request);
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

type DuplicateWorkflowMutationProps = {
    onSuccess?: (
        result: WorkflowModel,
        variables: DuplicateWorkflowRequest
    ) => void;
    onError?: (error: object, variables: DuplicateWorkflowRequest) => void;
};

export const useDuplicateWorkflowMutation = (
    mutationProps?: DuplicateWorkflowMutationProps
) =>
    useMutation({
        mutationFn: (request: DuplicateWorkflowRequest) => {
            return new WorkflowApi().duplicateWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

type PublishProjectMutationProps = {
    onSuccess?: (
        result: ProjectModel,
        variables: PublishProjectRequest
    ) => void;
    onError?: (error: object, variables: PublishProjectRequest) => void;
};

export const usePublishProjectMutation = (
    mutationProps?: PublishProjectMutationProps
) =>
    useMutation({
        mutationFn: (request: PublishProjectRequest) => {
            return new ProjectApi().publishProject(request);
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

type UpdateWorkflowMutationProps = {
    onSuccess?: (
        result: WorkflowModel,
        variables: UpdateWorkflowRequest
    ) => void;
    onError?: (error: object, variables: UpdateWorkflowRequest) => void;
};

export const useUpdateWorkflowMutation = (
    mutationProps?: UpdateWorkflowMutationProps
) =>
    useMutation({
        mutationFn: (request: UpdateWorkflowRequest) => {
            return new WorkflowApi().updateWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
