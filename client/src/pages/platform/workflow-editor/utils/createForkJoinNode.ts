import {DEFAULT_NODE_POSITION} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';

type ForkJoinNodeOptionsType = {
    createLeftGhost?: boolean;
};

type CreateForkJoinNodePropsType = {
    allNodes: Array<Node>;
    forkJoinId: string;
    isNested?: boolean;
    options?: ForkJoinNodeOptionsType;
};

/**
 * Creates a placeholder node for a fork-join task
 */
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

/**
 * Creates a top ghost node for a fork-join task
 */
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

/**
 * Creates a bottom ghost node for a fork-join task
 */
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

/**
 * Creates all necessary auxiliary nodes for a fork-join task node
 */
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

    const branches: Array<WorkflowTask> = forkJoinNodeData?.parameters?.branches ?? [];

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
