/* eslint-disable sort-keys */
import {
    GetClusterElementNodeOptionsRequest,
    GetWorkflowNodeOptionsRequest,
    type Option,
    WorkflowNodeOptionApi,
} from '@/shared/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const WorkflowNodeOptionKeys = {
    propertyWorkflowNodeOptions: (request: GetWorkflowNodeOptionsRequest, loadDependencyValueKey: string) => [
        ...WorkflowNodeOptionKeys.workflowNodeOptions,
        request.id,
        request.workflowNodeName,
        request.propertyName,
        request.searchText,
        loadDependencyValueKey,
        request.environmentId,
    ],
    workflowNodeOptions: ['workflowNodeOptions'] as const,

    propertyClusterElementNodeOptions: (
        request: GetClusterElementNodeOptionsRequest,
        loadDependencyValueKey: string
    ) => [
        ...WorkflowNodeOptionKeys.clusterElementNodeOptions,
        request.id,
        request.workflowNodeName,
        request.clusterElementType,
        request.clusterElementWorkflowNodeName,
        request.propertyName,
        request.searchText,
        loadDependencyValueKey,
    ],
    clusterElementNodeOptions: ['clusterElementNodeOptions'] as const,
};

export const useGetWorkflowNodeOptionsQuery = (
    {loadDependencyValueKey, request}: {loadDependencyValueKey: string; request: GetWorkflowNodeOptionsRequest},
    enabled?: boolean
) =>
    useQuery<Array<Option>, Error>({
        queryKey: WorkflowNodeOptionKeys.propertyWorkflowNodeOptions(request, loadDependencyValueKey),
        queryFn: () => new WorkflowNodeOptionApi().getWorkflowNodeOptions(request),
        enabled: enabled === undefined ? true : enabled,
        staleTime: 60000,
    });

export const useGetClusterElementNodeOptionsQuery = (
    {loadDependencyValueKey, request}: {loadDependencyValueKey: string; request: GetClusterElementNodeOptionsRequest},
    enabled?: boolean
) =>
    useQuery<Array<Option>, Error>({
        queryKey: WorkflowNodeOptionKeys.propertyClusterElementNodeOptions(request, loadDependencyValueKey),
        queryFn: () => new WorkflowNodeOptionApi().getClusterElementNodeOptions(request),
        enabled: enabled === undefined ? true : enabled,
        staleTime: 60000,
    });
