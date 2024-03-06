/* eslint-disable sort-keys */
import {
    GetWorkflowNodeDynamicPropertiesRequest,
    type PropertyModel,
    WorkflowNodeDynamicPropertiesApi,
} from '@/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const WorkflowNodeDynamicPropertyKeys = {
    propertyWorkflowNodeDynamicProperties: (request: GetWorkflowNodeDynamicPropertiesRequest) => [
        ...WorkflowNodeDynamicPropertyKeys.workflowNodeOptions,
        request,
    ],
    workflowNodeOptions: ['workflowNodeOptions'] as const,
};

export const useGetWorkflowNodeDynamicPropertiesQuery = (
    request: GetWorkflowNodeDynamicPropertiesRequest,
    enabled?: boolean
) =>
    useQuery<Array<PropertyModel>, Error>({
        queryKey: WorkflowNodeDynamicPropertyKeys.propertyWorkflowNodeDynamicProperties(request),
        queryFn: () => new WorkflowNodeDynamicPropertiesApi().getWorkflowNodeDynamicProperties(request),
        enabled: enabled === undefined ? true : enabled,
    });
