import {Node} from '@xyflow/react';

import {NodeDataType, WorkflowTaskType} from '../types';
import {DEFAULT_NODE_POSITION} from './constants';

type ForkJoinNodeOptionsType = {
    createLeftGhost?: boolean;
};

type CreateForkJoinNodePropsType = {
    allNodes: Array<Node>;
    forkJoinId: string;
    isNested?: boolean;
    options?: ForkJoinNodeOptionsType;
};

function createPlaceholderNode(forkJoinId: string, branchIndex: number): Node {
    return {
        data: {
            branchIndex: branchIndex,
            forkJoinId,
            label: '+',
            taskDispatcherId: forkJoinId,
        },
        id: `${forkJoinId}-forkJoin-placeholder-${branchIndex}`,
        position: DEFAULT_NODE_POSITION,
        type: 'placeholder',
    };
}

function createTopGhostNode(forkJoinId: string): Node {
    return {
        data: {
            forkJoinId,
            taskDispatcherId: forkJoinId,
        },
        id: `${forkJoinId}-forkJoin-top-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherTopGhostNode',
    };
}

function createBottomGhostNode(forkJoinId: string, isNested: boolean = false): Node {
    return {
        data: {
            isNestedBottomGhost: isNested,
            taskDispatcherId: forkJoinId,
        },
        id: `${forkJoinId}-forkJoin-bottom-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherBottomGhostNode',
    };
}

export default function createForkJoinNode({
    allNodes,
    forkJoinId,
    isNested = false,
    options = {
        createLeftGhost: false,
    },
}: CreateForkJoinNodePropsType): Node[] {
    const nodesWithForkJoin = [...allNodes];
    const insertIndex = nodesWithForkJoin.findIndex((node) => node.id === forkJoinId) + 1;
    const nodesToAdd = [];

    const forkJoinNodeData = nodesWithForkJoin.find((node) => node.id === forkJoinId)?.data as NodeDataType;

    const branches: Array<WorkflowTaskType> = forkJoinNodeData?.parameters?.branches ?? [];

    const branchCount = branches.filter((branch) => Array.isArray(branch) && branch.length > 0).length;

    nodesToAdd.push(createTopGhostNode(forkJoinId));

    if (options.createLeftGhost) {
        nodesToAdd.push({
            data: {
                forkJoinId,
                taskDispatcherId: forkJoinId,
            },
            id: `${forkJoinId}-taskDispatcher-left-ghost`,
            position: DEFAULT_NODE_POSITION,
            type: 'taskDispatcherLeftGhostNode',
        });
    }

    nodesToAdd.push(createPlaceholderNode(forkJoinId, branchCount));
    nodesToAdd.push(createBottomGhostNode(forkJoinId, isNested));

    nodesWithForkJoin.splice(insertIndex, 0, ...nodesToAdd);

    return nodesWithForkJoin;
}
