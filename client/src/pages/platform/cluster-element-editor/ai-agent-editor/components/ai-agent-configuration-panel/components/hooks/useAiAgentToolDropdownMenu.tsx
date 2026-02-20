import {getClusterElementByName} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {getTask} from '@/pages/platform/workflow-editor/utils/getTask';
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

    const {setActiveTab, setAiAgentNodeDetailsPanelOpen, setCurrentComponent, setCurrentNode} =
        useWorkflowNodeDetailsPanelStore(
            useShallow((state) => ({
                setActiveTab: state.setActiveTab,
                setAiAgentNodeDetailsPanelOpen: state.setAiAgentNodeDetailsPanelOpen,
                setCurrentComponent: state.setCurrentComponent,
                setCurrentNode: state.setCurrentNode,
            }))
        );

    const {invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();
    const queryClient = useQueryClient();

    const handleConfigureTool = useCallback(
        async (tool: ToolItemI) => {
            let toolMetadata: NodeDataType['metadata'] | undefined;
            let toolParameters: NodeDataType['parameters'] | undefined;

            if (rootClusterElementNodeData?.workflowNodeName && workflow.definition) {
                const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

                const mainClusterRootTask = getTask({
                    tasks: workflowDefinitionTasks,
                    workflowNodeName: rootClusterElementNodeData.workflowNodeName,
                });

                if (mainClusterRootTask?.clusterElements) {
                    const clusterElement = getClusterElementByName(mainClusterRootTask.clusterElements, tool.name);

                    if (clusterElement) {
                        toolMetadata = clusterElement.metadata;
                        toolParameters = clusterElement.parameters;
                    }
                }
            }

            const toolNodeData: NodeDataType = {
                clusterElementName: tool.operationName,
                clusterElementType: 'tools',
                componentName: tool.componentName,
                label: tool.label,
                metadata: toolMetadata,
                name: tool.name,
                operationName: tool.operationName,
                parameters: toolParameters,
                parentClusterRootId: rootClusterElementNodeData?.name,
                type: tool.type,
                version: tool.componentVersion,
                workflowNodeName: tool.name,
            };

            setActiveTab('description');
            setCurrentNode({...toolNodeData, description: ''});
            setAiAgentNodeDetailsPanelOpen(true);

            setCurrentComponent((previousCurrentComponent) => ({
                ...toolNodeData,
                displayConditions: previousCurrentComponent?.displayConditions,
                workflowNodeName: tool.name,
            }));
        },
        [
            rootClusterElementNodeData?.name,
            rootClusterElementNodeData?.workflowNodeName,
            setActiveTab,
            setAiAgentNodeDetailsPanelOpen,
            setCurrentComponent,
            setCurrentNode,
            workflow.definition,
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
