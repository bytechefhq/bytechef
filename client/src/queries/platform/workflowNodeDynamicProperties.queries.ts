/* eslint-disable sort-keys */
import {
    GetWorkflowNodeDynamicPropertiesRequest,
    type PropertyModel,
    WorkflowNodeDynamicPropertiesApi,
} from '@/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const WorkflowNodeDynamicPropertyKeys = {
    propertyWorkflowNodeDynamicProperties: (
        request: GetWorkflowNodeDynamicPropertiesRequest,
        loadDependencyValueKey: string
    ) => [
        ...WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties,
        request.id,
        request.workflowNodeName,
        request.propertyName,
        loadDependencyValueKey,
    ],
    workflowNodeDynamicProperties: ['workflowNodeDynamicProperties'] as const,
};

export const useGetWorkflowNodeDynamicPropertiesQuery = (
    {
        loadDependencyValueKey,
        request,
    }: {loadDependencyValueKey: string; request: GetWorkflowNodeDynamicPropertiesRequest},
    enabled?: boolean
) =>
    useQuery<Array<PropertyModel>, Error>({
        queryKey: WorkflowNodeDynamicPropertyKeys.propertyWorkflowNodeDynamicProperties(
            request,
            loadDependencyValueKey
        ),
        queryFn: () => new WorkflowNodeDynamicPropertiesApi().getWorkflowNodeDynamicProperties(request),
        enabled: enabled === undefined ? true : enabled,
        staleTime: 60000,
    });
