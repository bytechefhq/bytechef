import {WorkflowTestNodeStateI} from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {type Node} from '@xyflow/react';

/**
 * Resolves the workflow-node name a canvas node contributes to test-run node states. Structural ghost nodes
 * (task dispatcher top/bottom ghosts, case ghosts) carry their owning dispatcher's id as `taskDispatcherId`,
 * so the dispatcher's executed state flows through its plumbing edges.
 */
export function resolveTestStateNodeName(node?: Node): string | undefined {
    if (!node) {
        return undefined;
    }

    const data = node.data as {taskDispatcherId?: string; workflowNodeName?: string} | undefined;

    return data?.workflowNodeName ?? data?.taskDispatcherId ?? node.id;
}

/**
 * An edge is part of the executed path when BOTH endpoints resolve to nodes that ran. Requiring both ends keeps
 * the untaken side of a condition gray: its bottom-ghost resolves to the (executed) dispatcher, but the untaken
 * branch child never ran, so child->ghost edges stay neutral.
 *
 * Returns 'FAILED' when the traversal reached a failed node so the incoming edge can match the node's red state.
 */
function resolveNodeStatus(
    node: Node | undefined,
    workflowTestNodeStates: Record<string, WorkflowTestNodeStateI>
): WorkflowTestNodeStateI['status'] | undefined {
    const name = resolveTestStateNodeName(node);

    if (!name) {
        return undefined;
    }

    const nodeState = workflowTestNodeStates[name];

    if (nodeState) {
        return nodeState.status;
    }

    // Triggers never produce a task execution of their own, but if anything ran at all the trigger
    // necessarily fired — treat it as completed so the trigger's outgoing edge joins the executed path.
    if ((node?.data as {trigger?: boolean} | undefined)?.trigger && Object.keys(workflowTestNodeStates).length > 0) {
        return 'COMPLETED';
    }

    return undefined;
}

export default function getExecutedEdgeStatus(
    sourceNode: Node | undefined,
    targetNode: Node | undefined,
    workflowTestNodeStates: Record<string, WorkflowTestNodeStateI>
): 'COMPLETED' | 'FAILED' | undefined {
    const sourceStatus = resolveNodeStatus(sourceNode, workflowTestNodeStates);

    if (sourceStatus !== 'COMPLETED') {
        return undefined;
    }

    const targetStatus = resolveNodeStatus(targetNode, workflowTestNodeStates);

    if (targetStatus === 'COMPLETED' || targetStatus === 'FAILED') {
        return targetStatus;
    }

    return undefined;
}
