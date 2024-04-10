import {
    DeleteProjectWorkflowRequest,
    DuplicateWorkflowRequest,
    UpdateWorkflowRequest,
    WorkflowApi,
    WorkflowModel,
} from '@/middleware/automation/configuration';
import {
    GetWorkflowNodeOutputsRequest,
    WorkflowNodeOutputApi,
    WorkflowNodeOutputModel,
} from '@/middleware/platform/configuration';
import {useMutation} from '@tanstack/react-query';

interface DeleteWorkflowMutationProps {
    onSuccess?: (result: void, variables: DeleteProjectWorkflowRequest) => void;
    onError?: (error: Error, variables: DeleteProjectWorkflowRequest) => void;
}

export const useDeleteWorkflowMutation = (mutationProps?: DeleteWorkflowMutationProps) =>
    useMutation({
        mutationFn: (request: DeleteProjectWorkflowRequest) => {
            return new WorkflowApi().deleteProjectWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DuplicateWorkflowMutationProps {
    onSuccess?: (result: string, variables: DuplicateWorkflowRequest) => void;
    onError?: (error: Error, variables: DuplicateWorkflowRequest) => void;
}

export const useDuplicateWorkflowMutation = (mutationProps?: DuplicateWorkflowMutationProps) =>
    useMutation({
        mutationFn: (request: DuplicateWorkflowRequest) => {
            return new WorkflowApi().duplicateWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateWorkflowMutationProps {
    onSuccess?: (result: WorkflowModel, variables: UpdateWorkflowRequest) => void;
    onError?: (error: Error, variables: UpdateWorkflowRequest) => void;
}

export const useUpdateWorkflowMutation = (mutationProps?: UpdateWorkflowMutationProps) =>
    useMutation({
        mutationFn: (request: UpdateWorkflowRequest) => {
            return new WorkflowApi().updateWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateWorkflowNodeOutputsMutationProps {
    onSuccess?: (result: WorkflowNodeOutputModel[]) => void;
    onError?: (error: Error) => void;
}

export const useUpdateWorkflowNodeOutputsMutation = (mutationProps?: UpdateWorkflowNodeOutputsMutationProps) =>
    useMutation({
        mutationFn: (request: GetWorkflowNodeOutputsRequest) =>
            new WorkflowNodeOutputApi().getWorkflowNodeOutputs(request),
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
