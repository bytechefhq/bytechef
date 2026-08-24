import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {getTask} from '@/pages/platform/workflow-editor/utils/getTask';
import saveWorkflowDefinition from '@/pages/platform/workflow-editor/utils/saveWorkflowDefinition';
import {useCallback, useMemo} from 'react';

export const CHAT_OPERATION_NAME = 'chat';
export const STREAM_CHAT_OPERATION_NAME = 'streamChat';

// A streamed response cannot be validated against a JSON schema, so AiAgentConstants.STREAM_CHAT_PROPERTIES
// omits the structured output property that AiAgentConstants.CHAT_PROPERTIES declares. Every other property
// is shared by both actions, which is what makes the switch a toggle rather than a full action select.
const RESPONSE_PARAMETER_NAME = 'response';

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

    // The root task is read from the workflow definition rather than from rootClusterElementNodeData, which is
    // seeded once per root and never carries the prompts written by AiAgentPromptField. saveWorkflowDefinition
    // replaces a cluster root's parameters wholesale, so writing the stale copy back would wipe them.
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

            if (!rootClusterElementNodeData?.componentName || !updateWorkflowMutation) {
                return;
            }

            const componentVersion =
                Number(rootClusterElementNodeData.type?.split('/')[1]?.replace(/^v/, '')) ||
                rootClusterElementNodeData.version ||
                1;

            const parameters = {...(rootTask?.parameters ?? {})};

            if (streaming) {
                delete parameters[RESPONSE_PARAMETER_NAME];
            }

            const updatedRootClusterElementNodeData = {
                ...rootClusterElementNodeData,
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
        [rootClusterElementNodeData, rootTask?.parameters, setRootClusterElementNodeData, updateWorkflowMutation]
    );

    return {
        isStreaming: operationName === STREAM_CHAT_OPERATION_NAME,
        isStreamingSupported: operationName === CHAT_OPERATION_NAME || operationName === STREAM_CHAT_OPERATION_NAME,
        updateStreaming,
    };
}
