import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {getTask} from '@/pages/platform/workflow-editor/utils/getTask';
import saveWorkflowDefinition from '@/pages/platform/workflow-editor/utils/saveWorkflowDefinition';
import {useCallback, useMemo} from 'react';

export const CHAT_OPERATION_NAME = 'chat';
export const STREAM_CHAT_OPERATION_NAME = 'streamChat';

interface UseAiAgentStreamResponseI {
    isStreaming: boolean;
    isStreamingSupported: boolean;
    updateStreaming: (streaming: boolean) => void;
}

export default function useAiAgentStreamResponse(): UseAiAgentStreamResponseI {
    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);
    const setRootClusterElementNodeData = useWorkflowEditorStore((state) => state.setRootClusterElementNodeData);
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const {updateWorkflowMutation} = useWorkflowEditor();

    const rootTask = useMemo(() => {
        if (!workflow.definition || !rootClusterElementNodeData?.workflowNodeName) {
            return undefined;
        }

        try {
            const definition = JSON.parse(workflow.definition);

            return getTask({
                tasks: definition.tasks ?? [],
                workflowNodeName: rootClusterElementNodeData.workflowNodeName,
            });
        } catch {
            return undefined;
        }
    }, [rootClusterElementNodeData?.workflowNodeName, workflow.definition]);

    const operationName =
        rootTask?.type?.split('/')[2] ||
        rootClusterElementNodeData?.operationName ||
        rootClusterElementNodeData?.type?.split('/')[2] ||
        '';

    const updateStreaming = useCallback(
        (streaming: boolean) => {
            const targetOperationName = streaming ? STREAM_CHAT_OPERATION_NAME : CHAT_OPERATION_NAME;

            if (!rootClusterElementNodeData?.componentName || !updateWorkflowMutation || !rootTask) {
                return;
            }

            const componentVersion =
                Number(rootClusterElementNodeData.type?.split('/')[1]?.replace(/^v/, '')) ||
                rootClusterElementNodeData.version ||
                1;

            const parameters = {...(rootTask.parameters ?? {})};

            const updatedRootClusterElementNodeData = {
                ...rootClusterElementNodeData,
                clusterElements: rootTask.clusterElements,
                operationName: targetOperationName,
                parameters,
                type: `${rootClusterElementNodeData.componentName}/v${componentVersion}/${targetOperationName}`,
            };

            setRootClusterElementNodeData(updatedRootClusterElementNodeData);

            saveWorkflowDefinition({
                nodeData: updatedRootClusterElementNodeData,
                updateWorkflowMutation,
            });
        },
        [rootClusterElementNodeData, rootTask, setRootClusterElementNodeData, updateWorkflowMutation]
    );

    return {
        isStreaming: operationName === STREAM_CHAT_OPERATION_NAME,
        isStreamingSupported: operationName === CHAT_OPERATION_NAME || operationName === STREAM_CHAT_OPERATION_NAME,
        updateStreaming,
    };
}
