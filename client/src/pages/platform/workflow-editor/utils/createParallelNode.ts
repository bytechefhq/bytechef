import {DEFAULT_NODE_POSITION} from '@/shared/constants';
import {Node} from '@xyflow/react';

type ParallelNodeOptionsType = {
    createLeftGhost?: boolean;
};

type CreateParallelNodePropsType = {
    allNodes: Array<Node>;
    parallelId: string;
    isNested?: boolean;
    options?: ParallelNodeOptionsType;
};

/**
 * Creates a placeholder node for a parallel task
 */
function createPlaceholderNode(parallelId: string): Node {
    return {
        data: {
            label: '+',
            parallelId,
            taskDispatcherId: parallelId,
        },
        id: `${parallelId}-parallel-placeholder-0`,
        position: DEFAULT_NODE_POSITION,
        type: 'placeholder',
    };
}

/**
 * Creates a top ghost node for a parallel task
 */
function createTopGhostNode(parallelId: string): Node {
    return {
        data: {
            parallelId,
            taskDispatcherId: parallelId,
        },
        id: `${parallelId}-parallel-top-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherTopGhostNode',
    };
}

/**
 * Creates a bottom ghost node for a parallel task
 */
function createBottomGhostNode(parallelId: string, isNested: boolean = false): Node {
    return {
        data: {
            isNestedBottomGhost: isNested,
            taskDispatcherId: parallelId,
        },
        id: `${parallelId}-parallel-bottom-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherBottomGhostNode',
    };
}

/**
 * Creates all necessary auxiliary nodes for a parallel task node
 */
export default function createParallelNode({
    allNodes,
    isNested = false,
    options = {
        createLeftGhost: false,
    },
    parallelId,
}: CreateParallelNodePropsType): Node[] {
    const nodesWithParallel = [...allNodes];
    const insertIndex = nodesWithParallel.findIndex((node) => node.id === parallelId) + 1;
    const nodesToAdd = [];

    nodesToAdd.push(createTopGhostNode(parallelId));

    if (options.createLeftGhost) {
        nodesToAdd.push({
            data: {
                parallelId,
                taskDispatcherId: parallelId,
            },
            id: `${parallelId}-taskDispatcher-left-ghost`,
            position: DEFAULT_NODE_POSITION,
            type: 'taskDispatcherLeftGhostNode',
        });
    }

    nodesToAdd.push(createPlaceholderNode(parallelId));
    nodesToAdd.push(createBottomGhostNode(parallelId, isNested));

    nodesWithParallel.splice(insertIndex, 0, ...nodesToAdd);

    return nodesWithParallel;
}
