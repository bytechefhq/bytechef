import {TRIGGER_FAN_IN_BUS_OFFSET} from '@/shared/constants';

export interface TriggerFanInBusCenterI {
    centerX?: number;
    centerY?: number;
}

interface GetTriggerFanInBusCenterProps {
    isTriggerFanIn: boolean;
    sourceX: number;
    sourceY: number;
    targetX: number;
    targetY: number;
}

interface GetTriggerFanInButtonPositionProps {
    busCenter: TriggerFanInBusCenterI;
    targetX: number;
    targetY: number;
}

export function getTriggerFanInBusCenter({
    isTriggerFanIn,
    sourceX,
    sourceY,
    targetX,
    targetY,
}: GetTriggerFanInBusCenterProps): TriggerFanInBusCenterI {
    if (!isTriggerFanIn) {
        return {};
    }

    const isVertical = Math.abs(targetY - sourceY) >= Math.abs(targetX - sourceX);

    if (isVertical) {
        return {centerY: sourceY + TRIGGER_FAN_IN_BUS_OFFSET};
    }

    return {centerX: sourceX + TRIGGER_FAN_IN_BUS_OFFSET};
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
