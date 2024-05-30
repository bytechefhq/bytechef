/* eslint-disable sort-keys */
import {
    GetWorkflowNodeOptionsRequest,
    type OptionModel,
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
    ],
    workflowNodeOptions: ['workflowNodeOptions'] as const,
};

export const useGetWorkflowNodeOptionsQuery = (
    {loadDependencyValueKey, request}: {loadDependencyValueKey: string; request: GetWorkflowNodeOptionsRequest},
    enabled?: boolean
) =>
    useQuery<Array<OptionModel>, Error>({
        queryKey: WorkflowNodeOptionKeys.propertyWorkflowNodeOptions(request, loadDependencyValueKey),
        queryFn: () => new WorkflowNodeOptionApi().getWorkflowNodeOptions(request),
        enabled: enabled === undefined ? true : enabled,
        staleTime: 60000,
    });
