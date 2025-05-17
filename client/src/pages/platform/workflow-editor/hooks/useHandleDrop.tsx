import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {
    ActionDefinitionApi,
    ComponentDefinitionApi,
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    TriggerDefinitionApi,
} from '@/shared/middleware/platform/configuration';
import {ActionDefinitionKeys} from '@/shared/queries/platform/actionDefinitions.queries';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {TriggerDefinitionKeys} from '@/shared/queries/platform/triggerDefinitions.queries';
import {ClickedDefinitionType, NodeDataType} from '@/shared/types';
import {getRandomId} from '@/shared/util/random-utils';
import {QueryClient, useQueryClient} from '@tanstack/react-query';
import {Edge, Node} from '@xyflow/react';
import {PlayIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';
import {useParams} from 'react-router-dom';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import getFormattedName from '../utils/getFormattedName';
import getParametersWithDefaultValues from '../utils/getParametersWithDefaultValues';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import {TASK_DISPATCHER_CONFIG} from '../utils/taskDispatcherConfig';

/**
 * Creates workflow node data object from the dropped node
 */
function createWorkflowNodeData(droppedNode: ClickedDefinitionType): NodeDataType {
    return {
        componentName: droppedNode.name!,
        label: droppedNode.title,
        name: getFormattedName(droppedNode.name!),
        taskDispatcher: droppedNode.taskDispatcher,
        title: droppedNode?.title,
        version: droppedNode.version,
        workflowNodeName: getFormattedName(droppedNode.name!),
    };
}

/**
 * Initializes parameters for a workflow node
 */
async function initializeNodeParameters(nodeData: NodeDataType, queryClient: QueryClient) {
    if (nodeData.taskDispatcher) {
        const initalParameters = TASK_DISPATCHER_CONFIG[
            nodeData.componentName as keyof typeof TASK_DISPATCHER_CONFIG
        ]?.getInitialParameters([]);

        return {
            ...initalParameters,
        };
    } else if (nodeData.version && nodeData.componentName) {
        const draggedComponentDefinition = await queryClient.fetchQuery({
            queryFn: () =>
                new ComponentDefinitionApi().getComponentDefinition({
                    componentName: nodeData.componentName,
                    componentVersion: nodeData.version!,
                }),
            queryKey: ComponentDefinitionKeys.componentDefinition({
                componentName: nodeData.componentName,
                componentVersion: nodeData.version,
            }),
        });

        const getActionDefinitionRequest = {
            actionName: draggedComponentDefinition.actions?.[0].name as string,
            componentName: nodeData.componentName,
            componentVersion: draggedComponentDefinition.version,
        };

        const draggedComponentActionDefinition = await queryClient.fetchQuery({
            queryFn: () => new ActionDefinitionApi().getComponentActionDefinition(getActionDefinitionRequest),
            queryKey: ActionDefinitionKeys.actionDefinition(getActionDefinitionRequest),
        });

        return getParametersWithDefaultValues({
            properties: draggedComponentActionDefinition.properties || [],
        });
    }
}

async function getFirstActionName(nodeData: NodeDataType, queryClient: QueryClient) {
    if (nodeData.taskDispatcher || !nodeData.version || !nodeData.componentName) {
        return undefined;
    }

    const componentDefinition = await queryClient.fetchQuery({
        queryFn: () =>
            new ComponentDefinitionApi().getComponentDefinition({
                componentName: nodeData.componentName,
                componentVersion: nodeData.version!,
            }),
        queryKey: ComponentDefinitionKeys.componentDefinition({
            componentName: nodeData.componentName,
            componentVersion: nodeData.version,
        }),
    });

    return componentDefinition.actions?.[0].name;
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

        console.log('droppedNode', droppedNode);

        const nodeData = createWorkflowNodeData(droppedNode);

        const newWorkflowNode = {
            ...targetNode,
            data: nodeData,
            name: droppedNode.name,
            type: 'workflow',
        };

        const parameters = await initializeNodeParameters(nodeData, queryClient);

        captureComponentUsed(
            nodeData.componentName,
            !nodeData.taskDispatcher ? await getFirstActionName(nodeData, queryClient) : undefined,
            undefined
        );

        saveWorkflowDefinition({
            nodeData: {
                ...newWorkflowNode.data,
                parameters,
            },
            projectId: +projectId!,
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

        const nodeData = createWorkflowNodeData(droppedNode);

        console.log('nodeData', nodeData);

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

        const parameters = await initializeNodeParameters(nodeData, queryClient);

        captureComponentUsed(
            nodeData.componentName,
            !nodeData.taskDispatcher ? await getFirstActionName(nodeData, queryClient) : undefined,
            undefined
        );

        saveWorkflowDefinition({
            nodeData: {
                ...newWorkflowNode.data,
                parameters,
            },
            nodeIndex: targetEdgeIndex,
            projectId: +projectId!,
            queryClient,
            updateWorkflowMutation,
        });
    }

    async function handleDropOnTriggerNode(droppedNode: ComponentDefinitionBasic | TaskDispatcherDefinitionBasic) {
        const {icon, name, title, version} = droppedNode;

        const draggedComponentDefinition = await queryClient.fetchQuery({
            queryFn: () =>
                new ComponentDefinitionApi().getComponentDefinition({
                    componentName: name,
                    componentVersion: version,
                }),
            queryKey: ComponentDefinitionKeys.componentDefinition({
                componentName: name,
                componentVersion: version,
            }),
        });

        const {triggers} = draggedComponentDefinition;

        const newTriggerNodeData = {
            componentName: name,
            icon: icon ? (
                <InlineSVG className="size-9 text-gray-700" src={icon} />
            ) : (
                <PlayIcon className="size-9 text-gray-700" />
            ),
            label: title,
            name: 'trigger_1',
            trigger: true,
            type: `${name}/v${version}/${triggers?.[0].name}`,
            workflowNodeName: 'trigger_1',
        };

        const draggedComponentTriggerDefinition = await queryClient.fetchQuery({
            queryFn: () =>
                new TriggerDefinitionApi().getComponentTriggerDefinition({
                    componentName: name,
                    componentVersion: version,
                    triggerName: triggers?.[0].name as string,
                }),
            queryKey: TriggerDefinitionKeys.triggerDefinition({
                componentName: name,
                componentVersion: version,
                triggerName: triggers?.[0].name as string,
            }),
        });

        captureComponentUsed(name, undefined, triggers?.[0].name);

        saveWorkflowDefinition({
            nodeData: {
                ...newTriggerNodeData,
                operationName: draggedComponentTriggerDefinition.name,
                parameters: getParametersWithDefaultValues({
                    properties: draggedComponentTriggerDefinition.properties || [],
                }),
            },
            projectId: +projectId!,
            queryClient,
            updateWorkflowMutation,
        });
    }

    return [handleDropOnPlaceholderNode, handleDropOnWorkflowEdge, handleDropOnTriggerNode];
}
