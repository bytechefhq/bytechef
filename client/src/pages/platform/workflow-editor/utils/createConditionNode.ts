import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE, DEFAULT_NODE_POSITION} from '@/shared/constants';
import {Node} from '@xyflow/react';

type ConditionNodeOptionsType = {
    createLeftPlaceholder?: boolean;
    createRightPlaceholder?: boolean;
};

type CreateConditionNodePropsType = {
    allNodes: Array<Node>;
    conditionId: string;
    isNested?: boolean;
    options?: ConditionNodeOptionsType;
};

/**
 * Creates a placeholder node for a condition branch
 */
function createPlaceholderNode(conditionId: string, conditionCase: string, suffix: string): Node {
    return {
        data: {
            conditionCase,
            conditionId,
            label: '+',
            taskDispatcherId: conditionId,
        },
        id: `${conditionId}-condition-${suffix}-placeholder-0`,
        position: DEFAULT_NODE_POSITION,
        type: 'placeholder',
    };
}

/**
 * Creates a top ghost node for a loop
 */
function createTopGhostNode(conditionId: string): Node {
    return {
        data: {
            conditionId,
            taskDispatcherId: conditionId,
        },
        id: `${conditionId}-condition-top-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherTopGhostNode',
    };
}

/**
 * Creates a bottom ghost node for a condition
 */
function createBottomGhostNode(conditionId: string, isNested: boolean = false): Node {
    return {
        data: {
            conditionId: conditionId,
            isNestedBottomGhost: isNested,
            taskDispatcherId: conditionId,
        },
        id: `${conditionId}-condition-bottom-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherBottomGhostNode',
    };
}

/**
 * Creates all necessary auxiliary nodes for a condition task node
 */
export default function createConditionNode({
    allNodes,
    conditionId,
    isNested = false,
    options = {
        createLeftPlaceholder: true,
        createRightPlaceholder: true,
    },
}: CreateConditionNodePropsType): Node[] {
    const nodesWithCondition = [...allNodes];
    const insertIndex = nodesWithCondition.findIndex((node) => node.id === conditionId) + 1;
    const nodesToAdd = [];

    nodesToAdd.push(createTopGhostNode(conditionId));

    if (options.createLeftPlaceholder) {
        nodesToAdd.push(createPlaceholderNode(conditionId, CONDITION_CASE_TRUE, 'left'));
    }

    if (options.createRightPlaceholder) {
        nodesToAdd.push(createPlaceholderNode(conditionId, CONDITION_CASE_FALSE, 'right'));
    }

    nodesToAdd.push(createBottomGhostNode(conditionId, isNested));

    nodesWithCondition.splice(insertIndex, 0, ...nodesToAdd);

    return nodesWithCondition;
}
