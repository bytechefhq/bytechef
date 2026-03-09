/* eslint-disable sort-keys */
import {
    type GetWorkflowNodeDescription200Response,
    GetWorkflowNodeDescriptionRequest,
    GetWorkflowNodeOutputRequest,
    WorkflowNodeDescriptionApi,
} from '@/shared/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const WorkflowNodeDescriptionKeys = {
    workflowNodeDescription: (request: GetWorkflowNodeOutputRequest) => [
        ...WorkflowNodeDescriptionKeys.workflowNodeDescriptions,
        request.id,
        request.workflowNodeName,
        request.environmentId,
    ],
    workflowNodeDescriptions: ['workflowNodeDescriptions'] as const,
};

export const useGetWorkflowNodeDescriptionQuery = (request: GetWorkflowNodeDescriptionRequest, enabled?: boolean) =>
    useQuery<GetWorkflowNodeDescription200Response, Error>({
        queryFn: () => new WorkflowNodeDescriptionApi().getWorkflowNodeDescription(request),
        queryKey: WorkflowNodeDescriptionKeys.workflowNodeDescription(request),
        enabled: enabled === undefined ? true : enabled,
        staleTime: Infinity,
    });
