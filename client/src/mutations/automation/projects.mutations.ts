import {useMutation} from '@tanstack/react-query';
import {ProjectApi, ProjectModel, PublishProjectRequest} from 'middleware/automation/configuration';

interface CreateProjectMutationProps {
    onSuccess?: (result: ProjectModel, variables: ProjectModel) => void;
    onError?: (error: Error, variables: ProjectModel) => void;
}

export const useCreateProjectMutation = (mutationProps?: CreateProjectMutationProps) =>
    useMutation({
        mutationFn: (projectModel: ProjectModel) => {
            return new ProjectApi().createProject({
                projectModel,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DeleteProjectMutationProps {
    onSuccess?: (result: void, variables: number) => void;
    onError?: (error: Error, variables: number) => void;
}

export const useDeleteProjectMutation = (mutationProps?: DeleteProjectMutationProps) =>
    useMutation({
        mutationFn: (id: number) => {
            return new ProjectApi().deleteProject({id: id});
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DuplicateProjectMutationProps {
    onSuccess?: (result: ProjectModel, variables: number) => void;
    onError?: (error: Error, variables: number) => void;
}

export const useDuplicateProjectMutation = (mutationProps?: DuplicateProjectMutationProps) =>
    useMutation({
        mutationFn: (id: number) => {
            return new ProjectApi().duplicateProject({
                id: id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface PublishProjectMutationProps {
    onSuccess?: (result: ProjectModel, variables: PublishProjectRequest) => void;
    onError?: (error: Error, variables: PublishProjectRequest) => void;
}

export const usePublishProjectMutation = (mutationProps?: PublishProjectMutationProps) =>
    useMutation({
        mutationFn: (request: PublishProjectRequest) => {
            return new ProjectApi().publishProject(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateProjectMutationProps {
    onSuccess?: (result: ProjectModel, variables: ProjectModel) => void;
    onError?: (error: Error, variables: ProjectModel) => void;
}

export const useUpdateProjectMutation = (mutationProps?: UpdateProjectMutationProps) =>
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
