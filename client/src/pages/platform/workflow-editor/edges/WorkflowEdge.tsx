import {ContextMenu, ContextMenuContent, ContextMenuItem, ContextMenuTrigger} from '@/components/ui/context-menu';
import {NodeDataType} from '@/shared/types';
import {BaseEdge, EdgeLabelRenderer, EdgeProps, getSmoothStepPath} from '@xyflow/react';
import {ClipboardPlusIcon, PlusIcon} from 'lucide-react';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';
import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import getTaskDispatcherContext from '../utils/getTaskDispatcherContext';
import pasteNode from '../utils/pasteNode';
import BranchCaseLabel from './BranchCaseLabel';
import computeEdgeButtonPosition from './computeEdgeButtonPosition';
import computeEdgeCorrectedCoordinates from './computeEdgeCorrectedCoordinates';

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

    const [edgePath, edgeCenterX, edgeCenterY] = getSmoothStepPath({
        borderRadius: 10,
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
                sourceNodeComponentName,
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

    const copiedNode = useWorkflowEditorStore((state) => state.copiedNode);
    const copiedWorkflowId = useWorkflowEditorStore((state) => state.copiedWorkflowId);

    const canPaste = !!copiedNode && copiedWorkflowId === workflow.id;

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
                    hasEdgeButton
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
                        className="nodrag nopan p-8"
                        id={id}
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
                        style={{
                            pointerEvents: 'all',
                            position: 'absolute',
                            transform: `translate(-50%, -50%) translate(${buttonPosition.x}px,${buttonPosition.y}px)`,
                            zIndex: isDropzoneActive ? 40 : 'auto',
                        }}
                    >
                        <ContextMenu>
                            <ContextMenuTrigger asChild disabled={!canPaste}>
                                <div
                                    className={twMerge(
                                        'flex cursor-pointer items-center justify-center rounded transition-all',
                                        isDropzoneActive
                                            ? 'size-16 border-2 border-blue-100 bg-blue-100'
                                            : 'size-6 border-2 border-stroke-neutral-tertiary bg-white hover:scale-110 hover:border-stroke-brand-secondary-hover'
                                    )}
                                    id={`${id}-button`}
                                >
                                    <PlusIcon
                                        className={twMerge(
                                            `text-muted-foreground`,
                                            isDropzoneActive ? 'size-14 text-muted-foreground/50' : 'size-3.5'
                                        )}
                                    />
                                </div>
                            </ContextMenuTrigger>

                            <ContextMenuContent className="w-[280px]">
                                <ContextMenuItem
                                    className="flex w-full cursor-pointer flex-col items-start gap-1 px-[var(--spacing-1,4px)] py-0"
                                    disabled={!canPaste}
                                    onClick={handlePasteClick}
                                >
                                    <div className="flex w-full items-center gap-2 self-stretch text-content-neutral-primary">
                                        <ClipboardPlusIcon className="size-4 shrink-0" />

                                        <span>Paste Here</span>
                                    </div>

                                    <div className="flex w-full items-center gap-2 text-content-neutral-secondary">
                                        <span className="flex size-4 shrink-0 items-center justify-center">
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
                </WorkflowNodesPopoverMenu>
            </EdgeLabelRenderer>
        </>
    );
}
