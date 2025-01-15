import {EdgeProps, getBezierPath} from '@xyflow/react';

export default function PlaceholderEdge({
    id,
    sourcePosition,
    sourceX,
    sourceY,
    style,
    targetPosition,
    targetX,
    targetY,
}: EdgeProps) {
    const [edgePath] = getBezierPath({
        sourcePosition,
        sourceX,
        sourceY,
        targetPosition,
        targetX,
        targetY,
    });

    return (
        <path
            className="fill-none stroke-gray-300 stroke-2 [stroke-dasharray:3,5]"
            d={edgePath}
            id={id}
            style={style}
        />
    );
}
