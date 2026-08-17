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

/**
 * True when a parameter write elsewhere in the node can shift this options query's result.
 *
 * `loadDependencyValueKey` is the only part of the key that carries this: it is built from the property's
 * `lookupDependsOn` values plus the connection id, so it is empty exactly when the property declares neither.
 * An options function with no declared dependencies cannot read another parameter's value, so invalidating it
 * only costs a refetch — which is what made every Skills row flash "Refetching…" each time one was added.
 *
 * Kept beside the key builders on purpose: the reads are positional, so they must not drift from them.
 */
export const isDependentOptionsQueryKey = (queryKey: readonly unknown[]): boolean => {
    const [root] = queryKey;

    if (root === 'workflowNodeOptions') {
        return !!queryKey[5];
    }

    if (root === 'clusterElementNodeOptions') {
        return !!queryKey[7];
    }

    return true;
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
