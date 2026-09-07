import {
    ClusterElementDynamicPropertyKeys,
    WorkflowNodeDynamicPropertyKeys,
} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {QueryClient} from '@tanstack/react-query';

export default function invalidateOperationQueries(queryClient: QueryClient): void {
    queryClient.invalidateQueries({
        queryKey: WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties,
    });

    queryClient.invalidateQueries({
        queryKey: ClusterElementDynamicPropertyKeys.clusterElementDynamicProperties,
    });

    queryClient.invalidateQueries({
        queryKey: WorkflowNodeOptionKeys.workflowNodeOptions,
    });

    queryClient.invalidateQueries({
        queryKey: WorkflowNodeOptionKeys.clusterElementNodeOptions,
    });
}
