/* eslint-disable sort-keys */
import {
    GetClusterElementDynamicPropertiesRequest,
    GetWorkflowNodeDynamicPropertiesRequest,
    type Property,
    WorkflowNodeDynamicPropertiesApi,
} from '@/shared/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const WorkflowNodeDynamicPropertyKeys = {
    propertyWorkflowNodeDynamicProperties: (
        request: GetWorkflowNodeDynamicPropertiesRequest,
        lookupDependsOnValues: string
    ) => [
        ...WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties,
        request.id,
        request.workflowNodeName,
        request.propertyName,
        lookupDependsOnValues,
        request.environmentId,
    ],
    workflowNodeDynamicProperties: ['workflowNodeDynamicProperties'] as const,
};

export const ClusterElementDynamicPropertyKeys = {
    propertyClusterElementDynamicProperties: (
        request: GetClusterElementDynamicPropertiesRequest,
        lookupDependsOnValues: string
    ) => [
        ...ClusterElementDynamicPropertyKeys.clusterElementDynamicProperties,
        request.id,
        request.workflowNodeName,
        request.clusterElementType,
        request.clusterElementWorkflowNodeName,
        request.propertyName,
        lookupDependsOnValues,
    ],
    clusterElementDynamicProperties: ['clusterElementDynamicProperties'] as const,
};

export const useGetWorkflowNodeDynamicPropertiesQuery = (
    {
        lookupDependsOnValuesKey,
        request,
    }: {lookupDependsOnValuesKey: string; request: GetWorkflowNodeDynamicPropertiesRequest},
    enabled?: boolean
) =>
    useQuery<Array<Property>, Error>({
        queryKey: WorkflowNodeDynamicPropertyKeys.propertyWorkflowNodeDynamicProperties(
            request,
            lookupDependsOnValuesKey
        ),
        queryFn: () => new WorkflowNodeDynamicPropertiesApi().getWorkflowNodeDynamicProperties(request),
        enabled: enabled === undefined ? true : enabled,
        staleTime: 60000,
    });

export const useGetClusterElementDynamicPropertiesQuery = (
    {
        lookupDependsOnValuesKey,
        request,
    }: {lookupDependsOnValuesKey: string; request: GetClusterElementDynamicPropertiesRequest},
    enabled?: boolean
) =>
    useQuery<Array<Property>, Error>({
        queryKey: ClusterElementDynamicPropertyKeys.propertyClusterElementDynamicProperties(
            request,
            lookupDependsOnValuesKey
        ),
        queryFn: () => new WorkflowNodeDynamicPropertiesApi().getClusterElementDynamicProperties(request),
        enabled: enabled === undefined ? true : enabled,
        staleTime: 60000,
    });
