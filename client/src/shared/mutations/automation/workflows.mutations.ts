import {
    CreateProjectWorkflowRequest,
    DeleteWorkflowRequest,
    DuplicateWorkflowRequest,
    UpdateWorkflowRequest,
    Workflow,
    WorkflowApi,
} from '@/shared/middleware/automation/configuration';
import {useMutation} from '@tanstack/react-query';

interface CreateProjectWorkflowMutationProps {
    onSuccess?: (result: Workflow, variables: CreateProjectWorkflowRequest) => void;
    onError?: (error: Error, variables: CreateProjectWorkflowRequest) => void;
}

export const useCreateProjectWorkflowMutation = (mutationProps?: CreateProjectWorkflowMutationProps) => {
    return useMutation({
        mutationFn: (request: CreateProjectWorkflowRequest) => {
            return new WorkflowApi().createProjectWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
};

interface DeleteWorkflowMutationProps {
    onSuccess?: (result: void, variables: DeleteWorkflowRequest) => void;
    onError?: (error: Error, variables: DeleteWorkflowRequest) => void;
}

export const useDeleteWorkflowMutation = (mutationProps?: DeleteWorkflowMutationProps) =>
    useMutation({
        mutationFn: (request: DeleteWorkflowRequest) => {
            return new WorkflowApi().deleteWorkflow(request);
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
    onSuccess?: (result: Workflow, variables: UpdateWorkflowRequest) => void;
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
