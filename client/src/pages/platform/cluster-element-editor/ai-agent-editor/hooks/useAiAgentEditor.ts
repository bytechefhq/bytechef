import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ComponentDefinitionBasic, WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useCallback, useEffect} from 'react';
import {useShallow} from 'zustand/shallow';

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

    const {rootClusterElementNodeData, setMainClusterRootComponentDefinition} = useWorkflowEditorStore(
        useShallow((state) => ({
            rootClusterElementNodeData: state.rootClusterElementNodeData,
            setMainClusterRootComponentDefinition: state.setMainClusterRootComponentDefinition,
        }))
    );

    // Seed mainClusterRootComponentDefinition so saveClusterElementToWorkflow does
    // not early-return when a tool or model is added from the simple AI Agent
    // editor, where useClusterElementsLayout (the canvas hook that otherwise sets
    // this) does not run.
    const rootComponentVersion =
        Number(rootClusterElementNodeData?.type?.split('/')[1]?.replace(/^v/, '')) ||
        (rootClusterElementNodeData?.version as number | undefined) ||
        1;

    const {data: rootClusterElementDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: rootClusterElementNodeData?.componentName || '',
            componentVersion: rootComponentVersion,
        },
        !!rootClusterElementNodeData?.componentName
    );

    useEffect(() => {
        if (rootClusterElementDefinition && rootClusterElementNodeData?.workflowNodeName) {
            setMainClusterRootComponentDefinition(rootClusterElementDefinition);
        }
    }, [
        rootClusterElementDefinition,
        rootClusterElementNodeData?.workflowNodeName,
        setMainClusterRootComponentDefinition,
    ]);

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
