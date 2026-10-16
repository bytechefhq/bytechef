import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {
    ActionDefinitionApi,
    ComponentDefinitionApi,
    TaskDispatcherDefinition,
    TriggerDefinitionApi,
} from '@/shared/middleware/platform/configuration';
import {ActionDefinitionKeys} from '@/shared/queries/platform/actionDefinitions.queries';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {TriggerDefinitionKeys} from '@/shared/queries/platform/triggerDefinitions.queries';
import {invalidatePreviousWorkflowNodeOutputsForWorkflow} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {DEFINITION_STALE_TIME} from '@/shared/queries/queryConstants';
import {
    ClickedDefinitionType,
    NodeDataType,
    PropertyAllType,
    TaskDispatcherContextType,
    UpdateWorkflowMutationType,
} from '@/shared/types';
import {QueryClient, useQueryClient} from '@tanstack/react-query';
import {Edge, Node} from '@xyflow/react';

import {convertNameToSnakeCase} from '../../cluster-element-editor/utils/clusterElementsUtils';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import addClusterElementToClusterRoot from '../utils/addClusterElementToClusterRoot';
import calculateNodeInsertIndex from '../utils/calculateNodeInsertIndex';
import getFormattedName from '../utils/getFormattedName';
import getParametersWithDefaultValues from '../utils/getParametersWithDefaultValues';
import getTaskDispatcherContext from '../utils/getTaskDispatcherContext';
import {resolveClusterRootId} from '../utils/resolveClusterRootId';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import {TASK_DISPATCHER_CONFIG} from '../utils/taskDispatcherConfig';

async function createWorkflowNodeData(
    droppedNode: ClickedDefinitionType,
    queryClient: QueryClient,
    taskDispatcherDefinitions: TaskDispatcherDefinition[],
    targetTriggerName?: string
): Promise<{nodeData: NodeDataType; operationName?: string}> {
    const triggerName = droppedNode.trigger ? (targetTriggerName ?? getFormattedName('trigger')) : undefined;

    const baseNodeData: NodeDataType = {
        componentName: droppedNode.name!,
        label: droppedNode.title,
        name: droppedNode.trigger ? triggerName! : getFormattedName(droppedNode.name!),
        taskDispatcher: droppedNode.taskDispatcher,
        title: droppedNode?.title,
        trigger: droppedNode.trigger,
        version: droppedNode.version,
        workflowNodeName: droppedNode.trigger ? triggerName! : getFormattedName(droppedNode.name!),
    };

    if (baseNodeData.taskDispatcher) {
        const taskDispatcherDefinition = taskDispatcherDefinitions.find(
            (definition) => definition.name === baseNodeData.componentName
        );

        const initialParameters = TASK_DISPATCHER_CONFIG[
            baseNodeData.componentName as keyof typeof TASK_DISPATCHER_CONFIG
        ]?.getInitialParameters((taskDispatcherDefinition?.properties as Array<PropertyAllType>) || []);

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
        staleTime: DEFINITION_STALE_TIME,
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
            staleTime: DEFINITION_STALE_TIME,
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
            staleTime: DEFINITION_STALE_TIME,
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
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

async function saveDroppedNode({
    captureComponentUsed,
    nodeData,
    operationName,
    options,
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

    const workflowId = useWorkflowDataStore.getState().workflow.id;

    saveWorkflowDefinition({
        ...options,
        nodeData,
        onSuccess: () => {
            if (workflowId) {
                invalidatePreviousWorkflowNodeOutputsForWorkflow(queryClient, workflowId);
            }
        },
        updateWorkflowMutation,
    });
}

export default function useHandleDrop({
    taskDispatcherDefinitions,
}: {
    taskDispatcherDefinitions: TaskDispatcherDefinition[];
}): [
    (targetNode: Node, droppedNode: ClickedDefinitionType) => void,
    (targetEdge: Edge, droppedNode: ClickedDefinitionType) => void,
    (droppedNode: ClickedDefinitionType, targetTriggerName: string) => void,
    (droppedNode: ClickedDefinitionType) => void,
    (targetNode: Node, droppedNode: ClickedDefinitionType) => Promise<void>,
] {
    const {captureComponentUsed} = useAnalytics();
    const {updateWorkflowMutation} = useWorkflowEditor();
    const queryClient = useQueryClient();

    async function handleDropOnPlaceholderNode(targetNode: Node, droppedNode: ClickedDefinitionType) {
        const {nodeData, operationName} = await createWorkflowNodeData(
            droppedNode,
            queryClient,
            taskDispatcherDefinitions
        );

        await saveDroppedNode({
            captureComponentUsed,
            nodeData,
            operationName,
            options: {
                placeholderId: targetNode.id,
                taskDispatcherContext: getTaskDispatcherContext({node: targetNode}),
            },
            queryClient,
            updateWorkflowMutation: updateWorkflowMutation!,
        });
    }

    async function handleDropOnWorkflowEdge(targetEdge: Edge, droppedNode: ClickedDefinitionType) {
        const {nodes} = useWorkflowDataStore.getState();
        const {nodeData, operationName} = await createWorkflowNodeData(
            droppedNode,
            queryClient,
            taskDispatcherDefinitions
        );

        const insertIndex = calculateNodeInsertIndex(targetEdge.target);

        await saveDroppedNode({
            captureComponentUsed,
            nodeData,
            operationName,
            options: {
                nodeIndex: insertIndex,
                taskDispatcherContext: getTaskDispatcherContext({edge: targetEdge, nodes}),
            },
            queryClient,
            updateWorkflowMutation: updateWorkflowMutation!,
        });
    }

    async function handleDropOnTriggerNode(droppedNode: ClickedDefinitionType, targetTriggerName: string) {
        const {nodeData, operationName} = await createWorkflowNodeData(
            droppedNode,
            queryClient,
            taskDispatcherDefinitions,
            targetTriggerName
        );

        await saveDroppedNode({
            captureComponentUsed,
            nodeData,
            operationName,
            queryClient,
            updateWorkflowMutation: updateWorkflowMutation!,
        });
    }

    async function handleDropOnTriggerPlaceholder(droppedNode: ClickedDefinitionType) {
        const {nodeData, operationName} = await createWorkflowNodeData(
            droppedNode,
            queryClient,
            taskDispatcherDefinitions
        );

        await saveDroppedNode({
            captureComponentUsed,
            nodeData,
            operationName,
            queryClient,
            updateWorkflowMutation: updateWorkflowMutation!,
        });
    }

    async function handleDropOnClusterElementPlaceholder(targetNode: Node, droppedNode: ClickedDefinitionType) {
        const {clusterElementType, multipleClusterElementsNode} = targetNode.data as NodeDataType;
        const {nodes, workflow} = useWorkflowDataStore.getState();

        const sourceNodeId = targetNode.id.split('-')[0];
        const sourceNode = nodes.find((node) => node.id === sourceNodeId);
        const clusterRootId = sourceNode ? resolveClusterRootId(sourceNode.data as NodeDataType) : undefined;

        const clusterRootComponentDefinition = clusterRootId
            ? useWorkflowEditorStore.getState().clusterRootComponentDefinitions[clusterRootId]
            : undefined;

        if (!clusterElementType || !clusterRootId || !clusterRootComponentDefinition) {
            console.error(`Cannot resolve the cluster root of the placeholder ${targetNode.id}`);

            return;
        }

        const getComponentDefinitionRequest = {
            componentName: droppedNode.name!,
            componentVersion: droppedNode.version!,
        };

        const componentDefinition = await queryClient.query({
            queryFn: () => new ComponentDefinitionApi().getComponentDefinition(getComponentDefinitionRequest),
            queryKey: ComponentDefinitionKeys.componentDefinition(getComponentDefinitionRequest),
            staleTime: DEFINITION_STALE_TIME,
        });

        const clusterElementTypeName = convertNameToSnakeCase(clusterElementType);

        const matchingClusterElements =
            componentDefinition.clusterElements?.filter(
                (clusterElement) => clusterElement.type === clusterElementTypeName
            ) ?? [];

        if (matchingClusterElements.length === 0) {
            return;
        }

        if (matchingClusterElements.length > 1) {
            useWorkflowEditorStore
                .getState()
                .setPendingClusterElementDrop({componentDefinition, placeholderId: targetNode.id});

            return;
        }

        const [matchingClusterElement] = matchingClusterElements;

        captureComponentUsed(componentDefinition.name, undefined, matchingClusterElement.name);

        await addClusterElementToClusterRoot({
            clusterElementName: matchingClusterElement.name,
            clusterElementType,
            clusterRootComponentDefinition,
            clusterRootId,
            componentDefinition,
            isMultipleElements: !!multipleClusterElementsNode,
            label: droppedNode.title,
            queryClient,
            sourceNodeId,
            updateWorkflowMutation: updateWorkflowMutation!,
            workflow,
        });
    }

    return [
        handleDropOnPlaceholderNode,
        handleDropOnWorkflowEdge,
        handleDropOnTriggerNode,
        handleDropOnTriggerPlaceholder,
        handleDropOnClusterElementPlaceholder,
    ];
}
