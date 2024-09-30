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
import {getRandomId} from '@/shared/util/random-utils';
import {useQueryClient} from '@tanstack/react-query';
import {PlayIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';
import {Edge, Node, useReactFlow} from 'reactflow';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import getFormattedName from '../utils/getFormattedName';
import getParametersWithDefaultValues from '../utils/getParametersWithDefaultValues';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';

export default function useHandleDrop(): [
    (targetNode: Node, droppedNode: ComponentDefinitionBasic | TaskDispatcherDefinitionBasic) => void,
    (targetEdge: Edge, droppedNode: ComponentDefinitionBasic | TaskDispatcherDefinitionBasic) => void,
    (droppedNode: ComponentDefinitionBasic | TaskDispatcherDefinitionBasic) => void,
] {
    const {setWorkflow, workflow} = useWorkflowDataStore();

    const {componentNames} = workflow;

    const {captureComponentUsed} = useAnalytics();

    const {getEdges, getNodes, setEdges, setNodes} = useReactFlow();

    const newNodeId = getRandomId();
    const nodes = getNodes();
    const edges = getEdges();

    const {updateWorkflowMutation} = useWorkflowMutation();

    const queryClient = useQueryClient();

    async function handleDropOnPlaceholderNode(
        targetNode: Node,
        droppedNode: ComponentDefinitionBasic | TaskDispatcherDefinitionBasic
    ) {
        const newWorkflowNode = {
            ...targetNode,
            data: {
                componentName: droppedNode.name,
                icon: droppedNode?.icon ? (
                    <InlineSVG className="size-9 text-gray-700" src={droppedNode?.icon} />
                ) : (
                    <PlayIcon className="size-9 text-gray-700" />
                ),
                label: droppedNode?.title,
                name: getFormattedName(droppedNode.name!, nodes),
                workflowNodeName: getFormattedName(droppedNode.name!, nodes),
            },
            name: droppedNode.name,
            type: 'workflow',
        };

        const newPlaceholderNode = {
            data: {label: '+'},
            id: newNodeId,
            position: {x: 0, y: 150},
            type: 'placeholder',
        };

        setNodes((nodes) => {
            const nodeIndex = nodes.findIndex((node) => node.id === targetNode.id);

            nodes[nodeIndex] = newWorkflowNode;

            const tempComponentNames = [...componentNames];

            tempComponentNames.splice(nodeIndex - 1, 0, newWorkflowNode.data.componentName);

            setWorkflow({
                ...workflow,
                componentNames: tempComponentNames,
            });

            return [...nodes, newPlaceholderNode];
        });

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

        setEdges((edges) => {
            const edgeIndex = edges.findIndex((edge) => edge.id === sourceEdge?.id);

            edges[edgeIndex] = newWorkflowEdge;

            return [...edges, newPlaceholderEdge];
        });

        const draggedComponentDefinition = await queryClient.fetchQuery({
            queryFn: () =>
                new ComponentDefinitionApi().getComponentDefinition({
                    componentName: newWorkflowNode.data.componentName,
                }),
            queryKey: ComponentDefinitionKeys.componentDefinition({
                componentName: newWorkflowNode.data.componentName,
            }),
        });

        const getActionDefinitionRequest = {
            actionName: draggedComponentDefinition.actions?.[0].name as string,
            componentName: newWorkflowNode.data.componentName,
            componentVersion: draggedComponentDefinition.version,
        };

        const draggedComponentActionDefinition = await queryClient.fetchQuery({
            queryFn: () => new ActionDefinitionApi().getComponentActionDefinition(getActionDefinitionRequest),
            queryKey: ActionDefinitionKeys.actionDefinition(getActionDefinitionRequest),
        });

        captureComponentUsed(
            newWorkflowNode.data.componentName,
            draggedComponentDefinition.actions?.[0].name,
            undefined
        );

        saveWorkflowDefinition(
            {
                ...newWorkflowNode.data,
                parameters: getParametersWithDefaultValues({
                    properties: draggedComponentActionDefinition.properties || [],
                }),
            },
            workflow,
            updateWorkflowMutation
        );
    }

    async function handleDropOnWorkflowEdge(
        targetEdge: Edge,
        droppedNode: ComponentDefinitionBasic | TaskDispatcherDefinitionBasic
    ) {
        const previousNode = nodes.find((node) => node.id === targetEdge.source);

        const nextNode = nodes.find((node) => node.id === targetEdge.target);

        if (!nextNode || !previousNode) {
            return;
        }

        const targetEdgeIndex = edges.findIndex((edge) => edge.id === targetEdge.id);

        const newWorkflowNode = {
            data: {
                componentName: droppedNode.name,
                icon: droppedNode?.icon ? (
                    <InlineSVG className="size-9 text-gray-700" src={droppedNode.icon} />
                ) : (
                    <PlayIcon className="size-9 text-gray-700" />
                ),
                label: droppedNode?.title,
                name: getFormattedName(droppedNode.name!, nodes),
                workflowNodeName: getFormattedName(droppedNode.name!, nodes),
            },
            id: getRandomId(),
            name: droppedNode.name,
            position: {
                x: 0,
                y: 0,
            },
            type: 'workflow',
        };

        setNodes((nodes) => {
            const nextNodeIndex = nodes.findIndex((node) => node.id === nextNode.id);

            nodes.splice(nextNodeIndex, 0, newWorkflowNode);

            const tempComponentNames = [...componentNames];

            tempComponentNames.splice(nextNodeIndex - 1, 0, newWorkflowNode.data.componentName);

            setWorkflow({
                ...workflow,
                componentNames: tempComponentNames,
            });

            return nodes;
        });

        const newWorkflowEdge = {
            id: `${newWorkflowNode.id}=>${nextNode.id}`,
            source: newWorkflowNode.id,
            target: nextNode.id,
            type: 'workflow',
        };

        setEdges((edges) => {
            edges[targetEdgeIndex] = {
                ...targetEdge,
                id: `${previousNode.id}=>${newWorkflowNode.id}`,
                target: newWorkflowNode.id,
            };

            edges.splice(targetEdgeIndex, 0, newWorkflowEdge);

            return edges;
        });

        const draggedComponentDefinition = await queryClient.fetchQuery({
            queryFn: () =>
                new ComponentDefinitionApi().getComponentDefinition({
                    componentName: newWorkflowNode.data.componentName,
                }),
            queryKey: ComponentDefinitionKeys.componentDefinition({
                componentName: newWorkflowNode.data.componentName,
            }),
        });

        const getActionDefinitionRequest = {
            actionName: draggedComponentDefinition.actions?.[0].name as string,
            componentName: newWorkflowNode.data.componentName,
            componentVersion: draggedComponentDefinition?.version,
        };

        const draggedComponentActionDefinition = await queryClient.fetchQuery({
            queryFn: () => new ActionDefinitionApi().getComponentActionDefinition(getActionDefinitionRequest),
            queryKey: ActionDefinitionKeys.actionDefinition(getActionDefinitionRequest),
        });

        captureComponentUsed(
            newWorkflowNode.data.componentName,
            draggedComponentDefinition.actions?.[0].name,
            undefined
        );

        saveWorkflowDefinition(
            {
                ...newWorkflowNode.data,
                parameters: getParametersWithDefaultValues({
                    properties: draggedComponentActionDefinition.properties || [],
                }),
            },
            workflow,
            updateWorkflowMutation,
            targetEdgeIndex
        );
    }

    async function handleDropOnTriggerNode(droppedNode: ComponentDefinitionBasic | TaskDispatcherDefinitionBasic) {
        const {icon, name, title} = droppedNode;

        const draggedComponentDefinition = await queryClient.fetchQuery({
            queryFn: () =>
                new ComponentDefinitionApi().getComponentDefinition({
                    componentName: name,
                }),
            queryKey: ComponentDefinitionKeys.componentDefinition({
                componentName: name,
            }),
        });

        const {triggers, version} = draggedComponentDefinition;

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

        setNodes((nodes) => {
            const newTriggerNode = {
                data: newTriggerNodeData,
                id: getRandomId(),
                name: 'trigger_1',
                position: {
                    x: 0,
                    y: 0,
                },
                type: 'workflow',
            };

            nodes[0] = newTriggerNode;

            componentNames[0] = newTriggerNodeData.componentName;

            setWorkflow({
                ...workflow,
                componentNames: componentNames,
            });

            return nodes;
        });

        captureComponentUsed(name, undefined, triggers?.[0].name);

        saveWorkflowDefinition(
            {
                ...newTriggerNodeData,
                operationName: draggedComponentTriggerDefinition.name,
                parameters: getParametersWithDefaultValues({
                    properties: draggedComponentTriggerDefinition.properties || [],
                }),
            },
            workflow,
            updateWorkflowMutation
        );
    }

    return [handleDropOnPlaceholderNode, handleDropOnWorkflowEdge, handleDropOnTriggerNode];
}
