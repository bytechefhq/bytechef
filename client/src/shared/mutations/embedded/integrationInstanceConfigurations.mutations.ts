import {
    EnableIntegrationInstanceConfigurationRequest,
    EnableIntegrationInstanceConfigurationWorkflowRequest,
    IntegrationInstanceConfiguration,
    IntegrationInstanceConfigurationApi,
    IntegrationInstanceConfigurationTagApi,
    IntegrationInstanceConfigurationWorkflow,
    UpdateIntegrationInstanceConfigurationTagsRequest,
    UpdateIntegrationTagsRequest,
} from '@/shared/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface CreateIntegrationInstanceMutationProps {
    onSuccess?: (result: number, variables: IntegrationInstanceConfiguration) => void;
    onError?: (error: Error, variables: IntegrationInstanceConfiguration) => void;
}

export const useCreateIntegrationInstanceConfigurationMutation = (
    mutationProps?: CreateIntegrationInstanceMutationProps
) =>
    useMutation({
        mutationFn: (integrationInstanceConfiguration: IntegrationInstanceConfiguration) => {
            return new IntegrationInstanceConfigurationApi().createIntegrationInstanceConfiguration({
                integrationInstanceConfiguration,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DeleteIntegrationInstanceConfigurationMutationProps {
    onSuccess?: (result: void, variables: number) => void;
    onError?: (error: Error, variables: number) => void;
}

export const useDeleteIntegrationInstanceConfigurationMutation = (
    mutationProps?: DeleteIntegrationInstanceConfigurationMutationProps
) =>
    useMutation({
        mutationFn: (id: number) => {
            return new IntegrationInstanceConfigurationApi().deleteIntegrationInstanceConfiguration({
                id: id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface EnableIntegrationInstanceConfigurationMutationProps {
    onSuccess?: (result: void, variables: EnableIntegrationInstanceConfigurationRequest) => void;
    onError?: (error: Error, variables: EnableIntegrationInstanceConfigurationRequest) => void;
}

export const useEnableIntegrationInstanceConfigurationMutation = (
    mutationProps: EnableIntegrationInstanceConfigurationMutationProps
) =>
    useMutation({
        mutationFn: (request: EnableIntegrationInstanceConfigurationRequest) => {
            return new IntegrationInstanceConfigurationApi().enableIntegrationInstanceConfiguration(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface EnableIntegrationInstanceConfigurationWorkflowMutationProps {
    onSuccess?: (result: void, variables: EnableIntegrationInstanceConfigurationWorkflowRequest) => void;
    onError?: (error: Error, variables: EnableIntegrationInstanceConfigurationWorkflowRequest) => void;
}

export const useEnableIntegrationInstanceConfigurationWorkflowMutation = (
    mutationProps: EnableIntegrationInstanceConfigurationWorkflowMutationProps
) =>
    useMutation({
        mutationFn: (request: EnableIntegrationInstanceConfigurationWorkflowRequest) => {
            return new IntegrationInstanceConfigurationApi().enableIntegrationInstanceConfigurationWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateIntegrationInstanceConfigurationMutationProps {
    onSuccess?: (result: void, variables: IntegrationInstanceConfiguration) => void;
    onError?: (error: Error, variables: IntegrationInstanceConfiguration) => void;
}

export const useUpdateIntegrationInstanceConfigurationMutation = (
    mutationProps?: UpdateIntegrationInstanceConfigurationMutationProps
) =>
    useMutation({
        mutationFn: (integrationInstanceConfiguration: IntegrationInstanceConfiguration) => {
            return new IntegrationInstanceConfigurationApi().updateIntegrationInstanceConfiguration({
                id: integrationInstanceConfiguration.id!,
                integrationInstanceConfiguration,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

export const useUpdateIntegrationInstanceConfigurationWorkflowMutation = (
    mutationProps?: UpdateIntegrationInstanceConfigurationWorkflowMutationProps
) =>
    useMutation({
        mutationFn: (integrationInstanceConfigurationWorkflow: IntegrationInstanceConfigurationWorkflow) => {
            return new IntegrationInstanceConfigurationApi().updateIntegrationInstanceConfigurationWorkflow({
                id: integrationInstanceConfigurationWorkflow.integrationInstanceConfigurationId!,
                integrationInstanceConfigurationWorkflow: integrationInstanceConfigurationWorkflow,
                workflowId: integrationInstanceConfigurationWorkflow.id!,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateIntegrationInstanceConfigurationWorkflowMutationProps {
    onSuccess?: (result: void, variables: IntegrationInstanceConfigurationWorkflow) => void;
    onError?: (error: Error, variables: IntegrationInstanceConfigurationWorkflow) => void;
}

interface UpdateIntegrationInstanceConfigurationTagsMutationProps {
    onSuccess?: (result: void, variables: UpdateIntegrationInstanceConfigurationTagsRequest) => void;
    onError?: (error: Error, variables: UpdateIntegrationInstanceConfigurationTagsRequest) => void;
}

export const useUpdateIntegrationInstanceConfigurationTagsMutation = (
    mutationProps?: UpdateIntegrationInstanceConfigurationTagsMutationProps
) =>
    useMutation({
        mutationFn: (request: UpdateIntegrationTagsRequest) => {
            return new IntegrationInstanceConfigurationTagApi().updateIntegrationInstanceConfigurationTags(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
