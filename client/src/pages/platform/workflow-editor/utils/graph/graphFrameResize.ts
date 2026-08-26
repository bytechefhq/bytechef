import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';

import {computeGraphFrameSize, getGraphFrameId} from './graphFrameGeometry';
import {collectGraphMemberBoxes} from './graphMemberPlacement';

/**
 * Re-sizes one graph's frame node from where its contents currently sit. `layoutGraphFrames` owns
 * the authoritative size, but it only runs once a drag has ended and the definition has changed —
 * this is what makes the box grow and shrink under the cursor in between.
 *
 * The box is measured over every frame child the graph owns, which is the member tasks AND their
 * subtrees: that is the same pixel union `layoutGraphFrames` sizes the frame from (it unions each
 * member GROUP's bounding box). Measuring the member nodes alone would let the frame collapse away
 * from a dispatcher member's subtree the moment it is picked up, then snap back on drop. The union
 * comes from `collectGraphMemberBoxes`, the same per-member measurement auto-arrange lays out with
 * — unioning per member first and over the members after leaves the frame box unchanged.
 *
 * Returns the array it was given, unchanged and by reference, when there is no such frame or the
 * frame already carries the computed size — so a caller can skip a needless store write.
 */
export function resizeGraphFrameForMembers(nodes: Node[], graphId: string): Node[] {
    const frameId = getGraphFrameId(graphId);
    const frameIndex = nodes.findIndex((node) => node.id === frameId);

    if (frameIndex === -1) {
        return nodes;
    }

    const frameSize = computeGraphFrameSize(collectGraphMemberBoxes(graphId, nodes));
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
