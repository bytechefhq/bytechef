import {useMutation} from '@tanstack/react-query';
import {
    CreateIntegrationWorkflowRequest,
    DeleteIntegrationRequest,
    IntegrationModel,
    IntegrationsApi,
    UpdateIntegrationTagsRequest,
    WorkflowModel,
} from 'middleware/embedded/integration';

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
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
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
                id: integration.id!,
                integrationModel: integration,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
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
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
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
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

type CreateIntegrationWorkflowRequestMutationProps = {
    onSuccess?: (
        result: WorkflowModel,
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
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
