import {
    SaveWorkflowTestConfigurationConnectionRequest,
    SaveWorkflowTestConfigurationInputsRequest,
    SaveWorkflowTestConfigurationRequest,
    WorkflowTestConfigurationApi,
    WorkflowTestConfigurationModel,
} from '@/middleware/platform/configuration/';
import {useMutation} from '@tanstack/react-query';

type SaveWorkflowTestConfigurationMutationProps = {
    onSuccess?: (result: WorkflowTestConfigurationModel, variables: SaveWorkflowTestConfigurationRequest) => void;
    onError?: (error: Error, variables: SaveWorkflowTestConfigurationRequest) => void;
};

export const useSaveWorkflowTestConfigurationMutation = (mutationProps?: SaveWorkflowTestConfigurationMutationProps) =>
    useMutation({
        mutationFn: (request: SaveWorkflowTestConfigurationRequest) => {
            return new WorkflowTestConfigurationApi().saveWorkflowTestConfiguration(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

type SaveWorkflowTestConfigurationConnectionRequestProps = {
    onSuccess?: (result: void, variables: SaveWorkflowTestConfigurationConnectionRequest) => void;
    onError?: (error: Error, variables: SaveWorkflowTestConfigurationConnectionRequest) => void;
};

export const useSaveWorkflowTestConfigurationConnectionMutation = (
    mutationProps?: SaveWorkflowTestConfigurationConnectionRequestProps
) =>
    useMutation({
        mutationFn: (request: SaveWorkflowTestConfigurationConnectionRequest) => {
            return new WorkflowTestConfigurationApi().saveWorkflowTestConfigurationConnection(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

type SaveWorkflowTestConfigurationInputsRequestProps = {
    onSuccess?: (result: void, variables: SaveWorkflowTestConfigurationInputsRequest) => void;
    onError?: (error: Error, variables: SaveWorkflowTestConfigurationInputsRequest) => void;
};

export const useSaveWorkflowTestConfigurationInputsMutation = (
    mutationProps?: SaveWorkflowTestConfigurationInputsRequestProps
) =>
    useMutation({
        mutationFn: (request: SaveWorkflowTestConfigurationInputsRequest) => {
            return new WorkflowTestConfigurationApi().saveWorkflowTestConfigurationInputs(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
