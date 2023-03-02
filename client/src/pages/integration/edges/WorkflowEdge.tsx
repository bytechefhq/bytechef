import {EdgeProps, getBezierPath} from 'reactflow';
import PopoverMenu from '../components/PopoverMenu';

export default function WorkflowEdge({
    id,
    sourceX,
    sourceY,
    targetX,
    targetY,
    sourcePosition,
    targetPosition,
    style,
    markerEnd,
}: EdgeProps) {
    const [edgePath, edgeCenterX, edgeCenterY] = getBezierPath({
        sourceX,
        sourceY,
        sourcePosition,
        targetX,
        targetY,
        targetPosition,
    });

    return (
        <>
            <path
                id={id}
                style={style}
                className="fill-none stroke-gray-400 stroke-1"
                d={edgePath}
                markerEnd={markerEnd}
            />

            <PopoverMenu id={id} edge>
                <g transform={`translate(${edgeCenterX}, ${edgeCenterY})`}>
                    <rect
                        className="pointer-events-auto cursor-pointer fill-white stroke-gray-400 hover:fill-gray-200"
                        height={20}
                        rx={4}
                        ry={4}
                        width={20}
                        x={-10}
                        y={-10}
                    />

                    <text
                        className="pointer-events-none select-none fill-gray-500"
                        y={5}
                        x={-5}
                    >
                        +
                    </text>
                </g>
            </PopoverMenu>
        </>
    );
}
