import '@/shared/styles/dropdownMenu.css';
import {ContextMenu, ContextMenuContent, ContextMenuItem, ContextMenuTrigger} from '@/components/ui/context-menu';
import {TRIGGER_FAN_IN_BUS_OFFSET} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {BaseEdge, EdgeLabelRenderer, EdgeProps, getSmoothStepPath} from '@xyflow/react';
import {ClipboardPlusIcon, PlusIcon} from 'lucide-react';
import {type DragEvent, type MouseEvent, useCallback, useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';
import useWorkflowTestNodeStates from '../hooks/useWorkflowTestNodeStates';
import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import getTaskDispatcherContext from '../utils/getTaskDispatcherContext';
import pasteNode from '../utils/pasteNode';
import BranchCaseLabel from './BranchCaseLabel';
import styles from './WorkflowEdge.module.css';
import computeEdgeButtonPosition from './computeEdgeButtonPosition';
import computeEdgeCorrectedCoordinates from './computeEdgeCorrectedCoordinates';
import computeExitEdgeJogCenter from './computeExitEdgeJogCenter';
import getExecutedEdgeStatus from './getExecutedEdgeStatus';

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
    const [isDropzoneActive, setDropzoneActive] = useState<boolean>(false);
    const [menuReady, setMenuReady] = useState<boolean>(false);

    const {edges, nodes, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            edges: state.edges,
            nodes: state.nodes,
            workflow: state.workflow,
        }))
    );

    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);

    const {updateWorkflowMutation} = useWorkflowEditor();

    const sourceNodeId = id.split('=>')[0];
    const targetNodeId = id.split('=>')[1];

    const sourceNode = nodes.find((node) => node.id === sourceNodeId);
    const targetNode = nodes.find((node) => node.id === targetNodeId);

    const isMiddleCaseEdge = !!(data as Record<string, unknown>)?.isMiddleCase;
    const isHorizontal = layoutDirection === 'LR';

    const {
        correctedSourcePosition,
        correctedSourceX,
        correctedSourceY,
        correctedTargetPosition,
        correctedTargetX,
        correctedTargetY,
    } = computeEdgeCorrectedCoordinates({
        isHorizontal,
        isMiddleCaseEdge,
        sourceNodeType: sourceNode?.type,
        sourcePosition,
        sourceX,
        sourceY,
        targetNodeType: targetNode?.type,
        targetPosition,
        targetX,
        targetY,
    });

    // For the trigger fan-in "+" edge, pin the bus a fixed short distance below the
    // trigger row (matching RoundedSmoothStepEdge) so the bend aligns with the side
    // connectors and the "+" sits on the lower leg above the first task.
    const isTriggerFanIn = !!(data as Record<string, unknown>)?.triggerFanIn;
    // Orientation comes from the layout direction, never from this edge's own Δx/Δy. A trigger
    // far down the column has a dominant Δy even in LR, so per-edge geometry gave sibling fan-in
    // edges different bus orientations — and disagreed with the "+" placement below, which has
    // always keyed off the direction. The button then landed on the corner instead of the leg.
    const busCenter = isTriggerFanIn
        ? isHorizontal
            ? {centerX: correctedSourceX + TRIGGER_FAN_IN_BUS_OFFSET}
            : {centerY: correctedSourceY + TRIGGER_FAN_IN_BUS_OFFSET}
        : {};

    const exitJogCenter = computeExitEdgeJogCenter({
        correctedSourceX,
        correctedSourceY,
        correctedTargetX,
        correctedTargetY,
        isHorizontal,
        isTriggerFanIn,
        targetNodeType: targetNode?.type,
    });

    const [edgePath, edgeCenterX, edgeCenterY] = getSmoothStepPath({
        borderRadius: 10,
        ...busCenter,
        ...exitJogCenter,
        sourcePosition: correctedSourcePosition,
        sourceX: correctedSourceX,
        sourceY: correctedSourceY,
        targetPosition: correctedTargetPosition,
        targetX: correctedTargetX,
        targetY: correctedTargetY,
    });

    const caseKey = (targetNode?.data as NodeDataType)?.branchData?.caseKey;

    const sourceNodeComponentName = (sourceNode?.data as NodeDataType)?.componentName;

    const isSourceTaskDispatcherTopGhostNode = sourceNode?.type === 'taskDispatcherTopGhostNode';

    const buttonPosition = useMemo(() => {
        // For the trigger fan-in "+", center it on the lower leg using the raw handle
        // coordinates (geometry-independent — the reported edge center is unreliable
        // for the L-shaped middle edge of an even trigger count): midway between the
        // bus (a fixed offset below the trigger handles) and the target handle,
        // pinned to the target's cross-axis so it sits on the center connector.
        if (isTriggerFanIn && targetNode) {
            if (isHorizontal) {
                const busX = sourceX + TRIGGER_FAN_IN_BUS_OFFSET;

                return {x: (busX + targetX) / 2, y: targetY};
            }

            const busY = sourceY + TRIGGER_FAN_IN_BUS_OFFSET;

            return {x: targetX, y: (busY + targetY) / 2};
        }

        return computeEdgeButtonPosition({
            correctedSourceX,
            correctedSourceY,
            correctedTargetX,
            correctedTargetY,
            edgeCenterX,
            edgeCenterY,
            isHorizontal,
            sourceNodeComponentName,
            sourceNodeTaskDispatcherId: (sourceNode?.data as NodeDataType)?.taskDispatcherId,
            sourceNodeType: sourceNode?.type,
            targetNodeType: targetNode?.type,
        });
    }, [
        isTriggerFanIn,
        isHorizontal,
        correctedSourceX,
        correctedSourceY,
        correctedTargetX,
        correctedTargetY,
        sourceNode?.type,
        sourceNode?.data,
        targetNode,
        sourceNodeComponentName,
        sourceX,
        sourceY,
        targetX,
        targetY,
        edgeCenterX,
        edgeCenterY,
    ]);

    const copiedNode = useWorkflowEditorStore((state) => state.copiedNode);
    const copiedWorkflowId = useWorkflowEditorStore((state) => state.copiedWorkflowId);

    const clusterElementsCanvasOpen = useWorkflowEditorStore((state) => state.clusterElementsCanvasOpen);
    const workflowIsRunning = useWorkflowEditorStore((state) => state.workflowIsRunning);
    const workflowTestNodeStates = useWorkflowTestNodeStates();

    const executedEdgeStatus = getExecutedEdgeStatus(sourceNode, targetNode, workflowTestNodeStates);

    const canPaste = useMemo(
        () => !clusterElementsCanvasOpen && !!copiedNode && copiedWorkflowId === workflow.id,
        [clusterElementsCanvasOpen, copiedNode, copiedWorkflowId, workflow.id]
    );

    const copiedNodeLabel = copiedNode?.label || '';

    const displayLabel = useMemo(() => {
        if (!copiedNode) {
            return '';
        }

        return `${copiedNodeLabel} (${copiedNode.name})`;
    }, [copiedNode, copiedNodeLabel]);

    const handlePasteClick = useCallback(() => {
        if (!updateWorkflowMutation) {
            return;
        }

        const matchingEdge = edges.find((candidateEdge) => candidateEdge.id === id);

        const taskDispatcherContext = getTaskDispatcherContext({
            edge: matchingEdge,
            node: matchingEdge?.type === 'workflow' ? undefined : sourceNode,
            nodes,
        });

        pasteNode({
            sourceNodeName: sourceNodeId,
            taskDispatcherContext,
            updateWorkflowMutation,
        });
    }, [edges, id, nodes, sourceNode, sourceNodeId, updateWorkflowMutation]);

    const handleDragEnter = () => setDropzoneActive(true);

    const handleDragLeave = (event: DragEvent) => {
        const relatedTarget = event.relatedTarget as Node | null;

        if (!relatedTarget || !event.currentTarget.contains(relatedTarget)) {
            setDropzoneActive(false);
        }
    };

    const handleDragOver = (event: DragEvent) => {
        event.preventDefault();

        setDropzoneActive(true);
    };

    const handleDrop = (event: DragEvent) => {
        event.preventDefault();

        setDropzoneActive(false);
    };

    const handleClick = (event: MouseEvent) => event.stopPropagation();

    const handleOpenChange = (open: boolean) => {
        if (open) {
            setMenuReady(false);
            setTimeout(() => setMenuReady(true), 200);
        } else {
            setMenuReady(false);
        }
    };

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
                className={twMerge(
                    'fill-none stroke-stroke-neutral-tertiary stroke-2',
                    workflowIsRunning && styles.runningPath,
                    executedEdgeStatus === 'COMPLETED' && 'stroke-green-500',
                    executedEdgeStatus === 'FAILED' && 'stroke-red-500'
                )}
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
                <div
                    className="nodrag nopan p-8"
                    id={id}
                    onClick={handleClick}
                    onDragEnter={handleDragEnter}
                    onDragLeave={handleDragLeave}
                    onDragOver={handleDragOver}
                    onDrop={handleDrop}
                    style={{
                        pointerEvents: 'all',
                        position: 'absolute',
                        transform: `translate(-50%, -50%) translate(${buttonPosition.x}px,${buttonPosition.y}px)`,
                        zIndex: isDropzoneActive ? 40 : 'auto',
                    }}
                >
                    <ContextMenu onOpenChange={handleOpenChange}>
                        <ContextMenuTrigger asChild disabled={!canPaste}>
                            <div>
                                <WorkflowNodesPopoverMenu
                                    edgeId={id}
                                    hideClusterElementComponents
                                    hideTriggerComponents
                                    showPaste={canPaste}
                                    sourceNodeId={sourceNodeId}
                                >
                                    <div
                                        className={twMerge(
                                            'flex cursor-pointer items-center justify-center rounded border-2 transition-all',
                                            isDropzoneActive
                                                ? 'size-16 border-surface-brand-secondary-hover bg-surface-brand-secondary-hover'
                                                : 'size-6 border-stroke-neutral-tertiary bg-white hover:scale-110 hover:border-stroke-brand-secondary-hover'
                                        )}
                                        id={`${id}-button`}
                                    >
                                        <PlusIcon
                                            className={twMerge(
                                                'text-content-neutral-secondary',
                                                isDropzoneActive
                                                    ? 'size-14 text-content-neutral-secondary/50'
                                                    : 'size-3.5'
                                            )}
                                        />
                                    </div>
                                </WorkflowNodesPopoverMenu>
                            </div>
                        </ContextMenuTrigger>

                        <ContextMenuContent
                            className={twMerge(
                                'w-workflow-node-context-menu-width p-0',
                                !menuReady && 'pointer-events-none'
                            )}
                        >
                            <ContextMenuItem
                                className="dropdown-menu-item flex w-full flex-col items-start gap-1"
                                disabled={!canPaste}
                                onClick={handlePasteClick}
                            >
                                <div className="flex w-full items-center gap-2 self-stretch text-content-neutral-primary">
                                    <ClipboardPlusIcon className="size-4 shrink-0" />

                                    <span>Paste Here</span>
                                </div>

                                <div className="flex w-full items-center gap-2 text-content-neutral-secondary">
                                    <span className="flex size-4 shrink-0 items-center justify-center overflow-hidden [&>svg]:size-4">
                                        {copiedNode?.icon ?? null}
                                    </span>

                                    <span className="line-clamp-1 flex-1 text-xs font-normal" title={displayLabel}>
                                        {displayLabel}
                                    </span>
                                </div>
                            </ContextMenuItem>
                        </ContextMenuContent>
                    </ContextMenu>
                </div>
            </EdgeLabelRenderer>
        </>
    );
}
