/* eslint-disable sort-keys */
import {
    ConnectionDefinitionApi,
    ConnectionDefinitionModel,
    GetComponentConnectionDefinitionsRequest,
} from '@/shared/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export interface GetComponentConnectionDefinitionRequestI {
    componentName: string | undefined;
    componentVersion: number | undefined;
}

export const ConnectDefinitionKeys = {
    connectionDefinition: (request?: GetComponentConnectionDefinitionRequestI) => [
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

export const useGetConnectionDefinitionQuery = (request?: GetComponentConnectionDefinitionRequestI) =>
    useQuery<ConnectionDefinitionModel, Error>({
        queryKey: ConnectDefinitionKeys.connectionDefinition(request),
        queryFn: () =>
            new ConnectionDefinitionApi().getComponentConnectionDefinition({
                componentName: request?.componentName ?? '',
                componentVersion: request?.componentVersion ?? -1,
            }),
        enabled: !!request?.componentName,
    });

export const useGetConnectionDefinitionsQuery = (request: GetComponentConnectionDefinitionsRequest) =>
    useQuery<ConnectionDefinitionModel[], Error>({
        queryKey: ConnectDefinitionKeys.filteredConnectionDefinitions(request),
        queryFn: () => new ConnectionDefinitionApi().getComponentConnectionDefinitions(request),
        enabled: !!request?.componentName,
    });
