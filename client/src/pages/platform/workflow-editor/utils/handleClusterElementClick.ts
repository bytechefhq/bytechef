import {ClusterElementDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {ClusterElementsType, UpdateWorkflowMutationType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';
import {Node} from '@xyflow/react';

import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import getFormattedClusterElementName from './getFormattedClusterElementName';
import saveWorkflowDefinition from './saveWorkflowDefinition';

type ClusterElementsDefinitionType = 'CHAT_MEMORY' | 'MODEL' | 'RAG';
type StoredClusterElementsType = 'chatMemory' | 'model' | 'rag';

interface ClusterElementClickProps {
    clusterElementsData: ClusterElementsType | undefined;
    data: ClusterElementDefinitionBasic;
    projectId: string | undefined;
    queryClient: QueryClient;
    setPopoverOpen: (open: boolean) => void;
    sourceNode: Node | undefined;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

export default function handleClusterElementClick({
    clusterElementsData,
    data,
    projectId,
    queryClient,
    setPopoverOpen,
    sourceNode,
    updateWorkflowMutation,
}: ClusterElementClickProps) {
    const {aiAgentNodeData, setAiAgentNodeData} = useWorkflowEditorStore.getState();
    const {currentNode, setCurrentNode} = useWorkflowNodeDetailsPanelStore.getState();

    if (!clusterElementsData || !sourceNode) {
        return;
    }

    const updatedClusterElementsData: ClusterElementsType = {
        rag: clusterElementsData.rag
            ? {
                  label: clusterElementsData.rag.label,
                  name: clusterElementsData.rag.name,
                  parameters: clusterElementsData.rag.parameters || {},
                  type: clusterElementsData.rag.type,
              }
            : null,
        // eslint-disable-next-line sort-keys
        chatMemory: clusterElementsData.chatMemory
            ? {
                  label: clusterElementsData.chatMemory.label,
                  name: clusterElementsData.chatMemory.name,
                  parameters: clusterElementsData.chatMemory.parameters || {},
                  type: clusterElementsData.chatMemory.type,
              }
            : null,

        model: clusterElementsData.model
            ? {
                  label: clusterElementsData.model.label,
                  name: clusterElementsData.model.name,
                  parameters: clusterElementsData.model.parameters || {},
                  type: clusterElementsData.model.type,
              }
            : null,

        tools: clusterElementsData.tools
            ? clusterElementsData.tools.map((tool) => ({
                  label: tool.label,
                  name: tool.name,
                  parameters: tool.parameters || {},
                  type: tool.type,
              }))
            : [],
    };

    const propertyMap: Record<ClusterElementsDefinitionType, StoredClusterElementsType> = {
        CHAT_MEMORY: 'chatMemory',
        MODEL: 'model',
        RAG: 'rag',
    };

    if (data.type === 'TOOLS') {
        updatedClusterElementsData.tools = [
            ...(updatedClusterElementsData.tools || []),
            {
                label: data.title,
                name: getFormattedClusterElementName(data.name, 'tools'),
                parameters: {},
                type: `${data.componentName}/v${data.componentVersion}/${data.name}`,
            },
        ];
    } else if (data.type in propertyMap) {
        updatedClusterElementsData[propertyMap[data.type as ClusterElementsDefinitionType]] = {
            label: data.title,
            name: getFormattedClusterElementName(
                data.componentName,
                propertyMap[data.type as ClusterElementsDefinitionType]
            ),
            parameters: {},
            type: `${data.componentName}/v${data.componentVersion}/${propertyMap[data.type as ClusterElementsDefinitionType]}`,
        };
    }

    saveWorkflowDefinition({
        nodeData: {
            ...sourceNode.data,
            clusterElements: updatedClusterElementsData,
            componentName: String(sourceNode.data.componentName),
            name: String(sourceNode.data.name),
            workflowNodeName: String(sourceNode.data.workflowNodeName),
        },
        onSuccess: () => {
            setPopoverOpen(false);
            setAiAgentNodeData({
                ...aiAgentNodeData,
                clusterElements: updatedClusterElementsData,
                componentName: aiAgentNodeData?.componentName as string,
                name: aiAgentNodeData?.name as string,
                workflowNodeName: aiAgentNodeData?.workflowNodeName as string,
            });
            setCurrentNode({
                ...currentNode,
                clusterElements: updatedClusterElementsData,
                componentName: currentNode?.componentName as string,
                name: currentNode?.name as string,
                workflowNodeName: currentNode?.workflowNodeName as string,
            });
        },
        projectId: +projectId!,
        queryClient,
        updateWorkflowMutation,
    });
}
