/* eslint-disable sort-keys */
import {
    GetClusterElementOutputRequest,
    GetPreviousWorkflowNodeOutputsRequest,
    GetWorkflowNodeOutputRequest,
    WorkflowNodeOutput,
    WorkflowNodeOutputApi,
} from '@/shared/middleware/platform/configuration';
import {DEFINITION_STALE_TIME} from '@/shared/queries/queryConstants';
import {QueryClient, useQuery} from '@tanstack/react-query';

export const WorkflowNodeOutputKeys = {
    clusterElementOutput: (request: GetClusterElementOutputRequest) => [
        ...WorkflowNodeOutputKeys.workflowNodeOutputs,
        request.id,
        request.workflowNodeName,
        'clusterElement',
        request.clusterElementType,
        request.clusterElementWorkflowNodeName,
        request.environmentId,
    ],
    filteredPreviousWorkflowNodeOutputs: (request: GetPreviousWorkflowNodeOutputsRequest) => [
        ...WorkflowNodeOutputKeys.workflowNodeOutputs,
        request.id,
        'previousWorkflowNodeOutputs',
        request.lastWorkflowNodeName,
        request.environmentId,
    ],
    workflowNodeOutput: (request: GetWorkflowNodeOutputRequest) => [
        ...WorkflowNodeOutputKeys.workflowNodeOutputs,
        request.id,
        request.workflowNodeName,
        request.environmentId,
    ],
    workflowNodeOutputs: ['workflowNodeOutputs'] as const,
};

/** Drops all cached previous-node-output entries for a workflow (see query key shape). */
export function invalidatePreviousWorkflowNodeOutputsForWorkflow(queryClient: QueryClient, workflowId: string): void {
    queryClient.invalidateQueries({
        queryKey: [...WorkflowNodeOutputKeys.workflowNodeOutputs, workflowId, 'previousWorkflowNodeOutputs'],
    });
}

export const useGetClusterElementOutputQuery = (request: GetClusterElementOutputRequest, enabled?: boolean) =>
    useQuery<WorkflowNodeOutput, Error>({
        queryKey: WorkflowNodeOutputKeys.clusterElementOutput(request),
        queryFn: () => new WorkflowNodeOutputApi().getClusterElementOutput(request),
        enabled: enabled ?? true,
        staleTime: DEFINITION_STALE_TIME,
    });

export const useGetWorkflowNodeOutputQuery = (request: GetWorkflowNodeOutputRequest, enabled?: boolean) =>
    useQuery<WorkflowNodeOutput, Error>({
        queryKey: WorkflowNodeOutputKeys.workflowNodeOutput(request),
        queryFn: () => new WorkflowNodeOutputApi().getWorkflowNodeOutput(request),
        enabled: enabled ?? true,
        staleTime: DEFINITION_STALE_TIME,
    });

export const useGetPreviousWorkflowNodeOutputsQuery = (
    request: GetPreviousWorkflowNodeOutputsRequest,
    enabled?: boolean
) =>
    useQuery<WorkflowNodeOutput[], Error>({
        queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs(request),
        queryFn: () => new WorkflowNodeOutputApi().getPreviousWorkflowNodeOutputs(request),
        enabled: enabled ?? true,
        staleTime: DEFINITION_STALE_TIME,
    });
