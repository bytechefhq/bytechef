import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {
    ActionDefinitionApi,
    ComponentDefinitionApi,
    TriggerDefinitionApi,
} from '@/shared/middleware/platform/configuration';
import {ActionDefinitionKeys} from '@/shared/queries/platform/actionDefinitions.queries';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {TriggerDefinitionKeys} from '@/shared/queries/platform/triggerDefinitions.queries';
import {ClickedDefinitionType, NodeDataType, UpdateWorkflowMutationType} from '@/shared/types';
import {getRandomId} from '@/shared/util/random-utils';
import {QueryClient, useQueryClient} from '@tanstack/react-query';
import {Edge, Node} from '@xyflow/react';
import {useParams} from 'react-router-dom';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import getFormattedName from '../utils/getFormattedName';
import getParametersWithDefaultValues from '../utils/getParametersWithDefaultValues';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import {TASK_DISPATCHER_CONFIG} from '../utils/taskDispatcherConfig';

async function createWorkflowNodeData(
    droppedNode: ClickedDefinitionType,
    queryClient: QueryClient
): Promise<NodeDataType> {
    const baseNodeData: NodeDataType = {
        componentName: droppedNode.name!,
        label: droppedNode.title,
        name: droppedNode.trigger ? 'trigger_1' : getFormattedName(droppedNode.name!),
        taskDispatcher: droppedNode.taskDispatcher,
        title: droppedNode?.title,
        trigger: droppedNode.trigger,
        version: droppedNode.version,
        workflowNodeName: droppedNode.trigger ? 'trigger_1' : getFormattedName(droppedNode.name!),
    };

    if (baseNodeData.taskDispatcher) {
        const initialParameters = TASK_DISPATCHER_CONFIG[
            baseNodeData.componentName as keyof typeof TASK_DISPATCHER_CONFIG
        ]?.getInitialParameters([]);

        return {
            ...baseNodeData,
            parameters: initialParameters,
        };
    }

    if (!baseNodeData.version || !baseNodeData.componentName) {
        return baseNodeData;
    }

    const getComponentDefinitionRequest = {
        componentName: baseNodeData.componentName,
        componentVersion: baseNodeData.version,
    };

    const componentDefinition = await queryClient.fetchQuery({
        queryFn: () => new ComponentDefinitionApi().getComponentDefinition(getComponentDefinitionRequest),
        queryKey: ComponentDefinitionKeys.componentDefinition(getComponentDefinitionRequest),
    });

    if (baseNodeData.trigger) {
        const triggerName = componentDefinition.triggers?.[0].name as string;

        const getTriggerDefinitionRequest = {
            componentName: baseNodeData.componentName,
            componentVersion: componentDefinition.version,
            triggerName,
        };

        const triggerDefinition = await queryClient.fetchQuery({
            queryFn: () => new TriggerDefinitionApi().getComponentTriggerDefinition(getTriggerDefinitionRequest),
            queryKey: TriggerDefinitionKeys.triggerDefinition(getTriggerDefinitionRequest),
        });

        return {
            ...baseNodeData,
            parameters: {
                ...getParametersWithDefaultValues({
                    properties: triggerDefinition.properties || [],
                }),
            },
            type: `${baseNodeData.componentName}/v${componentDefinition.version}/${triggerName}`,
        };
    } else {
        const actionName = componentDefinition.actions?.[0].name as string;

        const getActionDefinitionRequest = {
            actionName,
            componentName: baseNodeData.componentName,
            componentVersion: componentDefinition.version,
        };

        const actionDefinition = await queryClient.fetchQuery({
            queryFn: () => new ActionDefinitionApi().getComponentActionDefinition(getActionDefinitionRequest),
            queryKey: ActionDefinitionKeys.actionDefinition(getActionDefinitionRequest),
        });

        return {
            ...baseNodeData,
            parameters: {
                ...getParametersWithDefaultValues({
                    properties: actionDefinition.properties || [],
                }),
            },
            type: `${baseNodeData.componentName}/v${componentDefinition.version}/${actionName}`,
        };
    }
}

async function getFirstActionName(nodeData: NodeDataType, queryClient: QueryClient) {
    if (nodeData.taskDispatcher || !nodeData.version || !nodeData.componentName) {
        return undefined;
    }

    const getComponentDefinitionRequest = {
        componentName: nodeData.componentName,
        componentVersion: nodeData.version,
    };

    const componentDefinition = await queryClient.fetchQuery({
        queryFn: () => new ComponentDefinitionApi().getComponentDefinition(getComponentDefinitionRequest),
        queryKey: ComponentDefinitionKeys.componentDefinition(getComponentDefinitionRequest),
    });

    if (nodeData.trigger) {
        return componentDefinition.triggers?.[0].name;
    }

    return componentDefinition.actions?.[0].name;
}

interface SaveDroppedNodeProps {
    captureComponentUsed: (name: string, actionName?: string, triggerName?: string) => void;
    nodeData: NodeDataType;
    options?: {
        nodeIndex?: number;
        placeholderId?: string;
    };
    queryClient: QueryClient;
    projectId: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

async function saveDroppedNode({
    captureComponentUsed,
    nodeData,
    options,
    projectId,
    queryClient,
    updateWorkflowMutation,
}: SaveDroppedNodeProps) {
    captureComponentUsed(
        nodeData.componentName,
        !nodeData.taskDispatcher ? await getFirstActionName(nodeData, queryClient) : undefined
    );

    saveWorkflowDefinition({
        nodeData,
        nodeIndex: options?.nodeIndex,
        placeholderId: options?.placeholderId,
        projectId: parseInt(projectId)!,
        queryClient,
        updateWorkflowMutation,
    });
}

export default function useHandleDrop(): [
    (targetNode: Node, droppedNode: ClickedDefinitionType) => void,
    (targetEdge: Edge, droppedNode: ClickedDefinitionType) => void,
    (droppedNode: ClickedDefinitionType) => void,
] {
    const newNodeId = getRandomId();

    const {setEdges} = useWorkflowDataStore();
    const {captureComponentUsed} = useAnalytics();
    const {updateWorkflowMutation} = useWorkflowMutation();
    const queryClient = useQueryClient();
    const {projectId} = useParams();

    async function handleDropOnPlaceholderNode(targetNode: Node, droppedNode: ClickedDefinitionType) {
        const {edges} = useWorkflowDataStore.getState();

        const sourceEdge = edges.find((edge) => edge.target === targetNode.id);

        if (!sourceEdge) {
            return;
        }

        const newWorkflowEdge = {
            id: `${sourceEdge.source}=>${targetNode.id}`,
            source: sourceEdge.source,
            target: targetNode.id,
            type: 'workflow',
        };

        const newPlaceholderEdge = {
            id: `${targetNode.id}=>${newNodeId}`,
            source: targetNode.id,
            target: newNodeId,
            type: 'placeholder',
        };

        const edgeIndex = edges.findIndex((edge) => edge.id === sourceEdge?.id);

        edges[edgeIndex] = newWorkflowEdge;

        setEdges([...edges, newPlaceholderEdge]);

        const nodeData = await createWorkflowNodeData(droppedNode, queryClient);

        if (!projectId) {
            return;
        }

        await saveDroppedNode({
            captureComponentUsed,
            nodeData,
            options: {
                placeholderId: newNodeId,
            },
            projectId,
            queryClient,
            updateWorkflowMutation,
        });
    }

    async function handleDropOnWorkflowEdge(targetEdge: Edge, droppedNode: ClickedDefinitionType) {
        const {edges, nodes} = useWorkflowDataStore.getState();

        const previousNode = nodes.find((node) => node.id === targetEdge.source);
        const nextNode = nodes.find((node) => node.id === targetEdge.target);

        if (!nextNode || !previousNode) {
            return;
        }

        const targetEdgeIndex = edges.findIndex((edge) => edge.id === targetEdge.id);

        const nodeData = await createWorkflowNodeData(droppedNode, queryClient);

        const newWorkflowNode = {
            data: nodeData,
            id: nodeData.workflowNodeName,
            name: droppedNode.name,
            position: {
                x: 0,
                y: 0,
            },
            type: 'workflow',
        };

        const newWorkflowEdge = {
            id: `${newWorkflowNode.id}=>${nextNode.id}`,
            source: newWorkflowNode.id,
            target: nextNode.id,
            type: 'workflow',
        };

        edges[targetEdgeIndex] = {
            ...targetEdge,
            id: `${previousNode.id}=>${newWorkflowNode.id}`,
            target: newWorkflowNode.id,
        };

        edges.splice(targetEdgeIndex, 0, newWorkflowEdge);

        setEdges(edges);

        if (!projectId) {
            return;
        }

        await saveDroppedNode({
            captureComponentUsed,
            nodeData,
            options: {
                nodeIndex: targetEdgeIndex,
            },
            projectId,
            queryClient,
            updateWorkflowMutation,
        });
    }

    async function handleDropOnTriggerNode(droppedNode: ClickedDefinitionType) {
        const nodeData = await createWorkflowNodeData(droppedNode, queryClient);

        if (!projectId) {
            return;
        }

        saveDroppedNode({
            captureComponentUsed,
            nodeData,
            projectId: projectId!,
            queryClient,
            updateWorkflowMutation,
        });
    }

    return [handleDropOnPlaceholderNode, handleDropOnWorkflowEdge, handleDropOnTriggerNode];
}
