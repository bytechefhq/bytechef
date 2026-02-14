import {Edge, Node, XYPosition} from '@xyflow/react';

export type DraggingPlaceholderStateType = {
    nodeId: string;
    nodeStartPosition: XYPosition;
    placeholderStartPosition: XYPosition;
};

/**
 * Determines whether the dragged node is the predecessor of the trailing
 * placeholder. If so, returns the state needed to move the placeholder
 * during the drag; otherwise returns null.
 *
 * A node is the predecessor when:
 *   1. It is the direct source of the edge targeting the trailing placeholder, OR
 *   2. It is a task dispatcher whose descendant (e.g. a bottom-ghost) is the source.
 */
export function buildDraggingPlaceholderState(
    draggedNode: Node,
    isTaskDispatcher: boolean,
    trailingPlaceholderId: string,
    edges: Edge[],
    nodes: Node[],
    dispatcherChildIds: Map<string, XYPosition>
): DraggingPlaceholderStateType | null {
    const trailingPlaceholderEdge = edges.find((edge) => edge.target === trailingPlaceholderId);

    if (!trailingPlaceholderEdge) {
        return null;
    }

    const trailingPlaceholder = nodes.find((searchNode) => searchNode.id === trailingPlaceholderId);

    if (!trailingPlaceholder) {
        return null;
    }

    const predecessorId = trailingPlaceholderEdge.source;

    if (predecessorId === draggedNode.id) {
        return {
            nodeId: draggedNode.id,
            nodeStartPosition: {...draggedNode.position},
            placeholderStartPosition: {...trailingPlaceholder.position},
        };
    }

    if (isTaskDispatcher && dispatcherChildIds.has(predecessorId)) {
        return {
            nodeId: draggedNode.id,
            nodeStartPosition: {...draggedNode.position},
            placeholderStartPosition: {...trailingPlaceholder.position},
        };
    }

    return null;
}

/**
 * Computes the new position of the trailing placeholder given the tracked
 * node's current position and the stored start positions.
 */
export function computePlaceholderDragPosition(
    state: DraggingPlaceholderStateType,
    currentNodePosition: XYPosition
): XYPosition {
    return {
        x: state.placeholderStartPosition.x + (currentNodePosition.x - state.nodeStartPosition.x),
        y: state.placeholderStartPosition.y + (currentNodePosition.y - state.nodeStartPosition.y),
    };
}
