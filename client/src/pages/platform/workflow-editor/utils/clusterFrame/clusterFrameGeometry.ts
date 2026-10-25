import {calculateNodeWidth} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import {
    CLUSTER_ELEMENT_NODE_WIDTH,
    CLUSTER_ELEMENT_PLACEHOLDER_WIDTH,
    NODE_HEIGHT,
    NODE_WIDTH,
    PLACEHOLDER_NODE_HEIGHT,
    ROOT_CLUSTER_WIDTH,
} from '@/shared/constants';
import {Node} from '@xyflow/react';

export const CLUSTER_FRAME_HEADER_HEIGHT = 40;

export const CLUSTER_FRAME_TITLE_LINE_HEIGHT = 20;

export const CLUSTER_FRAME_BOTTOM_PADDING = (CLUSTER_FRAME_HEADER_HEIGHT - CLUSTER_FRAME_TITLE_LINE_HEIGHT) / 2;

export const CLUSTER_FRAME_PADDING = 32;

export const CLUSTER_FRAME_BORDER_WIDTH = 2;

export const CLUSTER_FRAME_MIN_HEIGHT = 220;
export const CLUSTER_FRAME_MIN_WIDTH = 420;

export const CLUSTER_FRAME_LABEL_OVERHANG = 56;

const CLUSTER_FRAME_ROOT_HEIGHT = 72;
const CLUSTER_FRAME_HANDLE_LABEL_HEIGHT = 21;
const CLUSTER_FRAME_EDGE_LENGTH = 80;

export const CLUSTER_FRAME_MEMBER_ROW_Y =
    CLUSTER_FRAME_ROOT_HEIGHT + CLUSTER_FRAME_HANDLE_LABEL_HEIGHT + CLUSTER_FRAME_EDGE_LENGTH;

export const CLUSTER_FRAME_PLACEHOLDER_Y =
    CLUSTER_FRAME_ROOT_HEIGHT +
    CLUSTER_FRAME_HANDLE_LABEL_HEIGHT +
    (CLUSTER_FRAME_EDGE_LENGTH - PLACEHOLDER_NODE_HEIGHT) / 2;

interface ClusterMemberNodeDataI {
    clusterElementType?: string;
    clusterElementTypesCount?: number;
}

export interface ClusterMemberBoxI {
    height: number;
    width: number;
    x: number;
    y: number;
}

export interface ClusterFrameContentOriginI {
    x: number;
    y: number;
}

export const DEFAULT_CLUSTER_FRAME_CONTENT_ORIGIN: ClusterFrameContentOriginI = {
    x: CLUSTER_FRAME_PADDING,
    y: CLUSTER_FRAME_HEADER_HEIGHT,
};

export function toClusterFrameChildPosition(
    contentPosition: {x: number; y: number},
    contentOrigin: ClusterFrameContentOriginI = DEFAULT_CLUSTER_FRAME_CONTENT_ORIGIN
): {x: number; y: number} {
    return {x: contentPosition.x + contentOrigin.x, y: contentPosition.y + contentOrigin.y};
}

export function fromClusterFrameChildPosition(
    childPosition: {x: number; y: number},
    contentOrigin: ClusterFrameContentOriginI = DEFAULT_CLUSTER_FRAME_CONTENT_ORIGIN
): {x: number; y: number} {
    return {x: childPosition.x - contentOrigin.x, y: childPosition.y - contentOrigin.y};
}

export function getClusterMemberSize(memberNode: Node): {height: number; width: number} {
    if (memberNode.type === 'placeholder') {
        return {height: PLACEHOLDER_NODE_HEIGHT, width: CLUSTER_ELEMENT_PLACEHOLDER_WIDTH};
    }

    const memberNodeData = memberNode.data as ClusterMemberNodeDataI | undefined;

    if (memberNodeData?.clusterElementTypesCount) {
        return {
            height: NODE_HEIGHT,
            width: calculateNodeWidth(memberNodeData.clusterElementTypesCount) || ROOT_CLUSTER_WIDTH,
        };
    }

    if (memberNodeData?.clusterElementType) {
        return {height: CLUSTER_ELEMENT_NODE_WIDTH + CLUSTER_FRAME_LABEL_OVERHANG, width: CLUSTER_ELEMENT_NODE_WIDTH};
    }

    return {
        height: memberNode.measured?.height ?? memberNode.height ?? NODE_HEIGHT,
        width: memberNode.measured?.width ?? memberNode.width ?? NODE_WIDTH,
    };
}

function computeClusterContentExtent(childBoxes: ClusterMemberBoxI[]): {
    height: number;
    left: number;
    top: number;
    width: number;
} {
    const left = Math.min(0, ...childBoxes.map((childBox) => childBox.x));
    const top = Math.min(0, ...childBoxes.map((childBox) => childBox.y));
    const right = Math.max(...childBoxes.map((childBox) => childBox.x + childBox.width));
    const bottom = Math.max(...childBoxes.map((childBox) => childBox.y + childBox.height));

    return {height: bottom - top, left, top, width: right - left};
}

export function computeClusterFrameSize(childBoxes: ClusterMemberBoxI[]): {height: number; width: number} {
    if (childBoxes.length === 0) {
        return {height: CLUSTER_FRAME_MIN_HEIGHT, width: CLUSTER_FRAME_MIN_WIDTH};
    }

    const extent = computeClusterContentExtent(childBoxes);

    return {
        height: Math.max(
            CLUSTER_FRAME_MIN_HEIGHT,
            CLUSTER_FRAME_HEADER_HEIGHT + extent.height + CLUSTER_FRAME_BOTTOM_PADDING
        ),
        width: Math.max(CLUSTER_FRAME_MIN_WIDTH, extent.width + 2 * CLUSTER_FRAME_PADDING),
    };
}

export function computeClusterFrameContentOrigin(
    childBoxes: ClusterMemberBoxI[],
    frameWidth: number
): ClusterFrameContentOriginI {
    if (childBoxes.length === 0) {
        return DEFAULT_CLUSTER_FRAME_CONTENT_ORIGIN;
    }

    const extent = computeClusterContentExtent(childBoxes);

    return {
        x: (frameWidth - extent.width) / 2 - extent.left,
        y: CLUSTER_FRAME_HEADER_HEIGHT + Math.max(0, -extent.top),
    };
}
