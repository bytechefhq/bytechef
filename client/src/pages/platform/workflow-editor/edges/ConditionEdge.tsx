import {useState} from 'react';
import {BaseEdge, EdgeProps, getSmoothStepPath} from 'reactflow';
import {twMerge} from 'tailwind-merge';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';

export default function ConditionEdge({
    id,
    sourceHandleId,
    sourcePosition,
    sourceX,
    sourceY,
    targetPosition,
    targetX,
    targetY,
}: EdgeProps) {
    const [isDropzoneActive, setDropzoneActive] = useState(false);

    const [edgePath] = getSmoothStepPath({
        offset: 100,
        sourcePosition,
        sourceX,
        sourceY,
        targetPosition,
        targetX,
        targetY,
    });

    const translateX = sourceHandleId === 'left' ? sourceX - 100 : sourceX + 100;

    const transformStyle = `translate(${translateX}, ${sourceY + 55})`;

    return (
        <>
            <BaseEdge path={edgePath} />

            <WorkflowNodesPopoverMenu condition edge hideTriggerComponents id={id}>
                <g
                    onDragEnter={() => setDropzoneActive(true)}
                    onDragLeave={() => setDropzoneActive(false)}
                    onDragOver={(event) => event.preventDefault()}
                    onDrop={() => setDropzoneActive(false)}
                    transform={transformStyle}
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
