/* eslint-disable sort-keys */
import {
    GetWorkflowNodeOutputRequest,
    GetWorkflowNodeOutputsRequest,
    WorkflowNodeApi,
    WorkflowNodeOutputModel,
} from '@/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const WorkflowNodeOutputKeys = {
    filteredWorkflowNodeOutputs: (request: GetWorkflowNodeOutputsRequest) => [
        ...WorkflowNodeOutputKeys.workflowNodeOutputs,
        request.id,
        'lastWorkflowNodeName',
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
        queryFn: () => new WorkflowNodeApi().getWorkflowNodeOutput(request),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetWorkflowNodeOutputsQuery = (request: GetWorkflowNodeOutputsRequest, enabled?: boolean) =>
    useQuery<WorkflowNodeOutputModel[], Error>({
        queryKey: WorkflowNodeOutputKeys.filteredWorkflowNodeOutputs(request),
        queryFn: () => new WorkflowNodeApi().getWorkflowNodeOutputs(request),
        enabled: enabled === undefined ? true : enabled,
    });
