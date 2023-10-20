import {useMutation} from '@tanstack/react-query';
import {
    CreateIntegrationWorkflowRequest,
    DeleteIntegrationRequest,
    IntegrationModel,
    IntegrationsApi,
    UpdateIntegrationTagsRequest,
} from 'middleware/integration';

type CreateIntegrationMutationProps = {
    onSuccess?: (result: IntegrationModel, variables: IntegrationModel) => void;
    onError?: (error: object, variables: IntegrationModel) => void;
};

export const useCreateIntegrationMutation = (
    mutationProps?: CreateIntegrationMutationProps
) =>
    useMutation({
        mutationFn: (integration: IntegrationModel) => {
            return new IntegrationsApi().createIntegration({
                integrationModel: integration,
            });
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });

type UpdateIntegrationMutationProps = {
    onSuccess?: (result: IntegrationModel, variables: IntegrationModel) => void;
    onError?: (error: object, variables: IntegrationModel) => void;
};

export const useUpdateIntegrationMutation = (
    mutationProps?: UpdateIntegrationMutationProps
) =>
    useMutation({
        mutationFn: (integration: IntegrationModel) => {
            return new IntegrationsApi().updateIntegration({
                integrationModel: integration,
                id: integration.id!,
            });
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });

type UpdateIntegrationTagsMutationProps = {
    onSuccess?: (result: void, variables: UpdateIntegrationTagsRequest) => void;
    onError?: (error: object, variables: UpdateIntegrationTagsRequest) => void;
};

export const useUpdateIntegrationTagsMutation = (
    mutationProps?: UpdateIntegrationTagsMutationProps
) =>
    useMutation({
        mutationFn: (request: UpdateIntegrationTagsRequest) => {
            return new IntegrationsApi().updateIntegrationTags(request);
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });

type DeleteIntegrationMutationProps = {
    onSuccess?: (result: void, variables: DeleteIntegrationRequest) => void;
    onError?: (error: object, variables: DeleteIntegrationRequest) => void;
};

export const useDeleteIntegrationMutation = (
    mutationProps?: DeleteIntegrationMutationProps
) =>
    useMutation({
        mutationFn: (request: DeleteIntegrationRequest) => {
            return new IntegrationsApi().deleteIntegration(request);
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });

type CreateIntegrationWorkflowRequestMutationProps = {
    onSuccess?: (
        result: IntegrationModel,
        variables: CreateIntegrationWorkflowRequest
    ) => void;
    onError?: (
        error: object,
        variables: CreateIntegrationWorkflowRequest
    ) => void;
};

export const useCreateIntegrationWorkflowRequestMutation = (
    mutationProps?: CreateIntegrationWorkflowRequestMutationProps
) =>
    useMutation({
        mutationFn: (request: CreateIntegrationWorkflowRequest) => {
            return new IntegrationsApi().createIntegrationWorkflow(request);
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });
