import {LayoutDirectionType} from '@/shared/constants';
import {Rect, XYPosition} from '@xyflow/react';

interface GetInitialViewportPositionProps {
    canvasHeight: number;
    layoutDirection: LayoutDirectionType;
    leftInset: number;
    nodesBounds: Rect;
    offsetX: number;
    topInset: number;
}

export default function getInitialViewportPosition({
    canvasHeight,
    layoutDirection,
    leftInset,
    nodesBounds,
    offsetX,
    topInset,
}: GetInitialViewportPositionProps): XYPosition {
    const topAlignedY = topInset - nodesBounds.y;

    if (layoutDirection !== 'LR') {
        return {x: offsetX, y: topAlignedY};
    }

    const x = offsetX + leftInset - nodesBounds.x;

    if (canvasHeight <= 0) {
        return {x, y: topAlignedY};
    }

    const centeredY = canvasHeight / 2 - (nodesBounds.y + nodesBounds.height / 2);

    return {x, y: Math.max(centeredY, topAlignedY)};
}
