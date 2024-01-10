import {
    CreateWorkflowTestConfigurationRequest,
    UpdateWorkflowTestConfigurationConnectionRequest,
    UpdateWorkflowTestConfigurationInputsRequest,
    UpdateWorkflowTestConfigurationRequest,
    WorkflowTestConfigurationApi,
    WorkflowTestConfigurationModel,
} from '@/middleware/platform/workflow/test';
import {useMutation} from '@tanstack/react-query';

type CreateWorkflowTestConfigurationMutationProps = {
    onSuccess?: (result: WorkflowTestConfigurationModel, variables: CreateWorkflowTestConfigurationRequest) => void;
    onError?: (error: Error, variables: CreateWorkflowTestConfigurationRequest) => void;
};

export const useCreateWorkflowTestConfigurationMutation = (
    mutationProps?: CreateWorkflowTestConfigurationMutationProps
) =>
    useMutation({
        mutationFn: (request: CreateWorkflowTestConfigurationRequest) => {
            return new WorkflowTestConfigurationApi().createWorkflowTestConfiguration(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

type UpdateWorkflowTestConfigurationMutationProps = {
    onSuccess?: (result: WorkflowTestConfigurationModel, variables: UpdateWorkflowTestConfigurationRequest) => void;
    onError?: (error: Error, variables: UpdateWorkflowTestConfigurationRequest) => void;
};

export const useUpdateWorkflowTestConfigurationMutation = (
    mutationProps?: UpdateWorkflowTestConfigurationMutationProps
) =>
    useMutation({
        mutationFn: (request: UpdateWorkflowTestConfigurationRequest) => {
            return new WorkflowTestConfigurationApi().updateWorkflowTestConfiguration(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

type UpdateWorkflowTestConfigurationConnectionRequestProps = {
    onSuccess?: (result: void, variables: UpdateWorkflowTestConfigurationConnectionRequest) => void;
    onError?: (error: Error, variables: UpdateWorkflowTestConfigurationConnectionRequest) => void;
};

export const useUpdateWorkflowTestConfigurationConnectionMutation = (
    mutationProps?: UpdateWorkflowTestConfigurationConnectionRequestProps
) =>
    useMutation({
        mutationFn: (request: UpdateWorkflowTestConfigurationConnectionRequest) => {
            return new WorkflowTestConfigurationApi().updateWorkflowTestConfigurationConnection(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

type UpdateWorkflowTestConfigurationInputsRequestProps = {
    onSuccess?: (result: void, variables: UpdateWorkflowTestConfigurationInputsRequest) => void;
    onError?: (error: Error, variables: UpdateWorkflowTestConfigurationInputsRequest) => void;
};

export const useUpdateWorkflowTestConfigurationInputsMutation = (
    mutationProps?: UpdateWorkflowTestConfigurationInputsRequestProps
) =>
    useMutation({
        mutationFn: (request: UpdateWorkflowTestConfigurationInputsRequest) => {
            return new WorkflowTestConfigurationApi().updateWorkflowTestConfigurationInputs(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
