/* eslint-disable sort-keys */
import {
    GetWorkflowNodeOptionsRequest,
    type OptionModel,
    WorkflowNodeOptionApi,
} from '@/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const WorkflowNodeOptionKeys = {
    propertyWorkflowNodeOptions: (request: GetWorkflowNodeOptionsRequest) => [
        ...WorkflowNodeOptionKeys.workflowNodeOptions,
        request,
    ],
    workflowNodeOptions: ['workflowNodeOptions'] as const,
};

export const useGetWorkflowNodeOptionsQuery = (request: GetWorkflowNodeOptionsRequest, enabled?: boolean) =>
    useQuery<Array<OptionModel>, Error>({
        queryKey: WorkflowNodeOptionKeys.propertyWorkflowNodeOptions(request),
        queryFn: () => new WorkflowNodeOptionApi().getWorkflowNodeOptions(request),
        enabled: enabled === undefined ? true : enabled,
        staleTime: 60 * 1000,
    });
