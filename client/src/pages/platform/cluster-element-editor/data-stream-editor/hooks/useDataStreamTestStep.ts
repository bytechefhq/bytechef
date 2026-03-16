import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useSaveWorkflowNodeTestOutputMutation} from '@/shared/mutations/platform/workflowNodeTestOutputs.mutations';
import {
    WorkflowNodeOutputKeys,
    useGetWorkflowNodeOutputQuery,
} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo} from 'react';

export default function useDataStreamTestStep() {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);

    const workflow = useWorkflowDataStore((state) => state.workflow);

    const queryClient = useQueryClient();

    const rootWorkflowNodeName = rootClusterElementNodeData?.workflowNodeName || '';

    const invalidateNodeOutputs = useCallback(() => {
        queryClient.invalidateQueries({
            queryKey: [...WorkflowNodeOutputKeys.workflowNodeOutputs, workflow.id],
        });
    }, [queryClient, workflow.id]);

    const saveWorkflowNodeTestOutputMutation = useSaveWorkflowNodeTestOutputMutation({
        onSuccess: invalidateNodeOutputs,
    });

    const {data: workflowNodeOutput, isFetching: outputFetching} = useGetWorkflowNodeOutputQuery(
        {
            environmentId: currentEnvironmentId,
            id: workflow.id!,
            workflowNodeName: rootWorkflowNodeName,
        },
        !!workflow.id && !!rootWorkflowNodeName
    );

    const handleTestClick = useCallback(() => {
        if (!workflow.id || !rootWorkflowNodeName) {
            return;
        }

        saveWorkflowNodeTestOutputMutation.mutate({
            environmentId: currentEnvironmentId,
            id: workflow.id,
            workflowNodeName: rootWorkflowNodeName,
        });
    }, [currentEnvironmentId, rootWorkflowNodeName, saveWorkflowNodeTestOutputMutation, workflow.id]);

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
        rootWorkflowNodeName,
        sampleOutput,
        testError: saveWorkflowNodeTestOutputMutation.error,
        testing: saveWorkflowNodeTestOutputMutation.isPending,
    };
}
