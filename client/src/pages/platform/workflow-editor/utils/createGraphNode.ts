import {DEFAULT_NODE_POSITION, GRAPH_FRAME_NODE_TYPE, GRAPH_START_NODE_TYPE} from '@/shared/constants';
import {Node} from '@xyflow/react';

import {
    GRAPH_FRAME_MIN_HEIGHT,
    GRAPH_FRAME_MIN_WIDTH,
    GRAPH_FRAME_PADDING,
    GRAPH_START_SIZE,
    getGraphFrameId,
    toFrameChildPosition,
} from './graph/graphFrameGeometry';

type CreateGraphNodePropsType = {
    allNodes: Array<Node>;
    graphId: string;
    isNested?: boolean;
};

function createFrameNode(graphId: string, isNested: boolean): Node {
    return {
        data: {
            graphFrame: {graphId, height: GRAPH_FRAME_MIN_HEIGHT, width: GRAPH_FRAME_MIN_WIDTH},
            graphId,
            isNestedBottomGhost: isNested,
            taskDispatcherId: graphId,
        },
        draggable: false,
        height: GRAPH_FRAME_MIN_HEIGHT,
        id: getGraphFrameId(graphId),
        position: DEFAULT_NODE_POSITION,
        selectable: false,
        style: {pointerEvents: 'all'},
        type: GRAPH_FRAME_NODE_TYPE,
        width: GRAPH_FRAME_MIN_WIDTH,
    };
}

function createStartNode(graphId: string): Node {
    return {
        data: {
            graphId,
            graphStart: {graphId},
            taskDispatcherId: graphId,
        },
        draggable: false,
        height: GRAPH_START_SIZE.height,
        id: `${graphId}-graph-start`,
        parentId: getGraphFrameId(graphId),
        position: toFrameChildPosition({x: GRAPH_FRAME_PADDING, y: 0}),
        type: GRAPH_START_NODE_TYPE,
        width: GRAPH_START_SIZE.width,
    };
}

function createAddPlaceholderNode(graphId: string): Node {
    return {
        data: {
            graphId,
            label: '+',
            taskDispatcherId: graphId,
        },
        hidden: true,
        id: `${graphId}-graph-placeholder`,
        parentId: getGraphFrameId(graphId),
        position: DEFAULT_NODE_POSITION,
        type: 'placeholder',
    };
}

export default function createGraphNode({allNodes, graphId, isNested = false}: CreateGraphNodePropsType): Node[] {
    const nodesWithGraph = [...allNodes];
    const insertIndex = nodesWithGraph.findIndex((node) => node.id === graphId) + 1;

    const nodesToAdd: Array<Node> = [
        createFrameNode(graphId, isNested),
        createStartNode(graphId),
        createAddPlaceholderNode(graphId),
    ];

    nodesWithGraph.splice(insertIndex, 0, ...nodesToAdd);

    return nodesWithGraph;
}
