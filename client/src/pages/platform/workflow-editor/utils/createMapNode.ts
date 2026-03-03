import {DEFAULT_NODE_POSITION} from '@/shared/constants';
import {Node} from '@xyflow/react';

type MapNodeOptionsType = {
    createPlaceholder?: boolean;
    createLeftGhost?: boolean;
};

type CreateMapNodePropsType = {
    allNodes: Array<Node>;
    isNested?: boolean;
    mapId: string;
    options?: MapNodeOptionsType;
};

function createPlaceholderNode(mapId: string): Node {
    return {
        data: {
            label: '+',
            mapId,
            taskDispatcherId: mapId,
        },
        id: `${mapId}-map-placeholder-0`,
        position: DEFAULT_NODE_POSITION,
        type: 'placeholder',
    };
}

function createTopGhostNode(mapId: string): Node {
    return {
        data: {
            mapId,
            taskDispatcherId: mapId,
        },
        id: `${mapId}-map-top-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherTopGhostNode',
    };
}

function createLeftGhostNode(mapId: string): Node {
    return {
        data: {
            mapId,
            taskDispatcherId: mapId,
        },
        id: `${mapId}-taskDispatcher-left-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherLeftGhostNode',
    };
}

function createBottomGhostNode(mapId: string, isNested: boolean = false): Node {
    return {
        data: {
            isNestedBottomGhost: isNested,
            taskDispatcherId: mapId,
        },
        id: `${mapId}-map-bottom-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherBottomGhostNode',
    };
}

/**
 * Creates all necessary auxiliary nodes for the Map task dispatcher
 */
export default function createMapNode({
    allNodes,
    isNested = false,
    mapId,
    options = {
        createPlaceholder: true,
    },
}: CreateMapNodePropsType): Node[] {
    const nodesWithMap = [...allNodes];
    const insertIndex = nodesWithMap.findIndex((node) => node.id === mapId) + 1;
    const nodesToAdd = [];

    nodesToAdd.push(createTopGhostNode(mapId));
    nodesToAdd.push(createLeftGhostNode(mapId));

    if (options.createPlaceholder) {
        nodesToAdd.push(createPlaceholderNode(mapId));
    }

    nodesToAdd.push(createBottomGhostNode(mapId, isNested));

    nodesWithMap.splice(insertIndex, 0, ...nodesToAdd);

    return nodesWithMap;
}
