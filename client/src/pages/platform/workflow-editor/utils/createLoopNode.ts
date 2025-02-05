import {Node} from '@xyflow/react';

export default function createLoopNode({allNodes, taskNode}: {allNodes: Array<Node>; taskNode: Node}) {
    const placeholderNode: Node = {
        data: {label: '+', loopId: taskNode.id},
        id: `${taskNode.id}-loop-placeholder-0`,
        position: {x: 150, y: 0},
        type: 'placeholder',
    };

    const bottomPlaceholderNode: Node = {
        data: {label: '+'},
        id: `${taskNode.id}-loop-bottom-placeholder`,
        position: {x: 0, y: 0},
        type: 'placeholder',
    };

    const insertIndex = allNodes.findIndex((node) => node.id === taskNode.id) + 1;

    allNodes.splice(insertIndex, 0, placeholderNode, bottomPlaceholderNode);

    return allNodes;
}
