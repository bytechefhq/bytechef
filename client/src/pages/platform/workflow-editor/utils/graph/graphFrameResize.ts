import {LayoutDirectionType} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';

import {computeGraphFrameSize, getGraphFrameId} from './graphFrameGeometry';
import {collectGraphMemberBoxes} from './graphMemberPlacement';

interface ResizeGraphFrameForMembersProps {
    direction?: LayoutDirectionType;
    graphId: string;
    nodes: Node[];
}

export function resizeGraphFrameForMembers({
    direction = 'TB',
    graphId,
    nodes,
}: ResizeGraphFrameForMembersProps): Node[] {
    const frameId = getGraphFrameId(graphId);
    const frameIndex = nodes.findIndex((node) => node.id === frameId);

    if (frameIndex === -1) {
        return nodes;
    }

    const memberBoxes = collectGraphMemberBoxes(graphId, nodes);
    const frameSize = computeGraphFrameSize(memberBoxes, direction);
    const frameNode = nodes[frameIndex];
    const frameData = frameNode.data as NodeDataType;

    if (frameData.graphFrame?.height === frameSize.height && frameData.graphFrame?.width === frameSize.width) {
        return nodes;
    }

    const resizedNodes = [...nodes];

    resizedNodes[frameIndex] = {
        ...frameNode,
        data: {...frameNode.data, graphFrame: {graphId, height: frameSize.height, width: frameSize.width}},
        height: frameSize.height,
        width: frameSize.width,
    };

    return resizedNodes;
}
