import {DEFAULT_NODE_POSITION} from '@/shared/constants';
import {Node} from '@xyflow/react';

type EachNodeOptionsType = {
    createPlaceholder?: boolean;
    createLeftGhost?: boolean;
};

type CreateEachNodePropsType = {
    allNodes: Array<Node>;
    eachId: string;
    isNested?: boolean;
    options?: EachNodeOptionsType;
};

function createPlaceholderNode(eachId: string): Node {
    return {
        data: {
            eachId,
            label: '+',
            taskDispatcherId: eachId,
        },
        id: `${eachId}-each-placeholder-0`,
        position: DEFAULT_NODE_POSITION,
        type: 'placeholder',
    };
}

function createTopGhostNode(eachId: string): Node {
    return {
        data: {
            eachId,
            taskDispatcherId: eachId,
        },
        id: `${eachId}-each-top-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherTopGhostNode',
    };
}

function createLeftGhostNode(eachId: string): Node {
    return {
        data: {
            eachId,
            taskDispatcherId: eachId,
        },
        id: `${eachId}-taskDispatcher-left-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherLeftGhostNode',
    };
}

function createBottomGhostNode(eachId: string, isNested: boolean = false): Node {
    return {
        data: {
            isNestedBottomGhost: isNested,
            taskDispatcherId: eachId,
        },
        id: `${eachId}-each-bottom-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherBottomGhostNode',
    };
}

/**
 * Creates all necessary auxiliary nodes for the Each task dispatcher
 */
export default function createEachNode({
    allNodes,
    eachId,
    isNested = false,
    options = {
        createPlaceholder: true,
    },
}: CreateEachNodePropsType): Node[] {
    const nodesWithEach = [...allNodes];

    const insertIndex = nodesWithEach.findIndex((node) => node.id === eachId) + 1;
    const nodesToAdd = [];

    nodesToAdd.push(createTopGhostNode(eachId));
    nodesToAdd.push(createLeftGhostNode(eachId));

    if (options.createPlaceholder) {
        nodesToAdd.push(createPlaceholderNode(eachId));
    }

    nodesToAdd.push(createBottomGhostNode(eachId, isNested));

    nodesWithEach.splice(insertIndex, 0, ...nodesToAdd);

    return nodesWithEach;
}
