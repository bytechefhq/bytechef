import {TRIGGER_FAN_IN_BUS_OFFSET} from '@/shared/constants';
import {BaseEdge, EdgeProps, getSmoothStepPath} from '@xyflow/react';

export default function RoundedSmoothStepEdge({
    data,
    id,
    sourcePosition,
    sourceX,
    sourceY,
    style,
    targetPosition,
    targetX,
    targetY,
}: EdgeProps) {
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

    const [edgePath] = getSmoothStepPath({
        borderRadius: 10,
        ...busCenter,
        sourcePosition,
        sourceX,
        sourceY,
        targetPosition,
        targetX,
        targetY,
    });

    return <BaseEdge className="fill-none stroke-gray-300 stroke-2" id={id} path={edgePath} style={style} />;
}
