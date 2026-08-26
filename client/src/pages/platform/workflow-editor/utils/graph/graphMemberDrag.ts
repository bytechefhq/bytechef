import {Node, XYPosition} from '@xyflow/react';

import {fromFrameChildPosition} from './graphFrameGeometry';

interface BuildGraphMemberDragStopPositionsPropsI {
    /** graphId's pending auto-placed members: member name -> content-origin position. */
    autoPlacedPositions?: Record<string, XYPosition>;
    draggedNodeId: string;
    /** Where the member was dropped, in FRAME-relative coordinates. */
    draggedNodePosition: XYPosition;
}

function clampNonNegative(position: XYPosition): XYPosition {
    return {x: Math.max(0, position.x), y: Math.max(0, position.y)};
}

/**
 * The `nodePositions` payload one drop inside a graph frame persists: the dragged member's own
 * position plus every position the layout pre-pass had to invent for a member that carried none.
 *
 * Flushing those auto-placed siblings here is what the spec means by writing them back on the
 * user's first interaction with the graph. Persisting the dragged member alone would leave its
 * siblings unplaced, so the next relayout would re-invent their spots around the one pinned member
 * and the graph would visibly rearrange itself after an unrelated drag.
 *
 * The clamp is applied to the CONTENT position rather than the frame-relative one, because it is
 * content coordinates the spec requires to be non-negative — the frame-relative origin sits a
 * header band above them. `GRAPH_MEMBER_EXTENT` keeps a member out of that band and off negative
 * x today, so no drop a user can perform tells the two orders apart; this is the order that stays
 * correct if that extent is ever loosened, and the only correct one for a frame child that has no
 * extent at all.
 */
export function buildGraphMemberDragStopPositions({
    autoPlacedPositions,
    draggedNodeId,
    draggedNodePosition,
}: BuildGraphMemberDragStopPositionsPropsI): Record<string, XYPosition> {
    return {
        ...autoPlacedPositions,
        // Written last so a fresh drop wins over an auto-placed entry still held for this member.
        [draggedNodeId]: clampNonNegative(fromFrameChildPosition(draggedNodePosition)),
    };
}

/**
 * Narrows a set of co-dragged nodes to those sharing the dragged node's React Flow parent, i.e.
 * those whose positions are expressed in the same coordinate space.
 *
 * Dragging a task dispatcher shifts its descendants by the drag delta. A graph's members are
 * descendants of the graph dispatcher, but they are parented to the frame node — React Flow
 * already carries them when the frame moves, and their positions are frame-relative, so applying
 * the delta a second time would both double their travel and persist a bogus content position.
 * A dragged member that is itself a dispatcher keeps its own subtree, which is parented to the
 * same frame it is.
 */
export function filterToSharedParent(
    startPositions: Map<string, XYPosition>,
    nodes: Node[],
    parentId: string | undefined
): Map<string, XYPosition> {
    const nodesById = new Map(nodes.map((node) => [node.id, node]));

    return new Map(
        [...startPositions].filter(([nodeId]) => {
            const node = nodesById.get(nodeId);

            return !!node && node.parentId === parentId;
        })
    );
}
