import {
    DeleteProjectWorkflowRequest,
    DuplicateWorkflowRequest,
    WorkflowApi,
    WorkflowModel,
} from '@/middleware/helios/configuration';
import {UpdateWorkflowRequest, WorkflowApi as CoreWorkflowApi} from '@/middleware/hermes/configuration';
import {useMutation} from '@tanstack/react-query';

type DeleteWorkflowMutationProps = {
    onSuccess?: (result: void, variables: DeleteProjectWorkflowRequest) => void;
    onError?: (error: Error, variables: DeleteProjectWorkflowRequest) => void;
};

export const useDeleteWorkflowMutation = (mutationProps?: DeleteWorkflowMutationProps) =>
    useMutation({
        mutationFn: (request: DeleteProjectWorkflowRequest) => {
            return new WorkflowApi().deleteProjectWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

type DuplicateWorkflowMutationProps = {
    onSuccess?: (result: WorkflowModel, variables: DuplicateWorkflowRequest) => void;
    onError?: (error: Error, variables: DuplicateWorkflowRequest) => void;
};

export const useDuplicateWorkflowMutation = (mutationProps?: DuplicateWorkflowMutationProps) =>
    useMutation({
        mutationFn: (request: DuplicateWorkflowRequest) => {
            return new WorkflowApi().duplicateWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

type UpdateWorkflowMutationProps = {
    onSuccess?: (result: WorkflowModel, variables: UpdateWorkflowRequest) => void;
    onError?: (error: Error, variables: UpdateWorkflowRequest) => void;
};

export const useUpdateWorkflowMutation = (mutationProps?: UpdateWorkflowMutationProps) =>
    useMutation({
        mutationFn: (request: UpdateWorkflowRequest) => {
            console.log(request);
            return new CoreWorkflowApi().updateWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
