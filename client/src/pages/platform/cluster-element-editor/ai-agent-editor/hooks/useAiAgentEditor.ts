import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ComponentDefinitionBasic, WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {useCallback} from 'react';

interface UseAiAgentEditorPropsI {
    previousComponentDefinitions?: ComponentDefinitionBasic[];
    workflowNodeOutputs?: WorkflowNodeOutput[];
}

interface UseAiAgentEditorI {
    handleNodeDetailsPanelClose: () => void;
    showNodeDetailsPanel: boolean;
    updateWorkflowMutation: ReturnType<typeof useWorkflowEditor>['updateWorkflowMutation'];
}

export default function useAiAgentEditor({
    previousComponentDefinitions,
    workflowNodeOutputs,
}: UseAiAgentEditorPropsI): UseAiAgentEditorI {
    const aiAgentNodeDetailsPanelOpen = useWorkflowNodeDetailsPanelStore((state) => state.aiAgentNodeDetailsPanelOpen);
    const currentNodeClusterElementType = useWorkflowNodeDetailsPanelStore(
        (state) => state.currentNode?.clusterElementType
    );

    const {updateWorkflowMutation} = useWorkflowEditor();

    const handleNodeDetailsPanelClose = useCallback(() => {
        useWorkflowNodeDetailsPanelStore.getState().setAiAgentNodeDetailsPanelOpen(false);
    }, []);

    const showNodeDetailsPanel =
        aiAgentNodeDetailsPanelOpen &&
        (currentNodeClusterElementType === 'tools' || currentNodeClusterElementType === 'model') &&
        !!previousComponentDefinitions &&
        !!workflowNodeOutputs;

    return {
        handleNodeDetailsPanelClose,
        showNodeDetailsPanel,
        updateWorkflowMutation,
    };
}
