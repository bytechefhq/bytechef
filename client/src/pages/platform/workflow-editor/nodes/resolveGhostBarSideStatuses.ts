import {type Edge, type Node} from '@xyflow/react';

import getExecutedEdgeStatus from '../edges/getExecutedEdgeStatus';
import {WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';

type GhostBarStatusType = WorkflowTestNodeStateI['status'] | undefined;

export interface GhostBarSideStatusesI {
    leftStatus: GhostBarStatusType;
    rightStatus: GhostBarStatusType;
}

interface ResolveGhostBarSideStatusesProps {
    edges: Edge[];
    fallbackStatus: GhostBarStatusType;
    ghostNodeId: string;
    isBottomGhost: boolean;
    nodes: Node[];
    workflowTestNodeStates: Record<string, WorkflowTestNodeStateI>;
}

/** Resolves the executed status of each half of a task dispatcher ghost bar. */
export default function resolveGhostBarSideStatuses({
    edges,
    fallbackStatus,
    ghostNodeId,
    isBottomGhost,
    nodes,
    workflowTestNodeStates,
}: ResolveGhostBarSideStatusesProps): GhostBarSideStatusesI {
    const nodesById = new Map(nodes.map((node) => [node.id, node]));

    const resolveSideStatus = (side: 'left' | 'right'): GhostBarStatusType => {
        const handleId = `${ghostNodeId}-${side}`;

        const sideEdges = edges.filter((edge) =>
            isBottomGhost
                ? edge.target === ghostNodeId && edge.targetHandle === handleId
                : edge.source === ghostNodeId && edge.sourceHandle === handleId
        );

        if (sideEdges.length === 0) {
            return fallbackStatus;
        }

        let sideStatus: GhostBarStatusType;

        for (const sideEdge of sideEdges) {
            const edgeStatus = getExecutedEdgeStatus(
                nodesById.get(sideEdge.source),
                nodesById.get(sideEdge.target),
                workflowTestNodeStates
            );

            if (edgeStatus === 'FAILED') {
                return 'FAILED';
            }

            if (edgeStatus === 'COMPLETED') {
                sideStatus = 'COMPLETED';
            }
        }

        return sideStatus;
    };

    return {
        leftStatus: resolveSideStatus('left'),
        rightStatus: resolveSideStatus('right'),
    };
}
