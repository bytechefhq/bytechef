import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE, DEFAULT_NODE_POSITION} from '@/shared/constants';
import {Node} from '@xyflow/react';

type ConditionNodeOptionsType = {
    createLeftPlaceholder?: boolean;
    createRightPlaceholder?: boolean;
    createBottomGhost?: boolean;
};

type CreateConditionNodePropsType = {
    allNodes: Array<Node>;
    isNestedCondition?: boolean;
    options?: ConditionNodeOptionsType;
    taskNode: Node;
};

/**
 * Creates a placeholder node for a condition branch
 */
function createPlaceholderNode(taskNodeId: string, conditionCase: string, suffix: string): Node {
    return {
        data: {
            conditionCase,
            conditionId: taskNodeId,
            label: '+',
        },
        id: `${taskNodeId}-${suffix}-placeholder-0`,
        position: DEFAULT_NODE_POSITION,
        type: 'placeholder',
    };
}

/**
 * Creates a bottom ghost node for a condition
 */
function createBottomGhostNode(taskNodeId: string, isNested: boolean = false): Node {
    return {
        data: {
            conditionId: taskNodeId,
            isNestedConditionBottomGhost: isNested,
            taskDispatcherId: taskNodeId,
        },
        id: `${taskNodeId}-condition-bottom-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherBottomGhostNode',
    };
}

/**
 * Creates all necessary auxiliary nodes for a condition task node
 */
export default function createConditionNode({
    allNodes,
    isNestedCondition = false,
    options = {
        createBottomGhost: true,
        createLeftPlaceholder: true,
        createRightPlaceholder: true,
    },
    taskNode,
}: CreateConditionNodePropsType): Node[] {
    const nodesWithCondition = [...allNodes];

    if (options.createLeftPlaceholder) {
        nodesWithCondition.push(createPlaceholderNode(taskNode.id, CONDITION_CASE_TRUE, 'left'));
    }

    if (options.createRightPlaceholder) {
        nodesWithCondition.push(createPlaceholderNode(taskNode.id, CONDITION_CASE_FALSE, 'right'));
    }

    if (options.createBottomGhost) {
        nodesWithCondition.push(createBottomGhostNode(taskNode.id, isNestedCondition));
    }

    return nodesWithCondition;
}
