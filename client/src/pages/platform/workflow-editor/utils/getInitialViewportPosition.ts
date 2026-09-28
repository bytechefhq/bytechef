import {CANVAS_LEFT_TO_RIGHT_LEFT_OFFSET, CANVAS_TOP_OFFSET, LayoutDirectionType} from '@/shared/constants';
import {XYPosition} from '@xyflow/react';

interface GetInitialViewportPositionProps {
    layoutDirection: LayoutDirectionType;
    offsetX: number;
}

export default function getInitialViewportPosition({
    layoutDirection,
    offsetX,
}: GetInitialViewportPositionProps): XYPosition {
    return {
        x: layoutDirection === 'LR' ? offsetX + CANVAS_LEFT_TO_RIGHT_LEFT_OFFSET : offsetX,
        y: CANVAS_TOP_OFFSET,
    };
}
