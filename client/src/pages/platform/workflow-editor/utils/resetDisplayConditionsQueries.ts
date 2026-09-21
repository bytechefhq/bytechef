import {WorkflowNodeParameterKeys} from '@/shared/queries/platform/workflowNodeParameters.queries';
import {QueryClient} from '@tanstack/react-query';

/**
 * Display conditions are cached per workflow node name, not per operation, so a node whose operation or version
 * changed keeps serving the previous operation's conditions. Resetting (rather than invalidating) drops the cached
 * data, so the refetched response is always a new reference even when it is structurally equal to the old one.
 */
export default function resetDisplayConditionsQueries(queryClient: QueryClient, workflowId: string): void {
    queryClient.resetQueries({
        queryKey: [...WorkflowNodeParameterKeys.workflowNodeParameters, workflowId],
    });

    queryClient.resetQueries({
        queryKey: [...WorkflowNodeParameterKeys.clusterElementParameters, workflowId],
    });
}
