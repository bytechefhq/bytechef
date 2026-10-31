import {WorkflowNodeOutput, WorkflowNodeOutputApi} from '@/shared/middleware/platform/configuration';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {DEFINITION_STALE_TIME} from '@/shared/queries/queryConstants';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback} from 'react';

export function useWorkflowNodeOutputsForCompletion(
    workflowId: string | undefined,
    environmentId: number
): () => Promise<WorkflowNodeOutput[]> {
    const queryClient = useQueryClient();

    return useCallback(async () => {
        if (workflowId === undefined) {
            return [];
        }

        const request = {environmentId, id: workflowId};

        return queryClient.fetchQuery({
            queryFn: () => new WorkflowNodeOutputApi().getPreviousWorkflowNodeOutputs(request),
            queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs(request),
            staleTime: DEFINITION_STALE_TIME,
        });
    }, [environmentId, queryClient, workflowId]);
}
