import {
    ConnectionDefinitionApi,
    ConnectionDefinitionModel,
    GetComponentConnectionDefinitionRequest,
    GetComponentConnectionDefinitionsRequest,
} from '@/middleware/hermes/configuration';
import {useQuery} from '@tanstack/react-query';

export const ConnectDefinitionKeys = {
    connectionDefinition: (
        request?: GetComponentConnectionDefinitionRequest
    ) => [...ConnectDefinitionKeys.connectionDefinitions, request],
    connectionDefinitions: ['connectionDefinitions'],
    filteredConnectionDefinitions: (
        request: GetComponentConnectionDefinitionsRequest
    ) => [...ConnectDefinitionKeys.connectionDefinitions, request],
};

export const useGetConnectionDefinitionQuery = (
    request?: GetComponentConnectionDefinitionRequest
) =>
    useQuery<ConnectionDefinitionModel, Error>(
        ConnectDefinitionKeys.connectionDefinition(request),
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
        ConnectDefinitionKeys.filteredConnectionDefinitions(request),
        () =>
            new ConnectionDefinitionApi().getComponentConnectionDefinitions(
                request
            ),
        {enabled: false || enabledCondition}
    );
