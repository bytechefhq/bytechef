import {useMutation} from '@tanstack/react-query';
import {
    CreateProjectWorkflowRequest,
    DeleteProjectRequest,
    ProjectModel,
    ProjectsApi,
    UpdateProjectTagsRequest,
} from 'middleware/project';

type CreateProjectMutationProps = {
    onSuccess?: (result: ProjectModel, variables: ProjectModel) => void;
    onError?: (error: object, variables: ProjectModel) => void;
};

export const useCreateProjectMutation = (
    mutationProps?: CreateProjectMutationProps
) =>
    useMutation({
        mutationFn: (project: ProjectModel) => {
            return new ProjectsApi().createProject({
                projectModel: project,
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
        mutationFn: (project: ProjectModel) => {
            return new ProjectsApi().updateProject({
                projectModel: project,
                id: project.id!,
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

type DeleteProjectMutationProps = {
    onSuccess?: (result: void, variables: DeleteProjectRequest) => void;
    onError?: (error: object, variables: DeleteProjectRequest) => void;
};

export const useDeleteProjectMutation = (
    mutationProps?: DeleteProjectMutationProps
) =>
    useMutation({
        mutationFn: (request: DeleteProjectRequest) => {
            return new ProjectsApi().deleteProject(request);
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });

type CreateProjectWorkflowRequestMutationProps = {
    onSuccess?: (
        result: ProjectModel,
        variables: CreateProjectWorkflowRequest
    ) => void;
    onError?: (error: object, variables: CreateProjectWorkflowRequest) => void;
};

export const useCreateProjectWorkflowRequestMutation = (
    mutationProps?: CreateProjectWorkflowRequestMutationProps
) =>
    useMutation({
        mutationFn: (request: CreateProjectWorkflowRequest) => {
            return new ProjectsApi().createProjectWorkflow(request);
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });
