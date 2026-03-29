import {BaseEdge, EdgeProps, getSmoothStepPath} from '@xyflow/react';

export default function RoundedSmoothStepEdge({
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

    return <BaseEdge className="fill-none stroke-gray-300 stroke-2" id={id} path={edgePath} style={style} />;
}
