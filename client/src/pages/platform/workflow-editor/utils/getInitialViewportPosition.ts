import {CANVAS_LEFT_TO_RIGHT_LEFT_OFFSET, CANVAS_TOP_OFFSET, LayoutDirectionType} from '@/shared/constants';
import {XYPosition} from '@xyflow/react';

interface GetInitialViewportPositionProps {
    canvasHeight: number;
    flowHeight: number;
    layoutDirection: LayoutDirectionType;
    offsetX: number;
}

export default function getInitialViewportPosition({
    canvasHeight,
    flowHeight,
    layoutDirection,
    offsetX,
}: GetInitialViewportPositionProps): XYPosition {
    if (layoutDirection === 'LR') {
        return {
            x: offsetX + CANVAS_LEFT_TO_RIGHT_LEFT_OFFSET,
            y: flowHeight ? Math.round((flowHeight - canvasHeight) / 2) : 0,
        };
    }

    return {x: offsetX, y: CANVAS_TOP_OFFSET};
}
