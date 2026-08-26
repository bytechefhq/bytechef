import {BaseEdge, EdgeProps, getSmoothStepPath} from '@xyflow/react';

/**
 * Joins a graph frame's Start pill to the member the graph enters at (`parameters.startNode`,
 * falling back to the first declared node). There is exactly one of these per frame.
 *
 * Deliberately plainer than a `graphTransition`: the start marker is not part of the transitions
 * list, carries no condition and cannot be deleted, so it gets no label, no dash and no popover.
 * Re-dragging its target onto another member is what rewrites `startNode` (`useGraphConnections`'s
 * `handleReconnect`).
 */
export default function GraphStartEdge({
    id,
    sourcePosition,
    sourceX,
    sourceY,
    style,
    targetPosition,
    targetX,
    targetY,
}: EdgeProps) {
    const [edgePath] = getSmoothStepPath({
        borderRadius: 10,
        sourcePosition,
        sourceX,
        sourceY,
        targetPosition,
        targetX,
        targetY,
    });

    return (
        <BaseEdge className="fill-none stroke-stroke-neutral-tertiary stroke-2" id={id} path={edgePath} style={style} />
    );
}
