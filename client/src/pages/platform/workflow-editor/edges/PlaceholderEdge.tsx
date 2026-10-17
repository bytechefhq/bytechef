import {EdgeProps, getBezierPath, getSmoothStepPath} from '@xyflow/react';

import {getTriggerFanInBusCenter} from './computeTriggerFanIn';

export default function PlaceholderEdge({
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
    const isTriggerFanIn = !!(data as Record<string, unknown>)?.triggerFanIn;

    const pathParameters = {sourcePosition, sourceX, sourceY, targetPosition, targetX, targetY};

    const [edgePath] = isTriggerFanIn
        ? getSmoothStepPath({
              borderRadius: 10,
              ...getTriggerFanInBusCenter({isTriggerFanIn, sourcePosition, sourceX, sourceY}),
              ...pathParameters,
          })
        : getBezierPath(pathParameters);

    return (
        <path
            className="fill-none stroke-stroke-neutral-tertiary stroke-2 [stroke-dasharray:3,5]"
            d={edgePath}
            id={id}
            style={style}
        />
    );
}
