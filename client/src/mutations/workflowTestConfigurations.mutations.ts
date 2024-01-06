import {
    CreateWorkflowTestConfigurationRequest,
    UpdateWorkflowTestConfigurationRequest,
    WorkflowTestConfigurationApi,
    WorkflowTestConfigurationModel,
} from '@/middleware/hermes/test';
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
