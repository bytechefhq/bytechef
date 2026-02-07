import {
    DeleteWorkflowTestConfigurationConnectionOperationRequest,
    SaveWorkflowTestConfigurationInputsOperationRequest,
    SaveWorkflowTestConfigurationRequest,
    WorkflowTestConfiguration,
    WorkflowTestConfigurationApi,
} from '@/shared/middleware/platform/configuration';
import {useMutation} from '@tanstack/react-query';

interface DeleteWorkflowTestConfigurationConnectionRequestProps {
    onSuccess?: (result: void, variables: DeleteWorkflowTestConfigurationConnectionOperationRequest) => void;
    onError?: (error: Error, variables: DeleteWorkflowTestConfigurationConnectionOperationRequest) => void;
}

export const useDeleteWorkflowTestConfigurationConnectionMutation = (
    mutationProps?: DeleteWorkflowTestConfigurationConnectionRequestProps
) =>
    useMutation({
        mutationFn: (request: DeleteWorkflowTestConfigurationConnectionOperationRequest) => {
            return new WorkflowTestConfigurationApi().deleteWorkflowTestConfigurationConnection(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface SaveWorkflowTestConfigurationMutationProps {
    onSuccess?: (result: WorkflowTestConfiguration, variables: SaveWorkflowTestConfigurationRequest) => void;
    onError?: (error: Error, variables: SaveWorkflowTestConfigurationRequest) => void;
}

export const useSaveWorkflowTestConfigurationMutation = (mutationProps?: SaveWorkflowTestConfigurationMutationProps) =>
    useMutation({
        mutationFn: (request: SaveWorkflowTestConfigurationRequest) => {
            return new WorkflowTestConfigurationApi().saveWorkflowTestConfiguration(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface SaveWorkflowTestConfigurationInputsRequestProps {
    onSuccess?: (result: void, variables: SaveWorkflowTestConfigurationInputsOperationRequest) => void;
    onError?: (error: Error, variables: SaveWorkflowTestConfigurationInputsOperationRequest) => void;
}

export const useSaveWorkflowTestConfigurationInputsMutation = (
    mutationProps?: SaveWorkflowTestConfigurationInputsRequestProps
) =>
    useMutation({
        mutationFn: (request: SaveWorkflowTestConfigurationInputsOperationRequest) => {
            return new WorkflowTestConfigurationApi().saveWorkflowTestConfigurationInputs(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
