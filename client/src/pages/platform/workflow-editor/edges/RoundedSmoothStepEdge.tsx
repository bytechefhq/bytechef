import {TRIGGER_FAN_IN_BUS_OFFSET} from '@/shared/constants';
import {BaseEdge, EdgeProps, getSmoothStepPath} from '@xyflow/react';
import {twMerge} from 'tailwind-merge';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import computeExitEdgeJogCenter from './computeExitEdgeJogCenter';
import useExecutedEdgeStatus from './useExecutedEdgeStatus';

export default function RoundedSmoothStepEdge({
    data,
    id,
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

    const executedEdgeStatus = useExecutedEdgeStatus(id);

    // For trigger fan-in edges, pin the bus (the step's bend) a fixed short distance
    // below the trigger row so the trigger→bus drop stays short. The cross-axis
    // differs by layout: a downward (TB) bus uses centerY, a rightward (LR) bus centerX.
    //
    // Orientation comes from the layout direction, never from this edge's own Δx/Δy — the same
    // rule WorkflowEdge applies, and for the same reason. An OUTER trigger in a fan-in row sits
    // far to the side, so its Δx beats its Δy even in TB; keying off that gave it a vertical bus
    // while its siblings got a horizontal one, and the row's edges bent at visibly different
    // heights instead of meeting on one line.
    const isHorizontal = layoutDirection === 'LR';
    const isTriggerFanIn = !!(data as Record<string, unknown>)?.triggerFanIn;

    const busCenter = isTriggerFanIn
        ? isHorizontal
            ? {centerX: sourceX + TRIGGER_FAN_IN_BUS_OFFSET}
            : {centerY: sourceY + TRIGGER_FAN_IN_BUS_OFFSET}
        : {};

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
        isHorizontal,
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

    return (
        <BaseEdge
            className={twMerge(
                'fill-none stroke-stroke-neutral-tertiary stroke-2',
                executedEdgeStatus === 'COMPLETED' && 'stroke-green-500',
                executedEdgeStatus === 'FAILED' && 'stroke-red-500'
            )}
            id={id}
            path={edgePath}
            style={style}
        />
    );
}
