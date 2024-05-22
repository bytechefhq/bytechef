import {
    EnableIntegrationInstanceConfigurationRequest,
    EnableIntegrationInstanceConfigurationWorkflowRequest,
    IntegrationInstanceConfigurationApi,
    IntegrationInstanceConfigurationModel,
    IntegrationInstanceConfigurationTagApi,
    IntegrationInstanceConfigurationWorkflowModel,
    UpdateIntegrationInstanceConfigurationTagsRequest,
    UpdateIntegrationTagsRequest,
} from '@/shared/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface CreateIntegrationInstanceMutationProps {
    onSuccess?: (
        result: IntegrationInstanceConfigurationModel,
        variables: IntegrationInstanceConfigurationModel
    ) => void;
    onError?: (error: Error, variables: IntegrationInstanceConfigurationModel) => void;
}

export const useCreateIntegrationInstanceConfigurationMutation = (
    mutationProps?: CreateIntegrationInstanceMutationProps
) =>
    useMutation({
        mutationFn: (integrationInstanceConfigurationModel: IntegrationInstanceConfigurationModel) => {
            return new IntegrationInstanceConfigurationApi().createIntegrationInstanceConfiguration({
                integrationInstanceConfigurationModel,
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
    onSuccess?: (
        result: IntegrationInstanceConfigurationModel,
        variables: IntegrationInstanceConfigurationModel
    ) => void;
    onError?: (error: Error, variables: IntegrationInstanceConfigurationModel) => void;
}

export const useUpdateIntegrationInstanceConfigurationMutation = (
    mutationProps?: UpdateIntegrationInstanceConfigurationMutationProps
) =>
    useMutation({
        mutationFn: (integrationInstanceConfigurationModel: IntegrationInstanceConfigurationModel) => {
            return new IntegrationInstanceConfigurationApi().updateIntegrationInstanceConfiguration({
                id: integrationInstanceConfigurationModel.id!,
                integrationInstanceConfigurationModel,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

export const useUpdateIntegrationInstanceConfigurationWorkflowMutation = (
    mutationProps?: UpdateIntegrationInstanceConfigurationWorkflowMutationProps
) =>
    useMutation({
        mutationFn: (integrationInstanceConfigurationWorkflowModel: IntegrationInstanceConfigurationWorkflowModel) => {
            return new IntegrationInstanceConfigurationApi().updateIntegrationInstanceConfigurationWorkflow({
                id: integrationInstanceConfigurationWorkflowModel.integrationInstanceConfigurationId!,
                integrationInstanceConfigurationWorkflowModel: integrationInstanceConfigurationWorkflowModel,
                workflowId: integrationInstanceConfigurationWorkflowModel.id!,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateIntegrationInstanceConfigurationWorkflowMutationProps {
    onSuccess?: (
        result: IntegrationInstanceConfigurationWorkflowModel,
        variables: IntegrationInstanceConfigurationWorkflowModel
    ) => void;
    onError?: (error: Error, variables: IntegrationInstanceConfigurationWorkflowModel) => void;
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
