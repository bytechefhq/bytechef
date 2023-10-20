import {QueryClient, useMutation} from '@tanstack/react-query';
import {
    CreateProjectWorkflowRequest,
    DeleteProjectRequest,
    ProjectModel,
    ProjectsApi,
    UpdateProjectTagsRequest,
    WorkflowModel,
} from 'middleware/project';
import {ProjectKeys} from 'queries/projects.queries';

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

export interface IProjectWithWorkflows extends ProjectModel {
    workflows: WorkflowModel[];
}

type CreateProjectWorkflowRequestMutationProps = {
    onSuccess?: (
        result: IProjectWithWorkflows,
        variables: CreateProjectWorkflowRequest
    ) => void;
    onError?: (error: object, variables: CreateProjectWorkflowRequest) => void;
};

export const useCreateProjectWorkflowRequestMutation = (
    mutationProps?: CreateProjectWorkflowRequestMutationProps
) => {
    const queryClient = new QueryClient();

    return useMutation({
        mutationFn: async (request: CreateProjectWorkflowRequest) => {
            const project = await new ProjectsApi().createProjectWorkflow(
                request
            );

            const workflows = await new ProjectsApi().getProjectWorkflows({
                id: project.id as number,
            });

            queryClient.setQueryData(
                [ProjectKeys.projectWorkflows, {projectId: project.id}],
                workflows
            );

            return {...project, workflows};
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });
};

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

type UpdateProjectMutationProps = {
    onSuccess?: (result: ProjectModel, variables: ProjectModel) => void;
    onError?: (error: object, variables: ProjectModel) => void;
};

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
