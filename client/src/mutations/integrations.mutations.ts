import {useMutation} from '@tanstack/react-query';
import {
    DeleteIntegrationRequest,
    IntegrationModel,
    IntegrationsApi,
    UpdateIntegrationTagsRequest,
} from 'middleware/integration';

type IntegrationMutationProps = {
    onSuccess?: (result: IntegrationModel, variables: IntegrationModel) => void;
    onError?: (error: object, variables: IntegrationModel) => void;
};

export const useCreateIntegrationMutation = (
    mutationProps?: IntegrationMutationProps
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

export const useUpdateIntegrationMutation = (
    mutationProps?: IntegrationMutationProps
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

type IntegrationTagsMutationProps = {
    onSuccess?: (result: void, variables: UpdateIntegrationTagsRequest) => void;
    onError?: (error: object, variables: UpdateIntegrationTagsRequest) => void;
};

export const useUpdateIntegrationTagsMutation = (
    mutationProps?: IntegrationTagsMutationProps
) =>
    useMutation({
        mutationFn: (request: UpdateIntegrationTagsRequest) => {
            return new IntegrationsApi().updateIntegrationTags(request);
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });

type IntegrationDeleteMutationProps = {
    onSuccess?: (result: void, variables: DeleteIntegrationRequest) => void;
    onError?: (error: object, variables: DeleteIntegrationRequest) => void;
};

export const useDeleteIntegrationMutation = (
    mutationProps?: IntegrationDeleteMutationProps
) =>
    useMutation({
        mutationFn: (request: DeleteIntegrationRequest) => {
            return new IntegrationsApi().deleteIntegration(request);
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });

type WorkflowMutationProps = {
    onSuccess?: (result: IntegrationModel, variables: IntegrationModel) => void;
    onError?: (error: object, variables: IntegrationModel) => void;
};

export const useWorkflowMutation = (mutationProps?: WorkflowMutationProps) =>
    useMutation({
        mutationFn: (workflows: IntegrationModel) => {
            return new IntegrationsApi().postIntegration({
                integrationModel: workflows,
            });
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });
