import {Node} from '@xyflow/react';

export default function createLoopNode({allNodes, taskNode}: {allNodes: Array<Node>; taskNode: Node}) {
    const baseNode = {
        data: {taskDispatcherId: taskNode.id},
        position: {x: 0, y: 0},
        type: 'placeholder',
    };

    const placeholderNode: Node = {
        ...baseNode,
        data: {...baseNode.data, label: '+', loopId: taskNode.id},
        id: `${taskNode.id}-loop-placeholder-0`,
    };

    const leftGhostNode: Node = {
        ...baseNode,
        id: `${taskNode.id}-loop-left-ghost`,
        type: 'loopLeftGhostNode',
    };

    const bottomGhostNode: Node = {
        ...baseNode,
        id: `${taskNode.id}-loop-bottom-ghost`,
        type: 'taskDispatcherBottomGhostNode',
    };

    const insertIndex = allNodes.findIndex((node) => node.id === taskNode.id) + 1;

    allNodes.splice(insertIndex, 0, leftGhostNode, placeholderNode, bottomGhostNode);

    return allNodes;
}
