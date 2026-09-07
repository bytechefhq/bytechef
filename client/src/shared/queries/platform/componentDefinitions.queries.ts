/* eslint-disable sort-keys */
import {
    ComponentDefinition,
    ComponentDefinitionApi,
    ComponentDefinitionBasic,
    GetComponentDefinitionRequest,
    GetComponentDefinitionVersionsRequest,
    GetConnectionComponentDefinitionRequest,
} from '@/shared/middleware/platform/configuration';
import {DEFINITION_STALE_TIME} from '@/shared/queries/queryConstants';
import {useQuery} from '@tanstack/react-query';

export interface GetComponentDefinitionsRequestI {
    actionDefinitions?: boolean;
    clusterElementDefinitions?: boolean;
    connectionDefinitions?: boolean;
    triggerDefinitions?: boolean;
    include?: Array<string>;
}

export const ComponentDefinitionKeys = {
    componentDefinition: (request: GetComponentDefinitionRequest) => [
        ...ComponentDefinitionKeys.componentDefinitions,
        request.componentName,
        request.componentVersion,
    ],
    componentDefinitionVersions: (request: GetComponentDefinitionVersionsRequest) => [
        ...ComponentDefinitionKeys.componentDefinitions,
        request.componentName,
        'versions',
    ],
    componentDefinitions: ['componentDefinitions'] as const,
    connectionComponentDefinition: (request: GetConnectionComponentDefinitionRequest) => [
        ...ComponentDefinitionKeys.componentDefinitions,
        request.componentName,
        request.connectionVersion,
    ],
    filteredComponentDefinitions: (request?: GetComponentDefinitionsRequestI) => [
        ...ComponentDefinitionKeys.componentDefinitions,
        request,
    ],
};

export const useGetComponentDefinitionQuery = (request: GetComponentDefinitionRequest, enabled?: boolean) =>
    useQuery<ComponentDefinition, Error>({
        queryKey: ComponentDefinitionKeys.componentDefinition(request),
        queryFn: () => new ComponentDefinitionApi().getComponentDefinition(request),
        enabled: enabled === undefined ? true : enabled,
        staleTime: DEFINITION_STALE_TIME,
    });

export const useGetConnectionComponentDefinitionQuery = (
    request: GetConnectionComponentDefinitionRequest,
    enabled?: boolean
) =>
    useQuery<ComponentDefinition, Error>({
        queryKey: ComponentDefinitionKeys.connectionComponentDefinition(request),
        queryFn: () => new ComponentDefinitionApi().getConnectionComponentDefinition(request),
        enabled: enabled === undefined ? true : enabled,
        staleTime: DEFINITION_STALE_TIME,
    });

export const useGetComponentDefinitionVersionsQuery = (
    request: GetComponentDefinitionVersionsRequest,
    enabled?: boolean
) =>
    useQuery<Array<ComponentDefinitionBasic>, Error>({
        queryKey: ComponentDefinitionKeys.componentDefinitionVersions(request),
        queryFn: () => new ComponentDefinitionApi().getComponentDefinitionVersions(request),
        enabled: enabled ?? true,
        staleTime: DEFINITION_STALE_TIME,
    });
