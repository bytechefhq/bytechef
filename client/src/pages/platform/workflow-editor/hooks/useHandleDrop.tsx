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
import {
    ClickedDefinitionType,
    NodeDataType,
    StructureParentType,
    TaskDispatcherContextType,
    UpdateWorkflowMutationType,
} from '@/shared/types';
import {QueryClient, useQueryClient} from '@tanstack/react-query';
import {Edge, Node} from '@xyflow/react';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import calculateNodeInsertIndex from '../utils/calculateNodeInsertIndex';
import getFormattedName from '../utils/getFormattedName';
import getParametersWithDefaultValues from '../utils/getParametersWithDefaultValues';
import getTaskDispatcherContext from '../utils/getTaskDispatcherContext';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import {TASK_DISPATCHER_CONFIG} from '../utils/taskDispatcherConfig';

async function createWorkflowNodeData(
    droppedNode: ClickedDefinitionType,
    queryClient: QueryClient
): Promise<{nodeData: NodeDataType; operationName?: string}> {
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
            nodeData: {
                ...baseNodeData,
                parameters: initialParameters,
            },
            operationName: undefined,
        };
    }

    if (!baseNodeData.version || !baseNodeData.componentName) {
        return {
            nodeData: baseNodeData,
            operationName: undefined,
        };
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
            nodeData: {
                ...baseNodeData,
                parameters: {
                    ...getParametersWithDefaultValues({
                        properties: triggerDefinition.properties || [],
                    }),
                },
                type: `${baseNodeData.componentName}/v${componentDefinition.version}/${triggerName}`,
            },
            operationName: triggerName,
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
            nodeData: {
                ...baseNodeData,
                parameters: {
                    ...getParametersWithDefaultValues({
                        properties: actionDefinition.properties || [],
                    }),
                },
                type: `${baseNodeData.componentName}/v${componentDefinition.version}/${actionName}`,
            },
            operationName: actionName,
        };
    }
}

interface SaveDroppedNodeProps {
    captureComponentUsed: (name: string, actionName?: string, triggerName?: string) => void;
    nodeData: NodeDataType;
    operationName?: string;
    options?: {
        nodeIndex?: number;
        placeholderId?: string;
        taskDispatcherContext?: TaskDispatcherContextType;
    };
    queryClient: QueryClient;
    parentId: number;
    parentType: StructureParentType;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

async function saveDroppedNode({
    captureComponentUsed,
    nodeData,
    operationName,
    options,
    parentId,
    parentType,
    queryClient,
    updateWorkflowMutation,
}: SaveDroppedNodeProps) {
    if (nodeData.trigger) {
        captureComponentUsed(nodeData.componentName, undefined, operationName);
    } else if (!nodeData.taskDispatcher) {
        captureComponentUsed(nodeData.componentName, operationName, undefined);
    } else {
        captureComponentUsed(nodeData.componentName, undefined, undefined);
    }

    saveWorkflowDefinition({
        ...options,
        nodeData,
        parentId,
        parentType,
        queryClient,
        updateWorkflowMutation,
    });
}

export default function useHandleDrop({
    parentId,
    parentType,
}: {
    parentId?: number;
    parentType?: StructureParentType;
}): [
    (targetNode: Node, droppedNode: ClickedDefinitionType) => void,
    (targetEdge: Edge, droppedNode: ClickedDefinitionType) => void,
    (droppedNode: ClickedDefinitionType) => void,
] {
    const {captureComponentUsed} = useAnalytics();
    const {updateWorkflowMutation} = useWorkflowMutation();
    const queryClient = useQueryClient();

    async function handleDropOnPlaceholderNode(targetNode: Node, droppedNode: ClickedDefinitionType) {
        if (!parentId || !parentType) {
            console.error(`Parent "${parentId}" of type "${parentType}" not found.`);

            return;
        }

        const {nodeData, operationName} = await createWorkflowNodeData(droppedNode, queryClient);

        await saveDroppedNode({
            captureComponentUsed,
            nodeData,
            operationName,
            options: {
                placeholderId: targetNode.id,
                taskDispatcherContext: getTaskDispatcherContext({node: targetNode}),
            },
            parentId,
            parentType,
            queryClient,
            updateWorkflowMutation,
        });
    }

    async function handleDropOnWorkflowEdge(targetEdge: Edge, droppedNode: ClickedDefinitionType) {
        if (!parentId || !parentType) {
            console.error(`Parent "${parentId}" of type "${parentType}" not found.`);

            return;
        }

        const {nodes} = useWorkflowDataStore.getState();
        const {nodeData, operationName} = await createWorkflowNodeData(droppedNode, queryClient);

        const insertIndex = calculateNodeInsertIndex(targetEdge.target);

        await saveDroppedNode({
            captureComponentUsed,
            nodeData,
            operationName,
            options: {
                nodeIndex: insertIndex,
                taskDispatcherContext: getTaskDispatcherContext({edge: targetEdge, nodes}),
            },
            parentId,
            parentType,
            queryClient,
            updateWorkflowMutation,
        });
    }

    async function handleDropOnTriggerNode(droppedNode: ClickedDefinitionType) {
        if (!parentId || !parentType) {
            console.error(`Parent "${parentId}" of type "${parentType}" not found.`);

            return;
        }

        const {nodeData, operationName} = await createWorkflowNodeData(droppedNode, queryClient);

        await saveDroppedNode({
            captureComponentUsed,
            nodeData,
            operationName,
            parentId,
            parentType,
            queryClient,
            updateWorkflowMutation,
        });
    }

    return [handleDropOnPlaceholderNode, handleDropOnWorkflowEdge, handleDropOnTriggerNode];
}
