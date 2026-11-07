import {Node} from '@xyflow/react';

import {DEFAULT_NODE_POSITION} from './constants';

type ParallelNodeOptionsType = {
    createLeftGhost?: boolean;
};

type CreateParallelNodePropsType = {
    allNodes: Array<Node>;
    parallelId: string;
    isNested?: boolean;
    options?: ParallelNodeOptionsType;
};

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
