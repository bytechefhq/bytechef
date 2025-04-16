import {
    ClusterElementDefinition,
    ClusterElementDefinitionApi,
    type ClusterElementDefinitionBasic,
    GetComponentClusterElementDefinitionRequest,
    GetRootComponentClusterElementDefinitionsRequest,
} from '@/shared/middleware/platform/configuration';

/* eslint-disable sort-keys */

import {useQuery} from '@tanstack/react-query';

export const ClusterElementDefinitionKeys = {
    clusterElementDefinition: (request: GetComponentClusterElementDefinitionRequest) => [
        ...ClusterElementDefinitionKeys.clusterElementDefinitions,
        request.componentName,
        request.componentVersion,
        request.clusterElementName,
    ],
    filteredClusterElementDefinitions: (request: GetRootComponentClusterElementDefinitionsRequest) => [
        ...ClusterElementDefinitionKeys.clusterElementDefinitions,
        request.rootComponentName,
        request.rootComponentVersion,
        request.clusterElementType,
    ],
    clusterElementDefinitions: ['clusterElementDefinitions'] as const,
};

export const useGetClusterElementDefinitionQuery = (
    request: GetComponentClusterElementDefinitionRequest,
    enabled?: boolean
) =>
    useQuery<ClusterElementDefinition, Error>({
        queryKey: ClusterElementDefinitionKeys.clusterElementDefinition(request),
        queryFn: () => new ClusterElementDefinitionApi().getComponentClusterElementDefinition(request),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetRootComponentClusterElementDefinitions = (
    request: GetRootComponentClusterElementDefinitionsRequest,
    enabled?: boolean
) =>
    useQuery<ClusterElementDefinitionBasic[], Error>({
        queryKey: ClusterElementDefinitionKeys.filteredClusterElementDefinitions(request),
        queryFn: () => new ClusterElementDefinitionApi().getRootComponentClusterElementDefinitions(request),
        enabled: enabled === undefined ? true : enabled,
    });
