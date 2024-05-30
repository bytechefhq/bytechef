import {WorkspaceApi, WorkspaceModel} from '@/shared/middleware/automation/configuration';
import {useMutation} from '@tanstack/react-query';

interface CreateWorkspaceMutationProps {
    onError?: (error: Error, variables: WorkspaceModel) => void;
    onSuccess?: (result: WorkspaceModel, variables: WorkspaceModel) => void;
}

export const useCreateWorkspaceMutation = (mutationProps?: CreateWorkspaceMutationProps) =>
    useMutation<WorkspaceModel, Error, WorkspaceModel>({
        mutationFn: (workspaceModel: WorkspaceModel) => {
            return new WorkspaceApi().createWorkspace({
                workspaceModel,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DeleteWorkspaceMutationProps {
    onError?: (error: Error, variables: number) => void;
    onSuccess?: (result: void, variables: number) => void;
}

export const useDeleteWorkspaceMutation = (mutationProps?: DeleteWorkspaceMutationProps) =>
    useMutation<void, Error, number>({
        mutationFn: (id: number) => {
            return new WorkspaceApi().deleteWorkspace({
                id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateWorkspaceMutationProps {
    onError?: (error: Error, variables: WorkspaceModel) => void;
    onSuccess?: (result: WorkspaceModel, variables: WorkspaceModel) => void;
}

export const useUpdateWorkspaceMutation = (mutationProps?: UpdateWorkspaceMutationProps) =>
    useMutation<WorkspaceModel, Error, WorkspaceModel>({
        mutationFn: (workspaceModel: WorkspaceModel) => {
            return new WorkspaceApi().updateWorkspace({
                id: workspaceModel.id!,
                workspaceModel,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
