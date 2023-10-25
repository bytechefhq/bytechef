import {useState} from 'react';
import {EdgeProps, getBezierPath} from 'reactflow';
import {twMerge} from 'tailwind-merge';

import PopoverMenu from '../components/PopoverMenu';

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
                            'react-flow__edge pointer-events-auto cursor-pointer fill-white stroke-gray-400 hover:fill-gray-200 hover:scale-110',
                            isDropzoneActive &&
                                'scale-150 fill-blue-100 stroke-blue-100 z-40'
                        )}
                        id={id}
                        height={isDropzoneActive ? 72 : 24}
                        rx={4}
                        ry={4}
                        width={isDropzoneActive ? 72 : 24}
                        x={isDropzoneActive ? -36 : -12}
                        y={isDropzoneActive ? -36 : -12}
                    />

                    <text
                        className={twMerge(
                            'pointer-events-none select-none fill-gray-500'
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
