import {useMutation} from '@tanstack/react-query';
import {
    IntegrationModel,
    IntegrationsApi,
    PutIntegrationTagsRequest,
} from 'data-access/integration';

type MutationProps = {
    onSuccess?: (result: IntegrationModel, variables: IntegrationModel) => void;
    onError?: (error: object, variables: IntegrationModel) => void;
};

export const useIntegrationMutation = (mutationProps?: MutationProps) =>
    useMutation({
        mutationFn: (integration: IntegrationModel) => {
            return new IntegrationsApi().postIntegration({
                integrationModel: integration,
            });
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });

export const useIntegrationTagsMutation = () =>
    useMutation({
        mutationFn: (request: PutIntegrationTagsRequest) => {
            return new IntegrationsApi().putIntegrationTags(request);
        },
    });
