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
    ],
    workflowNodeDescriptions: ['workflowNodeDescriptions'] as const,
};

export const useGetWorkflowNodeDescriptionQuery = (request: GetWorkflowNodeDescriptionRequest, enabled?: boolean) =>
    useQuery<GetWorkflowNodeDescription200Response, Error>({
        queryKey: WorkflowNodeDescriptionKeys.workflowNodeDescription(request),
        queryFn: () => new WorkflowNodeDescriptionApi().getWorkflowNodeDescription(request),
        enabled: enabled === undefined ? true : enabled,
    });
