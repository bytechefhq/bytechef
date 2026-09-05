import {BaseEdge, EdgeProps, getSmoothStepPath} from '@xyflow/react';
import {twMerge} from 'tailwind-merge';

import useWorkflowIsRunning from '../hooks/useWorkflowIsRunning';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import AddBranchChip from './AddBranchChip';
import BinaryCaseLabel from './BinaryCaseLabel';
import styles from './WorkflowEdge.module.css';
import computeBinaryCaseLabel from './computeBinaryCaseLabel';
import computeExitEdgeJogCenter from './computeExitEdgeJogCenter';
import {getTriggerFanInBusCenter} from './computeTriggerFanIn';
import useExecutedEdgeStatus from './useExecutedEdgeStatus';

export default function RoundedSmoothStepEdge({
    data,
    id,
    source,
    sourceHandleId,
    sourcePosition,
    sourceX,
    sourceY,
    style,
    target,
    targetPosition,
    targetX,
    targetY,
}: EdgeProps) {
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);
    const workflowIsRunning = useWorkflowIsRunning();
    const executedEdgeStatus = useExecutedEdgeStatus(id);

    const isTriggerFanIn = !!(data as Record<string, unknown>)?.triggerFanIn;

    const addBranchPlaceholderId = (data as Record<string, unknown>)?.addBranchPlaceholderId as string | undefined;

    const busCenter = getTriggerFanInBusCenter({
        isTriggerFanIn,
        sourcePosition,
        sourceX,
        sourceY,
    });

    // Smoothstep edges into a bottom bar (empty-case placeholder trails in the
    // editor, EVERY trailing edge in the read-only conversion where all edges
    // become 'smoothstep') bend beside the bar instead of at the path midpoint
    // — the same no-crossing rule WorkflowEdge applies to its exit edges. This
    // component has no node lookup, so the bar is detected by its id suffix.
    const exitJogCenter = computeExitEdgeJogCenter({
        correctedSourceX: sourceX,
        correctedSourceY: sourceY,
        correctedTargetX: targetX,
        correctedTargetY: targetY,
        isHorizontal: layoutDirection === 'LR',
        isTriggerFanIn,
        targetNodeType: target.endsWith('-bottom-ghost') ? 'taskDispatcherBottomGhostNode' : undefined,
    });

    const [edgePath] = getSmoothStepPath({
        borderRadius: 10,
        ...busCenter,
        ...exitJogCenter,
        sourcePosition,
        sourceX,
        sourceY,
        targetPosition,
        targetX,
        targetY,
    });

    const binaryCaseLabel = computeBinaryCaseLabel({
        layoutDirection,
        source,
        sourceHandleId,
        sourceX,
        targetY,
    });

    return (
        <>
            <BaseEdge
                className={twMerge(
                    'fill-none stroke-stroke-neutral-tertiary stroke-2',
                    // The same canvas-wide running dash WorkflowEdge draws. A dispatcher's plumbing shares
                    // its path with the lane edges beside it (a "+" placeholder's edge leaves the bar from
                    // the lane's own handle), so a solid line here shows through the gaps of the lane's
                    // dashes and makes one side of the frame read as not running.
                    workflowIsRunning && styles.runningPath,
                    executedEdgeStatus === 'COMPLETED' && 'stroke-green-500',
                    executedEdgeStatus === 'FAILED' && 'stroke-red-500'
                )}
                id={id}
                path={edgePath}
                style={style}
            />

            {binaryCaseLabel && <BinaryCaseLabel edgeId={id} label={binaryCaseLabel} />}

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
        </>
    );
}
