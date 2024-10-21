import {BaseEdge, EdgeLabelRenderer, EdgeProps, getSmoothStepPath} from 'reactflow';

export default function ConditionEdge({
    id,
    label,
    sourcePosition,
    sourceX,
    sourceY,
    targetPosition,
    targetX,
    targetY,
}: EdgeProps) {
    const [edgePath] = getSmoothStepPath({
        sourcePosition,
        sourceX,
        sourceY,
        targetPosition,
        targetX,
        targetY,
    });

    return (
        <>
            <BaseEdge id={id} path={edgePath} />
            <EdgeLabelRenderer>
                <div
                    className="fill-none stroke-gray-400 stroke-1"
                    style={{
                        color: '#999',
                        fontWeight: 700,
                        position: 'absolute',
                        textTransform: 'uppercase',
                        transform: `translate(-50%, -50%) translate(${targetX}px, ${targetY - 40}px)`,
                    }}
                >
                    {label}
                </div>
            </EdgeLabelRenderer>
        </>
    );
}
