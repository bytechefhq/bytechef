import {TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {BaseEdge, EdgeLabelRenderer, EdgeProps, getSmoothStepPath} from '@xyflow/react';
import {PlusIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';

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
    const {nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
        }))
    );

    const [isDropzoneActive, setDropzoneActive] = useState<boolean>(false);

    const [edgePath, edgeCenterX, edgeCenterY] = getSmoothStepPath({
        sourcePosition,
        sourceX,
        sourceY,
        targetPosition,
        targetX,
        targetY,
    });

    const sourceNodeId = useMemo(() => id.split('=>')[0], [id]);

    const sourceNode = useMemo(() => nodes.find((node) => node.id === sourceNodeId), [nodes, sourceNodeId]);

    const sourceNodeComponentName = useMemo(() => (sourceNode?.data as NodeDataType)?.componentName, [sourceNode]);

    const buttonPosition = useMemo(() => {
        const isVerticalEdge = Math.abs(sourceY - targetY) > Math.abs(sourceX - targetX);

        if (isVerticalEdge) {
            return {
                x: edgeCenterX,
                y: edgeCenterY,
            };
        }

        let posX;
        let posY;

        if (sourceX < targetX) {
            posY = Math.min(sourceY, targetY) + Math.abs(targetY - sourceY) * 0.5;
        } else {
            posY = Math.min(sourceY, targetY) + Math.abs(targetY - sourceY) * 0.5;
        }

        if (id.includes('bottom-ghost')) {
            posX = sourceX;
        } else if (sourceNodeComponentName && TASK_DISPATCHER_NAMES.includes(sourceNodeComponentName as string)) {
            posX = targetX;
        }

        return {x: posX, y: posY};
    }, [sourceY, targetY, sourceX, targetX, id, sourceNodeComponentName, edgeCenterX, edgeCenterY]);

    return (
        <>
            <BaseEdge
                className="fill-none stroke-gray-300 stroke-2"
                id={id}
                markerEnd={markerEnd}
                path={edgePath}
                style={style}
            />

            <EdgeLabelRenderer key={id}>
                <WorkflowNodesPopoverMenu edgeId={id} hideTriggerComponents sourceNodeId={sourceNodeId}>
                    <div
                        className="nodrag nopan"
                        id={id}
                        style={{
                            pointerEvents: 'all',
                            position: 'absolute',
                            transform: `translate(-50%, -50%) translate(${buttonPosition.x}px,${buttonPosition.y}px)`,
                            zIndex: isDropzoneActive ? 40 : 'auto',
                        }}
                    >
                        <div
                            className={twMerge(
                                'flex size-6 cursor-pointer items-center justify-center rounded border-2 border-gray-300 bg-white transition-all hover:scale-110 hover:border-gray-400',
                                isDropzoneActive && 'z-40 size-14 scale-150 border-blue-100 bg-blue-100'
                            )}
                            id={`${id}-button`}
                            onDragEnter={() => setDropzoneActive(true)}
                            onDragLeave={() => setDropzoneActive(false)}
                            onDragOver={() => {
                                if (!isDropzoneActive) {
                                    setDropzoneActive(true);
                                }
                            }}
                            onDrop={() => setDropzoneActive(false)}
                        >
                            <PlusIcon className="size-3.5 text-muted-foreground" />
                        </div>
                    </div>
                </WorkflowNodesPopoverMenu>
            </EdgeLabelRenderer>
        </>
    );
}
