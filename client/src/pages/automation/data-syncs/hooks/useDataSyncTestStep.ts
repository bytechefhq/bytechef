import {DATA_SYNC_TASK_NODE_NAME} from '@/pages/automation/data-syncs/utils/dataSyncElements';
import {useSaveWorkflowNodeTestOutputMutation} from '@/shared/mutations/platform/workflowNodeTestOutputs.mutations';
import {
    WorkflowNodeOutputKeys,
    useGetWorkflowNodeOutputQuery,
} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo} from 'react';

interface UseDataSyncTestStepProps {
    draftWorkflowId: string;
}

export default function useDataSyncTestStep({draftWorkflowId}: UseDataSyncTestStepProps) {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const invalidateNodeOutputs = useCallback(() => {
        queryClient.invalidateQueries({
            queryKey: [...WorkflowNodeOutputKeys.workflowNodeOutputs, draftWorkflowId],
        });
    }, [draftWorkflowId, queryClient]);

    const saveWorkflowNodeTestOutputMutation = useSaveWorkflowNodeTestOutputMutation({
        onSuccess: invalidateNodeOutputs,
    });

    const {data: workflowNodeOutput, isFetching: outputFetching} = useGetWorkflowNodeOutputQuery(
        {
            environmentId: currentEnvironmentId,
            id: draftWorkflowId,
            workflowNodeName: DATA_SYNC_TASK_NODE_NAME,
        },
        !!draftWorkflowId
    );

    const handleTestClick = useCallback(() => {
        if (!draftWorkflowId) {
            return;
        }

        saveWorkflowNodeTestOutputMutation.mutate({
            environmentId: currentEnvironmentId,
            id: draftWorkflowId,
            workflowNodeName: DATA_SYNC_TASK_NODE_NAME,
        });
    }, [currentEnvironmentId, draftWorkflowId, saveWorkflowNodeTestOutputMutation]);

    const {outputSchema, sampleOutput} =
        workflowNodeOutput?.outputResponse || workflowNodeOutput?.variableOutputResponse || {};

    const hasItems = useMemo(
        () => Boolean(outputSchema && 'items' in outputSchema && outputSchema.items),
        [outputSchema]
    );

    const hasProperties = useMemo(
        () => Boolean(outputSchema && 'properties' in outputSchema && outputSchema.properties),
        [outputSchema]
    );

    return {
        handleTestClick,
        hasItems,
        hasProperties,
        outputFetching,
        outputSchema,
        rootWorkflowNodeName: DATA_SYNC_TASK_NODE_NAME,
        sampleOutput,
        testError: saveWorkflowNodeTestOutputMutation.error,
        testing: saveWorkflowNodeTestOutputMutation.isPending,
    };
}
