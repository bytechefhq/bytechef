import {
    ConnectionDefinitionApi,
    ConnectionDefinitionModel,
    GetComponentConnectionDefinitionRequest,
    GetComponentConnectionDefinitionsRequest,
} from '@/middleware/hermes/configuration';
import {useQuery} from '@tanstack/react-query';

export const ConnectDefinitionKeys = {
    connectionDefinition: ['connectionDefinition'],
    connectionDefinitionDetails: (
        request?: GetComponentConnectionDefinitionRequest
    ) => [...ConnectDefinitionKeys.connectionDefinition, request],
    connectionDefinitions: (
        request: GetComponentConnectionDefinitionsRequest
    ) => ['connectionDefinitions', request],
};

export const useGetConnectionDefinitionQuery = (
    request?: GetComponentConnectionDefinitionRequest
) =>
    useQuery<ConnectionDefinitionModel, Error>(
        ConnectDefinitionKeys.connectionDefinitionDetails(request),
        () =>
            new ConnectionDefinitionApi().getComponentConnectionDefinition(
                request!
            ),
        {
            enabled: !!request?.componentName,
        }
    );

export const useGetConnectionDefinitionsQuery = (
    request: GetComponentConnectionDefinitionsRequest,
    enabledCondition?: boolean
) =>
    useQuery<ConnectionDefinitionModel[], Error>(
        ConnectDefinitionKeys.connectionDefinitions(request),
        () =>
            new ConnectionDefinitionApi().getComponentConnectionDefinitions(
                request
            ),
        {enabled: false || enabledCondition}
    );
