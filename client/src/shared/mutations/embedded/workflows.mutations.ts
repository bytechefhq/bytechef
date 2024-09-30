import {
    CreateIntegrationWorkflowRequest,
    DeleteWorkflowRequest,
    IntegrationApi,
    UpdateWorkflowRequest,
    Workflow,
    WorkflowApi,
} from '@/shared/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface CreateIntegrationWorkflowMutationProps {
    onSuccess?: (result: Workflow, variables: CreateIntegrationWorkflowRequest) => void;
    onError?: (error: Error, variables: CreateIntegrationWorkflowRequest) => void;
}

export const useCreateIntegrationWorkflowMutation = (mutationProps?: CreateIntegrationWorkflowMutationProps) =>
    useMutation({
        mutationFn: (request: CreateIntegrationWorkflowRequest) => {
            return new IntegrationApi().createIntegrationWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

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
