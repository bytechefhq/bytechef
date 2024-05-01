/* eslint-disable sort-keys */
import {
    GetPreviousWorkflowNodeOutputsRequest,
    GetWorkflowNodeOutputRequest,
    WorkflowNodeOutputApi,
    WorkflowNodeOutputModel,
} from '@/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const WorkflowNodeOutputKeys = {
    filteredPreviousWorkflowNodeOutputs: (request: GetPreviousWorkflowNodeOutputsRequest) => [
        ...WorkflowNodeOutputKeys.workflowNodeOutputs,
        request.id,
        'previousWorkflowNodeOutputs',
        request.lastWorkflowNodeName,
    ],
    workflowNodeOutput: (request: GetWorkflowNodeOutputRequest) => [
        ...WorkflowNodeOutputKeys.workflowNodeOutputs,
        request.id,
        'workflowNodeOutput',
        request.workflowNodeName,
    ],
    workflowNodeOutputs: ['workflowNodeOutputs'] as const,
};

export const useGetWorkflowNodeOutputQuery = (request: GetWorkflowNodeOutputRequest, enabled?: boolean) =>
    useQuery<WorkflowNodeOutputModel, Error>({
        queryKey: WorkflowNodeOutputKeys.workflowNodeOutput(request),
        queryFn: () => new WorkflowNodeOutputApi().getWorkflowNodeOutput(request),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetPreviousWorkflowNodeOutputsQuery = (
    request: GetPreviousWorkflowNodeOutputsRequest,
    enabled?: boolean
) =>
    useQuery<WorkflowNodeOutputModel[], Error>({
        queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs(request),
        queryFn: () => new WorkflowNodeOutputApi().getPreviousWorkflowNodeOutputs(request),
        enabled: enabled === undefined ? true : enabled,
    });
