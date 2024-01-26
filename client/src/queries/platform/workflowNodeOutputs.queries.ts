import {
    GetWorkflowNodeOutputRequest,
    GetWorkflowNodeOutputsRequest,
    WorkflowNodeOutputApi,
    WorkflowNodeOutputModel,
} from '@/middleware/platform/configuration';

/* eslint-disable sort-keys */

import {useQuery} from '@tanstack/react-query';

export const WorkflowStepOutputs = {
    filteredWorkflowStepOutputs: (request: GetWorkflowNodeOutputsRequest) => [
        ...WorkflowStepOutputs.outputSchemas,
        request,
    ],
    workflowStepOutput: (request: GetWorkflowNodeOutputRequest) => [...WorkflowStepOutputs.outputSchemas, request],
    outputSchemas: ['workflowStepOutputs'] as const,
};

export const useGetWorkflowNodeOutputQuery = (request: GetWorkflowNodeOutputRequest, enabled?: boolean) =>
    useQuery<WorkflowNodeOutputModel, Error>({
        queryKey: WorkflowStepOutputs.workflowStepOutput(request),
        queryFn: () => new WorkflowNodeOutputApi().getWorkflowNodeOutput(request),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetWorkflowNodeOutputsQuery = (request: GetWorkflowNodeOutputsRequest, enabled?: boolean) =>
    useQuery<WorkflowNodeOutputModel[], Error>({
        queryKey: WorkflowStepOutputs.filteredWorkflowStepOutputs(request),
        queryFn: () => new WorkflowNodeOutputApi().getWorkflowNodeOutputs(request),
        enabled: enabled === undefined ? true : enabled,
    });
