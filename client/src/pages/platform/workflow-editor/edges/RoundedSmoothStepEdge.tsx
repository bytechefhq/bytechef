import {BaseEdge, EdgeProps, getSmoothStepPath} from '@xyflow/react';

import {getTriggerFanInBusCenter} from './computeTriggerFanIn';

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
    const busCenter = getTriggerFanInBusCenter({
        isTriggerFanIn: !!(data as Record<string, unknown>)?.triggerFanIn,
        sourceX,
        sourceY,
        targetX,
        targetY,
    });

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
