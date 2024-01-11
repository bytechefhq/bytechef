import {useMutation} from '@tanstack/react-query';
import {
    CreateIntegrationWorkflowRequest,
    DeleteIntegrationRequest,
    EnableIntegrationInstanceWorkflowRequest,
    IntegrationApi,
    IntegrationInstanceApi,
    IntegrationModel,
    IntegrationTagApi,
    UpdateIntegrationTagsRequest,
    WorkflowModel,
} from 'middleware/embedded/configuration';

interface CreateIntegrationMutationProps {
    onSuccess?: (result: IntegrationModel, variables: IntegrationModel) => void;
    onError?: (error: Error, variables: IntegrationModel) => void;
}

export const useCreateIntegrationMutation = (mutationProps?: CreateIntegrationMutationProps) =>
    useMutation({
        mutationFn: (integration: IntegrationModel) => {
            return new IntegrationApi().createIntegration({
                integrationModel: integration,
            });
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

interface UpdateIntegrationMutationProps {
    onSuccess?: (result: IntegrationModel, variables: IntegrationModel) => void;
    onError?: (error: Error, variables: IntegrationModel) => void;
}

export const useUpdateIntegrationMutation = (mutationProps?: UpdateIntegrationMutationProps) =>
    useMutation({
        mutationFn: (integration: IntegrationModel) => {
            return new IntegrationApi().updateIntegration({
                id: integration.id!,
                integrationModel: integration,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateIntegrationTagsMutationProps {
    onSuccess?: (result: void, variables: UpdateIntegrationTagsRequest) => void;
    onError?: (error: Error, variables: UpdateIntegrationTagsRequest) => void;
}

export const useUpdateIntegrationTagsMutation = (mutationProps?: UpdateIntegrationTagsMutationProps) =>
    useMutation({
        mutationFn: (request: UpdateIntegrationTagsRequest) => {
            return new IntegrationTagApi().updateIntegrationTags(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface CreateIntegrationWorkflowMutationProps {
    onSuccess?: (result: WorkflowModel, variables: CreateIntegrationWorkflowRequest) => void;
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

interface DeleteIntegrationMutationProps {
    onSuccess?: (result: void, variables: DeleteIntegrationRequest) => void;
    onError?: (error: Error, variables: DeleteIntegrationRequest) => void;
}

export const useDeleteIntegrationMutation = (mutationProps?: DeleteIntegrationMutationProps) =>
    useMutation({
        mutationFn: (request: DeleteIntegrationRequest) => {
            return new IntegrationApi().deleteIntegration(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
