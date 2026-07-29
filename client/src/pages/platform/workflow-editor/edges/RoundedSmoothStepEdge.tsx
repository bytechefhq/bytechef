import {TRIGGER_FAN_IN_BUS_OFFSET} from '@/shared/constants';
import {BaseEdge, EdgeProps, getSmoothStepPath} from '@xyflow/react';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import computeExitEdgeJogCenter from './computeExitEdgeJogCenter';

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

    // For trigger fan-in edges, pin the bus (the step's bend) a fixed short distance
    // below the trigger row so the trigger→bus drop stays short. The cross-axis
    // differs by layout: a downward (TB) bus uses centerY, a rightward (LR) bus centerX.
    const isTriggerFanIn = !!(data as Record<string, unknown>)?.triggerFanIn;
    const isVertical = Math.abs(targetY - sourceY) >= Math.abs(targetX - sourceX);

    const busCenter =
        isTriggerFanIn && isVertical
            ? {centerY: sourceY + TRIGGER_FAN_IN_BUS_OFFSET}
            : isTriggerFanIn
              ? {centerX: sourceX + TRIGGER_FAN_IN_BUS_OFFSET}
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

    return (
        <BaseEdge className="fill-none stroke-stroke-neutral-tertiary stroke-2" id={id} path={edgePath} style={style} />
    );
}
