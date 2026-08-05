import '@/shared/styles/dropdownMenu.css';
import {ContextMenu, ContextMenuContent, ContextMenuItem, ContextMenuTrigger} from '@/components/ui/context-menu';
import {NodeDataType} from '@/shared/types';
import {BaseEdge, EdgeLabelRenderer, EdgeProps, getSmoothStepPath} from '@xyflow/react';
import {ClipboardPlusIcon, PlusIcon} from 'lucide-react';
import {type MouseEvent, useCallback, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';
import useCanvasDropzone from '../hooks/useCanvasDropzone';
import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import getTaskDispatcherContext from '../utils/getTaskDispatcherContext';
import pasteNode from '../utils/pasteNode';
import AddBranchChip from './AddBranchChip';
import BinaryCaseLabel from './BinaryCaseLabel';
import BranchCaseLabel from './BranchCaseLabel';
import GraphNodeLabel from './GraphNodeLabel';
import styles from './WorkflowEdge.module.css';
import computeBinaryCaseLabel from './computeBinaryCaseLabel';
import computeEdgeButtonPosition from './computeEdgeButtonPosition';
import computeEdgeCorrectedCoordinates from './computeEdgeCorrectedCoordinates';
import computeExitEdgeJogCenter from './computeExitEdgeJogCenter';
import {getTriggerFanInBusCenter, getTriggerFanInButtonPosition} from './computeTriggerFanIn';
import getExecutedEdgeStatus from './getExecutedEdgeStatus';

export default function WorkflowEdge({
    data,
    id,
    markerEnd,
    source,
    sourceHandleId,
    sourcePosition,
    sourceX,
    sourceY,
    style,
    targetPosition,
    targetX,
    targetY,
}: EdgeProps) {
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

    const {dropzoneHandlers, isDropzoneActive} = useCanvasDropzone('task');

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

    const isTriggerFanIn = !!(data as Record<string, unknown>)?.triggerFanIn;

    const busCenter = useMemo(
        () =>
            getTriggerFanInBusCenter({
                isTriggerFanIn,
                sourcePosition: correctedSourcePosition,
                sourceX: correctedSourceX,
                sourceY: correctedSourceY,
            }),
        [correctedSourcePosition, correctedSourceX, correctedSourceY, isTriggerFanIn]
    );

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
    const graphNodeIndex = (targetNode?.data as NodeDataType)?.graphData?.nodeIndex;

    const binaryCaseLabel = computeBinaryCaseLabel({
        layoutDirection,
        source,
        sourceHandleId,
        sourceX,
        targetY,
    });

    const addBranchPlaceholderId = (data as Record<string, unknown>)?.addBranchPlaceholderId as string | undefined;

    const sourceNodeComponentName = (sourceNode?.data as NodeDataType)?.componentName;

    const isSourceTaskDispatcherTopGhostNode = sourceNode?.type === 'taskDispatcherTopGhostNode';

    const buttonPosition = useMemo(() => {
        if (isTriggerFanIn && targetNode) {
            return getTriggerFanInButtonPosition({busCenter, targetX: correctedTargetX, targetY: correctedTargetY});
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
        busCenter,
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
        edgeCenterX,
        edgeCenterY,
    ]);

    const copiedNode = useWorkflowEditorStore((state) => state.copiedNode);
    const copiedWorkflowId = useWorkflowEditorStore((state) => state.copiedWorkflowId);

    const clusterElementsCanvasOpen = useWorkflowEditorStore((state) => state.clusterElementsCanvasOpen);
    const workflowIsRunning = useWorkflowEditorStore((state) => state.workflowIsRunning);
    const workflowTestNodeStates = useWorkflowEditorStore((state) => state.workflowTestNodeStates);

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

    const handleClick = (event: MouseEvent) => event.stopPropagation();

    const handleOpenChange = (open: boolean) => {
        if (open) {
            setMenuReady(false);
            setTimeout(() => setMenuReady(true), 200);
        } else {
            setMenuReady(false);
        }
    };

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

            {binaryCaseLabel && <BinaryCaseLabel edgeId={id} label={binaryCaseLabel} />}

            {typeof graphNodeIndex === 'number' && isSourceTaskDispatcherTopGhostNode && (
                <GraphNodeLabel
                    edgeId={id}
                    layoutDirection={layoutDirection}
                    nodeIndex={graphNodeIndex}
                    sourceX={sourceX}
                    sourceY={sourceY}
                    targetX={targetX}
                    targetY={targetY}
                />
            )}

            {addBranchPlaceholderId && (
                <AddBranchChip
                    edgeId={id}
                    layoutDirection={layoutDirection}
                    placeholderId={addBranchPlaceholderId}
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
                    {...dropzoneHandlers}
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
