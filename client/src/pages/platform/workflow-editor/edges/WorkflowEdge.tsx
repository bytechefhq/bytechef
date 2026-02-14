import {NodeDataType} from '@/shared/types';
import {BaseEdge, EdgeLabelRenderer, EdgeProps, getSmoothStepPath} from '@xyflow/react';
import {PlusIcon} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import BranchCaseLabel from './BranchCaseLabel';
import computeEdgeButtonPosition from './computeEdgeButtonPosition';

export default function WorkflowEdge({
    data,
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
    const nodes = useWorkflowDataStore((state) => state.nodes);

    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);

    const [isDropzoneActive, setDropzoneActive] = useState<boolean>(false);

    const sourceNodeId = id.split('=>')[0];
    const targetNodeId = id.split('=>')[1];

    const sourceNode = nodes.find((node) => node.id === sourceNodeId);
    const targetNode = nodes.find((node) => node.id === targetNodeId);

    const isMiddleCaseEdge = !!(data as Record<string, unknown>)?.isMiddleCase;
    const isHorizontal = layoutDirection === 'LR';

    const correctedSourceX =
        !isHorizontal && isMiddleCaseEdge && sourceNode?.type === 'taskDispatcherTopGhostNode' ? targetX : sourceX;
    const correctedTargetX =
        !isHorizontal && isMiddleCaseEdge && targetNode?.type === 'taskDispatcherBottomGhostNode' ? sourceX : targetX;
    const correctedSourceY =
        isHorizontal && isMiddleCaseEdge && sourceNode?.type === 'taskDispatcherTopGhostNode' ? targetY : sourceY;
    const correctedTargetY =
        isHorizontal && isMiddleCaseEdge && targetNode?.type === 'taskDispatcherBottomGhostNode' ? sourceY : targetY;

    const [edgePath, edgeCenterX, edgeCenterY] = getSmoothStepPath({
        sourcePosition,
        sourceX: correctedSourceX,
        sourceY: correctedSourceY,
        targetPosition,
        targetX: correctedTargetX,
        targetY: correctedTargetY,
    });

    const caseKey = (targetNode?.data as NodeDataType)?.branchData?.caseKey;

    const sourceNodeComponentName = useMemo(() => (sourceNode?.data as NodeDataType)?.componentName, [sourceNode]);

    const isSourceTaskDispatcherTopGhostNode = sourceNode?.type === 'taskDispatcherTopGhostNode';

    const buttonPosition = useMemo(
        () =>
            computeEdgeButtonPosition({
                correctedSourceX,
                correctedSourceY,
                correctedTargetX,
                correctedTargetY,
                edgeCenterX,
                edgeCenterY,
                isHorizontal,
                sourceNodeComponentName: sourceNodeComponentName as string | undefined,
                sourceNodeTaskDispatcherId: (sourceNode?.data as NodeDataType)?.taskDispatcherId,
                sourceNodeType: sourceNode?.type,
                targetNodeType: targetNode?.type,
            }),
        [
            isHorizontal,
            correctedSourceX,
            correctedSourceY,
            correctedTargetX,
            correctedTargetY,
            sourceNode?.type,
            sourceNode?.data,
            targetNode?.type,
            sourceNodeComponentName,
            edgeCenterX,
            edgeCenterY,
        ]
    );

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
                <BranchCaseLabel
                    caseKey={caseKey}
                    edgeId={id}
                    layoutDirection={layoutDirection}
                    sourceX={sourceX}
                    sourceY={sourceY}
                    targetX={targetX}
                    targetY={targetY}
                />
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
                                    : 'size-6 border-2 border-stroke-neutral-tertiary bg-white hover:scale-110 hover:border-stroke-brand-secondary-hover'
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
