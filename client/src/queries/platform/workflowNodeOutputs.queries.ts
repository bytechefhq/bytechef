/* eslint-disable sort-keys */
import {
    GetWorkflowNodeOutputRequest,
    GetWorkflowNodeOutputsRequest,
    WorkflowNodeApi,
    WorkflowNodeOutputModel,
} from '@/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const WorkflowNodeOutputs = {
    filteredWorkflowNodeOutputs: (request: GetWorkflowNodeOutputsRequest) => [
        ...WorkflowNodeOutputs.outputSchemas,
        request,
    ],
    workflowNodeOutput: (request: GetWorkflowNodeOutputRequest) => [...WorkflowNodeOutputs.outputSchemas, request],
    outputSchemas: ['workflowNodeOutputs'] as const,
};

export const useGetWorkflowNodeOutputQuery = (request: GetWorkflowNodeOutputRequest, enabled?: boolean) =>
    useQuery<WorkflowNodeOutputModel, Error>({
        queryKey: WorkflowNodeOutputs.workflowNodeOutput(request),
        queryFn: () => new WorkflowNodeApi().getWorkflowNodeOutput(request),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetWorkflowNodeOutputsQuery = (request: GetWorkflowNodeOutputsRequest, enabled?: boolean) =>
    useQuery<WorkflowNodeOutputModel[], Error>({
        queryKey: WorkflowNodeOutputs.filteredWorkflowNodeOutputs(request),
        queryFn: () => new WorkflowNodeApi().getWorkflowNodeOutputs(request),
        enabled: enabled === undefined ? true : enabled,
    });
