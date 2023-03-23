import {useState} from 'react';
import {EdgeProps, getBezierPath} from 'reactflow';
import {twMerge} from 'tailwind-merge';

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
    const [isDropzoneActive, setDropzoneActive] = useState<boolean>(false);

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
                <g
                    transform={`translate(${edgeCenterX}, ${edgeCenterY})`}
                    onDrop={() => setDropzoneActive(false)}
                    onDragOver={(event) => event.preventDefault()}
                    onDragEnter={() => setDropzoneActive(true)}
                    onDragLeave={() => setDropzoneActive(false)}
                >
                    <rect
                        className={twMerge(
                            'react-flow__edge pointer-events-auto cursor-pointer fill-white stroke-gray-400 hover:fill-gray-200',
                            isDropzoneActive && 'fill-gray-500 stroke-white'
                        )}
                        id={id}
                        height={20}
                        rx={4}
                        ry={4}
                        width={20}
                        x={-10}
                        y={-10}
                    />

                    <text
                        className={twMerge(
                            'pointer-events-none select-none fill-gray-500',
                            isDropzoneActive && 'fill-white'
                        )}
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
