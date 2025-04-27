import {EDGE_STYLES} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

export default function createLoopBreakEdges(loopBreakNode: Node, nodes: Node[]): Edge[] {
    const edges: Edge[] = [];
    const loopBreakNodeData: NodeDataType = loopBreakNode.data as NodeDataType;

    const parentLoopId = loopBreakNodeData.loopData?.loopId;
    const parentLoopNode = nodes.find((node) => node.id === parentLoopId);

    const nextLoopSubtaskNode = nodes.find((node) => {
        const nodeData = node.data as NodeDataType;

        if (
            !nodeData.loopData ||
            nodeData.loopData.loopId !== parentLoopId ||
            loopBreakNodeData.loopData?.index === undefined
        ) {
            return false;
        }

        return nodeData.loopData.index === loopBreakNodeData.loopData?.index + 1;
    });

    if (nextLoopSubtaskNode) {
        const edgeFromLoopBreakToNextSubtask = {
            id: `${loopBreakNode.id}=>${nextLoopSubtaskNode.id}`,
            source: loopBreakNode.id,
            style: EDGE_STYLES,
            target: nextLoopSubtaskNode.id,
            type: 'workflow',
        };

        edges.push(edgeFromLoopBreakToNextSubtask);

        return edges;
    } else {
        const loopBottomGhostNodeId = `${parentLoopNode?.id}-loop-bottom-ghost`;

        const edgeFromLoopBreakToBottomGhost = {
            id: `${loopBreakNode.id}=>${loopBottomGhostNodeId}`,
            source: loopBreakNode.id,
            style: EDGE_STYLES,
            target: loopBottomGhostNodeId,
            targetHandle: `${loopBottomGhostNodeId}-right`,
            type: 'workflow',
        };

        edges.push(edgeFromLoopBreakToBottomGhost);
    }

    return edges;
}
