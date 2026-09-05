import {WorkflowTestNodeStateI} from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE} from '@/shared/constants';
import {type Edge, type Node} from '@xyflow/react';

import {toConditionCaseKey} from './collectTakenEmptyConditionCases';

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

export interface DisabledNodeBypassI {
    disabledTaskNames: Set<string>;
    edges: Edge[];
    nodes: Node[];
}

function isDisabledTaskNode(node: Node, disabledTaskNames: Set<string>): boolean {
    const data = node.data as {disabled?: boolean; workflowNodeName?: string} | undefined;

    if (!data?.workflowNodeName) {
        return false;
    }

    return Boolean(data.disabled) || disabledTaskNames.has(data.workflowNodeName);
}

function skipDisabledNodes(
    node: Node | undefined,
    direction: 'downstream' | 'upstream',
    {disabledTaskNames, edges, nodes}: DisabledNodeBypassI
): Node | undefined {
    const visitedNodeIds = new Set<string>();

    let currentNode = node;

    while (currentNode && isDisabledTaskNode(currentNode, disabledTaskNames) && !visitedNodeIds.has(currentNode.id)) {
        const currentNodeId = currentNode.id;

        visitedNodeIds.add(currentNodeId);

        const adjacentEdges = edges.filter((edge) =>
            direction === 'downstream' ? edge.source === currentNodeId : edge.target === currentNodeId
        );

        if (adjacentEdges.length !== 1) {
            return currentNode;
        }

        const adjacentNodeId = direction === 'downstream' ? adjacentEdges[0].target : adjacentEdges[0].source;

        const adjacentNode = nodes.find((candidateNode) => candidateNode.id === adjacentNodeId);

        if (!adjacentNode) {
            return currentNode;
        }

        currentNode = adjacentNode;
    }

    return currentNode;
}

/**
 * A disabled task is skipped at run time, so it never gets a test state and every edge touching it would drop out of
 * the executed path -- a single disabled step left a gray gap in the middle of a red or green trail. The run did pass
 * through that spot, so each endpoint that is a disabled task is replaced by the nearest enabled node beyond it: the
 * source walks upstream, the target downstream. Both edges around a disabled node then resolve to the same enabled
 * pair and color alike.
 *
 * The walk only follows a single edge. A disabled node with several edges on that side has no one path through it,
 * so it is kept as-is and the edge stays neutral.
 */
export function bypassDisabledEdgeEndpoints(
    sourceNode: Node | undefined,
    targetNode: Node | undefined,
    disabledNodeBypass: DisabledNodeBypassI
): {sourceNode: Node | undefined; targetNode: Node | undefined} {
    return {
        sourceNode: skipDisabledNodes(sourceNode, 'upstream', disabledNodeBypass),
        targetNode: skipDisabledNodes(targetNode, 'downstream', disabledNodeBypass),
    };
}

/**
 * An edge is part of the executed path when BOTH endpoints resolve to nodes that ran. Requiring both ends keeps
 * the untaken side of a condition gray: its bottom-ghost resolves to the (executed) dispatcher, but the untaken
 * branch child never ran, so child->ghost edges stay neutral.
 *
 * "Ran" means COMPLETED **or** FAILED on the source side, not COMPLETED alone. A task dispatcher whose nested
 * child fails is itself reported FAILED, and its ghost nodes borrow that status, so demanding COMPLETED there
 * broke the trail at the dispatcher's own boundary: `condition_1 -> top ghost -> condition_2` went gray between
 * two executed nodes. Widening the source cannot light an untaken branch, because the target still has to have
 * run.
 *
 * An edge into a FAILED node is COMPLETED, not FAILED: the data reached that node fine, and it was the node that
 * failed. Red belongs to the node alone, so the green trail runs right up to the step that broke.
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

/** A "+" placeholder standing in for a condition's empty case. */
export function isConditionPlaceholderNode(node: Node | undefined): boolean {
    return node?.type === 'placeholder' && Boolean((node.data as {conditionId?: string} | undefined)?.conditionId);
}

function isTakenEmptyConditionCase(placeholderNode: Node | undefined, takenEmptyConditionCases?: Set<string>): boolean {
    const data = placeholderNode?.data as {conditionCase?: string; conditionId?: string} | undefined;

    if (!data?.conditionId || !data.conditionCase || !takenEmptyConditionCases) {
        return false;
    }

    return takenEmptyConditionCases.has(toConditionCaseKey(data.conditionId, data.conditionCase));
}

function hasOnlyEmptyConditionCases(node: Node | undefined): boolean {
    const parameters = (node?.data as {parameters?: {[key: string]: unknown}} | undefined)?.parameters;

    if (!parameters || !(CONDITION_CASE_TRUE in parameters || CONDITION_CASE_FALSE in parameters)) {
        return false;
    }

    return [parameters[CONDITION_CASE_TRUE], parameters[CONDITION_CASE_FALSE]].every(
        (caseTasks) => !Array.isArray(caseTasks) || caseTasks.length === 0
    );
}

export default function getExecutedEdgeStatus(
    sourceNode: Node | undefined,
    targetNode: Node | undefined,
    workflowTestNodeStates: Record<string, WorkflowTestNodeStateI>,
    takenEmptyConditionCases?: Set<string>
): 'COMPLETED' | 'FAILED' | undefined {
    // A "+" placeholder is an insertion point, not a step: nothing runs there and no data passes through it. It
    // carries its dispatcher's id as `taskDispatcherId`, so without this the edges around a parallel's or
    // fork-join's add-a-branch slot would borrow the dispatcher's status and paint a path the run never took.
    //
    // The exception is a condition's empty case: a condition takes exactly one case per visit, so an empty case it
    // took is a path the run did follow. `takenEmptyConditionCases` says which ones it took.
    if (sourceNode?.type === 'placeholder' || targetNode?.type === 'placeholder') {
        const placeholderNode = sourceNode?.type === 'placeholder' ? sourceNode : targetNode;

        return isTakenEmptyConditionCase(placeholderNode, takenEmptyConditionCases) ? 'COMPLETED' : undefined;
    }

    // A condition whose two cases are both empty never records which one it took (see
    // `collectTakenEmptyConditionCases`), so neither half of its bar can light up. Coloring the stem into that bar
    // anyway would draw a trail that leads nowhere.
    if (targetNode?.type === 'taskDispatcherTopGhostNode' && hasOnlyEmptyConditionCases(sourceNode)) {
        return undefined;
    }

    const sourceStatus = resolveNodeStatus(sourceNode, workflowTestNodeStates);

    // A bottom ghost is a join, and the status it borrows is the dispatcher's aggregate: one failed branch makes
    // it FAILED for every branch. What an edge into the join shows is whether ITS branch got there, which only the
    // source can say -- a completed branch joined, a failed one stopped short of the join.
    if (targetNode?.type === 'taskDispatcherBottomGhostNode') {
        return sourceStatus === 'COMPLETED' ? 'COMPLETED' : undefined;
    }

    if (sourceStatus !== 'COMPLETED' && sourceStatus !== 'FAILED') {
        return undefined;
    }

    const targetStatus = resolveNodeStatus(targetNode, workflowTestNodeStates);

    if (targetStatus === 'COMPLETED' || targetStatus === 'FAILED') {
        return 'COMPLETED';
    }

    return undefined;
}
