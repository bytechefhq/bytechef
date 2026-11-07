import {Rect} from '@xyflow/react';

interface GetResizedCanvasViewportYProps {
    canvasHeight: number;
    nodesBounds: Rect;
    previousCanvasHeight: number;
    topInset: number;
    viewportY: number;
    zoom: number;
}

export default function getResizedCanvasViewportY({
    canvasHeight,
    nodesBounds,
    previousCanvasHeight,
    topInset,
    viewportY,
    zoom,
}: GetResizedCanvasViewportYProps): number {
    const centeredY = viewportY + (canvasHeight - previousCanvasHeight) / 2;

    if (centeredY >= viewportY) {
        return centeredY;
    }

    const topAlignedY = topInset - nodesBounds.y * zoom;

    return Math.max(centeredY, Math.min(topAlignedY, viewportY));
}
