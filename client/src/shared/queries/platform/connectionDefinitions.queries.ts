/* eslint-disable sort-keys */
import {
    ConnectionDefinition,
    ConnectionDefinitionApi,
    GetComponentConnectionDefinitionRequest,
    GetComponentConnectionDefinitionsRequest,
} from '@/shared/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const ConnectDefinitionKeys = {
    connectionDefinition: (request?: GetComponentConnectionDefinitionRequest) => [
        ...ConnectDefinitionKeys.connectionDefinitions,
        request?.componentName,
        request?.componentVersion,
    ],
    connectionDefinitions: ['connectionDefinitions'],
    filteredConnectionDefinitions: (request: GetComponentConnectionDefinitionsRequest) => [
        ...ConnectDefinitionKeys.connectionDefinitions,
        request,
    ],
};

export const useGetConnectionDefinitionQuery = (request: GetComponentConnectionDefinitionRequest, enabled?: boolean) =>
    useQuery<ConnectionDefinition, Error>({
        enabled: enabled === undefined ? true : enabled,
        queryKey: ConnectDefinitionKeys.connectionDefinition(request),
        queryFn: () => new ConnectionDefinitionApi().getComponentConnectionDefinition(request),
    });

export const useGetConnectionDefinitionsQuery = (
    request: GetComponentConnectionDefinitionsRequest,
    enabled?: boolean
) =>
    useQuery<ConnectionDefinition[], Error>({
        enabled: enabled === undefined ? true : enabled,
        queryKey: ConnectDefinitionKeys.filteredConnectionDefinitions(request),
        queryFn: () => new ConnectionDefinitionApi().getComponentConnectionDefinitions(request),
    });
