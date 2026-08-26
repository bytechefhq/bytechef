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

/**
 * Creates the auto-sizing container every member of a graph is parented to. The size minted here
 * is the configured minimum — the layout pre-pass recomputes it from the members' own boxes
 * (`computeGraphFrameSize`) once they have been laid out.
 *
 * Neither draggable nor selectable: the frame is chrome, and the graph dispatcher's own
 * `WorkflowNode` above it is what a user selects, deletes and configures.
 *
 * It is also the graph's own anchor in the surrounding chain, entered from the dispatcher above and
 * left for whatever follows. Every other task dispatcher puts a pair of ghost bars either side of
 * its body for its branch rails to hang off; a graph's members are free-form inside the frame and
 * nothing hangs off anything, so the bars would be two empty ranks of dead vertical space.
 */
function createFrameNode(graphId: string, isNested: boolean): Node {
    return {
        data: {
            graphFrame: {graphId, height: GRAPH_FRAME_MIN_HEIGHT, width: GRAPH_FRAME_MIN_WIDTH},
            graphId,
            // The frame IS the graph's exit in the surrounding chain — a graph has no ghost bars,
            // because nothing hangs off them the way a loop's body or a condition's branches do.
            // `createEdgeFromTaskDispatcherBottomGhostNode` reads both of these off it.
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

/**
 * Creates the non-deletable Start pill sitting at the frame's content origin. The `graphStart`
 * edge leaving it marks which member the graph enters at (`parameters.startNode`).
 */
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

/**
 * Creates the graph's single add-node placeholder. It is never painted — the frame's header
 * renders its own "Add node" button and opens the component popover with this node's id as
 * `sourceNodeId`, so the placeholder exists purely as the insertion anchor that
 * `getContextFromPlaceholderNode` and `insertTaskDispatcherSubtask` resolve a graph from.
 *
 * The id carries no index: a graph node is appended at the CURRENT `parameters.nodes` length,
 * read at insertion time (see `insertTaskDispatcherSubtask`'s `graph` branch) rather than baked
 * into an id that may already be stale by the time it is clicked.
 */
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

/**
 * Creates all auxiliary nodes for a graph task node: the `graphFrame` the members live inside and
 * the surrounding chain addresses the container through, the Start pill and the single add-node
 * placeholder.
 *
 * The member task nodes themselves are NOT created here — they already sit in `allNodes` as
 * ordinary task nodes (a `parameters.nodes[*]` entry IS a plain task). The layout pre-pass is what
 * parents them to the frame and gives them their frame-relative positions.
 */
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
