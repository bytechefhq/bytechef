import {GRAPH_TRANSITION_EDGE_COLOR, GRAPH_TRANSITION_EDGE_DANGLING_COLOR} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {InternalNode, Node, ViewportPortal, getSmoothStepPath, useConnection} from '@xyflow/react';

/**
 * The graph a connection drag belongs to — undefined for a drag that did not start inside one.
 *
 * Both ends of a graph's routing count: a member drawing a transition, and the Start pill having
 * its edge re-pointed. Every other node on the canvas is unconnectable, so in practice nothing else
 * reaches this, but keying on the node's own data rather than on the handle id keeps the overlay
 * tied to the same rule `resolveGraphConnection` applies.
 */
export function getGraphConnectionDragGraphId(fromNode: InternalNode | Node | null | undefined): string | undefined {
    const nodeData = fromNode?.data as NodeDataType | undefined;

    return nodeData?.graphData?.graphId ?? nodeData?.graphStart?.graphId;
}

/**
 * The line that follows the pointer while a graph transition is being drawn.
 *
 * React Flow paints its own, but only when the canvas-wide `nodesConnectable` is on
 * (`ConnectionLineWrapper` reads that flag straight off the store, and a per-node `connectable`
 * never reaches it). This canvas deliberately keeps the flag off — graph member handles are the
 * only connectable ones, and turning it on would offer a connection affordance on every node in
 * every workflow — so without this overlay drag-to-connect would be a blind drag: press, see
 * nothing, release and hope. A custom `connectionLineComponent` is no help either; it is rendered
 * BY the same gated wrapper.
 *
 * It renders through `ViewportPortal`, whose container sits at the viewport's own origin, so
 * `useConnection`'s flow coordinates can be used unchanged — that hook converts `to` into flow
 * space for exactly this purpose. Dashed and in the transition color, so it reads as the
 * not-yet-committed form of the edge it will become.
 */
const GraphConnectionLine = () => {
    const connection = useConnection();

    if (!connection.inProgress || !getGraphConnectionDragGraphId(connection.fromNode)) {
        return null;
    }

    const [path] = getSmoothStepPath({
        borderRadius: 10,
        sourcePosition: connection.fromPosition,
        sourceX: connection.from.x,
        sourceY: connection.from.y,
        targetPosition: connection.toPosition,
        targetX: connection.to.x,
        targetY: connection.to.y,
    });

    return (
        <ViewportPortal>
            <svg
                className="pointer-events-none"
                data-graph-connection-line
                style={{height: '100%', left: 0, overflow: 'visible', position: 'absolute', top: 0, width: '100%'}}
            >
                <path
                    d={path}
                    fill="none"
                    stroke={
                        connection.isValid === false
                            ? GRAPH_TRANSITION_EDGE_DANGLING_COLOR
                            : GRAPH_TRANSITION_EDGE_COLOR
                    }
                    strokeDasharray="6 4"
                    strokeWidth={2}
                />
            </svg>
        </ViewportPortal>
    );
};

export default GraphConnectionLine;
