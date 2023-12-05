/* eslint-disable sort-keys */
import {
    ConnectionDefinitionApi,
    ConnectionDefinitionModel,
    GetComponentConnectionDefinitionRequest,
    GetComponentConnectionDefinitionsRequest,
} from '@/middleware/hermes/configuration';
import {useQuery} from '@tanstack/react-query';

export const ConnectDefinitionKeys = {
    connectionDefinition: (request?: GetComponentConnectionDefinitionRequest) => [
        ...ConnectDefinitionKeys.connectionDefinitions,
        request,
    ],
    connectionDefinitions: ['connectionDefinitions'],
    filteredConnectionDefinitions: (request: GetComponentConnectionDefinitionsRequest) => [
        ...ConnectDefinitionKeys.connectionDefinitions,
        request,
    ],
};

export const useGetConnectionDefinitionQuery = (request?: GetComponentConnectionDefinitionRequest) =>
    useQuery<ConnectionDefinitionModel, Error>({
        queryKey: ConnectDefinitionKeys.connectionDefinition(request),
        queryFn: () => new ConnectionDefinitionApi().getComponentConnectionDefinition(request!),
        enabled: !!request?.componentName,
    });

export const useGetConnectionDefinitionsQuery = (request: GetComponentConnectionDefinitionsRequest) =>
    useQuery<ConnectionDefinitionModel[], Error>({
        queryKey: ConnectDefinitionKeys.filteredConnectionDefinitions(request),
        queryFn: () => new ConnectionDefinitionApi().getComponentConnectionDefinitions(request),
        enabled: !!request?.componentName,
    });
