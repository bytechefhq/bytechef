import {Rect, Viewport} from '@xyflow/react';

interface GetAxisCenteredViewportProps {
    axisX: number;
    bounds: Rect;
    flowHeight: number;
    flowWidth: number;
    maxZoom: number;
    minZoom: number;
    padding: number;
}

/**
 * Fits the nodes into the flow like fitView, but keeps the given vertical axis (the column the layout centers the
 * node icons on) in the middle of the flow instead of the middle of the node bounds, which are skewed by the labels
 * rendered to the right of each node.
 */
export const getAxisCenteredViewport = ({
    axisX,
    bounds,
    flowHeight,
    flowWidth,
    maxZoom,
    minZoom,
    padding,
}: GetAxisCenteredViewportProps): Viewport => {
    const halfWidth = Math.max(axisX - bounds.x, bounds.x + bounds.width - axisX, 1);
    const paddingFactor = 1 + padding;

    const fittingZoom = Math.min(
        flowWidth / (halfWidth * 2 * paddingFactor),
        flowHeight / (Math.max(bounds.height, 1) * paddingFactor)
    );

    const zoom = Math.min(Math.max(fittingZoom, minZoom), maxZoom);

    return {
        x: flowWidth / 2 - axisX * zoom,
        y: flowHeight / 2 - (bounds.y + bounds.height / 2) * zoom,
        zoom,
    };
};
