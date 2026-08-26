import {EDGE_STYLES, GRAPH_START_EDGE_TYPE, GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {GraphTransitionType, NodeDataType} from '@/shared/types';
import {Edge, MarkerType, Node} from '@xyflow/react';

import {getGraphFrameId} from './graph/graphFrameGeometry';
import {
    GRAPH_START_SOURCE_HANDLE_SUFFIX,
    GRAPH_TRANSITION_DYNAMIC_HANDLE_SUFFIX,
    GRAPH_TRANSITION_SOURCE_HANDLE_SUFFIX,
    GRAPH_TRANSITION_TARGET_HANDLE_SUFFIX,
} from './graph/graphHandleSuffixes';
import {isDynamicTransitionTarget} from './graph/graphTransitionMutations';

// Keeps transition edges painted above the frame's fill and its members' own chrome.
const GRAPH_TRANSITION_EDGE_Z_INDEX = 5;

/**
 * Creates every edge belonging to a graph dispatcher:
 *
 * - the structural link from the graph node into its frame, which is how the surrounding workflow
 *   enters the container (what LEAVES it is the chain's own business, see below);
 * - the `graphStart` edge joining the Start pill to the graph's entry member;
 * - one `graphTransition` edge per `parameters.transitions[]` entry, keyed by declaration index
 *   (declared order among a node's outgoing transitions is conditional priority, so the index is
 *   the transition's identity and is carried in both the edge id and its data).
 *
 * Members are laid out freely inside the frame, so nothing here connects a member to a ghost bar.
 */
export default function createGraphEdges(graphNode: Node): Edge[] {
    const nodeData: NodeDataType = graphNode.data as NodeDataType;

    const graphId = graphNode.id;
    const frameId = getGraphFrameId(graphId);
    const startId = `${graphId}-graph-start`;

    const nodes: Array<WorkflowTask> = nodeData.parameters?.nodes ?? [];
    const transitions: Array<GraphTransitionType> = nodeData.parameters?.transitions ?? [];

    const memberNames = new Set(nodes.map((node) => node.name));

    // The dispatcher drops straight into the frame. The frame's own outgoing edge is NOT built
    // here — the chain owns what follows a dispatcher, and builds it from the frame the same way it
    // used to build it from a bottom ghost bar.
    const edges: Edge[] = [
        {
            id: `${graphId}=>${frameId}`,
            source: graphId,
            style: EDGE_STYLES,
            target: frameId,
            targetHandle: `${frameId}-top`,
            type: 'smoothstep',
        },
    ];

    // `GraphTaskDispatcher.resolveStartNode` treats a null OR blank `startNode` as absent and
    // enters at the first declared node, so the pill points there too rather than at a member
    // named '' — the canvas must not show an entry point the runtime would never take.
    const declaredStartNode = nodeData.parameters?.startNode?.trim();
    const startNodeName = declaredStartNode || nodes[0]?.name;

    if (startNodeName) {
        edges.push({
            id: `${startId}=>${startNodeName}`,
            // The canvas turns edge reconnecting off wholesale, so this opt-in is what makes the
            // start edge — and only the start edge — re-pointable. Only its TARGET end moves: the
            // pill is where the graph is entered, and dragging that end would read as a transition
            // between two members rather than a change of entry point.
            reconnectable: 'target',
            source: startId,
            sourceHandle: `${startId}${GRAPH_START_SOURCE_HANDLE_SUFFIX}`,
            target: startNodeName,
            targetHandle: `${startNodeName}${GRAPH_TRANSITION_TARGET_HANDLE_SUFFIX}`,
            type: GRAPH_START_EDGE_TYPE,
        });
    }

    transitions.forEach((transition, index) => {
        // A dynamic `to` is an expression resolved at run time, so it names no target node — the
        // edge loops back onto its own source instead, painted as a short dashed stub ending in a
        // "dynamic" badge.
        const dynamic = isDynamicTransitionTarget(transition.to);

        // Only reachable through edits made outside the editor: deleting a member here also drops
        // every transition naming it. The edge is still emitted, but it will not reach the canvas —
        // a dangling endpoint means `source`/`target` names a node id that does not exist, and
        // React Flow drops such an edge before rendering. The flag rides along for the Transitions
        // panel, which is where a transition the canvas cannot draw is surfaced as a warning row.
        const dangling = !memberNames.has(transition.from) || (!dynamic && !memberNames.has(transition.to));

        edges.push({
            data: {
                condition: transition.condition,
                dangling,
                dynamic,
                graphId,
                index,
                to: transition.to,
            },
            id: `${graphId}-transition-${index}`,
            markerEnd: {type: MarkerType.ArrowClosed},
            source: transition.from,
            sourceHandle: `${transition.from}${GRAPH_TRANSITION_SOURCE_HANDLE_SUFFIX}`,
            target: dynamic ? transition.from : transition.to,
            targetHandle: dynamic
                ? `${transition.from}${GRAPH_TRANSITION_DYNAMIC_HANDLE_SUFFIX}`
                : `${transition.to}${GRAPH_TRANSITION_TARGET_HANDLE_SUFFIX}`,
            type: GRAPH_TRANSITION_EDGE_TYPE,
            zIndex: GRAPH_TRANSITION_EDGE_Z_INDEX,
        });
    });

    return edges;
}
