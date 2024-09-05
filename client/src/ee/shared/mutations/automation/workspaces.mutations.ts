import {Workspace, WorkspaceApi} from '@/ee/shared/middleware/automation/configuration';
import {useMutation} from '@tanstack/react-query';

interface CreateWorkspaceMutationProps {
    onError?: (error: Error, variables: Workspace) => void;
    onSuccess?: (result: Workspace, variables: Workspace) => void;
}

export const useCreateWorkspaceMutation = (mutationProps?: CreateWorkspaceMutationProps) =>
    useMutation<Workspace, Error, Workspace>({
        mutationFn: (workspace: Workspace) => {
            return new WorkspaceApi().createWorkspace({
                workspace,
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
    onError?: (error: Error, variables: Workspace) => void;
    onSuccess?: (result: Workspace, variables: Workspace) => void;
}

export const useUpdateWorkspaceMutation = (mutationProps?: UpdateWorkspaceMutationProps) =>
    useMutation<Workspace, Error, Workspace>({
        mutationFn: (workspace: Workspace) => {
            return new WorkspaceApi().updateWorkspace({
                id: workspace.id!,
                workspace,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
