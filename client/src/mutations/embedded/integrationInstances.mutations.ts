import {
    EnableIntegrationInstanceRequest,
    EnableIntegrationInstanceWorkflowRequest,
    IntegrationInstanceApi,
    IntegrationInstanceModel,
    IntegrationInstanceTagApi,
    IntegrationInstanceWorkflowModel,
    UpdateIntegrationInstanceTagsRequest,
    UpdateIntegrationTagsRequest,
} from '@/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface CreateIntegrationInstanceMutationProps {
    onSuccess?: (result: IntegrationInstanceModel, variables: IntegrationInstanceModel) => void;
    onError?: (error: Error, variables: IntegrationInstanceModel) => void;
}

export const useCreateIntegrationInstanceMutation = (mutationProps?: CreateIntegrationInstanceMutationProps) =>
    useMutation({
        mutationFn: (integrationInstanceModel: IntegrationInstanceModel) => {
            return new IntegrationInstanceApi().createIntegrationInstance({
                integrationInstanceModel,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DeleteIntegrationInstanceMutationProps {
    onSuccess?: (result: void, variables: number) => void;
    onError?: (error: Error, variables: number) => void;
}

export const useDeleteIntegrationInstanceMutation = (mutationProps?: DeleteIntegrationInstanceMutationProps) =>
    useMutation({
        mutationFn: (id: number) => {
            return new IntegrationInstanceApi().deleteIntegrationInstance({
                id: id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface EnableIntegrationInstanceMutationProps {
    onSuccess?: (result: void, variables: EnableIntegrationInstanceRequest) => void;
    onError?: (error: Error, variables: EnableIntegrationInstanceRequest) => void;
}

export const useEnableIntegrationInstanceMutation = (mutationProps: EnableIntegrationInstanceMutationProps) =>
    useMutation({
        mutationFn: (request: EnableIntegrationInstanceRequest) => {
            return new IntegrationInstanceApi().enableIntegrationInstance(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface EnableIntegrationInstanceWorkflowMutationProps {
    onSuccess?: (result: void, variables: EnableIntegrationInstanceWorkflowRequest) => void;
    onError?: (error: Error, variables: EnableIntegrationInstanceWorkflowRequest) => void;
}

export const useEnableIntegrationInstanceWorkflowMutation = (
    mutationProps: EnableIntegrationInstanceWorkflowMutationProps
) =>
    useMutation({
        mutationFn: (request: EnableIntegrationInstanceWorkflowRequest) => {
            return new IntegrationInstanceApi().enableIntegrationInstanceWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateIntegrationInstanceMutationProps {
    onSuccess?: (result: IntegrationInstanceModel, variables: IntegrationInstanceModel) => void;
    onError?: (error: Error, variables: IntegrationInstanceModel) => void;
}

export const useUpdateIntegrationInstanceMutation = (mutationProps?: UpdateIntegrationInstanceMutationProps) =>
    useMutation({
        mutationFn: (integrationInstanceModel: IntegrationInstanceModel) => {
            return new IntegrationInstanceApi().updateIntegrationInstance({
                id: integrationInstanceModel.id!,
                integrationInstanceModel,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

export const useUpdateIntegrationInstanceWorkflowMutation = (
    mutationProps?: UpdateIntegrationInstanceWorkflowMutationProps
) =>
    useMutation({
        mutationFn: (integrationInstanceWorkflowModel: IntegrationInstanceWorkflowModel) => {
            return new IntegrationInstanceApi().updateIntegrationInstanceWorkflow({
                id: integrationInstanceWorkflowModel.integrationInstanceId!,
                integrationInstanceWorkflowId: integrationInstanceWorkflowModel.id!,
                integrationInstanceWorkflowModel,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateIntegrationInstanceWorkflowMutationProps {
    onSuccess?: (result: IntegrationInstanceWorkflowModel, variables: IntegrationInstanceWorkflowModel) => void;
    onError?: (error: Error, variables: IntegrationInstanceWorkflowModel) => void;
}

interface UpdateIntegrationInstanceTagsMutationProps {
    onSuccess?: (result: void, variables: UpdateIntegrationInstanceTagsRequest) => void;
    onError?: (error: Error, variables: UpdateIntegrationInstanceTagsRequest) => void;
}

export const useUpdateIntegrationInstanceTagsMutation = (mutationProps?: UpdateIntegrationInstanceTagsMutationProps) =>
    useMutation({
        mutationFn: (request: UpdateIntegrationTagsRequest) => {
            return new IntegrationInstanceTagApi().updateIntegrationInstanceTags(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
