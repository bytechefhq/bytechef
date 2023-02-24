import {useMutation} from '@tanstack/react-query';
import {
    DeleteIntegrationRequest,
    IntegrationModel,
    IntegrationsApi,
    PutIntegrationTagsRequest,
} from 'middleware/integration';

type IntegrationMutationProps = {
    onSuccess?: (result: IntegrationModel, variables: IntegrationModel) => void;
    onError?: (error: object, variables: IntegrationModel) => void;
};

export const useIntegrationMutation = (
    mutationProps?: IntegrationMutationProps
) =>
    useMutation({
        mutationFn: (integration: IntegrationModel) => {
            return new IntegrationsApi().postIntegration({
                integrationModel: integration,
            });
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });

type IntegrationTagsMutationProps = {
    onSuccess?: (result: void, variables: PutIntegrationTagsRequest) => void;
    onError?: (error: object, variables: PutIntegrationTagsRequest) => void;
};

export const useIntegrationTagsMutation = (
    mutationProps?: IntegrationTagsMutationProps
) =>
    useMutation({
        mutationFn: (request: PutIntegrationTagsRequest) => {
            return new IntegrationsApi().putIntegrationTags(request);
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });

type IntegrationDeleteMutationProps = {
    onSuccess?: (result: void, variables: DeleteIntegrationRequest) => void;
    onError?: (error: object, variables: DeleteIntegrationRequest) => void;
};

export const useIntegrationDeleteMutation = (
    mutationProps?: IntegrationDeleteMutationProps
) =>
    useMutation({
        mutationFn: (request: DeleteIntegrationRequest) => {
            return new IntegrationsApi().deleteIntegration(request);
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });
