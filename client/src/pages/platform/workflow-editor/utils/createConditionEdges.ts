import {EDGE_STYLES, TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

/**
 * Creates all edges for a specific branch
 */
function createBranchEdges(
    conditionNode: Node,
    branchTasks: WorkflowTask[],
    branchSide: 'left' | 'right',
    allNodes: Node[]
): Edge[] {
    const edges: Edge[] = [];

    const edgesFromConditionToFirstTask = createBranchStartEdge(conditionNode, branchTasks, branchSide, allNodes);

    edges.push(...edgesFromConditionToFirstTask);

    if (branchTasks.length > 1) {
        const edgeBetweenTaskNodes = connectSequentialTasks(branchTasks);

        edges.push(...edgeBetweenTaskNodes);
    }

    const edgeFromLastTaskNodeToBottomGhost = createBranchExitEdge(conditionNode, branchTasks, branchSide, allNodes)[0];

    if (!edgeFromLastTaskNodeToBottomGhost) {
        return edges;
    }

    if (
        edgeFromLastTaskNodeToBottomGhost.source === conditionNode.id ||
        edgeFromLastTaskNodeToBottomGhost.target.includes(conditionNode.id)
    ) {
        edges.push(edgeFromLastTaskNodeToBottomGhost);
    }

    return edges;
}

/**
 * Create edge from condition to first node in a branch
 */
function createBranchStartEdge(
    conditionNode: Node,
    branchTasks: WorkflowTask[],
    branchSide: 'left' | 'right',
    allNodes: Node[]
): Edge[] {
    const edges: Edge[] = [];
    const conditionId = conditionNode.id;

    if (branchTasks.length > 0) {
        const firstTaskName = branchTasks[0].name;

        const firstTaskNode = allNodes.find((node) => node.id === firstTaskName);

        if (firstTaskNode) {
            const edgeToFirstTask = {
                id: `${conditionId}=>${firstTaskName}`,
                source: conditionId,
                sourceHandle: `${conditionId}-${branchSide}-source-handle`,
                style: EDGE_STYLES,
                target: firstTaskName,
                type: 'workflow',
            };

            edges.push(edgeToFirstTask);
        }
    } else {
        const placeholderId = `${conditionId}-${branchSide}-placeholder-0`;

        const edgeToPlaceholder = {
            id: `${conditionId}=>${placeholderId}`,
            source: conditionId,
            sourceHandle: `${conditionId}-${branchSide}-source-handle`,
            style: EDGE_STYLES,
            target: placeholderId,
            type: 'smoothstep',
        };

        edges.push(edgeToPlaceholder);

        const bottomGhostId = `${conditionId}-condition-bottom-ghost`;

        const edgeFromPlaceholderToBottomGhost = {
            id: `${placeholderId}=>${bottomGhostId}`,
            source: placeholderId,
            style: EDGE_STYLES,
            target: bottomGhostId,
            targetHandle: `${bottomGhostId}-${branchSide}`,
            type: 'smoothstep',
        };

        edges.push(edgeFromPlaceholderToBottomGhost);
    }

    return edges;
}

/**
 * Create edges between tasks in a branch
 */
function connectSequentialTasks(branchTasks: WorkflowTask[]): Edge[] {
    const edges: Edge[] = [];

    branchTasks.forEach((task, index) => {
        if (index < branchTasks.length - 1) {
            const sourceTaskName = task.name;
            const targetTaskName = branchTasks[index + 1].name;

            const sourceTaskComponentName = task.name.split('_')[0];

            if (TASK_DISPATCHER_NAMES.includes(sourceTaskComponentName)) {
                const edgeFromNestedConditionBottomGhostToNextSubtask = {
                    id: `${sourceTaskName}-condition-bottom-ghost=>${targetTaskName}`,
                    source: `${sourceTaskName}-condition-bottom-ghost`,
                    sourceHandle: `${sourceTaskName}-condition-bottom-ghost-bottom`,
                    style: EDGE_STYLES,
                    target: targetTaskName,
                    type: 'workflow',
                };

                edges.push(edgeFromNestedConditionBottomGhostToNextSubtask);

                return;
            }

            const edgeBetweenTasks = {
                id: `${sourceTaskName}=>${targetTaskName}`,
                source: sourceTaskName,
                style: EDGE_STYLES,
                target: targetTaskName,
                type: 'workflow',
            };

            edges.push(edgeBetweenTasks);
        }
    });

    return edges;
}

/**
 * Create edge from last node in a branch to bottom ghost
 */
function createBranchExitEdge(
    conditionNode: Node,
    branchTasks: WorkflowTask[],
    branchSide: 'left' | 'right',
    allNodes: Node[]
): Edge[] {
    const edges: Edge[] = [];

    if (branchTasks.length > 0) {
        const lastTaskName = branchTasks[branchTasks.length - 1].name;
        const bottomGhostId = `${conditionNode.id}-condition-bottom-ghost`;
        const lastTaskNode = allNodes.find((node) => node.id === lastTaskName);

        if (lastTaskNode?.data.componentName === 'condition') {
            const nestedConditionBottomGhostId = `${lastTaskName}-condition-bottom-ghost`;

            const nestedGhostToParentGhostEdge = {
                id: `${nestedConditionBottomGhostId}=>${bottomGhostId}`,
                source: nestedConditionBottomGhostId,
                style: EDGE_STYLES,
                target: bottomGhostId,
                targetHandle: `${bottomGhostId}-${branchSide}`,
                type: 'workflow',
            };

            edges.push(nestedGhostToParentGhostEdge);

            return edges;
        }

        const edgeFromLastTaskToBottomGhost = {
            id: `${lastTaskName}=>${bottomGhostId}`,
            source: lastTaskName,
            style: EDGE_STYLES,
            target: bottomGhostId,
            targetHandle: `${bottomGhostId}-${branchSide}`,
            type: 'workflow',
        };

        edges.push(edgeFromLastTaskToBottomGhost);
    }

    return edges;
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

    const inTrueBranch = (parentConditionTask.parameters?.caseTrue || []).some(
        (task: WorkflowTask) => task.name === conditionId
    );

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

    const allBranchTasks = [...(condition.parameters.caseTrue || []), ...(condition.parameters.caseFalse || [])];

    return allBranchTasks.some((task) => task.name === taskId);
}

/**
 * Creates all edges for a condition node and its branches
 */
export default function createAllConditionEdges(conditionNode: Node, allNodes: Node[]): Edge[] {
    const edges: Edge[] = [];
    const conditionNodeData: NodeDataType = conditionNode.data as NodeDataType;

    const {conditionData, parameters} = conditionNodeData;

    const isNestedCondition = conditionData !== undefined;
    const parentConditionId = isNestedCondition ? conditionData?.conditionId : undefined;

    const bottomGhostId = `${conditionNode.id}-condition-bottom-ghost`;
    const bottomGhostNode = allNodes.find((node) => node.id === bottomGhostId);

    if (bottomGhostNode) {
        bottomGhostNode.data = {
            ...bottomGhostNode.data,
            conditionId: conditionNode.id,
            isNestedConditionBottomGhost: isNestedCondition,
            parentConditionId,
        };
    }

    const trueTasks: WorkflowTask[] = parameters?.caseTrue || [];
    const falseTasks: WorkflowTask[] = parameters?.caseFalse || [];

    const trueEdges = createBranchEdges(conditionNode, trueTasks, 'left', allNodes);
    const falseEdges = createBranchEdges(conditionNode, falseTasks, 'right', allNodes);

    edges.push(...trueEdges);
    edges.push(...falseEdges);

    return edges;
}
