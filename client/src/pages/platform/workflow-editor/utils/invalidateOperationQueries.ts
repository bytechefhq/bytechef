import {
    ClusterElementDynamicPropertyKeys,
    WorkflowNodeDynamicPropertyKeys,
} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {QueryClient} from '@tanstack/react-query';

export default function invalidateOperationQueries(queryClient: QueryClient): void {
    void queryClient.invalidateQueries({
        queryKey: WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties,
    });

    void queryClient.invalidateQueries({
        queryKey: ClusterElementDynamicPropertyKeys.clusterElementDynamicProperties,
    });

    void queryClient.invalidateQueries({
        queryKey: WorkflowNodeOptionKeys.workflowNodeOptions,
    });

    void queryClient.invalidateQueries({
        queryKey: WorkflowNodeOptionKeys.clusterElementNodeOptions,
    });
}
