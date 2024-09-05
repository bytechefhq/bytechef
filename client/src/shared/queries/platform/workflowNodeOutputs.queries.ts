/* eslint-disable sort-keys */
import {
    GetPreviousWorkflowNodeOutputsRequest,
    GetWorkflowNodeOutputRequest,
    WorkflowNodeOutput,
    WorkflowNodeOutputApi,
} from '@/shared/middleware/platform/configuration';
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
        request.workflowNodeName,
    ],
    workflowNodeOutputs: ['workflowNodeOutputs'] as const,
};

export const useGetWorkflowNodeOutputQuery = (request: GetWorkflowNodeOutputRequest, enabled?: boolean) =>
    useQuery<WorkflowNodeOutput, Error>({
        queryKey: WorkflowNodeOutputKeys.workflowNodeOutput(request),
        queryFn: () => new WorkflowNodeOutputApi().getWorkflowNodeOutput(request),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetPreviousWorkflowNodeOutputsQuery = (
    request: GetPreviousWorkflowNodeOutputsRequest,
    enabled?: boolean
) =>
    useQuery<WorkflowNodeOutput[], Error>({
        queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs(request),
        queryFn: () => new WorkflowNodeOutputApi().getPreviousWorkflowNodeOutputs(request),
        enabled: enabled === undefined ? true : enabled,
    });
