import {useUpdateWorkflowMutation} from '@/mutations/automation/workflows.mutations';
import {ProjectKeys} from '@/queries/automation/projects.queries';
import getRandomId from '@/utils/getRandomId';
import {useQueryClient} from '@tanstack/react-query';
import {PlayIcon} from 'lucide-react';
import {ComponentDefinitionBasicModel, TaskDispatcherDefinitionBasicModel} from 'middleware/platform/configuration';
import InlineSVG from 'react-inlinesvg';
import {Edge, Node, useReactFlow} from 'reactflow';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import getFormattedName from '../utils/getFormattedName';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';

export default function useHandleDrop(): [
    (targetNode: Node, droppedNode: ComponentDefinitionBasicModel | TaskDispatcherDefinitionBasicModel) => void,
    (targetEdge: Edge, droppedNode: ComponentDefinitionBasicModel | TaskDispatcherDefinitionBasicModel) => void,
] {
    const {componentNames, projectId, setComponentNames, workflow} = useWorkflowDataStore();

    const {getEdges, getNodes, setEdges, setNodes} = useReactFlow();

    const newNodeId = getRandomId();
    const nodes = getNodes();
    const edges = getEdges();

    const queryClient = useQueryClient();

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projectWorkflows(projectId)});
        },
    });

    function handleDropOnPlaceholderNode(
        targetNode: Node,
        droppedNode: ComponentDefinitionBasicModel | TaskDispatcherDefinitionBasicModel
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

            setComponentNames(tempComponentNames);

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
    }

    function handleDropOnWorkflowEdge(
        targetEdge: Edge,
        droppedNode: ComponentDefinitionBasicModel | TaskDispatcherDefinitionBasicModel
    ) {
        const previousNode = nodes.find((node) => node.id === targetEdge.source);

        const nextNode = nodes.find((node) => node.id === targetEdge.target);

        if (!nextNode || !previousNode) {
            return;
        }

        const targetEdgeIndex = edges.findIndex((edge) => edge.id === targetEdge.id);

        const draggedNode = {
            data: {
                componentName: droppedNode.name,
                icon: droppedNode?.icon ? (
                    <InlineSVG className="size-9 text-gray-700" src={droppedNode.icon} />
                ) : (
                    <PlayIcon className="size-9 text-gray-700" />
                ),
                label: droppedNode?.title,
                name: getFormattedName(droppedNode.name!, nodes),
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

            nodes.splice(nextNodeIndex, 0, draggedNode);

            const tempComponentNames = [...componentNames];

            tempComponentNames.splice(nextNodeIndex - 1, 0, draggedNode.data.componentName);

            setComponentNames(tempComponentNames);

            return nodes;
        });

        const newWorkflowEdge = {
            id: `${draggedNode.id}=>${nextNode.id}`,
            source: draggedNode.id,
            target: nextNode.id,
            type: 'workflow',
        };

        setEdges((edges) => {
            edges[targetEdgeIndex] = {
                ...targetEdge,
                id: `${previousNode.id}=>${draggedNode.id}`,
                target: draggedNode.id,
            };

            edges.splice(targetEdgeIndex, 0, newWorkflowEdge);

            return edges;
        });

        saveWorkflowDefinition(draggedNode.data, workflow, updateWorkflowMutation, targetEdgeIndex);
    }

    return [handleDropOnPlaceholderNode, handleDropOnWorkflowEdge];
}
