import {Project, ProjectApi, PublishProjectOperationRequest} from '@/shared/middleware/automation/configuration';
import {useMutation} from '@tanstack/react-query';

interface CreateProjectMutationProps {
    onSuccess?: (result: number, variables: Project) => void;
    onError?: (error: Error, variables: Project) => void;
}

export const useCreateProjectMutation = (mutationProps?: CreateProjectMutationProps) =>
    useMutation({
        mutationFn: (project: Project) => {
            return new ProjectApi().createProject({
                project,
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
    onSuccess?: (result: Project, variables: number) => void;
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
    onSuccess?: (result: void, variables: PublishProjectOperationRequest) => void;
    onError?: (error: Error, variables: PublishProjectOperationRequest) => void;
}

export const usePublishProjectMutation = (mutationProps?: PublishProjectMutationProps) =>
    useMutation({
        mutationFn: (request: PublishProjectOperationRequest) => {
            return new ProjectApi().publishProject(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateProjectMutationProps {
    onSuccess?: (result: void, variables: Project) => void;
    onError?: (error: Error, variables: Project) => void;
}

export const useUpdateProjectMutation = (mutationProps?: UpdateProjectMutationProps) =>
    useMutation({
        mutationFn: (project: Project) => {
            return new ProjectApi().updateProject({
                id: project.id!,
                project,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
