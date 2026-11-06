import {type Edge, type Node} from '@xyflow/react';

import {type WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';
import getExecutedEdgeStatus, {bypassDisabledEdgeEndpoints} from './getExecutedEdgeStatus';

interface OrderExecutedTriggerFanInEdgesLastProps {
    disabledTaskNames: Set<string>;
    edges: Edge[];
    nodes: Node[];
    workflowTestNodeStates: Record<string, WorkflowTestNodeStateI>;
}

export default function orderExecutedTriggerFanInEdgesLast({
    disabledTaskNames,
    edges,
    nodes,
    workflowTestNodeStates,
}: OrderExecutedTriggerFanInEdgesLastProps): Edge[] {
    const executedFanInEdgeIds = new Set(
        edges
            .filter((edge) => (edge.data as Record<string, unknown> | undefined)?.triggerFanIn)
            .filter((edge) => {
                const sourceNode = nodes.find((node) => node.id === edge.source);
                const targetNode = nodes.find((node) => node.id === edge.target);

                const executedEdgeEndpoints = bypassDisabledEdgeEndpoints(sourceNode, targetNode, {
                    disabledTaskNames,
                    edges,
                    nodes,
                });

                return (
                    getExecutedEdgeStatus(
                        executedEdgeEndpoints.sourceNode,
                        executedEdgeEndpoints.targetNode,
                        workflowTestNodeStates
                    ) !== undefined
                );
            })
            .map((edge) => edge.id)
    );

    if (executedFanInEdgeIds.size === 0) {
        return edges;
    }

    return [
        ...edges.filter((edge) => !executedFanInEdgeIds.has(edge.id)),
        ...edges.filter((edge) => executedFanInEdgeIds.has(edge.id)),
    ];
}
