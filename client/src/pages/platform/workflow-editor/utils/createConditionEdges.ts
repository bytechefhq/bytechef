import {EDGE_STYLES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

import {createBranchExitEdges, createSubtaskChainEdges} from './taskDispatcherSubtaskEdges';

/**
 * Creates placeholder edges for an empty condition branch (top ghost -> placeholder -> bottom ghost).
 */
function createPlaceholderEdges(conditionId: string, branchSide: 'left' | 'right'): Edge[] {
    const topGhostNodeId = `${conditionId}-condition-top-ghost`;
    const bottomGhostNodeId = `${conditionId}-condition-bottom-ghost`;
    const placeholderNodeId = `${conditionId}-condition-${branchSide}-placeholder-0`;

    const baseEdge = {
        style: EDGE_STYLES,
        type: 'smoothstep',
    };

    return [
        {
            id: `${topGhostNodeId}=>${placeholderNodeId}`,
            source: topGhostNodeId,
            sourceHandle: `${topGhostNodeId}-${branchSide}`,
            target: placeholderNodeId,
            ...baseEdge,
        },
        {
            id: `${placeholderNodeId}=>${bottomGhostNodeId}`,
            source: placeholderNodeId,
            target: bottomGhostNodeId,
            targetHandle: `${bottomGhostNodeId}-${branchSide}`,
            ...baseEdge,
        },
    ];
}

/**
 * Creates all edges for a specific branch
 */
function createBranchEdges(
    conditionId: string,
    branchTasks: WorkflowTask[],
    branchSide: 'left' | 'right',
    allNodes: Node[]
): Edge[] {
    const edges: Edge[] = [];

    const edgesFromConditionToFirstTask = createBranchStartEdge(conditionId, branchTasks, branchSide, allNodes);

    edges.push(...edgesFromConditionToFirstTask);

    if (branchTasks.length > 1) {
        const edgeBetweenTaskNodes = createSubtaskChainEdges(branchTasks, 'bottom');

        edges.push(...edgeBetweenTaskNodes);
    }

    const edgeFromLastTaskNodeToBottomGhost = createBranchExitEdges(
        `${conditionId}-condition-bottom-ghost`,
        branchTasks,
        branchSide,
        allNodes
    )[0];

    if (!edgeFromLastTaskNodeToBottomGhost) {
        return edges;
    }

    if (
        edgeFromLastTaskNodeToBottomGhost.source === conditionId ||
        edgeFromLastTaskNodeToBottomGhost.target.includes(conditionId)
    ) {
        edges.push(edgeFromLastTaskNodeToBottomGhost);
    }

    return edges;
}

/**
 * Create edge from condition to first node in a branch
 */
function createBranchStartEdge(
    conditionId: string,
    branchSubtasks: WorkflowTask[],
    conditionCase: 'left' | 'right',
    allNodes: Node[]
): Edge[] {
    const topGhostNodeId = `${conditionId}-condition-top-ghost`;
    const firstSubtaskId = branchSubtasks[0].name;
    const firstTaskNode = allNodes.find((node) => node.id === firstSubtaskId);

    if (!firstTaskNode) {
        return [];
    }

    return [
        {
            id: `${topGhostNodeId}=>${firstSubtaskId}`,
            source: topGhostNodeId,
            sourceHandle: `${topGhostNodeId}-${conditionCase}`,
            style: EDGE_STYLES,
            target: firstSubtaskId,
            type: 'workflow',
        },
    ];
}

/**
 * Determine which branch (left or right) a condition is in
 */
export function getConditionBranchSide(
    conditionId: string,
    tasks: WorkflowTask[],
    parentConditionId: string
): 'left' | 'right' {
    const parentConditionTask = tasks?.find((task) => task.name === parentConditionId);

    if (!parentConditionTask) {
        return 'right';
    }

    const inTrueBranch = Array.isArray(parentConditionTask.parameters?.caseTrue)
        ? parentConditionTask.parameters.caseTrue.some((task: WorkflowTask) => task.name === conditionId)
        : false;

    return inTrueBranch ? 'left' : 'right';
}

/**
 * Check if a task is in any branch of a condition
 */
export function hasTaskInConditionBranches(conditionId: string, taskId: string, tasks: WorkflowTask[]): boolean {
    const condition = tasks?.find((task) => task.name === conditionId);

    if (!condition || !condition.parameters) {
        return false;
    }

    const caseTrueTasks = Array.isArray(condition.parameters.caseTrue) ? condition.parameters.caseTrue : [];
    const caseFalseTasks = Array.isArray(condition.parameters.caseFalse) ? condition.parameters.caseFalse : [];
    const allBranchTasks = [...caseTrueTasks, ...caseFalseTasks];

    return allBranchTasks.some((task) => task.name === taskId);
}

/**
 * Creates all edges for a condition node and its branches.
 *
 * Edge insertion order matters: dagre (with disableOptimalOrderHeuristic)
 * uses the order edges are added to determine cross-axis (left/right)
 * positioning within a rank. Left-branch edges must always be inserted
 * before right-branch edges so dagre places the TRUE branch on the left
 * and the FALSE branch on the right.
 */
export default function createConditionEdges(conditionNode: Node, allNodes: Node[]): Edge[] {
    const edges: Edge[] = [];
    const conditionNodeData: NodeDataType = conditionNode.data as NodeDataType;
    const conditionId = conditionNode.id;
    const topGhostNodeId = `${conditionId}-condition-top-ghost`;

    const {parameters} = conditionNodeData;

    edges.push({
        id: `${conditionId}=>${topGhostNodeId}`,
        source: conditionId,
        style: EDGE_STYLES,
        target: topGhostNodeId,
        type: 'smoothstep',
    });

    const caseTrueSubtasks: WorkflowTask[] = Array.isArray(parameters?.caseTrue) ? parameters.caseTrue : [];
    const caseFalseSubtasks: WorkflowTask[] = Array.isArray(parameters?.caseFalse) ? parameters.caseFalse : [];

    if (caseTrueSubtasks.length > 0) {
        edges.push(...createBranchEdges(conditionId, caseTrueSubtasks, 'left', allNodes));
    } else {
        edges.push(...createPlaceholderEdges(conditionId, 'left'));
    }

    if (caseFalseSubtasks.length > 0) {
        edges.push(...createBranchEdges(conditionId, caseFalseSubtasks, 'right', allNodes));
    } else {
        edges.push(...createPlaceholderEdges(conditionId, 'right'));
    }

    return edges;
}
