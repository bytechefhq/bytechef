import {useState} from 'react';
import {EdgeProps, getBezierPath} from 'reactflow';
import {twMerge} from 'tailwind-merge';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';

export default function WorkflowEdge({
    id,
    markerEnd,
    sourcePosition,
    sourceX,
    sourceY,
    style,
    targetPosition,
    targetX,
    targetY,
}: EdgeProps) {
    const [isDropzoneActive, setDropzoneActive] = useState<boolean>(false);

    const [edgePath, edgeCenterX, edgeCenterY] = getBezierPath({
        sourcePosition,
        sourceX,
        sourceY,
        targetPosition,
        targetX,
        targetY,
    });

    return (
        <>
            <path
                className="fill-none stroke-gray-400 stroke-1"
                d={edgePath}
                id={id}
                markerEnd={markerEnd}
                style={style}
            />

            <WorkflowNodesPopoverMenu edge hideTriggerComponents id={id}>
                <g
                    onDragEnter={() => setDropzoneActive(true)}
                    onDragLeave={() => setDropzoneActive(false)}
                    onDragOver={(event) => event.preventDefault()}
                    onDrop={() => setDropzoneActive(false)}
                    transform={`translate(${edgeCenterX}, ${edgeCenterY})`}
                >
                    <rect
                        className={twMerge(
                            'react-flow__edge pointer-events-auto cursor-pointer fill-white stroke-gray-400 hover:fill-gray-200 hover:scale-110',
                            isDropzoneActive && 'scale-150 fill-blue-100 stroke-blue-100 z-40'
                        )}
                        height={isDropzoneActive ? 72 : 24}
                        id={id}
                        rx={4}
                        ry={4}
                        width={isDropzoneActive ? 72 : 24}
                        x={isDropzoneActive ? -36 : -12}
                        y={isDropzoneActive ? -36 : -12}
                    />

                    <text className={twMerge('pointer-events-none select-none fill-gray-500')} x={-5} y={5}>
                        +
                    </text>
                </g>
            </WorkflowNodesPopoverMenu>
        </>
    );
}
