import {type Edge, type Node} from '@xyflow/react';

import getExecutedEdgeStatus, {bypassDisabledEdgeEndpoints} from '../edges/getExecutedEdgeStatus';
import {WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';

type GhostBarStatusType = WorkflowTestNodeStateI['status'] | undefined;

export interface GhostBarSideStatusesI {
    leftStatus: GhostBarStatusType;
    rightStatus: GhostBarStatusType;
}

interface ResolveGhostBarSideStatusesProps {
    disabledTaskNames?: Set<string>;
    edges: Edge[];
    fallbackStatus: GhostBarStatusType;
    ghostNodeId: string;
    isBottomGhost: boolean;
    nodes: Node[];
    takenEmptyConditionCases?: Set<string>;
    workflowTestNodeStates: Record<string, WorkflowTestNodeStateI>;
}

/** Resolves the executed status of each half of a task dispatcher ghost bar. */
export default function resolveGhostBarSideStatuses({
    disabledTaskNames = new Set(),
    edges,
    fallbackStatus,
    ghostNodeId,
    isBottomGhost,
    nodes,
    takenEmptyConditionCases,
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

        for (const sideEdge of sideEdges) {
            const edgeEndpoints = bypassDisabledEdgeEndpoints(
                nodesById.get(sideEdge.source),
                nodesById.get(sideEdge.target),
                {disabledTaskNames, edges, nodes}
            );

            const edgeStatus = getExecutedEdgeStatus(
                edgeEndpoints.sourceNode,
                edgeEndpoints.targetNode,
                workflowTestNodeStates,
                takenEmptyConditionCases
            );

            if (edgeStatus === 'COMPLETED') {
                return 'COMPLETED';
            }
        }

        return undefined;
    };

    return {
        leftStatus: resolveSideStatus('left'),
        rightStatus: resolveSideStatus('right'),
    };
}
