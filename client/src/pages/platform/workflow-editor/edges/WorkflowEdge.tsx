import {TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {BaseEdge, EdgeLabelRenderer, EdgeProps, getSmoothStepPath} from '@xyflow/react';
import {PlusIcon} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import BranchCaseLabel from './BranchCaseLabel';

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

    const sourceNodeId = id.split('=>')[0];
    const targetNodeId = id.split('=>')[1];

    const sourceNode = nodes.find((node) => node.id === sourceNodeId);
    const targetNode = nodes.find((node) => node.id === targetNodeId);

    const caseKey = (targetNode?.data as NodeDataType)?.branchData?.caseKey;

    const sourceNodeComponentName = useMemo(() => (sourceNode?.data as NodeDataType)?.componentName, [sourceNode]);

    const isSourceTaskDispatcherTopGhostNode = sourceNode?.type === 'taskDispatcherTopGhostNode';

    const buttonPosition = useMemo(() => {
        const isVerticalEdge = Math.abs(sourceY - targetY) > Math.abs(sourceX - targetX);

        const isEdgeFromBranchTopGhostNode =
            sourceNode?.type === 'taskDispatcherTopGhostNode' &&
            (sourceNode?.data as NodeDataType)?.taskDispatcherId?.startsWith('branch');

        if (isVerticalEdge && !isEdgeFromBranchTopGhostNode) {
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

        if (sourceNode?.type === 'taskDispatcherTopGhostNode') {
            posX = targetX;

            if (targetNode?.type === 'workflow' && isEdgeFromBranchTopGhostNode) {
                posY += 15;
            }
        } else if (sourceNode?.type === 'taskDispatcherBottomGhostNode') {
            posX = sourceX;
        } else if (targetNode?.type === 'taskDispatcherBottomGhostNode') {
            posX = sourceX;
        } else if (sourceNodeComponentName && TASK_DISPATCHER_NAMES.includes(sourceNodeComponentName as string)) {
            posX = targetX;
        }

        return {x: posX, y: posY};
    }, [
        sourceY,
        targetY,
        sourceX,
        targetX,
        sourceNode?.type,
        sourceNode?.data,
        targetNode?.type,
        sourceNodeComponentName,
        edgeCenterX,
        edgeCenterY,
    ]);

    useEffect(() => {
        const handleGlobalDragEnd = () => {
            setDropzoneActive(false);
        };

        document.addEventListener('dragend', handleGlobalDragEnd);
        document.addEventListener('drop', handleGlobalDragEnd);

        return () => {
            document.removeEventListener('dragend', handleGlobalDragEnd);
            document.removeEventListener('drop', handleGlobalDragEnd);
        };
    }, []);

    return (
        <>
            <BaseEdge
                className="fill-none stroke-gray-300 stroke-2"
                id={id}
                markerEnd={markerEnd}
                path={edgePath}
                style={style}
            />

            {caseKey && isSourceTaskDispatcherTopGhostNode && (
                <BranchCaseLabel caseKey={caseKey} edgeId={id} sourceY={sourceY} targetX={targetX} />
            )}

            <EdgeLabelRenderer key={id}>
                <WorkflowNodesPopoverMenu
                    edgeId={id}
                    hideClusterElementComponents
                    hideTriggerComponents
                    sourceNodeId={sourceNodeId}
                >
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
                                'flex cursor-pointer items-center justify-center rounded transition-all',
                                isDropzoneActive
                                    ? 'size-16 border-2 border-blue-100 bg-blue-100'
                                    : 'size-6 border-2 border-gray-300 bg-white hover:scale-110 hover:border-gray-400'
                            )}
                            id={`${id}-button`}
                            onDragEnter={() => setDropzoneActive(true)}
                            onDragLeave={(event) => {
                                const relatedTarget = event.relatedTarget as Node | null;

                                if (!relatedTarget || !event.currentTarget.contains(relatedTarget)) {
                                    setDropzoneActive(false);
                                }
                            }}
                            onDragOver={(event) => {
                                event.preventDefault();

                                setDropzoneActive(true);
                            }}
                            onDrop={(event) => {
                                event.preventDefault();

                                setDropzoneActive(false);
                            }}
                        >
                            <PlusIcon
                                className={twMerge(
                                    `text-muted-foreground`,
                                    isDropzoneActive ? 'size-14 text-muted-foreground/50' : 'size-3.5'
                                )}
                            />
                        </div>
                    </div>
                </WorkflowNodesPopoverMenu>
            </EdgeLabelRenderer>
        </>
    );
}
