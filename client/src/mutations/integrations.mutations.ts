import {useMutation} from '@tanstack/react-query';
import {
    IntegrationModel,
    IntegrationsApi,
    PutIntegrationTagsRequest,
} from 'data-access/integration';

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
