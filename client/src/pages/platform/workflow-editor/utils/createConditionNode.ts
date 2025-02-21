import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE} from '@/shared/constants';
import {Node} from '@xyflow/react';

const baseNode = {
    position: {x: 0, y: 0},
    type: 'placeholder',
};

export default function createConditionNode({
    allNodes,
    belowPlaceholderNode,
    sourcePlaceholderIndex,
    taskNode,
}: {
    allNodes: Array<Node>;
    belowPlaceholderNode?: Node;
    sourcePlaceholderIndex?: number;
    taskNode: Node;
}): Array<Node> {
    const leftPlaceholderNode: Node = {
        ...baseNode,
        data: {conditionCase: CONDITION_CASE_TRUE, conditionId: taskNode.id, label: '+'},
        id: `${taskNode.id}-left-placeholder-0`,
    };

    const rightPlaceholderNode: Node = {
        ...baseNode,
        data: {conditionCase: CONDITION_CASE_FALSE, conditionId: taskNode.id, label: '+'},
        id: `${taskNode.id}-right-placeholder-0`,
    };

    if (taskNode.data.conditionData && belowPlaceholderNode && sourcePlaceholderIndex) {
        allNodes.splice(sourcePlaceholderIndex + 1, 0, leftPlaceholderNode, rightPlaceholderNode, belowPlaceholderNode);

        return allNodes;
    }

    const bottomPlaceholderNode: Node = {
        ...baseNode,
        data: {label: '+'},
        id: `${taskNode.id}-bottom-placeholder`,
    };

    const insertIndex = allNodes.findIndex((node) => node.id === taskNode.id) + 1;

    allNodes.splice(insertIndex, 0, leftPlaceholderNode, rightPlaceholderNode, bottomPlaceholderNode);

    return allNodes;
}
