import {EdgeProps, getSmoothStepPath} from '@xyflow/react';
import {useState} from 'react';
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

    const [edgePath, edgeCenterX, edgeCenterY] = getSmoothStepPath({
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
                className="fill-none stroke-gray-300 stroke-2"
                d={edgePath}
                id={id}
                markerEnd={markerEnd}
                style={style}
            />

            <WorkflowNodesPopoverMenu edge hideTriggerComponents sourceNodeId={id}>
                <g
                    onDragEnter={() => setDropzoneActive(true)}
                    onDragLeave={() => setDropzoneActive(false)}
                    onDragOver={(event) => event.preventDefault()}
                    onDrop={() => setDropzoneActive(false)}
                    transform={`translate(${edgeCenterX}, ${edgeCenterY})`}
                >
                    <rect
                        className={twMerge(
                            'react-flow__edge pointer-events-auto cursor-pointer fill-white stroke-gray-400 hover:scale-110 hover:fill-gray-200',
                            isDropzoneActive && 'z-40 scale-150 fill-blue-100 stroke-blue-100'
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
