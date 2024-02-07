/* eslint-disable sort-keys */
import {
    type GetWorkflowNodeDescription200ResponseModel,
    GetWorkflowNodeDescriptionRequest,
    GetWorkflowNodeOutputRequest,
    WorkflowNodeApi,
} from '@/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const WorkflowNodeDescriptionKeys = {
    workflowNodeDescription: (request: GetWorkflowNodeOutputRequest) => [
        ...WorkflowNodeDescriptionKeys.workflowNodeDescriptions,
        request,
    ],
    workflowNodeDescriptions: ['workflowNodeDescriptions'] as const,
};

export const useGetWorkflowNodeDescriptionQuery = (request: GetWorkflowNodeDescriptionRequest, enabled?: boolean) =>
    useQuery<GetWorkflowNodeDescription200ResponseModel, Error>({
        queryKey: WorkflowNodeDescriptionKeys.workflowNodeDescription(request),
        queryFn: () => new WorkflowNodeApi().getWorkflowNodeDescription(request),
        enabled: enabled === undefined ? true : enabled,
    });
