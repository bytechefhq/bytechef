import {TRIGGER_FAN_IN_BUS_OFFSET} from '@/shared/constants';
import {Position} from '@xyflow/react';

export interface TriggerFanInBusCenterI {
    centerX?: number;
    centerY?: number;
}

interface GetTriggerFanInBusCenterProps {
    isTriggerFanIn: boolean;
    sourcePosition: Position;
    sourceX: number;
    sourceY: number;
}

interface GetTriggerFanInButtonPositionProps {
    busCenter: TriggerFanInBusCenterI;
    targetX: number;
    targetY: number;
}

export function getTriggerFanInBusCenter({
    isTriggerFanIn,
    sourcePosition,
    sourceX,
    sourceY,
}: GetTriggerFanInBusCenterProps): TriggerFanInBusCenterI {
    if (!isTriggerFanIn) {
        return {};
    }

    switch (sourcePosition) {
        case Position.Top:
            return {centerY: sourceY - TRIGGER_FAN_IN_BUS_OFFSET};
        case Position.Left:
            return {centerX: sourceX - TRIGGER_FAN_IN_BUS_OFFSET};
        case Position.Right:
            return {centerX: sourceX + TRIGGER_FAN_IN_BUS_OFFSET};
        default:
            return {centerY: sourceY + TRIGGER_FAN_IN_BUS_OFFSET};
    }
}

export function getTriggerFanInButtonPosition({busCenter, targetX, targetY}: GetTriggerFanInButtonPositionProps): {
    x: number;
    y: number;
} {
    if (busCenter.centerY !== undefined) {
        return {x: targetX, y: (busCenter.centerY + targetY) / 2};
    }

    if (busCenter.centerX !== undefined) {
        return {x: (busCenter.centerX + targetX) / 2, y: targetY};
    }

    return {x: targetX, y: targetY};
}
