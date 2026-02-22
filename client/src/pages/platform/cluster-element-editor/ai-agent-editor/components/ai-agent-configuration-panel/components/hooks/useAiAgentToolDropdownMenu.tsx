import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import handleDeleteTask from '@/pages/platform/workflow-editor/utils/handleDeleteTask';
import {NodeDataType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback} from 'react';
import {useShallow} from 'zustand/shallow';

import {ToolItemI} from './useAiAgentTools';

interface UseAiAgentToolDropdownMenuI {
    handleConfigureTool: (tool: ToolItemI) => Promise<void>;
    handleRemoveTool: (tool: ToolItemI) => void;
}

export default function useAiAgentToolDropdownMenu(): UseAiAgentToolDropdownMenuI {
    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);
    const setRootClusterElementNodeData = useWorkflowEditorStore((state) => state.setRootClusterElementNodeData);
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const {setActiveTab, setCurrentComponent, setCurrentNode, setWorkflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore(
            useShallow((state) => ({
                setActiveTab: state.setActiveTab,
                setCurrentComponent: state.setCurrentComponent,
                setCurrentNode: state.setCurrentNode,
                setWorkflowNodeDetailsPanelOpen: state.setWorkflowNodeDetailsPanelOpen,
            }))
        );

    const {invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();
    const queryClient = useQueryClient();

    const handleConfigureTool = useCallback(
        async (tool: ToolItemI) => {
            const toolNodeData: NodeDataType = {
                clusterElementName: tool.operationName,
                clusterElementType: 'tools',
                componentName: tool.componentName,
                label: tool.label,
                name: tool.name,
                operationName: tool.operationName,
                parentClusterRootId: rootClusterElementNodeData?.name,
                type: tool.type,
                version: tool.componentVersion,
                workflowNodeName: tool.name,
            };

            setActiveTab('description');
            setCurrentNode({...toolNodeData, description: ''});
            setWorkflowNodeDetailsPanelOpen(true);

            setCurrentComponent((previousCurrentComponent) => ({
                ...toolNodeData,
                displayConditions: previousCurrentComponent?.displayConditions,
                workflowNodeName: tool.name,
            }));
        },
        [
            rootClusterElementNodeData?.name,
            setActiveTab,
            setCurrentComponent,
            setCurrentNode,
            setWorkflowNodeDetailsPanelOpen,
        ]
    );

    const handleRemoveTool = useCallback(
        (tool: ToolItemI) => {
            if (!rootClusterElementNodeData) {
                return;
            }

            const toolNodeData: NodeDataType = {
                clusterElementType: 'tools',
                componentName: tool.componentName,
                name: tool.name,
                workflowNodeName: tool.name,
            };

            handleDeleteTask({
                clusterElementsCanvasOpen: true,
                data: toolNodeData,
                invalidateWorkflowQueries,
                queryClient,
                rootClusterElementNodeData,
                setCurrentNode,
                setRootClusterElementNodeData,
                updateWorkflowMutation,
                workflow,
            });
        },
        [
            invalidateWorkflowQueries,
            queryClient,
            rootClusterElementNodeData,
            setCurrentNode,
            setRootClusterElementNodeData,
            updateWorkflowMutation,
            workflow,
        ]
    );

    return {
        handleConfigureTool,
        handleRemoveTool,
    };
}
