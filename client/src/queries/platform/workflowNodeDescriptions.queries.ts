/* eslint-disable sort-keys */
import {
    type GetWorkflowNodeDescription200ResponseModel,
    GetWorkflowNodeDescriptionRequest,
    GetWorkflowNodeOutputRequest,
    WorkflowNodeApi,
} from '@/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const WorkflowNodeDescriptions = {
    workflowNodeDescription: (request: GetWorkflowNodeOutputRequest) => [
        ...WorkflowNodeDescriptions.outputSchemas,
        request,
    ],
    outputSchemas: ['workflowNodeDescriptions'] as const,
};

export const useGetWorkflowNodeDescriptionQuery = (request: GetWorkflowNodeDescriptionRequest, enabled?: boolean) =>
    useQuery<GetWorkflowNodeDescription200ResponseModel, Error>({
        queryKey: WorkflowNodeDescriptions.workflowNodeDescription(request),
        queryFn: () => new WorkflowNodeApi().getWorkflowNodeDescription(request),
        enabled: enabled === undefined ? true : enabled,
    });
